#!/bin/sh

#
# Deploy the Gemma REST API
#
# Usage: ./deploy.sh <ref>
#
# The <ref> argument is a Tomcat application name into which the REST API is
# deployed.
#
# Environment variables:
# GEMMA_REST_DEPLOY_SERVER: The server to deploy the REST API to.
# GEMMA_REST_PREFIX:        The prefix for the deployment directory (defaults to /var/local/tomcat). Note that the
#                           actual installation is done under $GEMMA_REST_PREFIX/$ref.
#
# Replaces gemma-web/deploy.sh, removed along with the gemma-web module in
# bb154eee88. gemma-web's WAR consumed gemma-rest as a compile-scope jar, so
# deploying it shipped the browser UI and /rest/v2/* in one artifact; this
# deploys the REST API on its own.
#
# It targets the same directories gemma-web did, so the Tomcat instance
# configuration already in place continues to apply. Two consequences:
#
#   - Any Gemma.war left there by the last gemma-web deploy is NOT removed.
#     Remove it by hand once you are satisfied the REST API is serving.
#   - gemma-rest ships no log4j2.xml of its own (gemma-web had
#     src/main/config/log4j2.xml and rsynced it into lib/), so whatever
#     logging configuration is already on the server is left untouched.
#
# The WAR only exists under the `gemma-rest-war` Maven profile -- a plain
# `mvn package` produces a JAR and this script will not find anything to
# deploy. Build it with:
#
#     mvn -P gemma-rest-war package -pl gemma-rest -am -DskipTests
#

set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <ref>"
  exit 1
fi

project_dir=$(dirname "$0")
gemma_rest_war="$project_dir"/target/gemma-rest.war

if [ -n "$(git -C "$project_dir" status --porcelain --untracked-files=no)" ]; then
  echo "The working directory is not clean. Please commit or stash your changes first."
  exit 1
fi

if [ -z "$GEMMA_REST_PREFIX" ]; then
  gemma_rest_prefix=/var/local/tomcat
else
  gemma_rest_prefix="$GEMMA_REST_PREFIX"
fi

ref=$1
deploy_server=$GEMMA_REST_DEPLOY_SERVER
deploy_dir="$gemma_rest_prefix/$ref"
if [ -z "$deploy_server" ]; then
  deploy_dest="$deploy_dir"
else
  deploy_dest="$deploy_server:$deploy_dir"
fi

if [ ! -f "$gemma_rest_war" ]; then
  echo "The Gemma REST WAR file does not exist. Please build the project first with the gemma-rest-war profile."
  exit 1
fi

echo "Deploying the Gemma REST API to $deploy_dest..."
rsync -v --chmod g+w --mkpath "$gemma_rest_war" "$deploy_dest/"
echo "Deployment completed!"
