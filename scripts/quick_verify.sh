#!/bin/bash

# Quick verification script
echo "Paxos Quick Verification"
echo "========================"

# Check Java version
echo ""
echo "Java version:"
java -version 2>&1 | head -1

# Kill any existing
pkill -f "paxos.CouncilMember" 2>/dev/null
sleep 1

# Clean logs
rm -rf logs
mkdir -p logs

echo ""
echo "Starting 5 members..."

# Start members
for i in {1..5}; do
    java -cp build paxos.CouncilMember M$i --profile reliable > logs/M$i.log 2>&1 &
    PIDS[$i]=$!
    echo "M$i started (PID: ${PIDS[$i]})"
done

echo ""
echo "Waiting for initialization..."
sleep 3

echo "Initiating proposal: M1 proposes M5..."
(echo "M5"; sleep 5) | java -cp build paxos.CouncilMember M1 --profile reliable > logs/proposal.log 2>&1 &
PROPOSAL_PID=$!

echo "Waiting for consensus..."
sleep 7

echo ""
echo "===== RESULTS ====="
if grep -q "CONSENSUS:" logs/*.log 2>/dev/null; then
    echo "✓ SUCCESS - Consensus reached!"
    grep "CONSENSUS:" logs/*.log | head -1
else
    echo "✗ FAILED - No consensus reached"
    echo ""
    echo "Debugging info:"
    echo "Number of log files: $(ls logs/*.log 2>/dev/null | wc -l)"
    echo ""
    echo "Sample from M1.log:"
    head -20 logs/M1.log 2>/dev/null
fi

echo ""
echo "Cleaning up..."
for pid in ${PIDS[@]}; do
    kill $pid 2>/dev/null
done
kill $PROPOSAL_PID 2>/dev/null
pkill -f "paxos.CouncilMember" 2>/dev/null

echo ""
echo "Done! Check logs/ directory for detailed output."