#!/bin/sh

#
# Deploy Gemma Web
#
# Usage: ./deploy.sh <ref>
#
# The <ref> argument is a Tomcat application name into which Gemma is deployed.
#
# Environment variables:
# GEMMA_WEB_DEPLOY_SERVER: The server to deploy Gemma Web to.
# GEMMA_WEB_PREFIX:        The prefix for the Gemma Web deployment directory (defaults to /var/local/tomcat). Note that
#                          the actual installation is done under /var/local/tomcat/$ref.
#

set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <ref>"
  exit 1
fi

project_dir=$(dirname "$0")
gemma_web_log4j="$project_dir"/src/main/config/log4j2.xml
gemma_web_war="$project_dir"/target/Gemma.war

if [ -n "$(git -C "$project_dir" status --porcelain --untracked-files=no)" ]; then
  echo "The working directory is not clean. Please commit or stash your changes first."
  exit 1
fi

if [ -z "$GEMMA_WEB_PREFIX" ]; then
  gemma_web_prefix=/var/local/tomcat
else
  gemma_web_prefix="$GEMMA_WEB_PREFIX"
fi

ref=$1
deploy_server=$GEMMA_WEB_DEPLOY_SERVER
deploy_dir="$gemma_web_prefix/$ref"
if [ -z "$deploy_server" ]; then
  deploy_dest="$deploy_dir"
else
  deploy_dest="$deploy_server:$deploy_dir"
fi

if [ ! -f "$gemma_web_war" ]; then
  echo "The Gemma WAR file does not exist. Please build the project first."
  exit 1
fi

echo "Deploying Gemma to $deploy_dest..."
rsync -v --chmod g+w --mkpath "$gemma_web_log4j" "$deploy_dest/lib/"
rsync -v --chmod g+w --mkpath "$gemma_web_war" "$deploy_dest/"
echo "Deployment completed!"
