package ubic.gemma.apps;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.linkAnalysis.EffectSizeService;
import ubic.gemma.analysis.linkAnalysis.GenePair;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Calculate the effect size
 * 
 * @author xwan
 * @author raymond
 */
public class EffectSizeCalculationCli extends AbstractSpringAwareCLI {
	private String geneListFile;
	private String goTerm;

	private String[] geneSymbols;

	private String outFilePrefix;

	private Taxon taxon;

	private EffectSizeService effectSizeService;

	private ExpressionExperimentService eeService;
	
	private Gene2GOAssociationService gene2GOService;

	private int stringency = 3;

	public static final int DEFAULT_STRINGENCY = 3;

	public EffectSizeCalculationCli() {
		super();
	}

	@SuppressWarnings("static-access")
	@Override
	protected void buildOptions() {
		Option goOption = OptionBuilder.hasArg().withArgName("GOTerm").withDescription("GO term to pair").withLongOpt("GOTerm").create('t');
		addOption(goOption);
		Option geneOption = OptionBuilder.hasArgs().withArgName("gene")
				.withDescription("Gene (official symbol) to pair").withLongOpt(
						"gene").create('g');
		addOption(geneOption);
		Option geneFileOption = OptionBuilder.hasArg().withArgName("geneFile")
				.withDescription(
						"File containing list of gene pair offical symbols")
				.withLongOpt("geneFile").create('f');
		addOption(geneFileOption);
		Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName(
				"Taxon").withDescription("the taxon of the genes").withLongOpt(
				"Taxon").create('t');
		addOption(taxonOption);
		Option outputFileOption = OptionBuilder.hasArg().isRequired()
				.withArgName("outFilePrefix").withDescription(
						"File prefix for saving the correlation data")
				.withLongOpt("outFilePrefix").create('o');
		addOption(outputFileOption);
		Option stringencyOption = OptionBuilder.hasArg().withArgName(
				"stringency").withDescription(
				"Vote count stringency for link selection").withLongOpt(
				"stringency").create('s');
		addOption(stringencyOption);
	}

	@Override
	protected void processOptions() {
		super.processOptions();
		if (hasOption('t')) {
			this.goTerm = getOptionValue('t');
		}
		if (hasOption('g')) {
			this.geneSymbols = getOptionValues('g');
		}
		if (hasOption('f')) {
			this.geneListFile = getOptionValue('f');
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
		if (hasOption('s')) {
			this.stringency = Integer.parseInt(getOptionValue('s'));
		} else {
			this.stringency = DEFAULT_STRINGENCY;
		}

		initBeans();
	}

	protected void initBeans() {
		effectSizeService = (EffectSizeService) this
				.getBean("effectSizeService");
		eeService = (ExpressionExperimentService) this
				.getBean("expressionExperimentService");
		gene2GOService = (Gene2GOAssociationService) this
				.getBean("gene2GOAssociationService");
	}

	@Override
	protected Exception doWork(String[] args) {
		Exception exc = processCommandLine("EffectSizeCalculation ", args);
		if (exc != null) {
			return exc;
		}
		StopWatch watch = new StopWatch();
		watch.start();

		Collection<ExpressionExperiment> EEs = eeService.findByTaxon(taxon);

		Collection<GenePair> genePairs;
		if (geneSymbols != null) {
			genePairs = effectSizeService.pairCoexpressedGenesByOfficialSymbol(
					geneSymbols, EEs, stringency);
		} else if (geneListFile != null) {
			try {
				genePairs = effectSizeService.pairCoexpressedGenesByOfficialSymbol(
						geneListFile, EEs, stringency);
			} catch (IOException e) {
				return e;
			}
		} else if (goTerm == null) {
			genePairs = effectSizeService.pairCoexpressedGenesByGOTerm(goTerm, taxon, EEs, stringency);
		} else {
			return new Exception("No genes to pair");
		}

		effectSizeService.calculateEffectSize(EEs, genePairs);

		try {
			effectSizeService.saveExprLevelToFile(outFilePrefix
					+ ".expr_lvl.txt", genePairs, EEs);
			effectSizeService.saveCorrelationsToFile(outFilePrefix
					+ ".corr.txt", genePairs, EEs);
			effectSizeService.saveCorrelationsToFigure(outFilePrefix
					+ ".corr.png", genePairs, EEs);
			effectSizeService.saveExprProfilesToFile(
					outFilePrefix + ".eps.txt", genePairs, EEs);
		} catch (IOException e) {
			return e;
		}

		return null;
	}

	public static void main(String[] args) {
		EffectSizeCalculationCli analysis = new EffectSizeCalculationCli();
		StopWatch watch = new StopWatch();
		watch.start();
		log.info("Starting Effect Size Analysis");
		Exception exc = analysis.doWork(args);
		if (exc != null) {
			log.error(exc.getMessage());
		}
		log.info("Finished analysis in " + watch.getTime() / 1000 + " seconds");
	}
}