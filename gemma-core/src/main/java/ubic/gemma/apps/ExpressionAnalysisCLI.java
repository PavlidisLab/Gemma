/**
 * 
 */
package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Create a relative expression level (dedv rank) matrix for a list of genes
 * 
 * @author raymond
 * 
 */
public class ExpressionAnalysisCLI extends AbstractGeneManipulatingCLI {
	private String outFile;

	private String inFile;

	private Taxon taxon;

	private ExpressionExperimentService eeService;

	private GeneService geneService;
	
	private ArrayDesignService adService;

	/*
	 * (non-Javadoc)
	 * 
	 * @see ubic.gemma.util.AbstractCLI#buildOptions()
	 */
	@Override
	protected void buildOptions() {
		Option inFileOption = OptionBuilder.hasArg().withArgName("inFile")
				.withDescription(
						"File containing list of genes in offical symbols")
				.withLongOpt("inFile").create('i');
		addOption(inFileOption);

		Option outFileOption = OptionBuilder.hasArg().isRequired().withArgName(
				"outFile").withDescription("File to save rank matrix to")
				.withLongOpt("outFile").create('o');
		addOption(outFileOption);

		Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName(
				"Taxon").withDescription("the taxon of the genes").withLongOpt(
				"Taxon").create('t');
		addOption(taxonOption);
	}

	protected void processOptions() {
		super.processOptions();
		if (hasOption('i')) {
			inFile = getOptionValue('i');
		}

		if (hasOption('o')) {
			outFile = getOptionValue('o');
		}

		String taxonName = getOptionValue('t');
		taxon = Taxon.Factory.newInstance();
		taxon.setCommonName(taxonName);
		TaxonService taxonService = (TaxonService) getBean("taxonService");
		taxon = taxonService.find(taxon);
		if (taxon == null) {
			log.info("No Taxon found!");
		}
		initBeans();
	}

	private void initBeans() {
		eeService = (ExpressionExperimentService) getBean("expressionExperimentService");
		geneService = (GeneService) getBean("geneService");
		adService = (ArrayDesignService) getBean("arrayDesignService");
	}

	private void saveRanksToFile(String outFile, Collection<Gene> genes,
			Collection<ExpressionExperiment> EEs) throws IOException {
		DecimalFormat df = new DecimalFormat("0.0000");
		log.info("Saving ranks to file " + outFile);
		PrintWriter out = new PrintWriter(new FileWriter(outFile));
		String header = "ExpressionExperiment";
		for (Gene gene : genes) {
			header += "\t" + gene.getOfficialSymbol();
		}
		out.println(header);
		
		int eeCount = 1;
		for (ExpressionExperiment EE : EEs) {
			log.info("Processing " + EE.getShortName() + " (" + eeCount++ + " of " + EEs.size() + ")");
			Collection<ArrayDesign> ADs = eeService.getArrayDesignsUsed(EE);
			Collection<Long> csIDs = new HashSet<Long>();
			for (ArrayDesign ad : ADs) {
				for (CompositeSequence cs : (Collection<CompositeSequence>) adService.loadCompositeSequences(ad)) {
					csIDs.add(cs.getId());
				}
			}
			Map<Long, Collection<Long>> cs2geneMap = geneService
					.getCS2GeneMap(csIDs);
			QuantitationType qt = (QuantitationType) eeService
					.getPreferredQuantitationType(EE).iterator().next();
			Map<DesignElementDataVector, Collection<Long>> dedv2geneMap = eeService
					.getDesignElementDataVectors(cs2geneMap, qt);
			
			// invert dedv2geneMap
			Map<Long, Collection<DesignElementDataVector>> gene2dedvMap = new HashMap<Long, Collection<DesignElementDataVector>>();
			for (DesignElementDataVector dedv : dedv2geneMap.keySet()) {
				Collection<Long> geneIds = dedv2geneMap.get(dedv);
				for (Long geneId : geneIds) {
    				Collection<DesignElementDataVector> dedvs = gene2dedvMap.get(dedv);
    				if (dedvs == null) {
    					dedvs = new HashSet<DesignElementDataVector>();
    					gene2dedvMap.put(geneId, dedvs);
    				}
    				dedvs.add(dedv);
				}
				
			}
			log.info("Loaded design element data vectors");
			
			int rankCount = 0;
			String line = EE.getName();
			for (Gene gene : genes) {
				line += "\t";
    			Double rank;
				List<Double> ranks = new ArrayList<Double>();
				Collection<DesignElementDataVector> dedvs = gene2dedvMap.get(gene.getId());
				if (dedvs == null) continue;
				for (DesignElementDataVector dedv : dedvs) {
					ranks.add(dedv.getRank());
				}
				if (ranks.size() < 1) continue;
				
				// take the median rank
				Collections.sort(ranks);
				rank = ranks.get(ranks.size() / 2);
				if (rank == null) continue;
				line += df.format(rank);
				rankCount++;
			}
			out.println(line);
			out.flush();
			log.info("Saved " + rankCount + " gene ranks");
		}
		out.close();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
	 */
	@Override
	protected Exception doWork(String[] args) {
		Exception e = processCommandLine("ExpressionAnalysis", args);
		if (e != null)
			return e;

		Collection<Gene> genes;
		if (inFile != null) {
			try {
				genes = readInGeneListFile( inFile, taxon, OFFICIAL_SYMBOL );
			} catch (IOException exc) {
				return exc;
			}
		} else {
			genes = geneService.getGenesByTaxon(taxon);
		}

		Collection<ExpressionExperiment> EEs = eeService.findByTaxon(taxon);
		
		try {
			saveRanksToFile(outFile, genes, EEs);
		} catch (IOException exc) {
			return exc;
		}

		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ExpressionAnalysisCLI analysis = new ExpressionAnalysisCLI();
		StopWatch watch = new StopWatch();
		watch.start();

		log.info("Starting expression analysis");
		Exception e = analysis.doWork(args);
		if (e != null)
			log.error(e.getMessage());
		watch.stop();
		log.info("Finished expression analysis in " + watch.getTime() / 1000
				+ " seconds");
	}

}
