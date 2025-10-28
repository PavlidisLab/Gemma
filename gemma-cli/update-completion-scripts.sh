#!/bin/sh

set -e

if [ -z "$MAVEN" ]; then
  mvn=mvn
else
  echo "Using Maven from $MAVEN"
  mvn=$MAVEN
fi
project_dir=$(dirname "$0")
gemma_cli_bin="$project_dir"/target/appassembler/bin/gemma-cli

if [ -z "$GEMMA_CLI_ALIAS" ]; then
  gemma_cli_alias=gemma-cli
else
  echo "Using alias $GEMMA_CLI_ALIAS for gemma-cli"
  gemma_cli_alias=$GEMMA_CLI_ALIAS
fi

if [ ! -f "$gemma_cli_bin"  ]; then
  echo "The $gemma_cli_bin executable does not exist. Building..."
  # make sure to clean, or else classes from CLIs build from other branches
  # might get mixed up
  "$mvn" clean package -f "$project_dir/../pom.xml" -am -pl gemma-cli -DskipTests
fi

gemma_cli_version=$("$gemma_cli_bin" --version)
actual_hash=$(echo "$gemma_cli_version" | rev | cut -d ' ' -f 1 | rev)
expected_hash=$(git rev-parse HEAD)
if [ "$actual_hash" != "$expected_hash" ]; then
  echo "The build hash of $gemma_cli_bin, $actual_hash, does not match the current HEAD: $expected_hash. Rebuilding..."
  "$mvn" clean package -f "$project_dir/../pom.xml" -am -pl gemma-cli -DskipTests
  gemma_cli_version=$("$gemma_cli_bin" --version)
fi

echo "Generating completion scripts for $gemma_cli_version..."

"$gemma_cli_bin" --verbosity warn --completion --completion-executable="$gemma_cli_alias" --completion-shell=bash > "$project_dir"/src/main/config/bash_completion.d/"$gemma_cli_alias" &
"$gemma_cli_bin" --verbosity warn --completion --completion-executable="$gemma_cli_alias" --completion-shell=fish > "$project_dir"/src/main/config/fish/completions/"$gemma_cli_alias".fish &

wait
