#!/bin/sh

set -e

#
# Environment variables:
# MAVEN:               Executable to use for Maven commands (defaults to mvn).
# GEMMA_APPDATA_HOME:  Directory where Gemma data is stored on the deployment server. This will be used to create
#                      symbolic links to specific Maven websites that are then accessible at https://gemma.msl.ubc.ca/resources/.
#

MAVEN=mvn

# TODO: check for local deployment
if "$($MAVEN help:evaluate -Dexpression=local-deploy -q -DforceStdout)" ]; then
  deploy_server=
else
  deploy_server=$($MAVEN help:evaluate -Dexpression=pavlab.server -q -DforceStdout)
fi
maven_site_dir=$($MAVEN help:evaluate -Dexpression=pavlab.siteDir -q -DforceStdout)
gemma_version=$($MAVEN help:evaluate -Dexpression=project.version -q -DforceStdout)
basecode_version=$($MAVEN help:evaluate -Dartifact=baseCode:baseCode -Dexpression=project.version -q -DforceStdout)

if [ -n "$deploy_server" ]; then
  echo "Deploying Maven site to $deploy_server:$maven_site_dir..."
else
  echo "Deploying Maven site to $maven_site_dir..."
fi
mvn -B site-deploy -DskipWebpack
([ -n "$deploy_server" ] && ssh "$deploy_server" || eval)  <EOF
  # maven site-deploy changes permission on the parent site directory (pavlab-starter-parent),
  # but the Gemma site directory is not a subdirectory of it.
  # This is replicating what the plugin is doing https://maven.apache.org/plugins/maven-site-plugin/deploy-mojo.html
  chmod -Rf g+w,a+rX "$maven_site_dir/gemma/gemma-$gemma_version" || true

  # create symbolic links
  if [ -n "$GEMMA_APPDATA_HOME" ]; then
    echo "Creating symbolic links for Gemma and baseCode in $GEMMA_APPDATA_HOME..."
    mkdir -p "$GEMMA_APPDATA_HOME"
    ln -Tsf "$maven_site_dir/gemma/gemma-$gemma_version" "$GEMMA_APPDATA_HOME/gemma-devsite"
    ln -Tsf "$maven_site_dir/baseCode/baseCode-$basecode_version" "$GEMMA_APPDATA_HOME/baseCode-site"
  fi
EOF
