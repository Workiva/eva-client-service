#!/bin/bash

# https://stackoverflow.com/questions/3601515/how-to-check-if-a-variable-is-set-in-bash
if [[ -z ${SKIP_TESTS+x} || ${SKIP_TESTS} == "false" ]]; then
    mvn -B verify -q
else 
    echo "Skipping Tests!"
    mvn -B verify -q -DskipTests=true
fi
