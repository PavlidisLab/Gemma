MAVEN := env("MAVEN", "mvn")
MAVEN_ARGS := "-am -Prelease -DskipTests"

# This is the general deployment server
DEPLOY_SERVER := "frink.pavlab.msl.ubc.ca"
PRODUCTION_DEPLOY_SERVER := "moe.pavlab.msl.ubc.ca"
DEVELOPMENT_DEPLOY_SERVER := "chalmers.pavlab.msl.ubc.ca"

export GEMMA_CLI_DEPLOY_SERVER := DEPLOY_SERVER
export GEMMA_CLI_PREFIX := "/space/opt/gemma-cli"
export GEMMA_CLI_WIKI_PAGE_SUFFIX := " (generated)"
export GEMMA_CLI_WIKI_DEPLOY_DEST := f"pavlab-wiki:Global/gemma/Gemma Landing Page/Gemma Curation/List of Gemma CLI Tools{{GEMMA_CLI_WIKI_PAGE_SUFFIX}}/"

export GEMMA_WEB_DEPLOY_SERVER := DEVELOPMENT_DEPLOY_SERVER
export GEMMA_WEB_PREFIX := "/var/local/tomcat"

export GEMMA_APPDATA_DIR := "/space/gemmaData"

export MAVEN_SITES_DEPLOY_SERVER := DEPLOY_SERVER

default: build

build:
	{{MAVEN}} {{MAVEN_ARGS}} package

build-web:
	{{MAVEN}} {{MAVEN_ARGS}} package -pl gemma-web

generate-dwr-client:
    /scripts/generate-dwr-client.py

build-cli:
	{{MAVEN}} {{MAVEN_ARGS}} package -pl gemma-cli

update-completion-scripts: build-cli
	env GEMMA_CLI_ALIAS=gemma-cli ./gemma-cli/update-completion-scripts.sh
	env GEMMA_CLI_ALIAS=gemma-cli-staging ./gemma-cli/update-completion-scripts.sh

generate-wiki page_suffix=" (generated)": build-cli
	./gemma-cli/target/appassembler/bin/gemma-cli --completion --completion-wiki --completion-wiki-output-dir gemma-cli/target/wiki --completion-wiki-page-suffix "{{GEMMA_CLI_WIKI_PAGE_SUFFIX}}"

deploy-wiki page_suffix: (generate-wiki page_suffix)
	env GEMMA_CLI_WIKI_PAGE_SUFFIX="{{page_suffix}}" ./scripts/deploy-cli-wiki.sh "{{GEMMA_CLI_WIKI_DEPLOY_DEST}}"

[confirm('Deploying to production manually is very dangerous. Proceed?')]
deploy-wiki-production: (deploy-wiki " (generated)")

deploy-wiki-staging: (deploy-wiki " (staging)")

deploy-wiki-dev: (deploy-wiki " (development)")

deploy-web ref: build-web
	./scripts/deploy-web.sh "{{ref}}"

[confirm('Deploying to production manually is very dangerous. Proceed?')]
deploy-web-production:
    just --justfile "{{justfile()}}" --set GEMMA_WEB_DEPLOY_SERVER {{PRODUCTION_DEPLOY_SERVER}} deploy-web 'gemma'

deploy-web-staging: (deploy-web 'gemma-staging')

deploy-web-dev: (deploy-web 'gemma')

deploy-maven-site:
    ./deploy-maven-site.sh

deploy-cli ref: build-cli
	./scripts/deploy-cli.sh "{{ref}}"

[confirm('Deploying to production manually is very dangerous. Proceed?')]
deploy-cli-production: (deploy-cli 'production')

deploy-cli-staging: (deploy-cli 'staging')

clean:
	{{MAVEN}} {{MAVEN_ARGS}} clean

clean-cli:
	{{MAVEN}} {{MAVEN_ARGS}} clean -pl gemma-cli

clean-web:
	{{MAVEN}} {{MAVEN_ARGS}} clean -pl gemma-web

hotfix-start:
    {{MAVEN}} gitflow:hotfix-start

hotfix-finish:
    {{MAVEN}} gitflow:hotfix-finish

release-start:
    {{MAVEN}} gitflow:release-start

release-finish:
    {{MAVEN}} gitflow:release-finish

support-start:
    {{MAVEN}} gitflow:support-start