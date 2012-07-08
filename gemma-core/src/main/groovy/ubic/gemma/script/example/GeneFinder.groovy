#!/usr/bin/groovy
package ubic.gemma.script.example
import ubic.gemma.script.framework.GemmaCliBuilder;
import ubic.gemma.script.framework.SpringSupport;

def cli = new GemmaCliBuilder(usage : "groovy GeneFinder [opts] <gene symbol> [more gene symbols]")

def opt = cli.parse(args)

sx = new SpringSupport();
gs = sx.getBean("geneService")

for (gene in opt.arguments()) {
    listogenes = gs.findByOfficialSymbol(gene);
    listogenes.each{ println(it) }
}


sx.shutdown()
