#!/bin/sh

project_dir=$(dirname "$0")

# hostname to use for generating files
gemma_host=gemma.msl.ubc.ca
# credentials if force-regenerating the files
gemma_credentials=
# set to true to force file regeneration, require admin credentials
force=false

curl -u "${gemma_credentials}" --compressed "https://${gemma_host}/rest/v2/datasets/1/data?force=${force}" | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/dataset-data.tsv &
curl -u "${gemma_credentials}" --compressed "https://${gemma_host}/rest/v2/datasets/1/data/processed?force=${force}" | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/dataset-processed-data.tsv &
curl -u "${gemma_credentials}" --compressed "https://${gemma_host}/rest/v2/datasets/1/data/raw?force=${force} | head" -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/dataset-processed-data.tsv &
curl -u "${gemma_credentials}" --compressed "https://${gemma_host}/rest/v2/datasets/GSE199762/data/singleCell?force=${force}" | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/dataset-single-cell-data.tsv &
curl -u "${gemma_credentials}" --compressed "https://${gemma_host}/rest/v2/platforms/2/annotations?force=${force}" | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/platform-annotations.tsv &
curl -u "${gemma_credentials}" --compressed "https://${gemma_host}/rest/v2/resultSets/425093" -H Accept:text/tab-separated-values | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/result-set.tsv &

wait