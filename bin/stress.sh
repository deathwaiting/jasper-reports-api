#!/usr/bin/env bash
#
# Runs the optional stress test: clean-installs the application (the web service under test),
# builds the stress jar (stress-test profile), boots it with java -jar, runs the Gatling
# simulation from the isolated stress-sim/ module, and ALWAYS tears the app down afterwards
# (success, failed Gatling assertions, or Ctrl-C) via the EXIT trap.
#
# The app is forked on the packaged fat jar's own classpath (the Spring Boot parent binds its
# repackage goal to the package phase; under the stress-test profile that jar also bundles the
# stress beans + Testcontainers), and the simulation runs in the standalone stress-sim module, so
# neither JVM sees the other's dependencies. The plain `clean install` first builds/installs the
# web service itself (the artifact under test); the `-Pstress-test package` then produces the jar
# the stress profile actually boots. The app allocates several GB under 1000 concurrent users —
# don't run this on a shared/small worker.
#
# Usage:
#   bin/stress.sh                     # default profile (1000 vusers, p99 < 3000 ms)
#   STRESS_TARGET_VUSERS=500 bin/stress.sh
#
# Overrides (env vars -> -Dstress.* system props for the app + simulation):
#   STRESS_PORT (default 18080)
#   STRESS_TARGET_VUSERS (default 1000)
#   STRESS_MAX_P99_MS (default 3000)
#   STRESS_RAMP_SECONDS (default 120)
#   STRESS_HOLD_SECONDS (default 120)
#   STRESS_DB_LATENCY_MS (default 0)   -> app JVM only; >=1 adds fake per-connection latency
#   STRESS_DATASET_SIZE (default 50000) -> app JVM only; employee rows seeded by LargeDatasetGenerator
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PORT="${STRESS_PORT:-18080}"
VUSERS="${STRESS_TARGET_VUSERS:-1000}"
MAX_P99="${STRESS_MAX_P99_MS:-3000}"
RAMP="${STRESS_RAMP_SECONDS:-120}"
HOLD="${STRESS_HOLD_SECONDS:-120}"
LATENCY="${STRESS_DB_LATENCY_MS:-0}"
DATASET="${STRESS_DATASET_SIZE:-50000}"

BASE_URL="http://127.0.0.1:${PORT}"
APP_PID=""

cleanup() {
  if [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null; then
    >&2 echo "== stopping the application (pid $APP_PID) =="
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

if command -v ss >/dev/null 2>&1 && ss -ltn | grep -q "[:.]${PORT} "; then
  >&2 echo "ERROR: port ${PORT} is already in use; a previous stress app may be orphaned (kill \$(cat target/stress-app.pid) or pkill -f jasper-reports-api)."
  exit 1
fi

echo "== clean-installing the application (the web service under test) =="
./mvnw -q clean install -DskipTests

echo "== building the stress jar (stress-test profile) =="
./mvnw -q -Pstress-test package -DskipTests

JAR="target/$(./mvnw -q help:evaluate -Dexpression=project.build.finalName -DforceStdout -Pstress-test 2>/dev/null).jar"
if [ ! -f "$JAR" ]; then
  >&2 echo "ERROR: expected jar not found: $JAR"
  exit 1
fi
echo "== starting the application: $JAR =="
java \
  -Dstress.db.latency.ms="${LATENCY}" \
  -Dstress.dataset.size="${DATASET}" \
  -jar "$JAR" \
  --server.port="${PORT}" \
  --spring.profiles.active=stress-test \
  > target/stress-app.log 2>&1 &
APP_PID=$!
echo "$APP_PID" > target/stress-app.pid

echo "== waiting for the application on ${BASE_URL} =="
READY=""
for _ in $(seq 1 120); do
  if ! kill -0 "$APP_PID" 2>/dev/null; then
    >&2 echo "ERROR: the application exited during startup; see target/stress-app.log"
    exit 1
  fi
  if curl -fsS -u user:pass -o /dev/null "${BASE_URL}/report/emp-report.pdf" 2>/dev/null; then
    READY=1
    break
  fi
  sleep 1
done
if [ -z "$READY" ]; then
  >&2 echo "ERROR: the application did not become ready within 120s; see target/stress-app.log"
  exit 1
fi

echo "== running the Gatling simulation (stress-sim module) =="
./mvnw -f stress-sim/pom.xml gatling:test \
  -Dstress.base.url="${BASE_URL}" \
  -Dstress.max.p99.ms="${MAX_P99}" \
  -Dstress.target.vusers="${VUSERS}" \
  -Dstress.ramp.duration.seconds="${RAMP}" \
  -Dstress.hold.duration.seconds="${HOLD}"

echo "== stress test finished; reports under stress-sim/target/gatling/ =="