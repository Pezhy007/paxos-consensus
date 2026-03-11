# MANUAL_RUN.md — Manual Execution Procedure

This guide explains how to run and test the Paxos Consensus implementation **manually**, from compilation to consensus verification.  
It is designed for demonstration or testing without using the automation scripts.

---

## 1️⃣ Prepare Environment

Make sure you are inside the **project root folder** (e.g., `PaxosProject/`).

Clean any old builds (optional but recommended):
```bash
rm -rf build logs
mkdir -p build logs
```

---

## 2️⃣ Compile All Java Files

From the project root:
```bash
javac -d build -cp build $(find src/main/java -name "*.java")
```

This compiles every Java source file (CouncilMember, NetworkHandler, Proposer, Acceptor, Learner, etc.) and places the `.class` files in `build/`.

---

## 3️⃣ Start All Nine Members (Each in a Separate Terminal)

Each council member is a separate Java process.  
You can start them in separate terminals or background processes.

### Example commands:
```bash
# Terminal 1
java -cp build paxos.CouncilMember M1 --profile reliable

# Terminal 2
java -cp build paxos.CouncilMember M2 --profile reliable

# Terminal 3
java -cp build paxos.CouncilMember M3 --profile reliable

# ... continue for M4–M9
```

Each member should print something like:
```
Council Member M1 initialized with profile: reliable
Council Member M1 is listening for messages...
```

---

## 4️⃣ Trigger a Proposal

Once all 9 members are running, choose a proposer (e.g., M1).

In a new terminal:
```bash
java -cp build paxos.CouncilMember M1 --profile reliable --propose M5
```

This tells M1 to propose electing M5 as the Council President.  
All other members will receive Paxos messages (`PREPARE`, `PROMISE`, `ACCEPT_REQUEST`, `ACCEPTED`).

---

## 5️⃣ Observe the Logs or Console Output

Each active member should eventually display:
```
Consensus reached: M5
CONSENSUS: M5 has been elected Council President!
```

If you redirected output to files, view them with:
```bash
tail -n 20 logs/M1.log
```

---

## 6️⃣ Verify Consensus Manually

To confirm that all non-faulty members agreed on the same result:
```bash
grep "CONSENSUS" logs/*.log
```

All outputs should reference the same candidate (e.g., `M5`).

---

## 7️⃣ Stopping the Cluster

To terminate all Java processes:
```bash
pkill -f "paxos.CouncilMember"
```

If ports remain busy (9001–9009, 10001–10009), release them manually:
```bash
for p in {9001..9009} {10001..10009}; do
  lsof -ti tcp:$p | xargs kill -9 2>/dev/null || true
done
```

---

## 8️⃣ Testing Different Profiles

Each member can simulate different network conditions.

| Profile | Description |
|----------|--------------|
| **reliable** | Instant message handling, no delay. |
| **standard** | Moderate, variable latency. |
| **latent** | Adds random, longer delays to simulate poor network. |
| **failure** | Randomly drops or ignores messages and may terminate early. |

Example:
```bash
java -cp build paxos.CouncilMember M2 --profile latent
java -cp build paxos.CouncilMember M3 --profile failure
```

---

## 9️⃣ Optional Experiments

### Concurrent Proposals
Start two proposers simultaneously:
```bash
java -cp build paxos.CouncilMember M1 --profile reliable --propose M1 &
java -cp build paxos.CouncilMember M8 --profile reliable --propose M8 &
```
Expected: Paxos resolves the conflict and only one candidate is chosen.

### Failure Recovery
Start M3 with `--profile failure`, let it crash, then have another proposer (e.g., M1) issue a new proposal:
```bash
java -cp build paxos.CouncilMember M1 --profile reliable --propose M5
```
Consensus should still be achieved.

---

## ✅ Expected Outcome

All non-faulty members agree on a single, final decision:
```
CONSENSUS: M5 has been elected Council President!
```

---

**Prepared by:** Pezhman Beheshtian 
**Student ID:** a1661736
**Date:** 19 Oct 2025
