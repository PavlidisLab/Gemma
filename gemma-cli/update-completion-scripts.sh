#!/bin/sh

set -e

project_dir=$(dirname "$0")
gemma_cli_bin="$project_dir"/target/appassembler/bin/gemma-cli

if [ ! -f "$gemma_cli_bin"  ]; then
  echo "The $gemma_cli_bin executable does not exist. Run 'mvn package' first."
  exit 1
fi

"$gemma_cli_bin" --verbosity warn --completion --completion-executable=gemma-cli --completion-shell=bash > "$project_dir"/src/main/config/bash_completion.d/gemma-cli &
"$gemma_cli_bin" --verbosity warn --completion --completion-executable=gemma-cli --completion-shell=fish > "$project_dir"/src/main/config/fish/completions/gemma-cli.fish &
"$gemma_cli_bin" --verbosity warn --completion --completion-executable=gemma-cli-staging --completion-shell=bash > "$project_dir"/src/main/config/bash_completion.d/gemma-cli-staging &
"$gemma_cli_bin" --verbosity warn --completion --completion-executable=gemma-cli-staging --completion-shell=fish > "$project_dir"/src/main/config/fish/completions/gemma-cli-staging.fish &

wait
