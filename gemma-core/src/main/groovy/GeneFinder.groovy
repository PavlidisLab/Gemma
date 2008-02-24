#!/usr/bin/groovy
package ubic.gemma.script
import ubic.gemma.script.SpringSupport
import ubic.gemma.script.GemmaCliBuilder

def cli = new GemmaCliBuilder(usage: 'groovy GeneFinder [opts] <gene symbol> [more gene symbols]')

def opt = cli.parse(args) 

sx = new SpringSupport(opt.u, opt.p);
gs = sx.getBean("geneService")

for (gene in opt.arguments()) {
    listogenes = gs.findByOfficialSymbol(gene);
    listogenes.each{
        println(it)
    }
}


sx.shutdown()
