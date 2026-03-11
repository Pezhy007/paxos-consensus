# GUIDE.md — PaxosProject Scripts Reference

This document explains how to use each script located in the `/scripts` directory.

---

## 🧰 Script Summary

| Script | Description | Command Example |
|--------|--------------|----------------|
| **clean.sh** | Cleans ports, stops all running Paxos members, removes `build/` and `logs/`, and recompiles all sources. | `./scripts/clean.sh` |
| **macos_test_all.sh** | Master runner that compiles, resets environment, and executes all assignment scenarios sequentially (1–3). | `./scripts/macos_test_all.sh` |
| **run_tests.sh** | Main testing harness implementing official Assignment 3 scenarios. Supports options: `1`, `2`, `3`, or `all`. | `./scripts/run_tests.sh all` |
| **test_full9.sh** | Quick verification: starts all 9 members (`reliable`), triggers one proposal (`M1 → M5`), confirms consensus. | `./scripts/test_full9.sh` |
| **quick_verify.sh** | Lightweight compilation and smoke test; ensures build success and basic messaging works. | `./scripts/quick_verify.sh` |
| **setup_and_run.sh** | Legacy setup helper used for configuration or pre-test initialization. | `./scripts/setup_and_run.sh` |
| **config/** | Contains `network.config`, mapping `M1–M9` to `localhost` and unique ports. | *(auto-read by CouncilMember)* |
| **logs/** | Stores runtime logs (`M1.log`–`M9.log`) for each scenario. | *(created automatically)* |

---

## ▶️ Typical Workflow

1. **Clean the environment**
   ```bash
   ./scripts/clean.sh
   ```

2. **Verify system works (optional)**
   ```bash
   ./scripts/test_full9.sh
   ```

3. **Run official scenarios (1–3)**
   ```bash
   ./scripts/run_tests.sh all
   ```

4. **Full automation**
   ```bash
   ./scripts/macos_test_all.sh
   ```

5. **Check consensus results**
   Logs should contain:
   ```
   CONSENSUS: M5 has been elected Council President!
   ```

---

## 💡 Tips

- Run `./scripts/clean.sh` whenever ports or logs conflict.
- To rerun a single scenario:
  ```bash
  ./scripts/run_tests.sh 2
  ```
- If network ports are busy (`Address already in use`), allow 2–3 seconds or rerun the clean script.
- Logs are evidence for grading — include them in your submission zip.

---

**Prepared by:** Pezhman Beheshtian 
**Student ID:** a1661736 
**Date:** 19 Oct 2025
