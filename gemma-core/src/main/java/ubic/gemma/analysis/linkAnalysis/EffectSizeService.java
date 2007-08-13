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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix3DNamed;
import ubic.basecode.gui.ColorMap;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.distribution.HistogramSampler;
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
import ubic.gemma.util.ConfigUtils;
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

	private CorrelationEffectMetaAnalysis metaAnalysis;

	private ArrayDesignService adService;

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

	// /**
	// * Save expression profiles to the specified file. Requires reading
	// expression profiles for each gene pair, which
	// * may take a while.
	// *
	// * @param fileName - file to save to
	// * @param genePairs - gene pairs to save
	// * @param EEs - expression experiments
	// * @throws IOException
	// */
	// public void saveExprProfilesToFile( String fileName, DenseDou
	// Collection<ExpressionExperiment> EEs ) throws IOException {
	// log.info( "Saving expression profiles to " + fileName );
	// Map<Long, Gene> geneMap = getGeneMapFromGenePairs( genePairs );
	// BufferedWriter out = new BufferedWriter( new FileWriter( fileName ) );
	// for ( GenePair genePair : genePairs ) {
	// Gene gene1 = geneMap.get( genePair.getFirstId() );
	// Gene gene2 = geneMap.get( genePair.getSecondId() );
	// for ( ExpressionExperiment EE : EEs ) {
	// long eeId = EE.getId();
	// Long dedvId1 = genePair.getFirstDedvId( eeId );
	// Long dedvId2 = genePair.getSecondDedvId( eeId );
	// if ( dedvId1 == null || dedvId2 == null ) continue;
	// DesignElementDataVector dedv1 = dedvService.load( dedvId1 );
	// DesignElementDataVector dedv2 = dedvService.load( dedvId2 );
	// ExpressionProfile ep1 = new ExpressionProfile( dedv1 );
	// ExpressionProfile ep2 = new ExpressionProfile( dedv2 );
	//
	// String line = gene1.getOfficialSymbol() + ":" + gene2.getOfficialSymbol()
	// + "\t" + EE.getShortName()
	// + "\t" + genePair.getCorrelation( eeId ) + "\n";
	// out.write( line );
	// line = ep1.getId() + "";
	// for ( double d : ep1.getExpressionLevels() ) {
	// line += "\t" + d;
	// }
	// line += "\n";
	// out.write( line );
	// line = ep2.getId() + "";
	// for ( double d : ep2.getExpressionLevels() ) {
	// line += "\t" + d;
	// }
	// line += "\n";
	// out.write( line );
	// out.flush();
	// }
	// }
	// out.close();
	// log.info( "Finished saving expression profiles" );
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
		for (int i = 0; i < filteredEeIds.size() ;i++ ) {
			Long eeId = filteredEeIds.get(i);
			int slice = filteredMatrix.getSliceIndexByName(eeId);
			for (int j = 0; j < matrix.rows(); j++) {
				for (int k = 0; k < matrix.columns(); k++) {
					double val = matrix.get(matrix.getSliceIndexByName(eeId), j, k);
					filteredMatrix.set(slice, j, k, val);
				}
			}
		}

		return filteredMatrix;

	}

	private List<Long> filterExpressionExperiments(
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
		return filteredEeIds;
	}

	// public void saveToFile(String fileName, DenseDoubleMatrix2DNamed matrix,
	// boolean geneSymbol) throws IOException {
	// PrintWriter out = new PrintWriter( new FileWriter( fileName ) );
	// DecimalFormat formatter = new DecimalFormat( "0.0000" );
	// Map<Long, Gene> geneMap = getGeneMap( matrix.getRowNames(),
	// matrix.getColNames() );
	//        
	// for ( Object gene1Id : matrix.getRowNames() ) {
	// int row = matrix.getRowIndexByName( gene1Id );
	// String gene1 = geneSymbol ? geneMap.get( gene1Id ).getOfficialSymbol() :
	// gene1Id.toString();
	// for ( Object gene2Id : matrix.getColNames() ) {
	// int col = matrix.getColIndexByName( gene2Id );
	// String gene2 = geneSymbol ? geneMap.get( gene2Id ).getOfficialSymbol() :
	// gene2Id.toString();
	// if ( gene1 == null || gene2 == null ) continue;
	// String genePair = gene1 + ":" + gene2;
	// // remove dupes
	// if ( pairs.contains( genePair ) ) continue;
	// pairs.add( genePair );
	//
	// String line = genePair;
	// for ( Long eeId : filteredEEs ) {
	// int slice = matrix.getSliceIndexByName( eeId );
	// line += "\t";
	// double val = matrix.get( slice, row, col );
	// line += formatter.format( val );
	// }
	// out.println( line );
	// out.flush();
	// }
	// }
	//        
	// }

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

	public void saveToFile(String fileName, DenseDoubleMatrix3DNamed matrix,
			boolean geneSymbol) throws IOException {
		DecimalFormat formatter = new DecimalFormat("0.0000");
		Map<Long, Gene> geneMap = getGeneMap(matrix.getRowNames(), matrix
				.getColNames());
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
					double val = matrix.get(slice, row, col);
					line += formatter.format(val);
				}
				out.println(line);
				out.flush();
			}
		}
		out.close();
	}

	private String getGeneName(Gene gene) {
		String officialSymbol = gene.getOfficialSymbol();
		return (officialSymbol == null) ? gene.getId().toString()
				: officialSymbol;
	}

	/**
	 * Save the gene pairs as a figure to the specified file name
	 * 
	 * @param figureFileName -
	 *            file name
	 * @param matrix -
	 *            matrix to save
	 * @throws IOException
	 */
	public void saveToFigure(String figureFileName,
			DenseDoubleMatrix3DNamed matrix) throws IOException {
		Map<Long, Gene> geneMap = getGeneMap(matrix.getRowNames(), matrix
				.getColNames());
		Map<Long, ExpressionExperiment> eeMap = getEeMap(matrix.getSliceNames());
		List<Long> eeIds = filterExpressionExperiments(matrix);

		int numGenePairs = matrix.rows() * matrix.columns();
		int numEes = eeIds.size();
		double[][] data = new double[numGenePairs][numEes];

		List rowNames = new ArrayList();
		List colNames = new ArrayList();
		for (int i = 0; i < matrix.rows(); i++) {
			Gene gene1 = geneMap.get(matrix.getRowName(i));
			for (int j = 0; j < matrix.columns(); j++) {
				Gene gene2 = geneMap.get(matrix.getColName(j));
				String rowName = getGeneName(gene1) + ":" + getGeneName(gene2);
				rowNames.add(rowName);
				for (int k = 0; k < eeIds.size(); k++) {
					colNames.add(eeMap.get(eeIds.get(k)));
					int slice = matrix.getSliceIndexByName(eeIds.get(k));
					double correlation = matrix.get(slice, i, j);
					data[i * matrix.columns() + j][k] = correlation;
				}
			}
		}

		DenseDoubleMatrix2DNamed dataMatrix = new DenseDoubleMatrix2DNamed(data);
		dataMatrix.setRowNames(rowNames);
		dataMatrix.setColumnNames(colNames);

		ColorMatrix dataColorMatrix = new ColorMatrix(dataMatrix);
		dataColorMatrix.setColorMap(ColorMap.GREENRED_COLORMAP);
		JMatrixDisplay dataMatrixDisplay = new JMatrixDisplay(dataColorMatrix);
		dataMatrixDisplay.saveImage(figureFileName, true);
		log.info("Saved correlation image to " + figureFileName);
	}

	/**
	 * Create and return a gene ID to gene map
	 * 
	 * @param ids -
	 *            gene IDs
	 * @return gene ID to gene map
	 */
	private Map<Long, Gene> getGeneMap(Collection<Long> queryGeneIds,
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
				allGenes.addAll(geneService.loadMultiple(idsInOneChunk));
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
				.loadMultiple(eeIds)) {
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

	private List<Long> getEeIds(Collection<ExpressionExperiment> EEs) {
		List<Long> eeIds = new ArrayList<Long>(EEs.size());
		for (ExpressionExperiment ee : EEs)
			eeIds.add(ee.getId());
		return eeIds;
	}

	/**
	 * Reads in a correlation matrix and randomises the correlations using the
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
				correlationMatrix.slices(), correlationMatrix.rows(),
				correlationMatrix.columns());
		Collection<Long> eeIds = correlationMatrix.getSliceNames();
		Map<Long, HistogramSampler> histSamplerMap = getHistogramSamplerMap(eeIds);
		for (Long eeId : eeIds) {
			int slice = correlationMatrix.getSliceIndexByName(eeId);
			HistogramSampler sampler = histSamplerMap.get(eeId);
			for (int row = 0; row < correlationMatrix.rows(); row++) {
				for (int col = 0; col < correlationMatrix.columns(); col++) {
					double correlation = correlationMatrix.get(slice, row, col);
					if (!Double.isNaN(correlation)) {
						double randCorrelation = sampler.nextSample();
						randCorrMatrix.set(slice, row, col, randCorrelation);
					}

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
		for (int i = 0; i < matrix.rows(); i++) {
			for (int j = 0; j < matrix.columns(); j++) {
				DoubleArrayList list = new DoubleArrayList();
				for (int k = 0; k < matrix.slices(); k++) {
					list.add(matrix.get(k, i, j));
				}
				list.sort();
				if (list.size() > 0)
					maxMatrix.set(i, j, list.get(list.size() - 1 - n));
			}
		}
		return maxMatrix;
	}

	private Map<Long, HistogramSampler> getHistogramSamplerMap(
			Collection<Long> eeIds) {
		Map<Long, ExpressionExperiment> eeMap = getEeMap(eeIds);
		Map<Long, HistogramSampler> histSamplers = new HashMap<Long, HistogramSampler>();
		for (Long eeId : eeIds) {
			ExpressionExperiment ee = eeMap.get(eeId);
			String fileName = ConfigUtils.getAnalysisStoragePath()
					+ ee.getShortName() + ".correlDist.txt";
			try {
				HistogramSampler sampler = readHistogramFile(fileName);
				histSamplers.put(ee.getId(), sampler);
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

	public CoexpressionMatrices calculateCoexpressionMatrices(
			Collection<ExpressionExperiment> EEs, List<Long> queryGeneIds,
			List<Long> targetGeneIds) {
		List<Long> eeIds = getEeIds(EEs);
		CoexpressionMatrices matrices = new CoexpressionMatrices(eeIds,
				queryGeneIds, targetGeneIds);
		DenseDoubleMatrix3DNamed correlationMatrix = matrices
				.getCorrelationMatrix();
		DenseDoubleMatrix3DNamed sampleSizeMatrix = matrices
				.getSampleSizeMatrix();
		DenseDoubleMatrix3DNamed exprLvlMatrix = matrices.getExprLvlMatrix();
		Map<Long, Gene> geneMap = getGeneMap(queryGeneIds, targetGeneIds);
		int count = 0;
		int totalEEs = EEs.size();
		StopWatch watch = new StopWatch();
		for (ExpressionExperiment ee : EEs) {
			watch.start();
			Map<Long, Collection<ExpressionProfile>> geneId2Eps = getGeneID2EPsMap(
					geneMap, ee);
			if (geneId2Eps == null)
				continue;
			int slice = correlationMatrix.getSliceIndexByName(ee.getId());

			for (Long qGeneId : queryGeneIds) {
				int row = correlationMatrix.getRowIndexByName(qGeneId);
				for (Long tGeneId : targetGeneIds) {
					int col = correlationMatrix.getColIndexByName(tGeneId);
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
						correlationMatrix.set(slice, row, col,
								epPair.correlation);
						exprLvlMatrix.set(slice, row, col,
								epPair.expressionLevel);
						sampleSizeMatrix
								.set(slice, row, col, epPair.sampleSize);
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
		return matrices;
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

	public class CoexpressionMatrices {
		private DenseDoubleMatrix3DNamed correlationMatrix;
		private DenseDoubleMatrix3DNamed sampleSizeMatrix;
		private DenseDoubleMatrix3DNamed exprLvlMatrix;

		public CoexpressionMatrices(List eeIds, List targetGeneIds,
				List queryGeneIds) {
			correlationMatrix = new DenseDoubleMatrix3DNamed(eeIds,
					queryGeneIds, targetGeneIds);
			sampleSizeMatrix = new DenseDoubleMatrix3DNamed(eeIds,
					queryGeneIds, targetGeneIds);
			exprLvlMatrix = new DenseDoubleMatrix3DNamed(eeIds, queryGeneIds,
					targetGeneIds);
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
		long epId1;
		long epId2;
		double correlation;
		double expressionLevel;
		int sampleSize;

		public ExpressionProfilePair(long epId1, long epId2,
				double correlation, double expressionLevel, int sampleSize) {
			super();
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
