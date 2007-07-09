package ubic.gemma.apps;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import cern.colt.list.ObjectArrayList;

import ubic.gemma.analysis.linkAnalysis.EffectSizeService;
import ubic.gemma.analysis.linkAnalysis.GenePair;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

public class NoCorrelationAnalysisCLI extends AbstractSpringAwareCLI {
	private String geneListFile;

	private String outFile;

	private String figureFile;

	private Taxon taxon;

	private EffectSizeService effectSizeService;

	private ExpressionExperimentService eeService;

	private GeneService geneService;
	
	private Gene2GOAssociationService gene2GOService;

	public NoCorrelationAnalysisCLI() {
		super();
	}

	@Override
	protected void buildOptions() {
		Option geneFileOption = OptionBuilder.hasArg().isRequired()
				.withArgName("geneFile").withDescription(
						"File containing list of gene pair offical symbols")
				.withLongOpt("geneFile").create('g');
		addOption(geneFileOption);
		Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName(
				"Taxon").withDescription("the taxon of the genes").withLongOpt(
				"Taxon").create('t');
		addOption(taxonOption);
		Option outputFileOption = OptionBuilder.hasArg().isRequired()
				.withArgName("outFile").withDescription(
						"File for saving the correlation data").withLongOpt(
						"outFile").create('o');
		addOption(outputFileOption);
		Option figureFileOption = OptionBuilder.hasArg().withArgName("figureFile")
				.withDescription("File for saving the figure").withLongOpt(
						"figureFile").create('f');
		addOption(figureFileOption);
	}

	@Override
	protected void processOptions() {
		super.processOptions();
		if (hasOption('g')) {
			this.geneListFile = getOptionValue('g');
		}
		if (hasOption('t')) {
			String taxonName = getOptionValue('t');
			taxon = Taxon.Factory.newInstance();
			taxon.setCommonName(taxonName);
			TaxonService taxonService = (TaxonService) this
					.getBean("taxonService");
			taxon = taxonService.find(taxon);
			if (taxon == null) {
				log.info("No Taxon found!");
			}
		}
		if (hasOption('o')) {
			this.outFile = getOptionValue('o');
		}
		if (hasOption('f')) {
			this.figureFile = getOptionValue('f');
		}
		effectSizeService = (EffectSizeService) this
				.getBean("effectSizeService");
		eeService = (ExpressionExperimentService) this
				.getBean("expressionExperimentService");
		geneService = (GeneService) this.getBean("geneService");
		gene2GOService = (Gene2GOAssociationService) this.getBean("gene2GOAssociationService");
	}

	@Override
	protected Exception doWork(String[] args) {
		Exception exc = processCommandLine("NoCorrelationAnalysis", args);
		if (exc != null) {
			return exc;
		}
		Collection<GenePair> genePairs;
		Gene gene = (Gene) geneService.findByOfficialSymbol("GRIN1").iterator().next();
		gene = ((Collection<Gene>) gene2GOService.findByGOTerm("0007268", taxon)).iterator().next();
		
		// try {
		// genePairs = effectSizeService.readGenesByOfficialSymbol(geneListFile,
		// geneService.getGenesByTaxon(taxon));
		// } catch (IOException e) {
		// return e;
		// }
		Collection<ExpressionExperiment> EEs = eeService.findByTaxon(taxon);
		genePairs = new HashSet<GenePair>();
		for (Gene gene2 : (Collection<Gene>) geneService.getGenesByTaxon(taxon)) {
			genePairs.add(new GenePair(gene.getId(), gene2.getId()));
		}
		effectSizeService.calculateEffectSize(EEs, genePairs);
		try {
			effectSizeService.saveExprLevelToFigure(figureFile, genePairs, EEs);

			BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
			for (GenePair genePair : genePairs) {
				String output = genePair.getFirstId() + ":"
						+ genePair.getSecondId();
				if (genePair.getMaxCorrelation() != null)
					output += "\t" + genePair.getMaxCorrelation();
				log.debug(output);
				ObjectArrayList corrs = genePair.getCorrelations();
				for (int i = 0; i < corrs.size(); i++) {
					Double corr = (Double) corrs.get(i);
					if (corr == null)
						continue;
					output += "\t" + corr;
				}
				out.write(output + "\n");
				out.flush();
			}
			out.close();
		} catch (IOException e) {
			return e;
		}
		return null;
	}

	public static void main(String[] args) {
		NoCorrelationAnalysisCLI analysis = new NoCorrelationAnalysisCLI();
		StopWatch watch = new StopWatch();
		watch.start();
		log.info("Starting No Correlation Analysis");
		Exception exc = analysis.doWork(args);
		if (exc != null) {
			log.error(exc.getMessage());
		}
		log.info("Finished analysis in " + watch.getTime() / 1000 + " seconds");
	}
}
