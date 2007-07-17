package ubic.gemma.analysis.linkAnalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.icu.text.NumberFormat.NumberFormatFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMap;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.metaanalysis.CorrelationEffectMetaAnalysis;
import ubic.gemma.analysis.coexpression.GeneCoExpressionAnalysis;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import cern.colt.list.DoubleArrayList;

/**
 * Effect size calculation service
 * 
 * @spring.bean id="effectSizeService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @spring.property name="dedvService" ref="designElementDataVectorService"
 * @spring.property name="goService" ref="geneOntologyService"
 * @author Raymond
 */
public class EffectSizeService {
	private ExpressionExperimentService eeService;

	private GeneService geneService;

	private DesignElementDataVectorService dedvService;

	private CorrelationEffectMetaAnalysis metaAnalysis;

	private GeneOntologyService goService;

	private ByteArrayConverter bac;

	private static Log log = LogFactory.getLog(EffectSizeService.class
			.getName());

	private static final String EXPR_LEVEL = "expression level";

	private static final String CORRELATION = "correlation";
	
	private static final String MAX_CORR = "max correlation";

	/**
	 * Minimum number of expression experiments for calculating the correlation
	 */
	protected static final int MIN_EE_SAMPLE_SIZE = 3;

	/**
	 * Process gene pairs in chunks
	 */
	protected static final int GENE_PAIR_CHUNK_SIZE = 400000;

	/**
	 * Load genes in chunks
	 */
	protected static final int GENE_LOAD_CHUNK_SIZE = 100;

	/**
	 * Load composite sequences in chunks
	 */
	protected static final int CS_CHUNK_SIZE = 500;

	/**
	 * Filter out expression profiles for genes with relatively low expression
	 * levels
	 */
	protected static final double MIN_EP_RANK = 0.3;

	/**
	 * Minimum number of expression experiments for the figure
	 */
	protected static final int MIN_FIGURE_GENE_PAIR_EE_NUM = 100;

	/**
	 * Create an effect size service
	 */
	public EffectSizeService() {
		metaAnalysis = new CorrelationEffectMetaAnalysis(true, false);
		bac = new ByteArrayConverter();
	}

	/**
	 * Read in gene pair IDs from the geneList into genePairs if they meet the
	 * stringency requirements, i.e. if the pairing is seen in enough other
	 * datasets.
	 * 
	 * @param geneListFile -
	 *            gene list file name
	 * @param stringency -
	 *            minimum stringency required for a gene pair
	 * @return list of gene pairs
	 */
	public Collection<GenePair> readGenePairsByID(String geneListFile,
			int stringency) throws IOException {
		log.debug("Reading gene pairs by ID from " + geneListFile);
		Collection<GenePair> genePairs = new ArrayList<GenePair>();
		BufferedReader in = new BufferedReader(new FileReader(new File(
				geneListFile)));
		String row = null;
		while ((row = in.readLine()) != null && !row.startsWith("#")) {
			row = row.trim();
			if (StringUtils.isBlank(row))
				continue;
			String[] subItems = row.split("\t");
			long firstId = Long.valueOf(subItems[0]);
			long secondId = Long.valueOf(subItems[1]);
			int count = Integer.valueOf(subItems[2]);
			if (count >= stringency) {
				genePairs.add(new GenePair(firstId, secondId));
			}
		}
		in.close();
		log.info("Read " + genePairs.size() + " gene pairs from "
				+ geneListFile);
		return genePairs;
	}

