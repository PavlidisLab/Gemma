#!/bin/sh

#
# Deploy the Gemma CLI Wiki
#
# Usage ./deploy-wiki.sh <dest>
#
# The <dest> argument must be a valid rclone destination.
#
# Environment variables:
# GEMMA_CLI_WIKI_PAGE_SUFFIX:  Suffix to append to each generated wiki page (defaults to " (generated)").
# GEMMA_CLI_WIKI_DEPLOY_TOKEN: Token to use for deploying
#

set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <dest>"
  exit 1
fi

project_dir=$(dirname "$0")
gemma_cli_dir="$project_dir"/target/appassembler
gemma_cli_wiki_dir="$project_dir"/target/wiki
if [ -z "$GEMMA_CLI_WIKI_PAGE_SUFFIX" ]; then
  gemma_cli_wiki_page_suffix=" (generated)"
else
  gemma_cli_wiki_page_suffix="$GEMMA_CLI_WIKI_PAGE_SUFFIX"
fi
# shellcheck disable=SC2236
if [ -n "$GEMMA_CLI_WIKI_DEPLOY_TOKEN" ]; then
  echo "Using provided deployment token."
  export RCLONE_WEBDAV_BEARER_TOKEN="$GEMMA_CLI_WIKI_DEPLOY_TOKEN"
fi
wiki_dest="$1"

if [ -n "$(git -C "$project_dir" status --porcelain --untracked-files=no)" ]; then
  echo "The working directory is not clean. Please commit or stash your changes first."
  exit 1
fi

if [ ! -d "$gemma_cli_dir" ]; then
  echo "The Gemma CLI directory does not exist. Please build the project first."
  exit 1
fi

echo "Generating Gemma CLI Wiki pages under $gemma_cli_wiki_dir..."
./gemma-cli/target/appassembler/bin/gemma-cli --completion --completion-wiki --completion-wiki-output-dir "$gemma_cli_wiki_dir" --completion-wiki-page-suffix "$gemma_cli_wiki_page_suffix"

# Deploy each page separately, or else sync will delete everything else
echo "Deploying Gemma CLI Wiki to $wiki_dest..."
rclone sync "$gemma_cli_wiki_dir/List of Gemma CLI Tools$gemma_cli_wiki_page_suffix/" "$wiki_dest" --exclude '@*/**' --exclude '*.url'

echo "Deployment completed!"
