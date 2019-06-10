#!/bin/bash

# Get environment variables
ECS_PORT=${CLIENT_SERVICE_PORT:-8080}
CAT_URL=${CATALOG_URL:-LOCAL}
SANITIZE=${SANITIZE_EXCEPTIONS:-true}

TRACER=${TRACE_RECORDER:-harbour}
DISABLE_TELEM=${DISABLE_TELEMETRY:-false}

# Communicate Settings Locally
if [ ${DEBUG:-false} == true ]; then
  echo "Running Eva-Client-Service with the following configuration:"
  echo "CLIENT_SERVICE_PORT - $ECS_PORT"
  echo "CATALOG_URL - $CAT_URL"
  echo "TRACE_RECORDER - $TRACER"
fi

# TODO - Spring configurations can make this a lot simpler
# Construct Command Args
CMD_ARGS="--server.port=$ECS_PORT
--eva.catalog=$CAT_URL
--clientservice.sanitizeEva=$SANITIZE"

# Set Java Memory Constraints
source /usr/local/bin/set-mem-constraints.sh

## Support for enabling the YourKit Profiling Agent
yourkit_options=""
YOURKIT_AGENT_PATH="${YOURKIT_AGENT_PATH:-/opt/yourkit-agent/linux-x86-64/libyjpagent.so}"
YOURKIT_AGENT_PORT="${YOURKIT_AGENT_PORT:-10001}"
if [ "${YOURKIT_AGENT_ENABLE:-false}" == true ]; then
  if [ -e "${YOURKIT_AGENT_PATH}" ]; then
    yourkit_options="-agentpath:${YOURKIT_AGENT_PATH}=port=${YOURKIT_AGENT_PORT}"
    echo "Enabling YourKit Profiling Agent"
  else
    echo "WARNING: Cannot enable YourKit Profiling Agent!"
    echo "         YourKit Agent lib does not exist at: ${YOURKIT_AGENT_PATH}"
  fi
fi

# Run
echo "Spinning Up Eva-Client-Service"
echo "YourKit Options - $yourkit_options"
java -server $JAVA_OPTS $yourkit_options -jar /opt/eva-client-service.jar -tracer=$TRACER -disableTelemetry=$DISABLE_TELEM $CMD_ARGS
