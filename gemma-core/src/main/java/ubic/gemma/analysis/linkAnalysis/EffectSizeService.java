package ubic.gemma.analysis.linkAnalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix3DNamed;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.distribution.HistogramSampler;
import ubic.basecode.math.metaanalysis.CorrelationEffectMetaAnalysis;
import ubic.gemma.analysis.coexpression.ExpressionProfile;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.ConfigUtils;
import cern.colt.list.DoubleArrayList;

/**
 * Effect size calculation service
 * 
 * @spring.bean id="effectSizeService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @spring.property name="dedvService" ref="designElementDataVectorService"
 * @author Raymond
 */
public class EffectSizeService {
	private ExpressionExperimentService eeService;

	private DesignElementDataVectorService dedvService;

	private CorrelationEffectMetaAnalysis metaAnalysis;

	private static Log log = LogFactory.getLog(EffectSizeService.class
			.getName());

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

	// /**
	// * Pair specified genes with coexpressed genes
	// *
	// * @param genes - genes to pair
	// * @param EEs - expression experiments
	// * @param stringency - minimum support for coexpressed genes
	// * @return - set of gene pairs
	// */
	// public Collection<Gene> getCleanCoexpressedGenes( Collection<Gene> genes,
	// Collection<ExpressionExperiment> EEs,
	// int stringency ) {
	// Collection<Gene> coexpressedGenes = new HashSet<Gene>();
	// for ( Gene gene : genes ) {
	// CoexpressionCollectionValueObject coexpressionCollectionValueObject = ( (
	// CoexpressionCollectionValueObject ) geneService
	// .getCoexpressedGenes( gene, EEs, stringency ) );
	// Collection<CoexpressionValueObject> coexpressionData =
	// coexpressionCollectionValueObject
	// .getGeneCoexpressionData();
	// Set<Long> ids = new HashSet<Long>();
	// for ( CoexpressionValueObject coexpressionValueObject : coexpressionData
	// ) {
	// Integer linkCount = coexpressionValueObject.getPositiveLinkCount();
	// long coexpressedGeneId = coexpressionValueObject.getGeneId();
	// if ( !ids.contains( new Double( coexpressedGeneId ) ) &&
	// coexpressedGeneId != gene.getId()
	// && linkCount != null ) {
	// coexpressedGenes.add( o );
	// }
	// ids.add( coexpressedGeneId );
	// }
	// }
	// return coexpressedGenes;
	// }

	public DenseDoubleMatrix3DNamed filterCoexpressionMatrix(
			DenseDoubleMatrix3DNamed matrix) {
		log.info("Filtering expression experiments...");
		List<Long> filteredEeIds = new ArrayList<Long>(matrix.slices());
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

		DenseDoubleMatrix3DNamed filteredMatrix = new DenseDoubleMatrix3DNamed(
				filteredEeIds.size(), matrix.rows(), matrix.columns());
		filteredMatrix.setSliceNames(filteredEeIds);
		for (int i = 0; i < filteredEeIds.size(); i++) {
			Long eeId = filteredEeIds.get(i);
			int slice = filteredMatrix.getSliceIndexByName(eeId);
			for (int j = 0; j < matrix.rows(); j++) {
				for (int k = 0; k < matrix.columns(); k++) {
					double val = matrix.get(matrix.getSliceIndexByName(eeId),
							j, k);
					filteredMatrix.set(slice, j, k, val);
				}
			}
		}

		return filteredMatrix;

	}

	public DenseDoubleMatrix2DNamed foldCoexpressionMatrix(
			DenseDoubleMatrix3DNamed matrix) {
		DenseDoubleMatrix2DNamed foldedMatrix = new DenseDoubleMatrix2DNamed(
				matrix.rows() * matrix.columns(), matrix.slices());
		foldedMatrix.setColumnNames(matrix.getSliceNames());
		for (int i = 0; i < matrix.rows(); i++) {
			for (int j = 0; j < matrix.columns(); j++) {
				for (int k = 0; k < matrix.slices(); k++) {
					int row = i * matrix.columns() + j;
					int column = k;
					double val = matrix.get(k, i, j);
					foldedMatrix.addRowName(matrix.getRowName(i) + ":"
							+ matrix.getColName(j));
					foldedMatrix.set(row, column, val);
				}
			}
		}
		return foldedMatrix;
	}

