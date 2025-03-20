#!/bin/sh

set -e

project_dir=$(dirname "$0")

# hostname to use for generating files
gemma_host=${GEMMA_HOST-https://gemma.msl.ubc.ca}
examples_dir="$project_dir"/src/main/resources/restapidocs/examples

# credentials if force-regenerating the files
if [ -n "$GEMMA_USERNAME" ]; then
  if [ -n "$GEMMA_PASSWORD" ]; then
    gemma_credentials="$GEMMA_USERNAME:$GEMMA_PASSWORD"
  elif [ -n "$GEMMA_PASSWORD_CMD" ]; then
    gemma_credentials="$GEMMA_USERNAME:$(eval "$GEMMA_PASSWORD_CMD" | head -n1)"
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

curl --show-error --silent -u "${gemma_credentials}" --compressed "${gemma_host}/rest/v2/datasets/1/data?force=${force}" | head -n 20 > "$examples_dir"/dataset-data.tsv
curl --show-error --silent -u "${gemma_credentials}" --compressed "${gemma_host}/rest/v2/datasets/1/data/processed?force=${force}" | head -n 20 > "$examples_dir"/dataset-processed-data.tsv
curl --show-error --silent -u "${gemma_credentials}" --compressed "${gemma_host}/rest/v2/datasets/1/data/raw?force=${force}" | head -n 20 > "$examples_dir"/dataset-processed-data.tsv
curl --show-error --silent -u "${gemma_credentials}" --compressed "${gemma_host}/rest/v2/datasets/1/design?force=${force}" | head -n 20 > "$examples_dir"/dataset-design.tsv
curl --show-error --silent -u "${gemma_credentials}" --compressed "${gemma_host}/rest/v2/datasets/GSE199762/data/singleCell?force=${force}" -H Accept:text/tab-separated-values | head -n 20 > "$examples_dir"/dataset-single-cell-data.tsv
curl --show-error --silent -u "${gemma_credentials}" --compressed "${gemma_host}/rest/v2/platforms/2/annotations?force=${force}" | head -n 20 > "$examples_dir"//platform-annotations.tsv
curl --show-error --silent -u "${gemma_credentials}" --compressed "${gemma_host}/rest/v2/resultSets/425093" -H Accept:text/tab-separated-values | head -n 20 > "$examples_dir"/result-set.tsv

curl --show-error -u "${gemma_credentials}" --compressed "${gemma_host}/rest/v2/datasets/GSE199762/singleCellDimension" -H Accept:text/tab-separated-values | head -n 20 > "$examples_dir"/dataset-single-cell-dimension.tsv
curl --show-error -u "${gemma_credentials}" --compressed "${gemma_host}/rest/v2/datasets/GSE199762/cellTypeAssignment" -H Accept:text/tab-separated-values | head -n 20 > "$examples_dir"/dataset-cell-type-assignment.tsv
curl --show-error -u "${gemma_credentials}" --compressed "${gemma_host}/rest/v2/datasets/GSE199762/cellLevelCharacteristics" -H Accept:text/tab-separated-values | head -n 20 > "$examples_dir"/dataset-cell-level-characteristics.tsv