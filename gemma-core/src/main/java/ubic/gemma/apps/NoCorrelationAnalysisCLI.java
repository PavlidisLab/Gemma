package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.linkAnalysis.EffectSizeService;
import ubic.gemma.analysis.linkAnalysis.GenePair;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

public class NoCorrelationAnalysisCLI extends AbstractSpringAwareCLI {
	private String geneListFile;
	private String partnerGeneListFile;

	private String outFilePrefix;

	private Taxon taxon;

	private EffectSizeService effectSizeService;

	private ExpressionExperimentService eeService;

	private GeneService geneService;

	public NoCorrelationAnalysisCLI() {
		super();
	}

	@Override
	protected void buildOptions() {
		Option geneFileOption = OptionBuilder.hasArg().isRequired()
				.withArgName("geneFile").withDescription(
						"File containing list of gene offical symbols")
				.withLongOpt("geneFile").create('g');
		addOption(geneFileOption);
		Option partnerFileOption = OptionBuilder.hasArg().isRequired()
				.withArgName("partnerGeneFile").withDescription(
						"File containing list of partner gene offical symbols")
				.withLongOpt("partnerGeneFile").create('a');
		addOption(partnerFileOption);
		Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName(
				"Taxon").withDescription("the taxon of the genes").withLongOpt(
				"Taxon").create('t');
		addOption(taxonOption);
		Option outputFileOption = OptionBuilder.hasArg().isRequired()
				.withArgName("outFilePrefix").withDescription(
						"File prefix for saving the output").withLongOpt(
						"outFilePrefix").create('o');
		addOption(outputFileOption);
	}

	@Override
	protected void processOptions() {
		super.processOptions();
		if (hasOption('g')) {
			this.geneListFile = getOptionValue('g');
		}
		if (hasOption('a')) {
			this.partnerGeneListFile = getOptionValue('a');
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
			this.outFilePrefix = getOptionValue('o');
		}
		initBeans();
	}

	protected void initBeans() {
		effectSizeService = (EffectSizeService) this
				.getBean("effectSizeService");
		eeService = (ExpressionExperimentService) this
				.getBean("expressionExperimentService");
		geneService = (GeneService) this.getBean("geneService");
	}

	@Override
	protected Exception doWork(String[] args) {
		Exception exc = processCommandLine("NoCorrelationAnalysis", args);
		if (exc != null) {
			return exc;
		}
		Collection<GenePair> genePairs;

		Collection<ExpressionExperiment> allEEs = eeService.findByTaxon(taxon);
		Collection<ExpressionExperiment> EEs = new ArrayList<ExpressionExperiment>();
		for (ExpressionExperiment ee : allEEs) {
			if (ee.getShortName().equals("GSE7529")) {
				log.info("Removing expression experiment GSE7529");
			} else {
				EEs.add(ee);
			}
		}
//		try {
//			genePairs = effectSizeService.pairGenesByOfficialSymbolFromFiles(
//					geneListFile, partnerGeneListFile, taxon);
//		} catch (IOException e) {
//			return e;
//		}
//
//		effectSizeService.calculateEffectSize(EEs, genePairs);
//
//		try {
//			effectSizeService.saveCorrelationsToFile(outFilePrefix
//					+ ".corr.txt", genePairs, EEs, false, false);
//			effectSizeService.saveMaxCorrelationsToFile(outFilePrefix
//					+ ".max_corr.txt", genePairs, EEs, false, false);
//			effectSizeService.saveExprLevelToFile(outFilePrefix
//					+ ".expr_lvl.txt", genePairs, EEs, false, false);
//			effectSizeService.saveExprProfilesToFile(
//					outFilePrefix + ".eps.txt", genePairs, EEs);
//		} catch (IOException e) {
//			return e;
//		}

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
