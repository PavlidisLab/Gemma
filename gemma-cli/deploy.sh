#!/bin/sh

#
# Deploy the Gemma CLI
#
# Usage: ./deploy <ref>
#
# The <ref> argument is a name under which the CLI is deployed. Note that this
# script is designed to re-use an existing deployment under that name to
# minimize the amount of data transferred with rsync.
#
# Environment variables:
# GEMMA_CLI_DEPLOY_SERVER: The server to deploy the Gemma CLI to.
# GEMMA_CLI_PREFIX:        The prefix for the Gemma CLI deployment directory (defaults to /space/opt/gemma-cli).
#

set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <ref>"
  exit 1
fi

project_dir=$(dirname "$0")
gemma_cli_dir="$project_dir"/target/appassembler

if [ -n "$(git -C "$project_dir" status --porcelain --untracked-files=no)" ]; then
  echo "The working directory is not clean. Please commit or stash your changes first."
  exit 1
fi

if [ ! -d "$gemma_cli_dir" ]; then
  echo "The Gemma CLI directory does not exist. Please build the project first."
  exit 1
fi

gemma_cli_bin="$gemma_cli_dir"/bin/gemma-cli
gemma_cli_version=$("$gemma_cli_bin" --version)
gemma_cli_hash=$(echo "$gemma_cli_version" | rev | cut -d ' ' -f 1 | rev)

ref=$1
deploy_server=$GEMMA_CLI_DEPLOY_SERVER
if [ -z "$GEMMA_CLI_PREFIX" ]; then
  gemma_cli_prefix=/space/opt/gemma-cli
else
  gemma_cli_prefix="$GEMMA_CLI_PREFIX"
fi
build_dir="$gemma_cli_prefix/builds/$gemma_cli_hash"
build_ref_dir="$gemma_cli_prefix/refs/$ref"

if [ -z "$deploy_server" ]; then
  build_dir_dest="$build_dir"
  build_ref_dest="$build_ref_dir"
else
  build_dir_dest="$deploy_server:$build_dir"
  build_ref_dest="$deploy_server:$build_ref_dir"
fi

echo "Deploying Gemma CLI to $build_dir_dest..."

if [ -z "$deploy_server" ]; then
  if test -e "$build_dir"; then
    echo "There is already a build deployed at $build_dir_dest."
    exit 0
  fi
else
  if ssh "$deploy_server" test -e "$build_dir" 2>/dev/null; then
    echo "There is already a build deployed at $build_dir_dest."
    exit 0
  fi
fi

echo "Copying $gemma_cli_dir to $build_dir_dest..."
rsync -av --chmod g+w --mkpath --link-dest "$build_ref_dir" "$gemma_cli_dir/" "$build_dir_dest/"

echo "Creating a symlink from $build_dir to $build_ref_dest..."
if [ -z "$deploy_server" ]; then
	mkdir -p "$gemma_cli_prefix/refs"
	ln -sTf "$build_dir" "$build_ref_dir"
else
	ssh "$deploy_server" mkdir -p "$gemma_cli_prefix/refs"
	ssh "$deploy_server" ln -sTf "$build_dir" "$build_ref_dir"
fi

echo "Deployment completed!"
