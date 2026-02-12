MAVEN := env("MAVEN", "mvn")
MAVEN_ARGS := "-am -Prelease -DskipTests"

export GEMMA_CLI_DEPLOY_SERVER := "pavlab"
export GEMMA_CLI_PREFIX := "/space/opt/gemma-cli"
export GEMMA_CLI_WIKI_PAGE_SUFFIX := " (generated)"
export GEMMA_CLI_WIKI_DEPLOY_DEST := f"pavlab-wiki:Global/gemma/Gemma Landing Page/Gemma Curation/List of Gemma CLI Tools{{GEMMA_CLI_WIKI_PAGE_SUFFIX}}/"

export GEMMA_WEB_DEPLOY_SERVER := "chalmers"
export GEMMA_WEB_PREFIX := "/var/local/tomcat"

default: build

build:
	{{MAVEN}} {{MAVEN_ARGS}} package

build-web:
	{{MAVEN}} {{MAVEN_ARGS}} package -pl gemma-web

build-cli:
	{{MAVEN}} {{MAVEN_ARGS}} package -pl gemma-cli

update-completion-scripts: build-cli
	env GEMMA_CLI_ALIAS=gemma-cli ./gemma-cli/update-completion-scripts.sh
	env GEMMA_CLI_ALIAS=gemma-cli-staging ./gemma-cli/update-completion-scripts.sh

generate-wiki: build-cli
	./gemma-cli/target/appassembler/bin/gemma-cli --completion --completion-wiki --completion-wiki-output-dir gemma-cli/target/wiki --completion-wiki-page-suffix "{{GEMMA_CLI_WIKI_PAGE_SUFFIX}}"

deploy-wiki: generate-wiki
	./gemma-cli/deploy-wiki.sh "{{GEMMA_CLI_WIKI_DEPLOY_DEST}}"

deploy-wiki-staging:
    just --justfile "{{justfile()}}" --set GEMMA_CLI_WIKI_PAGE_SUFFIX " (staging)" deploy-wiki

deploy-wiki-dev:
    just --justfile "{{justfile()}}" --set GEMMA_CLI_WIKI_PAGE_SUFFIX " (development)" deploy-wiki

deploy-web ref: build-web
	./gemma-web/deploy.sh "{{ref}}"

deploy-web-staging: (deploy-web 'gemma-staging')

deploy-web-dev: (deploy-web 'gemma')

deploy-cli ref: build-cli update-completion-scripts
	./gemma-cli/deploy.sh "{{ref}}"

deploy-cli-staging: (deploy-cli 'staging')

clean:
	{{MAVEN}} {{MAVEN_ARGS}} clean

clean-cli:
	{{MAVEN}} {{MAVEN_ARGS}} clean -pl gemma-cli

clean-web:
	{{MAVEN}} {{MAVEN_ARGS}} clean -pl gemma-web
