# Paxos Consensus Algorithm

A fault-tolerant implementation of the Paxos distributed consensus algorithm in Java, simulating a council election across 9 concurrent members with configurable network profiles. Developed as part of a third-year Distributed Systems course at the University of Adelaide.

## Overview

Nine council members (M1–M9) communicate over TCP/IP sockets to elect a council president. The system reaches consensus on a single winner even under high network latency, concurrent proposals, and member failures — demonstrating the core guarantees of the Paxos algorithm.

```
M1 (Proposer) ──PREPARE──▶ M2, M3, M4 ... M9 (Acceptors)
              ◀──PROMISE──
              ──ACCEPT──▶
              ◀──ACCEPTED──
                           ▼
                     CONSENSUS: M1 elected
```

## Features

- Full Paxos implementation: Prepare → Promise → Accept → Accepted → Consensus
- Each member acts as Proposer, Acceptor, and Learner
- Unique monotonically increasing proposal numbers (`counter.memberID`)
- Configurable network profiles per member at runtime
- Consensus reached when a majority (5 of 9) of acceptors agree
- Automated test script covering all three scenarios
- TCP/IP socket communication with a config file for port mapping

## Network Profiles

| Profile | Behaviour |
|---|---|
| `reliable` | Responds instantly |
| `latent` | High variable delay, may miss messages |
| `failure` | May crash or become permanently unresponsive |
| `standard` | Moderate variable delay |

## Build & Run

**Compile:**
```bash
javac *.java
```

**Launch members with profiles:**
```bash
java CouncilMember M1 --profile reliable
java CouncilMember M2 --profile latent
java CouncilMember M3 --profile failure
java CouncilMember M4 --profile standard
# ... M5-M9
```

**Run automated test suite:**
```bash
bash run_tests.sh
```

## Test Scenarios

**Scenario 1 — Ideal Network:** All members reliable, single proposal. Consensus reached quickly.

**Scenario 2 — Concurrent Proposals:** M1 and M8 propose simultaneously. Paxos resolves the conflict to a single winner.

**Scenario 3 — Fault Tolerance:** Mixed profiles with M3 failing mid-proposal. Remaining members reach consensus without stalling.

## Technical Details

- **Language:** Java
- **Communication:** TCP/IP sockets, port config via `network.config`
- **Message format:** Text-based `TYPE:SENDER_ID:PROPOSAL_NUM:PROPOSAL_VAL`
- **Message types:** `PREPARE`, `PROMISE`, `ACCEPT_REQUEST`, `ACCEPTED`
- **Failure simulation:** `Thread.sleep()` for latency, random non-response for failure
- **Majority:** 5 of 9 members required for consensus
