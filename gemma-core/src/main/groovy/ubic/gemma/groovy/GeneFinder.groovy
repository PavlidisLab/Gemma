#!/usr/bin/groovy
package ubic.gemma.groovy

import ubic.gemma.groovy.framework.GemmaCliBuilder
import ubic.gemma.groovy.framework.SpringSupport

//noinspection GroovyAssignabilityCheck
GemmaCliBuilder cli = new GemmaCliBuilder(usage: "groovy GeneFinder [opts] <gene symbol> [more gene symbols]")

def opt = cli.parse(args) as Object

sx = new SpringSupport()
gs = sx.getBean("geneService")

for (gene in opt.arguments()) {
    listogenes = gs.findByOfficialSymbol(gene)
    listogenes.each { println(it) }
}


sx.shutdown()
