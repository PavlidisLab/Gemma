package ubic.gemma.analysis.linkAnalysis;

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
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DenseMatrix3DNamed;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.metaanalysis.CorrelationEffectMetaAnalysis;
import ubic.gemma.analysis.coexpression.ExpressionProfile;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import cern.colt.list.DoubleArrayList;

/**
 * Effect size calculation service
 * 
 * @spring.bean id="effectSizeService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @spring.property name="dedvService" ref="designElementDataVectorService"
 * @spring.property name="adService" ref="arrayDesignService"
 * @author Raymond
 */
public class EffectSizeService {
	private ExpressionExperimentService eeService;

	private GeneService geneService;

	private DesignElementDataVectorService dedvService;

	private CorrelationEffectMetaAnalysis metaAnalysis;

	private ArrayDesignService adService;

	private static Log log = LogFactory.getLog(EffectSizeService.class
			.getName());

	private static final String EXPR_LEVEL = "expression level";

	private static final String CORRELATION = "correlation";

	private static final String MAX_CORR = "max correlation";

	protected static final int MIN_EP_NUM_SAMPLES = 3;

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
	}

	public Collection<Long> readGeneListFile(String fileName, Taxon taxon)
			throws IOException {
		Collection<Long> geneIds = new HashSet<Long>();
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		while (in.ready()) {
			String line = in.readLine();
			if (line.startsWith("#"))
				continue;
			String symbol = in.readLine().trim();
			for (Gene gene : ((Collection<Gene>) geneService
					.findByOfficialSymbolInexact(symbol))) {
				if (taxon.equals(gene.getTaxon())) {
					geneIds.add(gene.getId());
					break;
				}
			}
		}
		return geneIds;
	}

	// /**
	// * Pair specified genes with coexpressed genes
	// *
	// * @param genes -
	// * genes to pair
	// * @param EEs -
	// * expression experiments
	// * @param stringency -
	// * minimum support for coexpressed genes
	// * @return - set of gene pairs
	// */
	// public Collection<GenePair> pairCoexpressedGenes(Collection<Gene> genes,
	// Collection<ExpressionExperiment> EEs, int stringency) {
	// Collection<GenePair> genePairs = new ArrayList<GenePair>();
	// for (Gene gene : genes) {
	// CoexpressionCollectionValueObject coexpressedGeneCollection =
	// ((CoexpressionCollectionValueObject) geneService
	// .getCoexpressedGenes(gene, EEs, stringency));
	// Collection<CoexpressionValueObject> coexpressedGenes =
	// coexpressedGeneCollection
	// .getGeneCoexpressionData();
	// Set<Long> ids = new HashSet<Long>();
	// log.info("Pairing " + gene.getOfficialSymbol() + " with "
	// + coexpressedGenes.size()
	// + " coexpressed genes (stringency=" + stringency + ")");
	// for (CoexpressionValueObject coexpressedGene : coexpressedGenes) {
	// Integer linkCount = coexpressedGene.getPositiveLinkCount();
	// long coexpressedGeneId = coexpressedGene.getGeneId();
	// if (!ids.contains(new Double(coexpressedGeneId))
	// && coexpressedGeneId != gene.getId()
	// && linkCount != null) {
	// GenePair genePair = new GenePair(gene.getId(),
	// coexpressedGeneId);
	// genePair.setLinkCount(linkCount);
	// genePairs.add(genePair);
	// }
	// ids.add(coexpressedGeneId);
	// }
	// }
	// return genePairs;
	// }

	//
	// public void saveCorrelationsToFigure(String figureFileName,
	// Collection<GenePair> genePairs, Collection<ExpressionExperiment> EEs)
	// throws IOException {
	// saveToFigure(figureFileName, genePairs, EEs, CORRELATION);
	// }
	//
	// public void saveExprLevelToFigure(String figureFileName,
	// Collection<GenePair> genePairs, Collection<ExpressionExperiment> EEs)
	// throws IOException {
	// saveToFigure(figureFileName, genePairs, EEs, EXPR_LEVEL);
	// }
	//
	// public void saveCorrelationsToFile(String fileName,
	// Collection<GenePair> genePairs,
	// Collection<ExpressionExperiment> EEs, boolean linkCount,
	// boolean effectSize) throws IOException {
	// saveToFile(fileName, genePairs, EEs, linkCount, effectSize, CORRELATION);
	// }
	//
	// public void saveMaxCorrelationsToFile(String fileName,
	// Collection<GenePair> genePairs,
	// Collection<ExpressionExperiment> EEs, boolean linkCount,
	// boolean effectSize) throws IOException {
	// saveToFile(fileName, genePairs, EEs, linkCount, effectSize, MAX_CORR);
	// }
	//
	// public void saveExprLevelToFile(String fileName,
	// Collection<GenePair> genePairs,
	// Collection<ExpressionExperiment> EEs, boolean linkCount,
	// boolean effectSize) throws IOException {
	// saveToFile(fileName, genePairs, EEs, linkCount, effectSize, EXPR_LEVEL);
	// }

	// /**
	// * Save expression profiles to the specified file. Requires reading
	// * expression profiles for each gene pair, which may take a while.
	// *
	// * @param fileName -
	// * file to save to
	// * @param genePairs -
	// * gene pairs to save
	// * @param EEs -
	// * expression experiments
	// * @throws IOException
	// */
	// public void saveExprProfilesToFile(String fileName,
	// Collection<GenePair> genePairs, Collection<ExpressionExperiment> EEs)
	// throws IOException {
	// log.info("Saving expression profiles to " + fileName);
	// Map<Long, Gene> geneMap = getGeneMapFromGenePairs(genePairs);
	// BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
	// for (GenePair genePair : genePairs) {
	// Gene gene1 = geneMap.get(genePair.getFirstId());
	// Gene gene2 = geneMap.get(genePair.getSecondId());
	// for (ExpressionExperiment EE : EEs) {
	// long eeId = EE.getId();
	// Long dedvId1 = genePair.getFirstDedvId(eeId);
	// Long dedvId2 = genePair.getSecondDedvId(eeId);
	// if (dedvId1 == null || dedvId2 == null)
	// continue;
	// DesignElementDataVector dedv1 = dedvService.load(dedvId1);
	// DesignElementDataVector dedv2 = dedvService.load(dedvId2);
	// ExpressionProfile ep1 = new ExpressionProfile(dedv1);
	// ExpressionProfile ep2 = new ExpressionProfile(dedv2);
	//
	// String line = gene1.getOfficialSymbol() + ":"
	// + gene2.getOfficialSymbol() + "\t" + EE.getShortName()
	// + "\t" + genePair.getCorrelation(eeId) + "\n";
	// out.write(line);
	// line = ep1.getId() + "";
	// for (double d : ep1.getExpressionLevels()) {
	// line += "\t" + d;
	// }
	// line += "\n";
	// out.write(line);
	// line = ep2.getId() + "";
	// for (double d : ep2.getExpressionLevels()) {
	// line += "\t" + d;
	// }
	// line += "\n";
	// out.write(line);
	// out.flush();
	// }
	// }
	// out.close();
	// log.info("Finished saving expression profiles");
	// }

	private Collection<Long> filterExpressionExperiments(
			DenseMatrix3DNamed<ExpressionProfilePair> matrix) {
		log.info("Filtering expression experiments...");
		Collection<Long> filteredEeIds = new HashSet<Long>(matrix.slices());
		EE: for (Object eeId : matrix.getSliceNames()) {
			int slice = matrix.getSliceIndexByName(eeId);
			for (int i = 0; i < matrix.rows(); i++)
				for (int j = 0; j < matrix.columns(); j++)
					if (!matrix.isMissing(slice, i, j)) {
						filteredEeIds.add((Long) eeId);
						continue EE;
					}
		}
		log.info(filteredEeIds.size() + " of " + matrix.slices() + " passed");
		return filteredEeIds;
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
			String pair = gene1.getOfficialSymbol() + ":"
					+ gene2.getOfficialSymbol();
			if (!(pairs.contains(pair))
					|| gene1.getOfficialSymbol().equals(
							gene2.getOfficialSymbol())) {
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

	private void saveToFile(String fileName,
			DenseMatrix3DNamed<ExpressionProfilePair> matrix, String type,
			boolean geneSymbol) throws IOException {
		DecimalFormat formatter = new DecimalFormat("0.0000");
		Map<Long, Gene> geneMap = getGeneMapFromIDs(matrix.getRowNames(),
				matrix.getColNames());
		Map<Long, ExpressionExperiment> eeMap = getEeMap(matrix.getSliceNames());
		// filter output
		Collection<Long> filteredEEs = filterExpressionExperiments(matrix);

		PrintWriter out = new PrintWriter(new FileWriter(fileName));
		String header = "GenePair";
		for (Long eeId : filteredEEs)
			header += "\t" + eeMap.get(eeId).getShortName();
		out.println(header);

		HashSet<String> pairs = new HashSet<String>();
		for (Object gene1Id : matrix.getRowNames()) {
			int row = matrix.getRowIndexByName(gene1Id);
			String gene1 = geneSymbol ? geneMap.get(gene1Id)
					.getOfficialSymbol() : gene1Id.toString();
			for (Object gene2Id : matrix.getColNames()) {
				int col = matrix.getColIndexByName(gene2Id);
				String gene2 = geneSymbol ? geneMap.get(gene2Id)
						.getOfficialSymbol() : gene2Id.toString();
				if (gene1 == null || gene2 == null)
					continue;
				String genePair = gene1 + ":" + gene2;
				// remove dupes
				if (pairs.contains(genePair))
					continue;
				pairs.add(genePair);

				String line = genePair;
				for (Long eeId : filteredEEs) {
					int slice = matrix.getSliceIndexByName(eeId);
					line += "\t";
					ExpressionProfilePair epPair = matrix.get(slice, row, col);
					if (epPair != null) {
						if (type == CORRELATION)
							line += formatter.format(epPair.correlation);
						else if (type == EXPR_LEVEL)
							line += formatter.format(epPair.expressionLevel);
					}
				}
				out.println(line);
				out.flush();
			}
		}
		out.close();
	}

	public void saveExpressionLevelsToFile(String fileName,
			DenseMatrix3DNamed<ExpressionProfilePair> matrix, boolean geneSymbol)
			throws IOException {
		saveToFile(fileName, matrix, EXPR_LEVEL, geneSymbol);
	}

	public void saveCorrelationsToFile(String fileName,
			DenseMatrix3DNamed<ExpressionProfilePair> matrix, boolean geneSymbol)
			throws IOException {
		saveToFile(fileName, matrix, CORRELATION, geneSymbol);
	}

//	/**
//	 * Save the gene pairs as a figure to the specified file name
//	 * 
//	 * @param figureFileName -
//	 *            file name
//	 * @param numGenePairsToSave -
//	 *            number of gene pairs to save
//	 * @param genePairs -
//	 *            gene pair list
//	 * @param EEs -
//	 *            expression experiment list
//	 * @param figureType -
//	 *            type of figure to save
//	 * @throws IOException
//	 */
//	private void saveToFigure(String figureFileName,
//			CorrelationMatrix correlationMatrix,
//			Collection<ExpressionExperiment> EEs, String figureType)
//			throws IOException {
//		log.info("Saving " + figureType + " image to " + figureFileName);
//		Map<Long, Gene> geneMap = getGeneMapFromGenePairs(genePairs);
//		EEs = filterExpressionExperiments(EEs, genePairs);
//		genePairs = filterGenePairs(genePairs, geneMap);
//
//		ArrayList<GenePair> sortedGenePairs = new ArrayList<GenePair>(genePairs);
//		Collections.sort(sortedGenePairs);
//		double[][] data = new double[sortedGenePairs.size()][EEs.size()];
//		List<String> rowLabels = new ArrayList<String>();
//		List<String> colLabels = new ArrayList<String>();
//		for (ExpressionExperiment ee : EEs) {
//			colLabels.add(ee.getShortName());
//		}
//		for (GenePair genePair : sortedGenePairs) {
//			Gene gene1 = geneMap.get(genePair.getFirstId());
//			Gene gene2 = geneMap.get(genePair.getSecondId());
//			rowLabels.add(gene1.getOfficialSymbol() + "_"
//					+ gene2.getOfficialSymbol());
//
//		}
//		DoubleMatrixNamed dataMatrix = new DenseDoubleMatrix2DNamed(data);
//		dataMatrix.setRowNames(rowLabels);
//		dataMatrix.setColumnNames(colLabels);
//
//		for (GenePair genePair : sortedGenePairs) {
//			String rowName = geneMap.get(genePair.getFirstId())
//					.getOfficialSymbol()
//					+ "_"
//					+ geneMap.get(genePair.getSecondId()).getOfficialSymbol();
//			int rowIndex = dataMatrix.getRowIndexByName(rowName);
//			for (ExpressionExperiment ee : EEs) {
//				String colName = ee.getShortName();
//				int colIndex = dataMatrix.getColIndexByName(colName);
//				Double corr = genePair.getCorrelation(ee.getId());
//				if (figureType == CORRELATION && corr != null) {
//					dataMatrix.setQuick(rowIndex, colIndex, corr);
//				} else if (figureType == EXPR_LEVEL) {
//					Double exprLvl = getExpressionLevel(genePair, ee.getId());
//					if (exprLvl != null)
//						dataMatrix.setQuick(rowIndex, colIndex, exprLvl);
//				}
//			}
//		}
//		ColorMatrix dataColorMatrix = new ColorMatrix(dataMatrix);
//		dataColorMatrix.setColorMap(ColorMap.GREENRED_COLORMAP);
//		JMatrixDisplay dataMatrixDisplay = new JMatrixDisplay(dataColorMatrix);
//		dataMatrixDisplay.saveImage(figureFileName, true);
//		log.info("Finished saving " + figureType + " image");
//	}

	/**
	 * Create and return a gene ID to gene map
	 * 
	 * @param ids -
	 *            gene IDs
	 * @return gene ID to gene map
	 */
	private Map<Long, Gene> getGeneMapFromIDs(Collection<Long> queryGeneIds,
			Collection<Long> targetGeneIds) {
		Collection<Long> ids = new HashSet<Long>(queryGeneIds);
		ids.addAll(targetGeneIds);

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

	private Map<Long, ExpressionExperiment> getEeMap(Collection<Long> eeIds) {
		Map<Long, ExpressionExperiment> eeMap = new HashMap<Long, ExpressionExperiment>();
		for (ExpressionExperiment ee : (Collection<ExpressionExperiment>) eeService
				.load(eeIds)) {
			eeMap.put(ee.getId(), ee);
		}
		return eeMap;
	}

	private Map<Long, Collection<Long>> getSpecificCS2Gene(
			Map<Long, Collection<Long>> cs2gene) {
		Map<Long, Collection<Long>> specificCs2gene = new HashMap<Long, Collection<Long>>();
		for (Long csId : cs2gene.keySet()) {
			if (cs2gene.get(csId).size() == 1) {
				specificCs2gene.put(csId, cs2gene.get(csId));
			}
		}
		return specificCs2gene;
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
	private Map<DesignElementDataVector, Collection<Long>> getDesignElementDataVectors(
			Map<Long, Collection<Long>> cs2gene, QuantitationType qt,
			ExpressionExperiment ee) {
		Map<DesignElementDataVector, Collection<Long>> dedv2genes = eeService
				.getDesignElementDataVectors(cs2gene, qt);
		for (DesignElementDataVector dedv : dedv2genes.keySet()) {
			dedv.setExpressionExperiment(ee);
		}
		return dedv2genes;
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
		Collection<QuantitationType> qts = (Collection<QuantitationType>) eeService
				.getPreferredQuantitationType(ee);
		if (qts.size() < 1) {
			return null;
		}
		QuantitationType qt = qts.iterator().next();
		log.debug("Loading expression profiles for " + ee.getShortName());
		Collection<ArrayDesign> ads = eeService.getArrayDesignsUsed(ee);
		Collection<Long> csIds = new HashSet<Long>();
		for (ArrayDesign ad : ads) {
			Collection<CompositeSequence> css = adService
					.loadCompositeSequences(ad);
			for (CompositeSequence cs : css) {
				csIds.add(cs.getId());
			}
		}
		Map<Long, Collection<Long>> cs2gene = geneService.getCS2GeneMap(csIds);
		cs2gene = getSpecificCS2Gene(cs2gene);

		Map<Long, Collection<ExpressionProfile>> geneID2EPs = new HashMap<Long, Collection<ExpressionProfile>>();
		Map<DesignElementDataVector, Collection<Long>> dedv2genes = getDesignElementDataVectors(
				cs2gene, qt, ee);
		// build genes to expression profiles map
		for (DesignElementDataVector dedv : dedv2genes.keySet()) {
			Collection<Long> geneIds = dedv2genes.get(dedv);
			for (Long geneId : geneIds) {
				Collection<ExpressionProfile> eps = geneID2EPs.get(geneId);
				if (eps == null) {
					eps = new HashSet<ExpressionProfile>();
					geneID2EPs.put(geneId, eps);
				}
				eps.add(new ExpressionProfile(dedv));
			}
		}
		log.info(ee.getShortName() + ": Loaded " + dedv2genes.keySet().size()
				+ " expression profiles.");
		return geneID2EPs;
	}

	public DenseDoubleMatrix2DNamed calculateEffectSizeMatrix(
			DenseMatrix3DNamed<ExpressionProfilePair> epMatrix) {
		DenseDoubleMatrix2DNamed matrix = new DenseDoubleMatrix2DNamed(epMatrix
				.rows(), epMatrix.columns());
		matrix.setRowNames(epMatrix.getRowNames());
		matrix.setColumnNames(epMatrix.getColNames());

		for (Object rowId : epMatrix.getRowNames()) {
			int rowIndex = matrix.getRowIndexByName(rowId);
			for (Object colId : epMatrix.getColNames()) {
				int colIndex = matrix.getColIndexByName(colId);
				DoubleArrayList correlations = new DoubleArrayList(epMatrix
						.slices());
				DoubleArrayList sampleSizes = new DoubleArrayList(epMatrix
						.slices());
				for (Object sliceId : epMatrix.getSliceNames()) {
					int sliceIndex = epMatrix.getSliceIndexByName(sliceId);
					ExpressionProfilePair epPair = epMatrix.get(sliceIndex,
							rowIndex, colIndex);
					if (epPair != null) {
						correlations.add(epPair.correlation);
						sampleSizes.add(epPair.sampleSize);
					}
				}
				metaAnalysis.run(correlations, sampleSizes);
				double effectSize = metaAnalysis.getE();
				matrix.set(rowIndex, colIndex, effectSize);
			}
		}
		return matrix;
	}

	public DenseMatrix3DNamed<ExpressionProfilePair> calculateCorrelationMatrix(
			Collection<ExpressionExperiment> EEs, List<Long> queryGeneIds,
			List<Long> targetGeneIds) {
		List<Long> eeIds = new ArrayList<Long>(EEs.size());
		for (ExpressionExperiment ee : EEs)
			eeIds.add(ee.getId());
		DenseMatrix3DNamed<ExpressionProfilePair> matrix = new DenseMatrix3DNamed<ExpressionProfilePair>(
				eeIds, queryGeneIds, targetGeneIds);
		Map<Long, Gene> geneMap = getGeneMapFromIDs(queryGeneIds, targetGeneIds);
		int count = 0;
		int totalEEs = EEs.size();
		StopWatch watch = new StopWatch();
		for (ExpressionExperiment ee : EEs) {
			watch.start();
			Map<Long, Collection<ExpressionProfile>> geneId2Eps = getGeneID2EPsMap(
					geneMap, ee);
			if (geneId2Eps == null)
				continue;
			int slice = matrix.getSliceIndexByName(ee.getId());

			for (Long qGeneId : queryGeneIds) {
				int row = matrix.getRowIndexByName(qGeneId);
				for (Long tGeneId : targetGeneIds) {
					int col = matrix.getColIndexByName(tGeneId);
					Collection<ExpressionProfile> queryEps = geneId2Eps
							.get(qGeneId);
					Collection<ExpressionProfile> targetEps = geneId2Eps
							.get(tGeneId);

					List<ExpressionProfilePair> epPairs = new ArrayList<ExpressionProfilePair>();
					for (ExpressionProfile queryEp : queryEps) {
						for (ExpressionProfile targetEp : targetEps) {
							if (isValidExpressionProfile(queryEp)
									&& isValidExpressionProfile(targetEp))
								epPairs.add(new ExpressionProfilePair(queryEp,
										targetEp));
						}
					}
					Collections.sort(epPairs);

					if (epPairs.size() > 0) {
						ExpressionProfilePair epPair = epPairs.get(epPairs
								.size() / 2);
						matrix.set(slice, row, col, epPair);
					}
				}
			}
			watch.stop();
			log.info(ee.getShortName() + " (" + count + " of " + totalEEs
					+ "): calculated correlation of "
					+ geneId2Eps.values().size() + " expression profiles in "
					+ watch.getTime() / 1000 + " seconds");
			watch.reset();
		}
		return matrix;
	}

	private boolean isValidExpressionProfile(ExpressionProfile ep) {
		return ep.getRank() != null && ep.getRank() > MIN_EP_RANK
				&& ep.getNumValidSamples() > MIN_EP_NUM_SAMPLES;
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

	public void setAdService(ArrayDesignService adService) {
		this.adService = adService;
	}

	protected class GenePair {
		private long firstId;

		private long secondId;

		/**
		 * Construct a gene pair with the specified pair of IDs and count
		 * 
		 * @param id1 -
		 *            ID of first gene
		 * @param id2 -
		 *            ID of second gene
		 */
		public GenePair(long id1, long id2) {
			this.firstId = id1;
			this.secondId = id2;
		}

		public long getFirstId() {
			return firstId;
		}

		public void setFirstId(long firstId) {
			this.firstId = firstId;
		}

		public long getSecondId() {
			return secondId;
		}

		public void setSecondId(long secondId) {
			this.secondId = secondId;
		}
	}

	private class ExpressionProfilePair implements
			Comparable<ExpressionProfilePair> {
		long epId1;
		long epId2;
		double correlation;
		double expressionLevel;
		int sampleSize;

		public ExpressionProfilePair(long epId1, long epId2,
				double correlation, double expressionLevel, int sampleSize) {
			this.epId1 = epId1;
			this.epId2 = epId2;
			this.correlation = correlation;
			this.expressionLevel = expressionLevel;
			this.sampleSize = sampleSize;
		}

		public ExpressionProfilePair(ExpressionProfile ep1,
				ExpressionProfile ep2) {
			this.epId1 = ep1.getId();
			this.epId2 = ep2.getId();
			this.correlation = CorrelationStats.correl(ep1
					.getExpressionLevels(), ep2.getExpressionLevels());
			this.sampleSize = ep1.getExpressionLevels().length;
			this.expressionLevel = (ep1.getRank() + ep2.getRank()) / 2;
		}

		public int compareTo(ExpressionProfilePair epPair) {
			return correlation > epPair.correlation ? 1 : -1;
		}
	}
}
