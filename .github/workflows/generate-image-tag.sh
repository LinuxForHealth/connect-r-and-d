#!/bin/bash
# (C) Copyright IBM Corp. 2020
# SPDX-License-Identifier: Apache-2.0

# generate-image-tag.sh
# Generates The image tags used to push images to the LFH connect image repository.
# The "push" event creates a "snapshot" tag, while the "release" event creates "snapshot", "latest", and a <version tag>.
#
# The following variables are available in the execution environment:
# - IMAGE_REPO_NAME: The target image repository
# - GITHUB_EVENT_NAME: The name of the triggering GitHub event
# - GITHUB_REFS: The branch or tag ref which triggered the workflow run

set -o errexit
set -o nounset
set -o pipefail

TAGS="${IMAGE_REPO_NAME}:snapshot"

if [[ -z "${IMAGE_REPO_NAME}" || -z "${GITHUB_EVENT_NAME}" || -z "${GITHUB_REF}" ]]; then
  echo "Environment is not set."
  echo "Expected IMAGE_REPO_NAME, GITHUB_EVENT_NAME, and GITHUB_REF to be set"
  exit 1
fi

if [[ "${GITHUB_EVENT_NAME}" == "release" ]]; then
  TAGS="${TAGS},${IMAGE_REPO_NAME}:latest,${IMAGE_REPO_NAME}:${GITHUB_REF#refs/tags/}"
fi

echo "${TAGS}"
