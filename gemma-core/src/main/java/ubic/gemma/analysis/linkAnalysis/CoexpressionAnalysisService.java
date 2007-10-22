package ubic.gemma.analysis.linkAnalysis;

import hep.aida.ref.Histogram1D;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix3DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.io.reader.HistogramReader;
import ubic.basecode.io.writer.HistogramWriter;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.distribution.HistogramSampler;
import ubic.basecode.math.metaanalysis.CorrelationEffectMetaAnalysis;
import ubic.gemma.analysis.preprocess.filter.ExpressionExperimentFilter;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.ConfigUtils;
import cern.colt.list.DoubleArrayList;

/**
 * Coexpression analysis
 * 
 * @spring.bean id="coexpressionAnalysisService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @spring.property name="dedvService" ref="designElementDataVectorService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="csService" ref="compositeSequenceService"
 * @spring.property name="adService" ref="arrayDesignService"
 * @author Raymond
 */
public class CoexpressionAnalysisService {
	private ExpressionExperimentService eeService;

	private DesignElementDataVectorService dedvService;

	private CorrelationEffectMetaAnalysis metaAnalysis;

	private ArrayDesignService adService;

	private CompositeSequenceService csService;

	private GeneService geneService;

	private static final int NUM_HISTOGRAM_SAMPLES = 10000;

	private static final int NUM_HISTOGRAM_BINS = 2000;

	private static Log log = LogFactory
			.getLog(CoexpressionAnalysisService.class.getName());

	protected static final int MIN_NUM_USED = 5;

