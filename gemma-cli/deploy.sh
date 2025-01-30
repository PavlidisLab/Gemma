#!/bin/sh

#
# Deploy the Gemma CLI
#

set -e

if [[ -z "$1" || -z "$2" ]]; then
  echo "Usage: $0 <gemma_cli_dir> <build_ref>"
  exit 1
fi

deploy_server=frink.pavlab.msl.ubc.ca
gemma_cli_dir=$1
build_ref=$2

gemma_cli_bin="$gemma_cli_dir"/bin/gemma-cli
gemma_cli_version=$("$gemma_cli_bin" --version)
gemma_cli_hash=$(echo "$gemma_cli_version" | rev | cut -d ' ' -f 1 | rev)

build_dir="/space/opt/gemma-cli/builds/$gemma_cli_hash"
build_ref_dir="/space/opt/gemma-cli/refs/$build_ref"

if ssh "$deploy_server" test -e "$build_dir"; then
	echo "There is already a build deployed at $build_dir."
	exit 1
fi

echo "Copying $gemma_cli_dir to $deploy_server:$build_dir..."
rsync -av --link-dest "$build_ref_dir" "$gemma_cli_dir/" "$deploy_server:$build_dir/"

echo "Creating a symlink from $build_dir to $build_ref_dir..."
ssh "$deploy_server" ln -sTf "$build_dir" "$build_ref_dir"

echo "Deployment completed!"