#!/bin/bash

# generate-image-tag.sh
# Generates The image tags used to push images to the LFH connect image repository

if [[ -z "${IMAGE_REPO_NAME}" || -z "${GITHUB_EVENT_NAME}" || -z "${GITHUB_REF}" ]]; then
  echo "Environment is not set."
  echo "Expected IMAGE_REPO_NAME, GITHUB_EVENT_NAME, and GITHUB_REF to be set"
  exit 1
fi

echo "Image Repo Name: ${IMAGE_REPO_NAME}"
echo "GitHub Event Name: ${GITHUB_EVENT_NAME}"
echo "GitHub Ref: ${GITHUB_REF}"