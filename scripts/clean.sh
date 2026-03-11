#!/usr/bin/env bash
# clean.sh — Fresh start for Paxos project (kill ports, stop nodes, wipe logs/build, rebuild)
# macOS-friendly (Bash 3.2). Safe to run before any test.
set -e
set -o pipefail

# ---------- Colors (optional) ----------
if [[ -t 1 ]]; then
  RED="$(printf '\033[31m')"; GRN="$(printf '\033[32m')"; YLW="$(printf '\033[33m')"
  CYN="$(printf '\033[36m')"; BLD="$(printf '\033[1m')"; RST="$(printf '\033[0m')"
else
  RED=""; GRN=""; YLW=""; CYN=""; BLD=""; RST=""
fi

# ---------- Project root ----------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
if [[ -d "$SCRIPT_DIR/../src/main/java" ]]; then
  PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
fi
cd "$PROJECT_ROOT"

echo -e "${BLD}${CYN}🧹 Fresh start: cleaning ports, processes, logs, and build...${RST}"

# ---------- Dependencies (best-effort) ----------
need() { command -v "$1" >/dev/null 2>&1 || { echo -e "${YLW}Warning:${RST} $1 not found"; }; }
need lsof
need pkill
need javac

# ---------- Kill Paxos ports & processes ----------
echo -e "${CYN}🔒 Freeing Paxos ports (9001–9009, 10001–10009)...${RST}"
PAXOS_PORTS="9001 9002 9003 9004 9005 9006 9007 9008 9009 10001 10002 10003 10004 10005 10006 10007 10008 10009"
for p in $PAXOS_PORTS; do
  PIDS="$(lsof -ti tcp:$p 2>/dev/null || true)"
  if [[ -n "$PIDS" ]]; then
    echo -e "${YLW}Killing PID(s) on port $p:${RST} $PIDS"
    # shellcheck disable=SC2086
    kill -9 $PIDS 2>/dev/null || true
  fi
done

echo -e "${CYN}⏹  Stopping Paxos Java processes...${RST}"
pkill -f "paxos.CouncilMember" 2>/dev/null || true

# ---------- Wipe logs & build ----------
echo -e "${CYN}🗑️  Removing build/ and logs/...${RST}"
rm -rf build logs
mkdir -p logs

# ---------- Rebuild ----------
if [[ -d "src/main/java" ]]; then
  echo -e "${CYN}🔧 Rebuilding sources...${RST}"
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
  echo -e "${YLW}src/main/java not found — skipping compile.${RST}"
fi

echo -e "${BLD}${GRN}✅ Clean slate ready.${RST}"
