#!/usr/bin/env bash
# run_tests.sh — Fixed: start proposers FIRST to avoid duplicate bind of Mx
# Scenarios: 1 (ideal), 2 (concurrent), 3 (fault tolerance: 3a/3b/3c)
set -e
set -o pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# --- helpers ---
clean() { ./scripts/clean.sh >/dev/null; }

start_member() {
  # $1 = M#, $2 = profile, $3.. = extra args (e.g., --propose M5)
  local mid="$1"; local prof="$2"; shift 2
  java -cp build paxos.CouncilMember "$mid" --profile "$prof" "$@" > "logs/${mid}.log" 2>&1 &
  echo $! > "logs/${mid}.pid"
}

start_others_excluding() {
  # $1 = common profile (or "mixed"), rest = excluded IDs
  local prof="$1"; shift
  local exclude="$*"
  for i in 1 2 3 4 5 6 7 8 9; do
    case " $exclude " in *" M$i "*) continue;; esac
    # mixed profiles for scenario 3
    if [[ "$prof" == "mixed" ]]; then
      case "M$i" in
        M1) start_member M1 reliable ;;
        M2) start_member M2 latent ;;
        M3) start_member M3 failure ;;
        *)  start_member "M$i" standard ;;
      esac
    else
      start_member "M$i" "$prof"
    fi
    sleep 0.05
  done
}

await_consensus() {
  # $1 = timeout seconds
  local timeout="${1:-14}"
  local start=$(date +%s)
  while true; do
    if grep -Eq "CONSENSUS:|Consensus reached" logs/*.log 2>/dev/null; then
      return 0
    fi
    sleep 0.2
    if (( $(date +%s) - start > timeout )); then
      return 1
    fi
  done
}

assert_single_winner() {
  local winners
  winners=$(grep -hE "CONSENSUS:|Consensus reached" logs/*.log 2>/dev/null \
    | sed -E 's/.*CONSENSUS: *//; s/ has been elected Council President!.*//; s/.*Consensus reached[: ]*//; s/[[:space:]]+$//' \
    | awk '{print $1}' | sort | uniq)
  local count=$(echo "$winners" | wc -w | tr -d ' ')
  if [[ "$count" -ne 1 ]]; then
    echo "❌ Multiple or zero distinct winners detected: [$winners]"
    return 1
  fi
  echo "Winner: $winners"
  return 0
}

print_sample() {
  echo "---- tail M1 ----"; tail -n 30 logs/M1.log || true
  echo "---- tail M4 ----"; tail -n 30 logs/M4.log || true
  echo "---- tail M8 ----"; tail -n 30 logs/M8.log || true
}

scenario1() {
  echo "=== Scenario 1: Ideal (all reliable); single proposal M4 -> M5 ==="
  clean
  # Start proposer FIRST to avoid duplicate M4
  start_member M4 reliable --propose M5
  sleep 0.3
  # Start the rest (all reliable), excluding M4
  start_others_excluding reliable M4
  # Wait and check
  if await_consensus 12; then
    echo "✅ Consensus reached (S1)"; grep -H "CONSENSUS:|Consensus reached" logs/*.log | head -5; assert_single_winner
  else
    echo "❌ No consensus (S1)"; print_sample; return 2
  fi
  return 0
}

scenario2() {
  echo "=== Scenario 2: Concurrent (all reliable); M1->M1 and M8->M8 ==="
  clean
  # Start both proposers FIRST
  start_member M1 reliable --propose M1
  start_member M8 reliable --propose M8
  sleep 0.3
  # Start the rest, excluding M1 and M8
  start_others_excluding reliable M1 M8
  if await_consensus 14; then
    echo "✅ Consensus reached (S2)"; grep -H "CONSENSUS:|Consensus reached" logs/*.log | head -10; assert_single_winner
  else
    echo "❌ No consensus (S2)"; print_sample; return 2
  fi
  return 0
}

scenario3() {
  echo "=== Scenario 3: Fault tolerance (mixed profiles) ==="
  # 3a
  echo "--- 3a: M4 (standard) proposes M5 ---"
  clean
  start_member M4 standard --propose M5
  sleep 0.3
  start_others_excluding mixed M4
  if await_consensus 16; then
    echo "✅ Consensus reached (3a)"; grep -H "CONSENSUS:|Consensus reached" logs/*.log | head -5
  else
    echo "❌ No consensus (3a)"; print_sample; return 2
  fi

  # 3b
  echo "--- 3b: M2 (latent) proposes M2 ---"
  clean
  start_member M2 latent --propose M2
  sleep 0.3
  start_others_excluding mixed M2
  if await_consensus 18; then
    echo "✅ Consensus reached (3b)"; grep -H "CONSENSUS:|Consensus reached" logs/*.log | head -5
  else
    echo "❌ No consensus (3b)"; print_sample; return 2
  fi

  # 3c
  echo "--- 3c: M3 (failure) proposes M3 then crashes; M1 takes over ---"
  clean
  start_member M3 failure --propose M3
  sleep 0.3
  start_others_excluding mixed M3
  # Let M3 send PREPARE then crash
  sleep 1.0
  if [[ -f logs/M3.pid ]]; then kill -9 "$(cat logs/M3.pid)" 2>/dev/null || true; fi
  # Take over
  start_member M1 reliable --propose M5
  if await_consensus 18; then
    echo "✅ Consensus reached (3c)"; grep -H "CONSENSUS:|Consensus reached" logs/*.log | head -5
  else
    echo "❌ No consensus (3c)"; print_sample; return 2
  fi
  return 0
}

# Dispatcher
S="${1:-all}"
case "$S" in
  1) scenario1 ;;
  2) scenario2 ;;
  3) scenario3 ;;
  all) scenario1; scenario2; scenario3;;
  *) echo "Usage: $0 [1|2|3|all]"; exit 1;;
esac
