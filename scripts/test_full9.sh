#!/usr/bin/env bash
# test_full9.sh — start 9 nodes and reach consensus
set -e
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
if [[ -d "$SCRIPT_DIR/../src/main/java" ]]; then
  PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
fi
cd "$PROJECT_ROOT"

./scripts/clean.sh

echo "🚀 Starting full cluster (M1..M9)"
rm -rf logs && mkdir -p logs

# Start M1 with proposal from the beginning
java -cp build paxos.CouncilMember M1 --profile reliable --propose M5 > logs/M1.log 2>&1 &
sleep 0.3

for i in 2 3 4 5 6 7 8 9; do
  java -cp build paxos.CouncilMember M$i --profile reliable > logs/M$i.log 2>&1 &
done

# Allow time for prepare/promise/accept/accepted
sleep 8

echo "----- RESULTS -----"
if grep -q "Consensus reached" logs/*.log 2>/dev/null; then
  echo "✅ Consensus reached:"
  grep -H "Consensus reached" logs/*.log | head -1
  exit 0
else
  echo "❌ No consensus found"
  echo "Tail M1:"; tail -n 30 logs/M1.log || true
  exit 2
fi
