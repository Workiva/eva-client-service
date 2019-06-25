#!/bin/bash
# Verifies that docs have been generated and updated
RED='\033[0;31m'
NC='\033[0m' # No Color

md5Command=md5sum
if [ $(uname -s) == "Darwin" ]; then
  md5Command=(md5 -q)
fi

# Store MD5 of Original Files
MD5_ROOT_README_BEFORE=$(find README.md -type f -exec $md5Command {} \; | sort -k 2 | $md5Command)
MD5_DOCS_BEFORE=$(find ./docs -type f -exec $md5Command {} \; | sort -k 2 | $md5Command)

# Update Table of Contents
./.circleci/scripts/update-tocs.sh

# Calculate Later
MD5_ROOT_README_AFTER=$(find README.md -type f -exec $md5Command {} \; | sort -k 2 | $md5Command)
MD5_DOCS_AFTER=$(find ./docs -type f -exec $md5Command {} \; | sort -k 2 | $md5Command)

# Verify root README.md
if [ "$MD5_ROOT_README_BEFORE" != "$MD5_ROOT_README_AFTER" ]; then
  printf "${RED}Aborting, parent README file TOC was not updated${NC}\n"
  printf "${RED}Run ./.circleci/scripts/update-tocs.sh${NC}\n"
  exit 1
fi

# Verify ./docs content
if [ "$MD5_DOCS_BEFORE" != "$MD5_DOCS_AFTER" ]; then
  printf "${RED}Aborting, ./docs TOC(s) were not updated${NC}\n"
  printf "${RED}Run ./.circleci/scripts/update-tocs.sh${NC}\n"
  exit 1
fi

# Regenerate Postman Documentation
./scripts/ci/update-postman-docs.sh
./.circleci/scripts/update-tocs.sh
MD5_DOCS_AFTER=$(find ./docs -type f -exec $md5Command {} \; | sort -k 2 | $md5Command)

if [ "$MD5_DOCS_BEFORE" != "$MD5_DOCS_AFTER" ]; then
  printf "${RED}Aborting, ./docs postman documentation was not regenerated${NC}\n"
  printf "${RED}Run ./scripts/ci/update-postman-docs.sh${NC}\n"
  exit 1
fi
