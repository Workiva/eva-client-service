#!/bin/bash
if [ -z "$GIT_BRANCH" ]; then
	echo "GIT_BRANCH environment variable not set, skipping codecov push"
else
	TRACKING_REMOTE="$(git for-each-ref --format='%(upstream:short)' $(git symbolic-ref -q HEAD) | cut -d'/' -f1 | xargs git ls-remote --get-url | cut -d':' -f2 | sed 's/.git$//')"
	REPO_NAME=${TRACKING_REMOTE##*/}
	CODECOV_COMPAT_REMOTE="Workiva/$REPO_NAME"
	GIT_BRANCH=${GIT_BRANCH##*/}
	bash <(curl -s https://codecov.workiva.net/bash) -u https://codecov.workiva.net -B $GIT_BRANCH -r $CODECOV_COMPAT_REMOTE || echo "ERROR: Codecov failed to upload reports."
fi
