#!/bin/bash
# Updates table of contents in all relevant readmes
# Requires markdown-toc
MAX_DEPTH=4
MAX_DEPTH_API_DOCS=3
MAX_DEPTH_EXAMPLES=2

# Parent README
npx markdown-toc -i README.md --maxdepth $MAX_DEPTH

# Version 1 Endpoints
pushd docs/v1
find . -name '*.md' -a ! -name 'README.md' \
  -exec npx markdown-toc -i "{}" --maxdepth $MAX_DEPTH_API_DOCS \;
popd

# Examples
pushd docs/v1/examples
find . -name '*.md' -a ! -name 'README.md' \
  -exec npx markdown-toc -i "{}" --maxdepth $MAX_DEPTH_EXAMPLES \;
popd