	/**
	 * Read a file containing a pair of genes (official symbols) per line
	 * 
	 * @param geneListFile -
	 *            gene list file name
	 * @return a collection (hash set) of gene pairs
	 * @throws IOException
	 */
	public Collection<GenePair> readGenePairsByOfficialSymbol(
			String geneListFile) throws IOException {
		log.debug("Reading gene pairs by official symbol from " + geneListFile);
		Collection<GenePair> genePairs = new ArrayList<GenePair>();
		BufferedReader in = new BufferedReader(new FileReader(geneListFile));
		String line = null;
		while ((line = in.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			String[] symbols = line.trim().split("\t");
			if (symbols.length < 2) {
				continue;
			}
			long id1 = ((Gene) geneService.findByOfficialSymbol(symbols[0])
					.iterator().next()).getId();
			long id2 = ((Gene) geneService.findByOfficialSymbol(symbols[1])
					.iterator().next()).getId();
			genePairs.add(new GenePair(id1, id2));
		}
		log.info("Read " + genePairs.size() + " gene pairs from "
				+ geneListFile);
		return genePairs;
	}

	/**
	 * Reads the genes (official symbols) in the specified file and pairs each
	 * with a specified set of genes.
	 * 
	 * @param geneListFile -
	 *            list of genes to read in
	 * @param partnerGenes -
	 *            genes to pair with
	 * @return a collection of gene pairs
	 * @throws IOException
	 */
	public Collection<GenePair> pairGenesByOfficialSymbol(String geneListFile,
			Collection<Gene> partnerGenes, Taxon taxon) throws IOException {
		log.debug("Reading genes by official symbol from " + geneListFile);
		Collection<GenePair> genePairs = new ArrayList<GenePair>();
		BufferedReader in = new BufferedReader(new FileReader(geneListFile));
		String line = null;
		int count = 0;
		while ((line = in.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			String symbol = line.trim();
			for (Gene gene1 : ((Collection<Gene>) geneService.findByOfficialSymbol(symbol))) {
				if (taxon.equals(gene1.getTaxon())) {
        			for (Gene gene2 : partnerGenes)
        				genePairs.add(new GenePair(gene1.getId(), gene2.getId()));
        			break;
				}
			}
			count++;
		}
		log.info("Paired " + count + " genes with " + partnerGenes.size());
		return genePairs;
	}

	/**
	 * Pair specified genes with coexpressed genes
	 * 
	 * @param geneSymbols -
	 *            official gene symbols
	 * @param taxon -
	 *            taxon of genes
	 * @param EEs -
	 *            expression experiments
	 * @param stringency -
	 *            minimum support for coexpressed genes
	 * @return - set of gene pairs
	 */
	public Collection<GenePair> pairCoexpressedGenesByOfficialSymbol(
			String[] geneSymbols, Taxon taxon,
			Collection<ExpressionExperiment> EEs, int stringency) {
		Collection<Gene> genes = new HashSet<Gene>();
		for (String geneSymbol : geneSymbols) {
			Collection<Gene> c = (Collection<Gene>) geneService
					.findByOfficialSymbol(geneSymbol);
			for (Gene gene : c) {
				if (gene.getTaxon().equals(taxon)) {
					genes.add(gene);
				}
			}
		}
		return pairCoexpressedGenes(genes, EEs, stringency);
	}

	/**
	 * Pair specified genes with coexpressed genes
	 * 
	 * @param genes -
	 *            genes to pair
	 * @param taxon -
	 *            taxon of genes
	 * @param EEs -
	 *            expression experiments
	 * @param stringency -
	 *            minimum support for coexpressed genes
	 * @return - set of gene pairs
	 */
	public Collection<GenePair> pairCoexpressedGenes(Collection<Gene> genes,
			Collection<ExpressionExperiment> EEs, int stringency) {
		Collection<GenePair> genePairs = new ArrayList<GenePair>();
		for (Gene gene : genes) {
			CoexpressionCollectionValueObject coexpressedGeneCollection = ((CoexpressionCollectionValueObject) geneService
					.getCoexpressedGenes(gene, EEs, stringency));
			Collection<CoexpressionValueObject> coexpressedGenes = coexpressedGeneCollection
					.getGeneCoexpressionData();
			Set<Long> ids = new HashSet<Long>();
			log.info("Pairing " + gene.getOfficialSymbol() + " with "
					+ coexpressedGenes.size()
					+ " coexpressed genes (strigency=" + stringency + ")");
			for (CoexpressionValueObject coexpressedGene : coexpressedGenes) {
				Integer linkCount = coexpressedGene.getPositiveLinkCount();
				long coexpressedGeneId = coexpressedGene.getGeneId();
				if (!ids.contains(new Double(coexpressedGeneId))
						&& coexpressedGeneId != gene.getId()
						&& linkCount != null) {
					GenePair genePair = new GenePair(gene.getId(),
							coexpressedGeneId);
					genePair.setLinkCount(linkCount);
					genePairs.add(genePair);
				}
				ids.add(coexpressedGeneId);
			}
		}
		return genePairs;
	}

	/**
	 * Pair the genes specified in a file (separated with new lines) with
	 * coexpressed genes
	 * 
	 * @param geneListFile -
	 *            list of genes to pair
	 * @param taxon -
	 *            taxon of genes
	 * @param EEs -
	 *            expression experiments
	 * @param stringency -
	 *            minimum support of coexpressed genes
	 * @return set of gene pairs
	 * @throws IOException
	 */
	public Collection<GenePair> pairCoexpressedGenesByOfficialSymbol(
			String geneListFile, Taxon taxon,
			Collection<ExpressionExperiment> EEs, int stringency)
			throws IOException {
		List<String> geneSymbols = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new FileReader(geneListFile));
		String line;
		while ((line = in.readLine()) != null) {
			geneSymbols.add(line.trim());
		}
		String[] geneSymbolStrings = new String[geneSymbols.size()];
		geneSymbols.toArray(geneSymbolStrings);
		return pairCoexpressedGenesByOfficialSymbol(geneSymbolStrings, taxon,
				EEs, stringency);
	}

	/**
	 * Pair the matching the GO term with coexpressed genes
	 * 
	 * @param geneListFile -
	 *            list of genes to pair
	 * @param taxon -
	 *            taxon of genes
	 * @param EEs -
	 *            expression experiments
	 * @param stringency -
	 *            minimum support of coexpressed genes
	 * @return set of gene pairs
	 * @throws IOException
	 */
	public Collection<GenePair> pairCoexpressedGenesByGOTerm(String goId,
			Taxon taxon, Collection<ExpressionExperiment> EEs, int stringency) {
		while (!goService.isReady()) {
			try {
    			Thread.sleep(500);
			} catch (InterruptedException e) {
				continue;
			}
		}
		Collection<Gene> genes = goService.getGenes(goId, taxon);
		return (genes == null)? null : pairCoexpressedGenes(genes, EEs, stringency);
	}

	/**
	 * Calculate the effect size for each gene pair from the specified
	 * expression experiments
	 * 
	 * @param EEs -
	 *            expression experiments
	 * @param genePairs -
	 *            gene pairs
	 */
	public void calculateEffectSize(Collection<ExpressionExperiment> EEs,
			Collection<GenePair> genePairs) {
		Map<Long, Integer> eeSampleSizeMap = calculateCorrelations(EEs,
				genePairs);

		StopWatch watch = new StopWatch();
		watch.start();
		log.debug("Start computing effect size for " + genePairs.size()
				+ " gene pairs");
		for (GenePair pair : genePairs) {
			DoubleArrayList correlations = new DoubleArrayList();
			DoubleArrayList sampleSizes = new DoubleArrayList();
			for (ExpressionExperiment ee : EEs) {
				Integer sampleSize = eeSampleSizeMap.get(ee.getId());
				Double corr = pair.getCorrelation(ee.getId());
				if (sampleSize != null && corr != null) {
					sampleSizes.add(sampleSize);
					correlations.add(corr);
				}
			}
			metaAnalysis.run(correlations, sampleSizes);
			double effectSize = metaAnalysis.getE();
			pair.setEffectSize(effectSize);
		}
		watch.stop();
		log.info("Computed effect size of " + genePairs.size()
				+ " genes pairs in " + watch.getTime() + " ms");
	}

	public void saveCorrelationsToFigure(String figureFileName,
			Collection<GenePair> genePairs, Collection<ExpressionExperiment> EEs)
			throws IOException {
		saveToFigure(figureFileName, genePairs, EEs, CORRELATION);
	}

	public void saveExprLevelToFigure(String figureFileName,
			Collection<GenePair> genePairs, Collection<ExpressionExperiment> EEs)
			throws IOException {
		saveToFigure(figureFileName, genePairs, EEs, EXPR_LEVEL);
	}

	public void saveCorrelationsToFile(String fileName,
			Collection<GenePair> genePairs, Collection<ExpressionExperiment> EEs, boolean linkCount, boolean effectSize)
			throws IOException {
		saveToFile(fileName, genePairs, EEs, linkCount, effectSize, CORRELATION);
	}
	
	public void saveMaxCorrelationsToFile(String fileName,
			Collection<GenePair> genePairs, Collection<ExpressionExperiment> EEs, boolean linkCount, boolean effectSize)
			throws IOException {
		saveToFile(fileName, genePairs, EEs, linkCount, effectSize, MAX_CORR);
	}

	public void saveExprLevelToFile(String fileName,
			Collection<GenePair> genePairs, Collection<ExpressionExperiment> EEs, boolean linkCount, boolean effectSize)
			throws IOException {
		saveToFile(fileName, genePairs, EEs, linkCount, effectSize, EXPR_LEVEL);
	}

	/**
	 * Save expression profiles to the specified file. Requires reading
	 * expression profiles for each gene pair, which may take a while.
	 * 
	 * @param fileName -
	 *            file to save to
	 * @param genePairs -
	 *            gene pairs to save
	 * @param EEs -
	 *            expression experiments
	 * @throws IOException
	 */
	public void saveExprProfilesToFile(String fileName,
			Collection<GenePair> genePairs, Collection<ExpressionExperiment> EEs)
			throws IOException {
		log.info("Saving expression profiles to " + fileName);
		Map<Long, Gene> geneMap = getGeneMapFromGenePairs(genePairs);
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
		for (GenePair genePair : genePairs) {
			Gene gene1 = geneMap.get(genePair.getFirstId());
			Gene gene2 = geneMap.get(genePair.getSecondId());
			for (ExpressionExperiment EE : EEs) {
				long eeId = EE.getId();
				Long dedvId1 = genePair.getFirstDedvId(eeId);
				Long dedvId2 = genePair.getSecondDedvId(eeId);
				if (dedvId1 == null || dedvId2 == null)
					continue;
				DesignElementDataVector dedv1 = dedvService.load(dedvId1);
				DesignElementDataVector dedv2 = dedvService.load(dedvId2);
				ExpressionProfile ep1 = new ExpressionProfile(dedv1);
				ExpressionProfile ep2 = new ExpressionProfile(dedv2);

				String line = gene1.getOfficialSymbol() + ":"
						+ gene2.getOfficialSymbol() + "\t" + EE.getShortName()
						+ "\t" + genePair.getCorrelation(eeId) + "\n";
				out.write(line);
				line = ep1.getId() + "";
				for (double d : ep1.val) {
					line += "\t" + d;
				}
				line += "\n";
				out.write(line);
				line = ep2.getId() + "";
				for (double d : ep2.val) {
					line += "\t" + d;
				}
				line += "\n";
				out.write(line);
				out.flush();
			}
		}
		out.close();
		log.info("Finished saving expression profiles");
	}

	private Collection<ExpressionExperiment> filterExpressionExperiments(
			Collection<ExpressionExperiment> EEs, Collection<GenePair> genePairs) {
		log.info("Filtering expression experiments...");
		Collection<ExpressionExperiment> filteredEEs = new HashSet<ExpressionExperiment>(
				EEs.size());
		for (ExpressionExperiment EE : EEs) {
			for (GenePair genePair : genePairs) {
				if (genePair.getCorrelation(EE.getId()) != null) {
					filteredEEs.add(EE);
					break;
				}
			}
		}
		log.info("Filtered out " + (EEs.size() - filteredEEs.size())
				+ " expression experiments");
		return filteredEEs;
	}

	private Collection<GenePair> filterGenePairs(
			Collection<GenePair> genePairs, Map<Long, Gene> geneMap) {
		log.info("Filtering gene pairs...");
		Collection<GenePair> filteredGenePairs = new HashSet<GenePair>(
				genePairs.size());
		Set<String> pairs = new HashSet<String>();
		for (GenePair genePair : genePairs) {
			Gene gene1 = geneMap.get(genePair.getFirstId());
			Gene gene2 = geneMap.get(genePair.getSecondId());
			String pair = gene1.getOfficialSymbol() + ":" + gene2.getOfficialSymbol();
			if (!(pairs.contains(pair)) || gene1.getOfficialSymbol().equals(gene2.getOfficialSymbol())) {
				filteredGenePairs.add(genePair);
			}
			pairs.add(pair);
		}
		log
				.info("Filtered out "
						+ (genePairs.size() - filteredGenePairs.size())
						+ " gene pairs");
		return filteredGenePairs;
	}

	private void saveToFile(String fileName, Collection<GenePair> genePairs,
			Collection<ExpressionExperiment> EEs, boolean linkCount, boolean effectSize, String type)
			throws IOException {
		DecimalFormat formatter = new DecimalFormat("0.0000");
		log.info("Saving " + type + " data to " + fileName);
		Map<Long, Gene> geneMap = getGeneMapFromGenePairs(genePairs);
		// filter output
		EEs = filterExpressionExperiments(EEs, genePairs);
		genePairs = filterGenePairs(genePairs, geneMap);

		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
		String header = "GenePair";
		if (linkCount)
			header += "\tLinkCount";
		if (effectSize)
			header += "\tEffectSize";
		if (type == MAX_CORR) {
			header += "\tMaxCorrelation";
			header += "\tNumExpressionExpts";
		} else {
    		for (ExpressionExperiment EE : EEs)
    			header += "\t" + EE.getShortName();
		}
		header += "\n";
		out.write(header);

		for (GenePair genePair : genePairs) {
			Gene gene1 = geneMap.get(genePair.getFirstId());
			Gene gene2 = geneMap.get(genePair.getSecondId());
			String line = gene1.getOfficialSymbol() + ":"
					+ gene2.getOfficialSymbol();
			if (linkCount)
    			line += "\t" + genePair.getLinkCount();
			if (effectSize)
    			line += "\t" + formatter.format(genePair.getEffectSize());
			if (type == MAX_CORR) {
				Double maxCorr = genePair.getMaxCorrelation();
				if (maxCorr != null) {
    				line += "\t" + formatter.format(maxCorr);
    				line += "\t" + genePair.getNumExpressionExperiments();
				} else {
					line += "\tNA\tNA";
				}
			} else {
    			for (ExpressionExperiment EE : EEs) {
    				line += "\t";
    				Double corr = genePair.getCorrelation(EE.getId());
    				if (type == EXPR_LEVEL) {
    					Double exprLvl = getExpressionLevel(genePair, EE.getId());
    					if (exprLvl != null) {
    						line += formatter.format(exprLvl);
    					}
    				} else if (type == CORRELATION && corr != null) {
    					line += formatter.format(corr);
    				}
    			}
			}
			line += "\n";
			out.write(line);
			out.flush();
		}
		log.info("Finished saving " + type + " data");

		out.close();
	}

	/**
	 * Load the design element data vectors required to get the expression level for the specified gene pair
	 * @param genePair - gene pair
	 * @param eeId - expresssion experiment ID
	 * @return (average) expression level for that gene pair
	 */
	private Double getExpressionLevel(GenePair genePair, long eeId) {
		Long dedvId1 = genePair.getFirstDedvId(eeId);
		Long dedvId2 = genePair.getSecondDedvId(eeId);
		if (dedvId1 == null || dedvId2 == null) {
			return null;
		}
		DesignElementDataVector dedv1 = dedvService.load(dedvId1);
		DesignElementDataVector dedv2 = dedvService.load(dedvId2);
		if (dedv1.getRank() != null && dedv2.getRank() != null) {
			Double exprLvl = (dedv1.getRank() + dedv2.getRank()) / 2;
			return exprLvl;
		} else {
			return null;
		}
	}

	/**
	 * Save the gene pairs as a figure to the specified file name
	 * 
	 * @param figureFileName -
	 *            file name
	 * @param numGenePairsToSave -
	 *            number of gene pairs to save
	 * @param genePairs -
	 *            gene pair list
	 * @param EEs -
	 *            expression experiment list
	 * @param figureType -
	 *            type of figure to save
	 * @throws IOException
	 */
	private void saveToFigure(String figureFileName,
			Collection<GenePair> genePairs,
			Collection<ExpressionExperiment> EEs, String figureType)
			throws IOException {
		log.info("Saving " + figureType + " image to " + figureFileName);
		Map<Long, Gene> geneMap = getGeneMapFromGenePairs(genePairs);
		EEs = filterExpressionExperiments(EEs, genePairs);
		genePairs = filterGenePairs(genePairs, geneMap);

		ArrayList<GenePair> sortedGenePairs = new ArrayList<GenePair>(genePairs);
		Collections.sort(sortedGenePairs);
		double[][] data = new double[sortedGenePairs.size()][EEs.size()];
		List<String> rowLabels = new ArrayList<String>();
		List<String> colLabels = new ArrayList<String>();
		for (ExpressionExperiment ee : EEs) {
			colLabels.add(ee.getShortName());
		}
		for (GenePair genePair : sortedGenePairs) {
			Gene gene1 = geneMap.get(genePair.getFirstId());
			Gene gene2 = geneMap.get(genePair.getSecondId());
			rowLabels.add(gene1.getOfficialSymbol() + "_"
					+ gene2.getOfficialSymbol());

		}
		DoubleMatrixNamed dataMatrix = new DenseDoubleMatrix2DNamed(data);
		dataMatrix.setRowNames(rowLabels);
		dataMatrix.setColumnNames(colLabels);

		for (GenePair genePair : sortedGenePairs) {
			String rowName = geneMap.get(genePair.getFirstId())
					.getOfficialSymbol()
					+ "_"
					+ geneMap.get(genePair.getSecondId()).getOfficialSymbol();
			int rowIndex = dataMatrix.getRowIndexByName(rowName);
			for (ExpressionExperiment ee : EEs) {
				String colName = ee.getShortName();
				int colIndex = dataMatrix.getColIndexByName(colName);
				Double corr = genePair.getCorrelation(ee.getId());
				if (figureType == CORRELATION && corr != null) {
					dataMatrix.setQuick(rowIndex, colIndex, corr);
				} else if (figureType == EXPR_LEVEL) {
					Double exprLvl = getExpressionLevel(genePair, ee.getId());
					if (exprLvl != null)
						dataMatrix.setQuick(rowIndex, colIndex, exprLvl);
				}
			}
		}
		ColorMatrix dataColorMatrix = new ColorMatrix(dataMatrix);
		dataColorMatrix.setColorMap(ColorMap.GREENRED_COLORMAP);
		JMatrixDisplay dataMatrixDisplay = new JMatrixDisplay(dataColorMatrix);
		dataMatrixDisplay.saveImage(figureFileName, true);
		log.info("Finished saving " + figureType + " image");
	}

	/**
	 * Create and return a gene ID to gene map from a collection of gene pairs
	 * 
	 * @param genePairs -
	 *            gene pairs
	 * @return gene ID to gene map
	 */
	private Map<Long, Gene> getGeneMapFromGenePairs(
			Collection<GenePair> genePairs) {
		Collection<Long> geneIDs = new ArrayList<Long>();
		for (GenePair genePair : genePairs) {
			geneIDs.add(genePair.getFirstId());
			geneIDs.add(genePair.getSecondId());
		}
		return getGeneMapFromIDs(geneIDs);

	}

	/**
	 * Create and return a gene ID to gene map
	 * 
	 * @param ids -
	 *            gene IDs
	 * @return gene ID to gene map
	 */
	private Map<Long, Gene> getGeneMapFromIDs(Collection<Long> ids) {
		Map<Long, Gene> geneMap = new HashMap<Long, Gene>();
		Collection<Long> idsInOneChunk = new HashSet<Long>();
		Collection<Gene> allGenes = new HashSet<Gene>();
		log.info("Loading genes");
		StopWatch qWatch = new StopWatch();
		qWatch.start();
		int count = 0;
		for (Long geneID : ids) {
			idsInOneChunk.add(geneID);
			count++;
			if (count % GENE_LOAD_CHUNK_SIZE == 0 || count == ids.size()) {
				allGenes.addAll(geneService.load(idsInOneChunk));
				idsInOneChunk.clear();
			}
		}
		qWatch.stop();
		log.info(ids.size() + " genes loaded in " + qWatch.getTime() / 1000
				+ " seconds");

		for (Gene gene : allGenes) {
			if (ids.contains(gene.getId())) {
				geneMap.put(gene.getId(), gene);
			}
		}
		return geneMap;
	}

	/**
	 * Build and return the gene to probe (composite sequences) map
	 * 
	 * @param genes
	 * @return gene to probe map
	 */
	private Map<Long, Collection<Long>> getGene2CSMap(Collection<Gene> genes) {
		log.info("Loading composite sequences for " + genes.size() + " genes");
		if (genes.size() <= CS_CHUNK_SIZE) {
			Map<Long, Collection<Long>> gene2cs = geneService
					.getCompositeSequenceMap(genes);
			log.info("Loaded " + gene2cs.keySet().size()
					+ " composite sequences");
			return gene2cs;
		}
		Map<Long, Collection<Long>> gene2cs = new HashMap<Long, Collection<Long>>();
		int count = 0;
		Collection<Gene> genesChunk = new HashSet<Gene>();
		for (Gene gene : genes) {
			genesChunk.add(gene);
			count++;
			if (count % CS_CHUNK_SIZE == 0 || count == genes.size()) {
				gene2cs.putAll(geneService.getCompositeSequenceMap(genesChunk));
				genesChunk.clear();
			}
		}
		log.info("Loaded " + gene2cs.keySet().size() + " composite sequences");
		return gene2cs;
	}

	/**
	 * Inverts the specified gene to probe (cs) map for the specified gene to
	 * produce a probe to gene(s) map
	 * 
	 * @param geneId -
	 *            ID of gene that probes map to
	 * @param gene2cs -
	 *            gene to probe map
	 * @return probe to gene map
	 */
	private Map<Long, Collection<Gene>> getCS2GeneMap(
			Map<Long, Collection<Long>> gene2cs, Map<Long, Gene> geneMap, boolean specificOnly) {
		Map<Long, Collection<Gene>> cs2gene = new HashMap<Long, Collection<Gene>>();
		for (Long geneId : gene2cs.keySet()) {
			Collection<Long> csIds = gene2cs.get(geneId);
			for (Long csId : csIds) {
				Collection<Gene> genes = cs2gene.get(csId);
				if (genes == null) {
					genes = new HashSet<Gene>();
					cs2gene.put(csId, genes);
				}
				genes.add(geneMap.get(geneId));
			}
		}
		
		Map<Long, Collection<Gene>> specificCs2gene = new HashMap<Long, Collection<Gene>>();
		if (specificOnly) {
			for (Long csId : cs2gene.keySet()) {
				if (cs2gene.get(csId).size() == 1) {
					specificCs2gene.put(csId, cs2gene.get(csId));
				} 
			}
			cs2gene = specificCs2gene;
		}
		return cs2gene;
	}

	/**
	 * Retrieve an expression profile for a set of genes, i.e. a map of design
	 * element data vectors to the set of genes that it represents.
	 * 
	 * @param cs2gene -
	 *            a probe to gene map
	 * @param qt -
	 *            the quantitation type of the expression profile desired
	 * @param ee -
	 *            the expression experiment
	 * @return map of design element data vectors to its set of genes
	 */
	private Map<DesignElementDataVector, Collection<Gene>> getDesignElementDataVectors(
			Map<Long, Collection<Gene>> cs2gene, QuantitationType qt,
			ExpressionExperiment ee) {
		Map<DesignElementDataVector, Collection<Gene>> dedv2genes = eeService
				.getDesignElementDataVectors(cs2gene, qt);
		for (DesignElementDataVector dedv : dedv2genes.keySet()) {
			dedv.setExpressionExperiment(ee);
		}
		return dedv2genes;
	}

	/**
	 * Get the preferred quantitation type for the expression experiment
	 * 
	 * @param ee
	 *            expression experiment
	 * @return preferred quantitation type
	 */
	private QuantitationType getPreferredQT(ExpressionExperiment ee) {
		
		Collection<QuantitationType> qts = eeService.getQuantitationTypes(ee);
		for (QuantitationType qt : qts) {
			if (qt.getIsPreferred())
				return qt;
		}
		log.warn("Unable to find preferred quantitation type for "
				+ ee.getShortName());
		return null;
	}

	/**
	 * Get a gene ID to expression profiles map for an expression experiment
	 * (specified by the quantitation type)
	 * 
	 * @param genes -
	 *            genes to map
	 * @param qt -
	 *            quantitation type of the expression experiment
	 * @return gene ID to expression profile map
	 */
	private Map<Long, Collection<ExpressionProfile>> getGeneID2EPsMap(
			Map<Long, Gene> geneMap, ExpressionExperiment ee) {
		QuantitationType qt = getPreferredQT(ee);
		if (qt == null) {
			return null;
		}
		log.debug("Loading expression profiles for " + ee.getShortName());
		Map<Long, Collection<Long>> gene2cs = getGene2CSMap(geneMap.values());
		// reverse the gene2cs map, discarding nonspecific probes
		Map<Long, Collection<Gene>> cs2gene = getCS2GeneMap(gene2cs, geneMap, true);

		Map<Long, Collection<ExpressionProfile>> geneID2EPs = new HashMap<Long, Collection<ExpressionProfile>>();
		Map<DesignElementDataVector, Collection<Gene>> dedv2genes = getDesignElementDataVectors(
				cs2gene, qt, ee);
		// build genes to expression profiles map
		for (DesignElementDataVector dedv : dedv2genes.keySet()) {
			Collection<Gene> genes = dedv2genes.get(dedv);
			for (Gene gene : genes) {
				Long id = gene.getId();
				Collection<ExpressionProfile> eps = geneID2EPs.get(id);
				if (eps == null) {
					eps = new HashSet<ExpressionProfile>();
					geneID2EPs.put(id, eps);
				}
				eps.add(new ExpressionProfile(dedv));
			}
		}
		log.info(ee.getShortName() + ": Loaded " + dedv2genes.keySet().size()
				+ " expression profiles.");
		return geneID2EPs;
	}

	/**
	 * Calculate correlations for the specified gene pairs in the specified EEs.
	 * 
	 * @param EEs -
	 *            expression experiments
	 * @param genePairs -
	 *            gene pair list
	 * @param geneMap -
	 *            gene ID to gene map
	 * @return expression experiment ID to sample size map
	 */
	private Map<Long, Integer> calculateCorrelations(
			Collection<ExpressionExperiment> EEs, Collection<GenePair> genePairs) {
		if (genePairs.size() < GENE_PAIR_CHUNK_SIZE) {
			return calculateCorrelationsChunk(EEs, genePairs);
		}
		Collection<GenePair> oneChunkGenePairs = new HashSet<GenePair>();
		Map<Long, Integer> eeSampleSizeMap = new HashMap<Long, Integer>();
		int count = 0;
		for (GenePair genePair : genePairs) {
			oneChunkGenePairs.add(genePair);
			count++;
			if (count % GENE_PAIR_CHUNK_SIZE == 0 || count == genePairs.size()) {
				eeSampleSizeMap.putAll(calculateCorrelationsChunk(EEs,
						oneChunkGenePairs));
				oneChunkGenePairs.clear();
			}
		}
		return eeSampleSizeMap;
	}

	/**
	 * Calculate correlations for the specified gene pairs in the specified EEs.
	 * 
	 * @param EEs -
	 *            expression experiments
	 * @param genePairs -
	 *            gene pair list
	 * @return expression experiment ID to sample size map
	 */
	private Map<Long, Integer> calculateCorrelationsChunk(
			Collection<ExpressionExperiment> EEs, Collection<GenePair> genePairs) {
		Map<Long, Integer> eeSampleSizeMap = new HashMap<Long, Integer>();
		Map<Long, Gene> geneMap = getGeneMapFromGenePairs(genePairs);
		int count = 0;
		int totalEEs = EEs.size();
		for (ExpressionExperiment ee : EEs) {
			long eeId = ee.getId();
			StopWatch watch = new StopWatch();
			watch.start();
			Map<Long, Collection<ExpressionProfile>> geneID2EPs = getGeneID2EPsMap(
					geneMap, ee);
			if (geneID2EPs == null) {
				continue;
			}

			for (GenePair genePair : genePairs) {
				Collection<ExpressionProfile> source = geneID2EPs.get(genePair
						.getFirstId());
				Collection<ExpressionProfile> target = geneID2EPs.get(genePair
						.getSecondId());
				if (source == null || target == null)
					continue;
				Map<Double, List<Long>> corrEPMap = new TreeMap<Double, List<Long>>();
				for (ExpressionProfile ep1 : source) {
					if (ep1.rank != null && ep1.rank > MIN_EP_RANK) {
						for (ExpressionProfile ep2 : target) {
							if (ep2.rank != null
									&& ep2.rank > MIN_EP_RANK
									&& ep1.val.length == ep2.val.length
									&& ep1.val.length > GeneCoExpressionAnalysis.MINIMUM_SAMPLE) {
								List<Long> eps = new ArrayList<Long>(2);
								eps.add(ep1.id);
								eps.add(ep2.id);
								corrEPMap.put(CorrelationStats.correl(ep1.val,
										ep2.val), eps);
							}
						}
					}
				}
				if (corrEPMap.keySet().size() > 0) {
					Double corr = (Double) (corrEPMap.keySet().toArray())[corrEPMap
							.keySet().size() / 2];
					int ss = source.iterator().next().val.length;
					if (corr != null && ss > MIN_EE_SAMPLE_SIZE) {
						List<Long> dedvIds = corrEPMap.get(corr);
						long dedvId1 = dedvIds.get(0);
						long dedvId2 = dedvIds.get(1);
						genePair.addCorrelation(eeId, dedvId1, dedvId2, corr);
						eeSampleSizeMap.put(eeId, ss);
					}
				}
			}
			log.info(ee.getShortName() + " (" + count + " of " + totalEEs
					+ "): calculated correlation of "
					+ geneID2EPs.values().size() + " expression profiles in "
					+ watch.getTime() / 1000 + " seconds");
			count++;
		}
		return eeSampleSizeMap;
	}

	/**
	 * Stores the expression profile data.
	 * 
	 * @author xwan
	 * @author Raymond
	 */
	protected class ExpressionProfile {
		DesignElementDataVector dedv = null;

		double[] val;

		Double rank;

		long id;

		/**
		 * Construct an ExpressionProfile from the specified
		 * DesignElementDataVector
		 * 
		 * @param dedv -
		 *            vector to convert
		 */
		public ExpressionProfile(DesignElementDataVector dedv) {
			this.dedv = dedv;
			this.id = dedv.getId();
			byte[] bytes = dedv.getData();
			val = bac.byteArrayToDoubles(bytes);
			rank = dedv.getRank();
		}

		/**
		 * Get the ID of the vector
		 * 
		 * @return the vector ID
		 */
		public long getId() {
			return this.id;
		}

		/**
		 * Get the relative expression level of the expression profile: k/n
		 * where k is the rank of the expression level and n is the number of
		 * expression profiles for that quantitation type
		 * 
		 * @return relative expression level
		 */
		public Double getRank() {
			return rank;
		}

	}

	public void setEeService(ExpressionExperimentService eeService) {
		this.eeService = eeService;
	}

	public void setGeneService(GeneService geneService) {
		this.geneService = geneService;
	}

	public void setDedvService(DesignElementDataVectorService dedvService) {
		this.dedvService = dedvService;
	}

	public void setGoService(GeneOntologyService goService) {
		this.goService = goService;
	}
}
