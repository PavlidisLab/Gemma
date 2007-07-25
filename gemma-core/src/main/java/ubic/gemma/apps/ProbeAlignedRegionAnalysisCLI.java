package ubic.gemma.apps;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.util.AbstractSpringAwareCLI;

public class ProbeAlignedRegionAnalysisCLI extends AbstractSpringAwareCLI {

	private Taxon taxon;
	private String outFileName;

	private ExpressionExperimentService eeService;

	private ArrayDesignService adService;

	@Override
	protected void buildOptions() {
		Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName(
				"Taxon").withDescription("the taxon of the genes").withLongOpt(
				"Taxon").create('t');
		addOption(taxonOption);
		Option outputFileOption = OptionBuilder.hasArg().isRequired()
				.withArgName("outFileName").withDescription(
						"File name for saving the correlation data")
				.withLongOpt("outFilePrefix").create('o');
		addOption(outputFileOption);
	}

	protected void processOptions() {
		super.processOptions();
		String taxonName = getOptionValue('t');
		taxon = Taxon.Factory.newInstance();
		taxon.setCommonName(taxonName);
		TaxonService taxonService = (TaxonService) this.getBean("taxonService");
		taxon = taxonService.find(taxon);
		if (taxon == null) {
			log.info("No Taxon found!");
		}
		if (hasOption('o')) {
			this.outFileName = getOptionValue('o');
		}
		initBeans();
	}

	private void initBeans() {
		eeService = (ExpressionExperimentService) getBean("expressionExperimentService");
		adService = (ArrayDesignService) getBean("arrayDesignService");
	}

	@Override
	protected Exception doWork(String[] args) {
		Collection<ExpressionExperiment> EEs = eeService.findByTaxon(taxon);
		for (ExpressionExperiment EE : EEs) {
			Collection<CompositeSequence> CSs = new HashSet<CompositeSequence>();
			Collection<ArrayDesign> ADs = eeService.getArrayDesignsUsed(EE);
			for (ArrayDesign AD : ADs) {
    			CSs.addAll(adService.compositeSequenceWithoutGenes(AD));
			}
			
			for (CompositeSequence CS : CSs) {
			}
		}
		return null;
	}

	public static void main(String[] args) {
		ProbeAlignedRegionAnalysisCLI analysis = new ProbeAlignedRegionAnalysisCLI();
		StopWatch watch = new StopWatch();
		watch.start();
		Exception e = analysis.doWork(args);
		if (e != null) {
			log.error(e.getMessage());
		}
		watch.stop();
		log.info("Probe aligned region analysis completed in "
				+ watch.getTime() / 1000 + " seconds");
	}
}
