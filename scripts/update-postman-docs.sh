#!/bin/bash
# Verifies that docs have been generated and updated

# v1 Documentation
mkdir -p ./docs/v1/examples/

# Run Generator
node ./docs/postman/doc-generator/generator.js \
     ./docs/postman/v1/colls \
     ./docs/postman/v1/envs/EVA_Examples.postman_environment.json \
     ./docs/v1/examples

# Generate Table of Contents on Files
./scripts/update-tocs.sh
