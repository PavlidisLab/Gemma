#!/bin/sh

project_dir=$(dirname "$0")

http get https://gemma.msl.ubc.ca/rest/v2/datasets/1/data | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/dataset-data.tsv &
http get https://gemma.msl.ubc.ca/rest/v2/datasets/1/data/processed | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/dataset-processed-data.tsv &
http get https://gemma.msl.ubc.ca/rest/v2/datasets/1/data/raw | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/dataset-processed-data.tsv &
http get https://gemma.msl.ubc.ca/rest/v2/platforms/2/annotations | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/platform-annotations.tsv &
http get https://gemma.msl.ubc.ca/rest/v2/resultSets/425093 Accept:text/tab-separated-values | head -n 20 > "$project_dir"/src/main/resources/restapidocs/examples/result-set.tsv &

wait