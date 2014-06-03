package ubic.gemma.script.example

import java.util.concurrent.ForkJoinPool;

import ubic.gemma.script.framework.SpringSupport;
import ubic.gemma.tasks.visualization.DifferentialExpressionVisualizationValueObject.GeneScore;
import ubic.gemma.model.analysis.expression.diff.Direction

cli = new CliBuilder()
cli.h(longOpt: 'help', 'Usage: groovy DiffExprMatrix -togiv')
cli.o(argName: 'file name', longOpt: 'outFile', required: true, args: 1, 'Output results to this file')
cli.t(argName: 'common name', longOpt: 'taxon', required: true, args: 1, 'Taxon of genes to fetch')
cli.g(argName: 'file name', longOpt: 'geneFile', required: false, args: 1, 'File containing list of gene official symbols to load')
cli.i(longOpt: 'filterNonSpecific', 'Filter non-specific probes')
cli.e(argName: 'file name', longOpt: 'eeFile', required: false, args: 1, 'File containing list of data sets to load')

cli.c(longOpt: 'correctedPval', required: false, 'Output corrected p-values')
cli.d(longOpt: 'direction', required: false, 'Output direction')


def summarizeDirection(vals) {
    //println "${vals[0]} class ${vals[0].getClass()}"
    
    if (Direction.EITHER in vals || (Direction.DOWN in vals && Direction.UP in vals) ) {
        val = Direction.EITHER
    } else if (Direction.DOWN in vals) {
        val = Direction.DOWN
    } else if (Direction.UP in vals) {
        val = Direction.UP
    } else {
        val = null
    }
    return val
}

def median(a) {
    b = a.sort();
    if (a.size() % 2 == 0) {
        return (b[(int) (b.size() / 2 - 1)] + b[(int) (b.size() / 2)]) / 2.0;
    } else {
        return b[(int) (b.size() / 2)];
    }
}

opts = cli.parse(args)
if (!opts) return;
if (opts.hasOption("h")) cli.usage();

geneSymbols = (opts.hasOption("g"))? new File(opts.getOptionValue("g")).readLines() : null;
filterNonSpecific = opts.i;
if (filterNonSpecific) {
    System.out.println "Filtering non-specific probes";
}
outputDirection = opts.d;
outputCorrectedPval = opts.c;
if (outputDirection) {
    System.out.println "Retrieving effect sizes";
} else if (outputCorrectedPval) {
    System.out.println "Retrieving corrected p-values";
} else {
    System.out.println "Retrieving (uncorrected) p-values";
}

sx = new SpringSupport();
taxonService = sx.getBean("taxonService");
geneService = sx.getBean("geneService");
deaService = sx.getBean("differentialExpressionAnalysisService")
gdeService = sx.getBean("geneDifferentialExpressionService")
csService = sx.getBean("compositeSequenceService")
eeService = sx.getBean("expressionExperimentService")
derService = sx.getBean( "differentialExpressionResultService" )

taxonName = opts.getOptionValue("t");
taxon = taxonService.findByCommonName(taxonName);

eeNames = (opts.hasOption("e"))? new File(opts.getOptionValue("e")).readLines() : null;

if (geneSymbols != null && geneSymbols.size() > 0) {
    System.out.println "${new Date()}: Attempting to load ${geneSymbols.size()} $taxonName genes...";
    genes = geneSymbols.collect { geneService.findByOfficialSymbol(it, taxon) };
    genes = genes.findAll { it != null };
} else {
    System.out.println "${new Date()}: Loading all known $taxonName genes...";
    //genes = geneService.loadKnownGenes(taxon);
    genes = geneService.getGenesByTaxon(taxon);
}
System.out.println "${new Date()}: Loaded ${genes.size()} $taxonName genes.";

rssUsed = [];
int count = 1;
int filterCount = 0;

geneRsVal = [:];


if (outputDirection) {
    println("Outputting Direction")
} else if(outputCorrectedPval) {
    println("Outputting correctedPval")
} else {
    println("Outputting PVal")
}

ids = new HashSet();
// gene = geneService.findByOfficialSymbol("wt1")[0]
for (gene in genes) {
    // not working anymore, see bug 3696
    //ees = (eeNames == null)? deaService.findExperimentsWithAnalyses(gene) : eeNames.collect { eeService.findByShortName(it) };
    ees = (eeNames == null)? eeService.findByGene(gene) : eeNames.collect { eeService.findByShortName(it) };
    println("Processing ${gene.getOfficialSymbol()} eeNames: ${ees.size()}")
    
    des = gdeService.getDifferentialExpression(gene, ees)
    der = derService.find(gene, ees)
    
    geneRsVal[gene.officialSymbol] = [:];

    probeIdMap = [:];
    if (filterNonSpecific) {
        probes = csService.loadMultiple(des*.probeId);
        for (probe in probes) {
            probeIdMap[probe.id] = probe;
        }
    }
    
    for (de in des) {
        ee = de.expressionExperiment;
        efs = de.experimentalFactors;
        if (outputDirection) {
            val = de.direction;
        } else if(outputCorrectedPval) {
            val = de.correctedP;
        } else {
            val = de.p;
        }

        probeId = de.probeId;
        if (filterNonSpecific) {
            if (csService.getGenes(probeIdMap[probeId]).size() > 1) {
                filterCount++;
                continue;
            }
        }

        id = "${ee.id}.${(efs*.id).sort().join('.')}"
        
        //println "$id $de.direction $de.p"
        
        ids << id;

        if (geneRsVal[gene.officialSymbol][id] == null) {
            geneRsVal[gene.officialSymbol][id] = []
        }
        geneRsVal[gene.officialSymbol][id] << val;

    }
    
    if (count++ % 100 == 0) {
        System.out.println "${new Date()}: Processed $count genes; $filterCount probes filtered";
    }
    
    //println("Count ${count}")
    //if (count >= 3) {
        //TODO!!!!!!
        //break;
    //}
}

System.out.println "${new Date()}: Finished processing ${genes.size()} genes across ${ids.size()} factors";

f = opts.getOptionValue("o");
outFile = new File(f);
outFile.delete();
println "${new Date()}: Writing results to $f";
fOut = new BufferedWriter(new PrintWriter(outFile));

// header
fOut << "Gene\t${ids.collect{"ARSID." + it}.join('\t')}\n"

count = 1;
for (gene in genes) {
    line = "${gene.officialSymbol}";
    for (id in ids) {
        val = null;
        vals = geneRsVal[gene.officialSymbol][id];
        
        // special case that we get multiple p-values for the same factor names
        if (vals != null && vals.size() > 0) {
            try {
                if (outputDirection) {
                    val = summarizeDirection(vals);
                } else {
                    val = median(vals);
                }
            } catch (Exception e) {
                System.err.println "$gene.officialSymbol $id $vals";
                e.printStackTrace();
            }
        }
        
        //println "vals=$vals val=$val"
        
        if (outputDirection) {
            strFmt = "%s"
        } else {
            strFmt = "%.3E"
        }
        line += "\t" + ((val != null)? sprintf( strFmt, val ) : "NA")
    }
    line += "\n";
    fOut << line;
    if (count++ % 100 == 0) {
        System.out.println "${new Date()}: Wrote $count genes";
    }
    
    //println line
    
    
    //println("Count ${count}")
    //if (count >= 3) {
        //TODO!!!!!!
        //break;
    //}
}

fOut.close()

System.out.println "${new Date()}: Finished";

sx.shutdown();

