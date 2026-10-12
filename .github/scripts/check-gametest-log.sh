#!/usr/bin/env bash
# Verifies that a mc-runtime-test client run actually executed the gametests of the clientgametest mod.
# mc-runtime-test exits with 0 in some failure cases (error screens, no tests found),
# so we check the log in addition to the exit code.
#
# Usage: check-gametest-log.sh <path/to/latest.log>
set -uo pipefail

log="${1:-run/logs/latest.log}"
marker="Hi from dummy GameTest!"
failed=0

error() {
  if [ -n "${GITHUB_ACTIONS:-}" ]; then
    echo "::error::$1"
  else
    echo "ERROR: $1" >&2
  fi
  failed=1
}

if [ ! -f "$log" ]; then
  error "Log $log not found."
  exit 1
fi

# Helps to tell a hang on a screen (#120) apart from a hang while waiting for tests (#121).
echo "Last state: $(grep -oE "(Waiting for player to load|Waiting for GameTest|Screen not yet null|Running game tests).*" "$log" | tail -n 1)"

if grep -n "RuntimeException: Error Screen" "$log"; then
  error "An error screen was shown, so no game tests were run."
fi

# https://github.com/headlesshq/mc-runtime-test/issues/119
if grep -n "No tests found" "$log"; then
  error "mc-runtime-test did not find any gametests (#119)."
fi

if grep -n "Failed to find the minimum amount of gametests" "$log"; then
  error "mc-runtime-test found less gametests than expected (#119)."
fi

if grep -n "Test failed: " "$log"; then
  error "A gametest failed."
fi

if ! grep -q "$marker" "$log"; then
  error "The clientgametest gametest never ran (expected '$marker' in the log)."
  echo "Last lines of $log:"
  tail -n 15 "$log"
fi

if ! grep -q "Successfully finished" "$log"; then
  error "The runtime client did not finish successfully (#120/#121: stuck on a screen or waiting for tests)."
fi

if [ "$failed" -eq 0 ]; then
  echo "Gametests ran and passed."
fi

exit "$failed"