	private Map<Long, ExpressionExperiment> getEeMap(Collection<Long> eeIds) {
		Map<Long, ExpressionExperiment> eeMap = new HashMap<Long, ExpressionExperiment>();
		for (ExpressionExperiment ee : (Collection<ExpressionExperiment>) eeService
				.loadMultiple(eeIds)) {
			eeMap.put(ee.getId(), ee);
		}
		return eeMap;
	}

	private Map<DesignElementDataVector, Collection<Gene>> getSpecificDedv2gene(
			Map<DesignElementDataVector, Collection<Gene>> dedv2genes) {
		Map<DesignElementDataVector, Collection<Gene>> specificDedv2gene = new HashMap<DesignElementDataVector, Collection<Gene>>();
		for (DesignElementDataVector dedv : dedv2genes.keySet()) {
			if (dedv2genes.get(dedv).size() == 1) {
				specificDedv2gene.put(dedv, dedv2genes.get(dedv));
			}
		}
		return specificDedv2gene;
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
	private Map<Gene, Collection<ExpressionProfile>> getGene2epsMap(
			ExpressionExperiment ee) {
		Collection<QuantitationType> qts = (Collection<QuantitationType>) eeService
				.getPreferredQuantitationType(ee);
		if (qts.size() < 1) {
			return null;
		}
		QuantitationType qt = qts.iterator().next();
		log.debug("Loading expression profiles for " + ee.getShortName());
		Collection<DesignElementDataVector> dedvs = eeService
				.getDesignElementDataVectors(ee, qts);
		Map<DesignElementDataVector, Collection<Gene>> dedv2genes = dedvService
				.getDedv2GenesMap(dedvs, qt);
		dedv2genes = getSpecificDedv2gene(dedv2genes);

		Map<Gene, Collection<ExpressionProfile>> gene2eps = new HashMap<Gene, Collection<ExpressionProfile>>();
		// build genes to expression profiles map
		for (DesignElementDataVector dedv : dedv2genes.keySet()) {
			Collection<Gene> genes = dedv2genes.get(dedv);
			for (Gene gene : genes) {
				Collection<ExpressionProfile> eps = gene2eps.get(gene);
				if (eps == null) {
					eps = new HashSet<ExpressionProfile>();
					gene2eps.put(gene, eps);
				}
				eps.add(new ExpressionProfile(dedv));
			}
		}
		log.info(ee.getShortName() + ": Loaded " + dedv2genes.keySet().size()
				+ " expression profiles.");
		return gene2eps;
	}

	public DenseDoubleMatrix2DNamed calculateEffectSizeMatrix(
			DenseDoubleMatrix3DNamed correlationMatrix,
			DenseDoubleMatrix3DNamed sampleSizeMatrix) {
		DenseDoubleMatrix2DNamed matrix = new DenseDoubleMatrix2DNamed(
				correlationMatrix.rows(), correlationMatrix.columns());
		matrix.setRowNames(correlationMatrix.getRowNames());
		matrix.setColumnNames(correlationMatrix.getColNames());

		for (Object rowId : correlationMatrix.getRowNames()) {
			int rowIndex = matrix.getRowIndexByName(rowId);
			for (Object colId : correlationMatrix.getColNames()) {
				int colIndex = matrix.getColIndexByName(colId);
				DoubleArrayList correlations = new DoubleArrayList(
						correlationMatrix.slices());
				DoubleArrayList sampleSizes = new DoubleArrayList(
						correlationMatrix.slices());
				for (Object sliceId : correlationMatrix.getSliceNames()) {
					int sliceIndex = correlationMatrix
							.getSliceIndexByName(sliceId);
					double correlation = correlationMatrix.get(sliceIndex,
							rowIndex, colIndex);
					double sampleSize = sampleSizeMatrix.get(sliceIndex,
							rowIndex, colIndex);
					correlations.add(correlation);
					sampleSizes.add(sampleSize);
				}
				metaAnalysis.run(correlations, sampleSizes);
				double effectSize = metaAnalysis.getE();
				matrix.set(rowIndex, colIndex, effectSize);
			}
		}
		return matrix;
	}

	/**
	 * Reads in a correlation matrix and randomizes the correlations using the
	 * distributions provided by link analysis
	 * 
	 * @param EEs -
	 *            expression experiments
	 * @param correlationMatrix
	 * @return
	 */
	public DenseDoubleMatrix3DNamed calculateRandomCorrelationMatrix(
			DenseDoubleMatrix3DNamed correlationMatrix) {
		DenseDoubleMatrix3DNamed randCorrMatrix = new DenseDoubleMatrix3DNamed(
				correlationMatrix.getSliceNames(), correlationMatrix
						.getRowNames(), correlationMatrix.getColNames());
		Collection<Long> eeIds = correlationMatrix.getSliceNames();
		Map<Long, ExpressionExperiment> eeMap = getEeMap(eeIds);
		Map<ExpressionExperiment, HistogramSampler> histSamplerMap = getHistogramSamplerMap(eeMap
				.values());
		for (Long eeId : eeIds) {
			int slice = correlationMatrix.getSliceIndexByName(eeId);
			HistogramSampler sampler = histSamplerMap.get(eeMap.get(eeId));
			if (sampler != null) {
				for (int row = 0; row < correlationMatrix.rows(); row++) {
					for (int col = 0; col < correlationMatrix.columns(); col++) {
						double correlation = correlationMatrix.get(slice, row,
								col);
						if (!Double.isNaN(correlation)) {
							double randCorrelation = sampler.nextSample();
							randCorrMatrix
									.set(slice, row, col, randCorrelation);
						} else {
							randCorrMatrix.set(slice, row, col, Double.NaN);
						}

					}
				}
			} else {
				for (int row = 0; row < randCorrMatrix.rows(); row++) {
					for (int col = 0; col < randCorrMatrix.columns(); col++)
						randCorrMatrix.set(slice, row, col, Double.NaN);
				}
			}
		}
		return randCorrMatrix;
	}

	/**
	 * Fold the 3D correlation matrix to a 2D matrix with maximum correlations
	 * 
	 * @param matrix -
	 *            correlation matrix
	 * @param n -
	 *            the Nth largest correlation
	 * @return matrix with Nth largest correlations
	 */
	public DenseDoubleMatrix2DNamed getMaxCorrelationMatrix(
			DenseDoubleMatrix3DNamed matrix, int n) {
		DenseDoubleMatrix2DNamed maxMatrix = new DenseDoubleMatrix2DNamed(
				matrix.rows(), matrix.columns());
		maxMatrix.setRowNames(matrix.getRowNames());
		maxMatrix.setColumnNames(matrix.getColNames());
		for (int i = 0; i < matrix.rows(); i++) {
			for (int j = 0; j < matrix.columns(); j++) {
				DoubleArrayList list = new DoubleArrayList();
				for (int k = 0; k < matrix.slices(); k++) {
					double val = matrix.get(k, i, j);
					if (!Double.isNaN(val)) {
						list.add(val);
					}
				}
				list.sort();
				if (list.size() > 0)
					maxMatrix.set(i, j, list.get(list.size() - 1 - n));
				else
					maxMatrix.set(i, j, Double.NaN);
			}
		}
		return maxMatrix;
	}

	private Map<ExpressionExperiment, HistogramSampler> getHistogramSamplerMap(
			Collection<ExpressionExperiment> ees) {
		Map<ExpressionExperiment, HistogramSampler> histSamplers = new HashMap<ExpressionExperiment, HistogramSampler>();
		for (ExpressionExperiment ee : ees) {
			String fileName = ConfigUtils.getAnalysisStoragePath()
					+ ee.getShortName() + ".correlDist.txt";
			try {
				HistogramSampler sampler = readHistogramFile(fileName);
				histSamplers.put(ee, sampler);
			} catch (IOException e) {
				log.error(e.getMessage());
				log
						.error("ERROR: Unable to read correlation distribution file for "
								+ ee.getShortName());
			}
		}
		return histSamplers;
	}

	/**
	 * Read a correlation distribution
	 * 
	 * @param fileName
	 * @return a histogram sampler for the read distribution
	 * @throws IOException
	 */
	public HistogramSampler readHistogramFile(String fileName)
			throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		int numHeaderLines = 1;
		LinkedList<Double> bins = new LinkedList<Double>();
		List<Integer> countList = new LinkedList<Integer>();
		while (in.ready()) {
			String line = in.readLine();
			if (line.startsWith("#") || numHeaderLines-- > 0)
				continue;
			String fields[] = line.split("\t");
			Double bin = Double.valueOf(fields[0]);
			bins.add(bin);
			Integer count = Integer.valueOf(fields[1]);
			countList.add(count);
		}

		double min = bins.getFirst().doubleValue();
		double max = bins.getLast().doubleValue();
		int[] counts = new int[countList.size()];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = countList.get(i);
		}
		return new HistogramSampler(counts, min, max);
	}

	/**
	 * Create and populate the coexpression matrices (correlation matrix, sample
	 * size matrix, expression level matrix)
	 * 
	 * @param ees
	 * @param queryGenes
	 * @param targetGenes
	 * @return
	 */
	public CoexpressionMatrices calculateCoexpressionMatrices(
			Collection<ExpressionExperiment> ees, Collection<Gene> queryGenes,
			Collection<Gene> targetGenes) {
		CoexpressionMatrices matrices = new CoexpressionMatrices(ees,
				queryGenes, targetGenes);
		DenseDoubleMatrix3DNamed correlationMatrix = matrices
				.getCorrelationMatrix();
		DenseDoubleMatrix3DNamed sampleSizeMatrix = matrices
				.getSampleSizeMatrix();
		DenseDoubleMatrix3DNamed exprLvlMatrix = matrices.getExprLvlMatrix();
		int count = 0;
		int totalEes = ees.size();
		StopWatch watch = new StopWatch();
		for (ExpressionExperiment ee : ees) {
			watch.start();
			Map<Gene, Collection<ExpressionProfile>> gene2eps = getGene2epsMap(ee);
			if (gene2eps == null)
				continue;
			int slice = correlationMatrix.getSliceIndexByName(ee.getId());

			for (Gene qGene : queryGenes) {
				int row = correlationMatrix.getRowIndexByName(qGene.getId());
				for (Gene tGene : targetGenes) {
					int col = correlationMatrix
							.getColIndexByName(tGene.getId());
					Collection<ExpressionProfile> queryEps = gene2eps
							.get(qGene);
					Collection<ExpressionProfile> targetEps = gene2eps
							.get(tGene);

					List<ExpressionProfilePair> epPairs = new ArrayList<ExpressionProfilePair>();
					if (queryEps != null && targetEps != null) {
						for (ExpressionProfile queryEp : queryEps) {
							for (ExpressionProfile targetEp : targetEps) {
								if (isValidExpressionProfile(queryEp)
										&& isValidExpressionProfile(targetEp)
										&& queryEp.getNumSamples() == targetEp
												.getNumSamples())
									epPairs.add(new ExpressionProfilePair(
											queryEp, targetEp));
							}
						}
						Collections.sort(epPairs);
					}

					if (epPairs.size() > 0) {
						ExpressionProfilePair epPair = epPairs.get(epPairs
								.size() / 2);
						correlationMatrix.set(slice, row, col,
								epPair.correlation);
						exprLvlMatrix.set(slice, row, col,
								epPair.expressionLevel);
						sampleSizeMatrix
								.set(slice, row, col, epPair.sampleSize);
					} else {
						correlationMatrix.set(slice, row, col, Double.NaN);
						exprLvlMatrix.set(slice, row, col, Double.NaN);
						sampleSizeMatrix.set(slice, row, col, Double.NaN);
					}
				}
			}
			watch.stop();
			log.info(ee.getShortName() + " (" + ++count + " of " + totalEes
					+ "): calculated correlation of "
					+ gene2eps.values().size() + " expression profiles in "
					+ watch.getTime() / 1000 + " seconds");
			watch.reset();
		}
		return matrices;
	}

	private boolean isValidExpressionProfile(ExpressionProfile ep) {
		return ep != null && ep.getRank() != null && ep.getRank() > MIN_EP_RANK
				&& ep.getNumValidSamples() > MIN_EP_NUM_SAMPLES;
	}

	public void setEeService(ExpressionExperimentService eeService) {
		this.eeService = eeService;
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

	public class CoexpressionMatrices {
		private DenseDoubleMatrix3DNamed correlationMatrix;
		private DenseDoubleMatrix3DNamed sampleSizeMatrix;
		private DenseDoubleMatrix3DNamed exprLvlMatrix;

		public CoexpressionMatrices(Collection<ExpressionExperiment> ees,
				Collection<Gene> queryGenes, Collection<Gene> targetGenes) {
			List eeNames = getEeNames(ees);
			List queryGeneNames = getGeneNames(queryGenes);
			List targetGeneNames = getGeneNames(targetGenes);

			correlationMatrix = new DenseDoubleMatrix3DNamed(eeNames,
					queryGeneNames, targetGeneNames);
			sampleSizeMatrix = new DenseDoubleMatrix3DNamed(eeNames,
					queryGeneNames, targetGeneNames);
			exprLvlMatrix = new DenseDoubleMatrix3DNamed(eeNames,
					queryGeneNames, targetGeneNames);
		}

		private List getEeNames(Collection<ExpressionExperiment> ees) {
			List eeNames = new ArrayList();
			for (ExpressionExperiment ee : ees) {
				eeNames.add(ee.getId());
			}
			return eeNames;
		}

		private List getGeneNames(Collection<Gene> genes) {
			List geneNames = new ArrayList();
			for (Gene gene : genes) {
				geneNames.add(gene.getId());
			}
			return geneNames;
		}

		public DenseDoubleMatrix3DNamed getCorrelationMatrix() {
			return correlationMatrix;
		}

		public void setCorrelationMatrix(
				DenseDoubleMatrix3DNamed correlationMatrix) {
			this.correlationMatrix = correlationMatrix;
		}

		public DenseDoubleMatrix3DNamed getExprLvlMatrix() {
			return exprLvlMatrix;
		}

		public void setExprLvlMatrix(DenseDoubleMatrix3DNamed exprLvlMatrix) {
			this.exprLvlMatrix = exprLvlMatrix;
		}

		public DenseDoubleMatrix3DNamed getSampleSizeMatrix() {
			return sampleSizeMatrix;
		}

		public void setSampleSizeMatrix(
				DenseDoubleMatrix3DNamed sampleSizeMatrix) {
			this.sampleSizeMatrix = sampleSizeMatrix;
		}
	}

	private class ExpressionProfilePair implements
			Comparable<ExpressionProfilePair> {
		ExpressionProfile ep1;
		ExpressionProfile ep2;
		double correlation;
		double expressionLevel;
		int sampleSize;

		public ExpressionProfilePair(ExpressionProfile ep1,
				ExpressionProfile ep2) {
			this.ep1 = ep1;
			this.ep2 = ep2;
			this.correlation = CorrelationStats.correl(ep1
					.getExpressionLevels(), ep2.getExpressionLevels());
			this.sampleSize = ep1.getExpressionLevels().length;
			this.expressionLevel = (ep1.getRank() + ep2.getRank()) / 2;
		}

		public int compareTo(ExpressionProfilePair epPair) {
			return correlation > epPair.correlation ? 1 : -1;
		}
	}

	public void setDedvService(DesignElementDataVectorService dedvService) {
		this.dedvService = dedvService;
	}
}
