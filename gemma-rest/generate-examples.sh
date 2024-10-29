#!/bin/sh

set -e

project_dir=$(dirname "$0")

# hostname to use for generating files
gemma_host=${GEMMA_HOST-gemma.msl.ubc.ca}

# credentials if force-regenerating the files
if [ -n "$GEMMA_USERNAME" ]; then
  if [ -n "$GEMMA_PASSWORD" ]; then
    gemma_credentials="$GEMMA_USERNAME:$GEMMA_PASSWORD"
  elif [ -n "$GEMMA_PASSWORD_CMD" ]; then
    gemma_credentials="$GEMMA_USERNAME:$($GEMMA_PASSWORD_CMD)"
  else
    # curl will prompt for the password
    gemma_credentials="$GEMMA_USERNAME"
  fi
else
  echo "GEMMA_USERNAME is not set."
  exit 1
fi
# set to true to force file regeneration, require admin credentials
force=false

echo "Updating REST API docs examples from $gemma_host..."

curl -u "${gemma_credentials}" --compressed "https://${gemma_host}/rest/v2/datasets/1/data?force=${force}" | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/dataset-data.tsv &
curl -u "${gemma_credentials}" --compressed "https://${gemma_host}/rest/v2/datasets/1/data/processed?force=${force}" | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/dataset-processed-data.tsv &
curl -u "${gemma_credentials}" --compressed "https://${gemma_host}/rest/v2/datasets/1/data/raw?force=${force} | head" -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/dataset-processed-data.tsv &
curl -u "${gemma_credentials}" --compressed "https://${gemma_host}/rest/v2/datasets/1/design?force=${force}" | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/dataset-design.tsv &
curl -u "${gemma_credentials}" --compressed -H Accept:text/tab-separated-values "https://${gemma_host}/rest/v2/datasets/GSE199762/data/singleCell?force=${force}" | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/dataset-single-cell-data.tsv &
curl -u "${gemma_credentials}" --compressed "https://${gemma_host}/rest/v2/platforms/2/annotations?force=${force}" | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/platform-annotations.tsv &
curl -u "${gemma_credentials}" --compressed "https://${gemma_host}/rest/v2/resultSets/425093" -H Accept:text/tab-separated-values | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/result-set.tsv &

wait