	/**
	 * Create an effect size service
	 */
	public CoexpressionAnalysisService() {
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

	public DoubleMatrixNamed foldCoexpressionMatrix(
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
					Gene gene1 = (Gene) matrix.getRowName(i);
					Gene gene2 = (Gene) matrix.getColName(j);
					GenePair genePair = new GenePair(gene1, gene2);
					foldedMatrix.addRowName(genePair);
					foldedMatrix.set(row, column, val);
				}
			}
		}
		return foldedMatrix;
	}

	public DoubleMatrixNamed calculateEffectSizeMatrix(
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
	 * Fold the 3D correlation matrix to a 2D matrix with maximum correlations
	 * 
	 * @param matrix -
	 *            correlation matrix
	 * @param n -
	 *            the Nth largest correlation
	 * @return matrix with Nth largest correlations
	 */
	public DoubleMatrixNamed getMaxCorrelationMatrix(
			DenseDoubleMatrix3DNamed matrix, int n) {
		log.info("Calculating " + n + "-max matrix");
		StopWatch watch = new StopWatch();
		watch.start();
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
				double val = Double.NaN;
				if (list.size() > n)
					val = list.get(list.size() - 1 - n);
				maxMatrix.set(i, j, val);
			}
		}
		watch.stop();
		log.info("Finished calculating " + n + "-max matrix in " + watch);
		return maxMatrix;
	}

	public DoubleMatrixNamed calculateMaxCorrelationPValueMatrix(
			DoubleMatrixNamed maxCorrelationMatrix, int n,
			Collection<ExpressionExperiment> ees) {
		log.info("Calculating " + n + "-max p-value matrix");
		StopWatch watch = new StopWatch();
		watch.start();
		DoubleMatrixNamed pMatrix = new DenseDoubleMatrix2DNamed(
				maxCorrelationMatrix.rows(), maxCorrelationMatrix.columns());
		pMatrix.setRowNames(maxCorrelationMatrix.getRowNames());
		pMatrix.setColumnNames(maxCorrelationMatrix.getColNames());

		// fill matrix with NaNs
		for (int i = 0; i < pMatrix.rows(); i++)
			for (int j = 0; j < pMatrix.columns(); j++)
				pMatrix.set(i, j, Double.NaN);

		// fill a histogram with the empirical distribution of max correlations
		Histogram1D hist = new Histogram1D(
				"Max correlation empirical distribution", NUM_HISTOGRAM_BINS,
				-1d, 1d);
		Collection<HistogramSampler> histSamplers = getHistogramSamplers(ees);
		for (int i = 0; i < NUM_HISTOGRAM_SAMPLES; i++) {
			DoubleArrayList samples = new DoubleArrayList(histSamplers.size());
			for (HistogramSampler sampler : histSamplers) {
				samples.add(sampler.nextSample());
			}
			samples.sort();
			if (samples.size() > n)
				hist.fill(samples.get(samples.size() - 1 - n));
		}

		HistogramWriter out = new HistogramWriter();
		try {
			out.write(hist, new FileWriter("hist.txt"));
		} catch (IOException e) {
		}

		// calculate the p-value
		for (int i = 0; i < maxCorrelationMatrix.rows(); i++) {
			for (int j = 0; j < maxCorrelationMatrix.columns(); j++) {
				double corr = maxCorrelationMatrix.get(i, j);
				if (Double.isNaN(corr) || corr == 0d)
					pMatrix.set(i, j, Double.NaN);
				else {
					double pVal = getPvalue(hist, corr);
					pMatrix.set(i, j, pVal);
				}
			}
		}
		watch.stop();
		log.info("Finished calculating " + n + "-max p-value matrix in " + watch);
		return pMatrix;
	}

	private double getPvalue(Histogram1D histogram, double x) {
		int bin = histogram.xAxis().coordToIndex(x);
		double sum = 0.0d;
		for (int i = 0; i <= bin; i++) {
			sum += histogram.binHeight(i);
		}
		if (sum == 0d)
			return 0d;
		else
			return sum / NUM_HISTOGRAM_SAMPLES;
	}

	public Collection<HistogramSampler> getHistogramSamplers(
			Collection<ExpressionExperiment> ees) {
		Collection<HistogramSampler> histSamplers = new HashSet<HistogramSampler>();
		for (ExpressionExperiment ee : ees) {
			String fileName = ConfigUtils.getAnalysisStoragePath()
					+ ee.getShortName() + ".correlDist.txt";
			try {
				HistogramSampler sampler = readHistogramFile(fileName);
				if (sampler == null)
					log.error("ERROR: " + ee.getShortName()
							+ " has an invalid correlation distribution");
				else
					histSamplers.add(sampler);
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
	private HistogramSampler readHistogramFile(String fileName)
			throws IOException {
		HistogramReader in = new HistogramReader(fileName,
				"Correlation Histogram");
		Histogram1D hist = in.read1D();
		HistogramSampler sampler = new HistogramSampler(hist);
		return sampler;
	}

	public Map<Gene, Collection<CompositeSequence>> getGene2CsMap(
			Collection<CompositeSequence> css) {
        // FIXME this used to return only known genes.
		Map<CompositeSequence, Collection<Gene>> cs2gene = geneService
				.getCS2GeneMap(css);
		// filter for specific cs 2 gene
		for (Iterator<Map.Entry<CompositeSequence, Collection<Gene>>> it = cs2gene
				.entrySet().iterator(); it.hasNext();) {
			Map.Entry<CompositeSequence, Collection<Gene>> entry = it.next();
			Collection<Gene> genes = entry.getValue();
			if (genes.size() > 1)
				it.remove();
		}

		// TODO: add service function for inverted map (gene2cs)
		Map<Gene, Collection<CompositeSequence>> gene2css = new HashMap<Gene, Collection<CompositeSequence>>();
		for (Map.Entry<CompositeSequence, Collection<Gene>> entry : cs2gene
				.entrySet()) {
			CompositeSequence cs = entry.getKey();
			Collection<Gene> genes = entry.getValue();
			for (Gene gene : genes) {
				Collection<CompositeSequence> c = gene2css.get(gene);
				if (c == null) {
					c = new HashSet<CompositeSequence>();
					gene2css.put(gene, c);
				}
				c.add(cs);
			}
		}
		return gene2css;
	}

	public ExpressionDataDoubleMatrix getExpressionDataMatrix(
			ExpressionExperiment ee, FilterConfig filterConfig) {
		StopWatch watch = new StopWatch();
		watch.start();
		log.info(ee.getShortName() + ": Getting expression data matrix");
		Collection<ArrayDesign> ads = eeService.getArrayDesignsUsed(ee);

		// get quantitation types
		Collection<QuantitationType> qts;
		qts = (Collection<QuantitationType>) eeService
				.getPreferredQuantitationType(ee);
		if (qts.size() < 1) {
			log.info(ee.getShortName() + ": No preferred quantitation types");
			return null;
		}

		// get dedvs to build expression data matrix
		Collection<DesignElementDataVector> dedvs;
		dedvs = eeService.getDesignElementDataVectors(ee, qts);
		dedvService.thaw(dedvs);

		// build and filter expression data matrix
		ExpressionExperimentFilter filter = new ExpressionExperimentFilter(ee,
				ads, filterConfig);
		ExpressionDataDoubleMatrix eeDoubleMatrix;
		try {
			eeDoubleMatrix = filter.getFilteredMatrix(dedvs);
		} catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}
		watch.stop();
		log.info("Retrieved expression data matrix in " + watch);
		return eeDoubleMatrix;
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
			Collection<Gene> targetGenes, FilterConfig filterConfig) {

		CoexpressionMatrices matrices = new CoexpressionMatrices(ees,
				queryGenes, targetGenes);
		DenseDoubleMatrix3DNamed correlationMatrix = matrices
				.getCorrelationMatrix();
		DenseDoubleMatrix3DNamed sampleSizeMatrix = matrices
				.getSampleSizeMatrix();
		int count = 1;
		int numEes = ees.size();
		// calculate correlations
		log.info("Calculating correlation and sample size matrices");
		StopWatch watch = new StopWatch();
		watch.start();
		for (ExpressionExperiment ee : ees) {
			log.info("Processing " + ee.getShortName() + " (" + count++
					+ " of " + numEes + ")");
			int slice = correlationMatrix.getSliceIndexByName(ee);

			// get all the composite sequences
			Collection<ArrayDesign> ads = eeService.getArrayDesignsUsed(ee);
			Collection<CompositeSequence> css = new HashSet<CompositeSequence>();
			for (ArrayDesign ad : ads) {
				css.addAll(adService.loadCompositeSequences(ad));
			}
			Map<Gene, Collection<CompositeSequence>> gene2css = getGene2CsMap(css);

			ExpressionDataDoubleMatrix dataMatrix = getExpressionDataMatrix(ee,
					filterConfig);
			if (dataMatrix == null) {
				log.error("ERROR: cannot process " + ee.getShortName());
				continue;
			}
			for (Gene qGene : queryGenes) {
				int row = correlationMatrix.getRowIndexByName(qGene);
				for (Gene tGene : targetGenes) {
					int col = correlationMatrix.getColIndexByName(tGene);
					Collection<CompositeSequence> queryCss = gene2css
							.get(qGene);
					Collection<CompositeSequence> targetCss = gene2css
							.get(tGene);

					if (queryCss != null && targetCss != null) {
						CorrelationSampleSize corr = calculateCorrelation(
								queryCss, targetCss, dataMatrix);
						if (corr != null) {
							correlationMatrix.set(slice, row, col,
									corr.correlation);
							sampleSizeMatrix.set(slice, row, col,
									corr.sampleSize);
						}
					}
				}
			}
		}
		watch.stop();
		log.info("Calculated correlations of all " + numEes + " in " + watch);
		return matrices;
	}

	private CorrelationSampleSize calculateCorrelation(
			Collection<CompositeSequence> queryCss,
			Collection<CompositeSequence> targetCss,
			ExpressionDataDoubleMatrix dataMatrix) {
		TreeMap<Double, Double> correlNumUsedMap = new TreeMap<Double, Double>();
		for (CompositeSequence queryCs : queryCss) {
			for (CompositeSequence targetCs : targetCss) {
				Double[] queryVals = dataMatrix.getRow(queryCs);
				Double[] targetVals = dataMatrix.getRow(targetCs);
				if (queryVals != null && targetVals != null) {
					double[] v1 = new double[queryVals.length];
					double[] v2 = new double[targetVals.length];
					for (int i = 0; i < queryVals.length; i++) {
						if (queryVals[i] != null)
							v1[i] = queryVals[i];
						else
							v1[i] = Double.NaN;
					}
					for (int i = 0; i < targetVals.length; i++) {
						if (targetVals[i] != null)
							v2[i] = targetVals[i];
						else
							v2[i] = Double.NaN;
					}

					int numUsed = 0;
					for (int i = 0; i < v1.length && i < v2.length; i++)
						if (!Double.isNaN(v1[i]) && !Double.isNaN(v2[i]))
							numUsed++;
					if (numUsed > MIN_NUM_USED) {
						double correlation = CorrelationStats.correl(v1, v2);
						correlNumUsedMap.put(correlation, (double) numUsed);
					}
				}
			}
		}
		if (correlNumUsedMap.size() == 0) {
			return null;
		}
		List<Double> correlations = new ArrayList<Double>(correlNumUsedMap
				.keySet());
		Double correlation = correlations.get(correlations.size() / 2);
		Double sampleSize = correlNumUsedMap.get(correlation);
		CorrelationSampleSize c = new CorrelationSampleSize();
		c.correlation = correlation;
		c.sampleSize = sampleSize;
		return c;

	}

	private class CorrelationSampleSize {
		double correlation;

		double sampleSize;
	}

	public void setEeService(ExpressionExperimentService eeService) {
		this.eeService = eeService;
	}

	public class CoexpressionMatrices {
		private DenseDoubleMatrix3DNamed correlationMatrix;

		private DenseDoubleMatrix3DNamed sampleSizeMatrix;

		private Map<ExpressionExperiment, String> eeNameMap;

		private Map<Gene, String> geneNameMap;

		public CoexpressionMatrices(Collection<ExpressionExperiment> ees,
				Collection<Gene> queryGenes, Collection<Gene> targetGenes) {
			List eeList = new ArrayList(ees);
			List qGeneList = new ArrayList(queryGenes);
			List tGeneList = new ArrayList(targetGenes);

			correlationMatrix = new DenseDoubleMatrix3DNamed(eeList, qGeneList,
					tGeneList);
			sampleSizeMatrix = new DenseDoubleMatrix3DNamed(eeList, qGeneList,
					tGeneList);
			// NaN matrices
			for (int k = 0; k < correlationMatrix.slices(); k++) {
				for (int i = 0; i < correlationMatrix.rows(); i++) {
					for (int j = 0; j < correlationMatrix.columns(); j++) {
						correlationMatrix.set(k, i, j, Double.NaN);
						sampleSizeMatrix.set(k, i, j, Double.NaN);
					}
				}
			}

			// generate name maps
			eeNameMap = new HashMap<ExpressionExperiment, String>();
			for (ExpressionExperiment ee : ees)
				eeNameMap.put(ee, ee.getShortName());

			geneNameMap = new HashMap<Gene, String>();
			for (Gene gene : queryGenes) {
				String name = gene.getOfficialSymbol();
				if (name == null)
					name = gene.getId().toString();
				geneNameMap.put(gene, name);
			}
			for (Gene gene : targetGenes) {
				String name = gene.getOfficialSymbol();
				if (name == null)
					name = gene.getId().toString();
				geneNameMap.put(gene, name);
			}
		}

		public DenseDoubleMatrix3DNamed getCorrelationMatrix() {
			return correlationMatrix;
		}

		public void setCorrelationMatrix(
				DenseDoubleMatrix3DNamed correlationMatrix) {
			this.correlationMatrix = correlationMatrix;
		}

		public DenseDoubleMatrix3DNamed getSampleSizeMatrix() {
			return sampleSizeMatrix;
		}

		public void setSampleSizeMatrix(
				DenseDoubleMatrix3DNamed sampleSizeMatrix) {
			this.sampleSizeMatrix = sampleSizeMatrix;
		}

		public Map<ExpressionExperiment, String> getEeNameMap() {
			return eeNameMap;
		}

		public Map<Gene, String> getGeneNameMap() {
			return geneNameMap;
		}
	}

	public class GenePair {
		private Gene gene1;

		private Gene gene2;

		public GenePair(Gene gene1, Gene gene2) {
			this.gene1 = gene1;
			this.gene2 = gene2;
		}

		public String toString() {
			String s1 = gene1.getOfficialSymbol();
			String s2 = gene2.getOfficialSymbol();
			if (s1 == null)
				s1 = gene1.getId().toString();
			if (s2 == null)
				s2 = gene2.getId().toString();
			return s1 + ":" + s2;
		}
	}

	public void setDedvService(DesignElementDataVectorService dedvService) {
		this.dedvService = dedvService;
	}

	public void setGeneService(GeneService geneService) {
		this.geneService = geneService;
	}

	public ArrayDesignService getAdService() {
		return adService;
	}

	public void setAdService(ArrayDesignService adService) {
		this.adService = adService;
	}

	public void setCsService(CompositeSequenceService csService) {
		this.csService = csService;
	}
}
