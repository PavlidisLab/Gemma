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

# TODO: remove gemma-cli-sc once merged in development
gemma_cli_aliases="gemma-cli gemma-cli-staging gemma-cli-sc"

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

for a in $gemma_cli_aliases; do
  "$gemma_cli_bin" --verbosity warn --completion --completion-executable=$a --completion-shell=bash > "$project_dir"/src/main/config/bash_completion.d/$a &
  "$gemma_cli_bin" --verbosity warn --completion --completion-executable=$a --completion-shell=fish > "$project_dir"/src/main/config/fish/completions/$a.fish &
done

wait
