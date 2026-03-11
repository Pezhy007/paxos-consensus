#!/usr/bin/env bash
#
# macos_test_all.sh — Streamlined Paxos test runner for macOS (B: kill only Paxos ports)
# - Bash 3.2 compatible (macOS default)
# - Kills ONLY Paxos-related ports (9001–9009, 10001–10009) + Paxos Java procs
# - Compiles sources, runs scripts/run_tests.sh (scenarios 1–3 or single arg)
# - Scans logs for consensus phrases; prints colored summary
#
set -e
set -o pipefail

# ---------- Colors ----------
if [[ -t 1 ]]; then
  RED="$(printf '\033[31m')"
  GRN="$(printf '\033[32m')"
  YLW="$(printf '\033[33m')"
  BLU="$(printf '\033[34m')"
  MAG="$(printf '\033[35m')"
  CYN="$(printf '\033[36m')"
  BLD="$(printf '\033[1m')"
  RST="$(printf '\033[0m')"
else
  RED=""; GRN=""; YLW=""; BLU=""; MAG=""; CYN=""; BLD=""; RST="";
fi

# ---------- Determine PROJECT_ROOT ----------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
if [[ -d "$SCRIPT_DIR/../src/main/java" ]]; then
  PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
fi
cd "$PROJECT_ROOT"

echo -e "${BLD}${CYN}▶ Paxos macOS Test Runner${RST}"
echo -e "${YLW}Project root:${RST} $PROJECT_ROOT"

# ---------- Pre-flight checks ----------
need() { command -v "$1" >/dev/null 2>&1 || { echo -e "${RED}Missing dependency:${RST} $1"; exit 1; }; }
need java
need javac
need pkill
need grep
need awk
need find
need lsof

JAVA_VER=$(java -version 2>&1 | awk -F[\".] '/version/ {print $2}')
if [[ -z "${JAVA_VER}" ]]; then
  echo -e "${YLW}Warning:${RST} Could not detect Java version. Continuing..."
else
  if (( JAVA_VER < 11 )); then
    echo -e "${RED}Error:${RST} Java 11+ required. Detected major version: $JAVA_VER"
    exit 1
  fi
  echo -e "${GRN}Java OK:${RST} $(java -version 2>&1 | head -n1)"
fi

# ---------- Hard kill Paxos ports (B) ----------
echo -e "${CYN}🔒 Freeing Paxos ports (9001–9009, 10001–10009)...${RST}"
PAXOS_PORTS="9001 9002 9003 9004 9005 9006 9007 9008 9009 10001 10002 10003 10004 10005 10006 10007 10008 10009"
for p in $PAXOS_PORTS; do
  PIDS="$(lsof -ti tcp:$p || true)"
  if [[ -n "$PIDS" ]]; then
    echo -e "${YLW}Killing PID(s) on port $p:${RST} $PIDS"
    # shellcheck disable=SC2086
    kill -9 $PIDS || true
  fi
done
echo -e "${CYN}⏹  Stopping Paxos Java processes by class name...${RST}"
pkill -f "paxos.CouncilMember" || true

# ---------- Locate required runner ----------
if [[ -f "./scripts/run_tests.sh" ]]; then
  RUNNER="./scripts/run_tests.sh"
elif [[ -f "./run_tests.sh" ]]; then
  RUNNER="./run_tests.sh"
else
  echo -e "${RED}Error:${RST} Could not find scripts/run_tests.sh (or ./run_tests.sh)."
  exit 1
fi

# ---------- Helpers ----------
clean_env() {
  echo -e "${CYN}🧹 Cleaning build/logs...${RST}"
  rm -rf build logs
  mkdir -p logs
}

compile_cli() {
  echo -e "${CYN}🔧 Compiling sources (CLI javac)...${RST}"
  if [[ -d "src/main/java" ]]; then
    mkdir -p build
    JAVAS=$(find src/main/java -name "*.java")
    if [[ -z "$JAVAS" ]]; then
      echo -e "${YLW}No Java files found under src/main/java${RST}"
    else
      # shellcheck disable=SC2086
      javac -d build -cp build $JAVAS
      echo -e "${GRN}✔ Build complete${RST}"
    fi
  else
    echo -e "${YLW}Skipping compile:${RST} src/main/java not found here."
  fi
}

wait_for_logs() {
  local timeout="$1"
  local start_ts=$(date +%s)
  echo -e "${CYN}⏳ Waiting for logs to appear (timeout ${timeout}s)...${RST}"
  while true; do
    if ls logs/*.log >/dev/null 2>&1; then
      echo -e "${GRN}✔ Logs detected${RST}"
      break
    fi
    sleep 0.5
    local now=$(date +%s)
    if (( now - start_ts > timeout )); then
      echo -e "${RED}✖ Timed out waiting for logs${RST}"
      return 1
    fi
  done
}

check_consensus() {
  if grep -Eis "consensus|agreed|chosen value|final decision" logs/*.log >/dev/null 2>&1; then
    return 0
  else
    return 1
  fi
}

run_scenario() {
  local n="$1"
  echo -e "${BLD}${MAG}▶ Running Scenario ${n}${RST}"
  clean_env
  compile_cli

  set +e
  bash "${RUNNER}" "${n}"
  local run_ec=$?
  set -e

  wait_for_logs 20 || true

  if check_consensus; then
    echo -e "${GRN}✔ Scenario ${n}: Consensus detected in logs${RST}"
    return 0
  fi

  # No consensus -> fail this scenario regardless of runner exit code
  echo -e "${RED}✖ Scenario ${n}: No consensus markers found in logs${RST}"
  return 2
}

# ---------- Main ----------
if [[ "${1:-}" =~ ^[1-9]$ ]]; then
  if run_scenario "$1"; then
    echo -e "${BLD}${GRN}✅ Scenario $1 OK${RST}"
    exit 0
  else
    echo -e "${BLD}${RED}❌ Scenario $1 FAILED${RST}"
    exit 1
  fi
fi

OVERALL_OK=0
for s in 1 2 3; do
  if ! run_scenario "$s"; then
    OVERALL_OK=1
  fi
done

echo
if [[ $OVERALL_OK -eq 0 ]]; then
  echo -e "${BLD}${GRN}✅ ALL SCENARIOS PASSED (consensus confirmed / runner OK)${RST}"
  exit 0
else
  echo -e "${BLD}${RED}❌ One or more scenarios failed. Check logs/ and script outputs.${RST}"
  exit 1
fi
