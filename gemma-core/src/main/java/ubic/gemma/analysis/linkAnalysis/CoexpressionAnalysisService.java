package ubic.gemma.analysis.linkAnalysis;

import hep.aida.ref.Histogram1D;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix3DNamed;
import ubic.basecode.io.reader.HistogramReader;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.distribution.HistogramSampler;
import ubic.basecode.math.metaanalysis.CorrelationEffectMetaAnalysis;
import ubic.gemma.analysis.coexpression.ExpressionProfile;
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
 * @spring.property name="adService" ref="arrayDesignService"
 * @author Raymond
 */
public class CoexpressionAnalysisService {
    private ExpressionExperimentService eeService;

    private DesignElementDataVectorService dedvService;

    private CorrelationEffectMetaAnalysis metaAnalysis;


    private ArrayDesignService adService;
    private GeneService geneService;
    
    private static final int NUM_HISTOGRAM_SAMPLES = 1000;
    
    private static final int NUM_HISTOGRAM_BINS = 2000;

    private static Log log = LogFactory.getLog( CoexpressionAnalysisService.class.getName() );

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
     * Filter out expression profiles for genes with relatively low expression levels
     */
    protected static final double MIN_EP_RANK = 0.3;

    /**
     * Minimum number of expression experiments for the figure
     */
    protected static final int MIN_FIGURE_GENE_PAIR_EE_NUM = 100;

    /**
     * Create an effect size service
     */
    public CoexpressionAnalysisService() {
        metaAnalysis = new CorrelationEffectMetaAnalysis( true, false );
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

    public DenseDoubleMatrix3DNamed filterCoexpressionMatrix( DenseDoubleMatrix3DNamed matrix ) {
        log.info( "Filtering expression experiments..." );
        List<Long> filteredEeIds = new ArrayList<Long>( matrix.slices() );
        EE: for ( Object eeId : matrix.getSliceNames() ) {
            int slice = matrix.getSliceIndexByName( eeId );
            for ( int i = 0; i < matrix.rows(); i++ )
                for ( int j = 0; j < matrix.columns(); j++ )
                    if ( !matrix.isMissing( slice, i, j ) ) {
                        filteredEeIds.add( ( Long ) eeId );
                        continue EE;
                    }
        }
        log.info( filteredEeIds.size() + " of " + matrix.slices() + " passed" );

        DenseDoubleMatrix3DNamed filteredMatrix = new DenseDoubleMatrix3DNamed( filteredEeIds.size(), matrix.rows(),
                matrix.columns() );
        filteredMatrix.setSliceNames( filteredEeIds );
        for ( int i = 0; i < filteredEeIds.size(); i++ ) {
            Long eeId = filteredEeIds.get( i );
            int slice = filteredMatrix.getSliceIndexByName( eeId );
            for ( int j = 0; j < matrix.rows(); j++ ) {
                for ( int k = 0; k < matrix.columns(); k++ ) {
                    double val = matrix.get( matrix.getSliceIndexByName( eeId ), j, k );
                    filteredMatrix.set( slice, j, k, val );
                }
            }
        }

        return filteredMatrix;

    }

    public DenseDoubleMatrix2DNamed foldCoexpressionMatrix( DenseDoubleMatrix3DNamed matrix ) {
        DenseDoubleMatrix2DNamed foldedMatrix = new DenseDoubleMatrix2DNamed( matrix.rows() * matrix.columns(), matrix
                .slices() );
        foldedMatrix.setColumnNames( matrix.getSliceNames() );
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                for ( int k = 0; k < matrix.slices(); k++ ) {
                    int row = i * matrix.columns() + j;
                    int column = k;
                    double val = matrix.get( k, i, j );
                    foldedMatrix.addRowName( matrix.getRowName( i ) + ":" + matrix.getColName( j ) );
                    foldedMatrix.set( row, column, val );
                }
            }
        }
        return foldedMatrix;
    }

    private Map<DesignElementDataVector, Collection<Gene>> getSpecificDedv2gene(
            Map<DesignElementDataVector, Collection<Gene>> dedv2genes ) {
        Map<DesignElementDataVector, Collection<Gene>> specificDedv2gene = new HashMap<DesignElementDataVector, Collection<Gene>>();
        for ( DesignElementDataVector dedv : dedv2genes.keySet() ) {
            if ( dedv2genes.get( dedv ).size() == 1 ) {
                specificDedv2gene.put( dedv, dedv2genes.get( dedv ) );
            }
        }
        return specificDedv2gene;
    }

    /**
     * Get a gene ID to expression profiles map for an expression experiment (specified by the quantitation type)
     * 
     * @param genes - genes to map
     * @param qt - quantitation type of the expression experiment
     * @return gene ID to expression profile map
     */
    private Map<Gene, Collection<DesignElementDataVector>> getGene2DedvsMap( Collection<DesignElementDataVector> dedvs,
            QuantitationType qt ) {
        Map<DesignElementDataVector, Collection<Gene>> dedv2genes = dedvService.getDedv2GenesMap( dedvs, qt );
        dedv2genes = getSpecificDedv2gene( dedv2genes );

        Map<Gene, Collection<DesignElementDataVector>> gene2dedvs = new HashMap<Gene, Collection<DesignElementDataVector>>();
        // build genes to expression profiles map
        for ( DesignElementDataVector dedv : dedv2genes.keySet() ) {
            Collection<Gene> genes = dedv2genes.get( dedv );
            for ( Gene gene : genes ) {
                Collection<DesignElementDataVector> vectors = gene2dedvs.get( gene );
                if ( vectors == null ) {
                    vectors = new HashSet<DesignElementDataVector>();
                    gene2dedvs.put( gene, vectors );
                }
                vectors.add( dedv );
            }
        }
        return gene2dedvs;
    }

    public DenseDoubleMatrix2DNamed calculateEffectSizeMatrix( DenseDoubleMatrix3DNamed correlationMatrix,
            DenseDoubleMatrix3DNamed sampleSizeMatrix ) {
        DenseDoubleMatrix2DNamed matrix = new DenseDoubleMatrix2DNamed( correlationMatrix.rows(), correlationMatrix
                .columns() );
        matrix.setRowNames( correlationMatrix.getRowNames() );
        matrix.setColumnNames( correlationMatrix.getColNames() );

        for ( Object rowId : correlationMatrix.getRowNames() ) {
            int rowIndex = matrix.getRowIndexByName( rowId );
            for ( Object colId : correlationMatrix.getColNames() ) {
                int colIndex = matrix.getColIndexByName( colId );
                DoubleArrayList correlations = new DoubleArrayList( correlationMatrix.slices() );
                DoubleArrayList sampleSizes = new DoubleArrayList( correlationMatrix.slices() );
                for ( Object sliceId : correlationMatrix.getSliceNames() ) {
                    int sliceIndex = correlationMatrix.getSliceIndexByName( sliceId );
                    double correlation = correlationMatrix.get( sliceIndex, rowIndex, colIndex );
                    double sampleSize = sampleSizeMatrix.get( sliceIndex, rowIndex, colIndex );
                    correlations.add( correlation );
                    sampleSizes.add( sampleSize );
                }
                metaAnalysis.run( correlations, sampleSizes );
                double effectSize = metaAnalysis.getE();
                matrix.set( rowIndex, colIndex, effectSize );
            }
        }
        return matrix;
    }

    /**
     * Fold the 3D correlation matrix to a 2D matrix with maximum correlations
     * 
     * @param matrix - correlation matrix
     * @param n - the Nth largest correlation
     * @return matrix with Nth largest correlations
     */
    public DenseDoubleMatrix2DNamed getMaxCorrelationMatrix( DenseDoubleMatrix3DNamed matrix, int n ) {
        DenseDoubleMatrix2DNamed maxMatrix = new DenseDoubleMatrix2DNamed( matrix.rows(), matrix.columns() );
        maxMatrix.setRowNames( matrix.getRowNames() );
        maxMatrix.setColumnNames( matrix.getColNames() );
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                DoubleArrayList list = new DoubleArrayList();
                for ( int k = 0; k < matrix.slices(); k++ ) {
                    double val = matrix.get( k, i, j );
                    if ( !Double.isNaN( val ) ) {
                        list.add( val );
                    }
                }
                list.sort();
                double val = Double.NaN;
                if ( list.size() > n ) val = list.get( list.size() - 1 - n );
                maxMatrix.set( i, j, val );
            }
        }
        return maxMatrix;
    }
    
    public DenseDoubleMatrix2DNamed calculateMaxCorrelationPValueMatrix(DenseDoubleMatrix2DNamed maxCorrelationMatrix, int n, Collection<ExpressionExperiment> ees) {
    	DenseDoubleMatrix2DNamed pMatrix = new DenseDoubleMatrix2DNamed(maxCorrelationMatrix.rows(), maxCorrelationMatrix.columns());
    	pMatrix.setRowNames(maxCorrelationMatrix.getRowNames());
    	pMatrix.setColumnNames(maxCorrelationMatrix.getColNames());
    	
    	// fill a histogram with the empirical distribution of max correlations
    	Histogram1D hist = new Histogram1D("Max correlation empirical distribution", NUM_HISTOGRAM_BINS, -1d, 1d);
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
    	
    	// calculate the p-value
    	for (int i = 0; i < maxCorrelationMatrix.rows(); i++) {
    		for (int j = 0; j < maxCorrelationMatrix.columns(); j++) {
    			double corr = maxCorrelationMatrix.get(i, j);
    			if (Double.isNaN(corr))
    				pMatrix.set(i, j, Double.NaN);
    			else {
        			double pVal = getPvalue(hist, corr);
        			pMatrix.set(i, j, pVal);
    			}
    		}
        }
    	return pMatrix;
    	
    }
    
    private double getPvalue(Histogram1D histogram, double x) {
    	int bin = histogram.xAxis().coordToIndex(x);
    	double pVal = 0.0d;
    	for (int i = 0; i <= bin; i++) {
    		pVal += histogram.binHeight(i);
    	}
    	return pVal;
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
		HistogramReader in = new HistogramReader(fileName, "Correlation Histogram");
		Histogram1D hist = in.read1D();
		HistogramSampler sampler = new HistogramSampler(hist);
		return sampler;
	}


    /**
     * Create and populate the coexpression matrices (correlation matrix, sample size matrix, expression level matrix)
     * 
     * @param ees
     * @param queryGenes
     * @param targetGenes
     * @return
     */
    public CoexpressionMatrices calculateCoexpressionMatrices( Collection<ExpressionExperiment> ees,
            Collection<Gene> queryGenes, Collection<Gene> targetGenes, FilterConfig filterConfig ) {
        CoexpressionMatrices matrices = new CoexpressionMatrices( ees, queryGenes, targetGenes );
        DenseDoubleMatrix3DNamed correlationMatrix = matrices.getCorrelationMatrix();
        DenseDoubleMatrix3DNamed sampleSizeMatrix = matrices.getSampleSizeMatrix();
        int count = 0;
        int totalEes = ees.size();
        for ( ExpressionExperiment ee : ees ) {
            StopWatch watch = new StopWatch();
            watch.start();
            log.info( ee.getShortName() + ": Calculating coexpression matrices" );
            Collection<ArrayDesign> ads = eeService.getArrayDesignsUsed( ee );

            Collection<CompositeSequence> css = new HashSet<CompositeSequence>();
            for ( ArrayDesign ad : ads ) {
                css.addAll( adService.loadCompositeSequences( ad ) );
            }

            // get quantitation types
            Collection<QuantitationType> qts;
            qts = ( Collection<QuantitationType> ) eeService.getPreferredQuantitationType( ee );
            if ( qts.size() < 1 ) {
            	log.info(ee.getShortName() + ": No preferred quantitation types, skipping");
                continue;
            }

            // get dedvs to build expression data matrix
            Collection<DesignElementDataVector> dedvs;
            dedvs = eeService.getDesignElementDataVectors( ee, qts);
            dedvService.thaw(dedvs);

            Map<Gene, Collection<CompositeSequence>> gene2css = geneService.getCS2GeneMap(css);

            // build and filter expression data matrix
            ExpressionExperimentFilter filter = new ExpressionExperimentFilter( ee, ads, filterConfig );
            ExpressionDataDoubleMatrix eeDoubleMatrix;
            try {
                eeDoubleMatrix = filter.getFilteredMatrix( dedvs );
            } catch (Exception e) {
            	log.error(e.getMessage());
            	continue;
            }
            int slice = correlationMatrix.getSliceIndexByName( ee.getId() );

            for ( Gene qGene : queryGenes ) {
                int row = correlationMatrix.getRowIndexByName( qGene.getId() );
                for ( Gene tGene : targetGenes ) {
                    int col = correlationMatrix.getColIndexByName( tGene.getId() );
                    Collection<CompositeSequence> queryCss = gene2css.get( qGene );
                    Collection<CompositeSequence> targetCss = gene2css.get( tGene );

                    List<ExpressionProfilePair> epPairs = new ArrayList<ExpressionProfilePair>();
                    if ( queryCss != null && targetCss != null ) {
                        for ( CompositeSequence queryCs : queryCss ) {
                            for ( CompositeSequence targetCs : targetCss ) {
                                ExpressionProfile queryEp = new ExpressionProfile( queryCs.getId(), null,
                                        eeDoubleMatrix.getRow( queryCs ) );
                                ExpressionProfile targetEp = new ExpressionProfile( targetCs.getId(), null,
                                        eeDoubleMatrix.getRow( targetCs ) );
                                if ( isValidExpressionProfile( queryEp ) && isValidExpressionProfile( targetEp )
                                        && queryEp.getNumSamples() == targetEp.getNumSamples() )
                                    epPairs.add( new ExpressionProfilePair( queryEp, targetEp ) );
                            }
                        }
                        Collections.sort( epPairs );
                    }

                    if ( epPairs.size() > 0 ) {
                        ExpressionProfilePair epPair = epPairs.get( epPairs.size() / 2 );
                        correlationMatrix.set( slice, row, col, epPair.correlation );
                        sampleSizeMatrix.set( slice, row, col, epPair.sampleSize );
                    } else {
                        correlationMatrix.set( slice, row, col, Double.NaN );
                        sampleSizeMatrix.set( slice, row, col, Double.NaN );
                    }
                }
            }
            watch.stop();
            log.info( ee.getShortName() + " (" + ++count + " of " + totalEes + "): calculated correlation of "
                    + css.size() + " expression profiles in " + watch );
            watch.reset();
        }
        return matrices;
    }

    private boolean isValidExpressionProfile( ExpressionProfile ep ) {
        return ep != null && ep.getRank() != null && ep.getRank() > MIN_EP_RANK
                && ep.getNumValidSamples() > MIN_EP_NUM_SAMPLES;
    }

    public void setEeService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }

    protected class GenePair {
        private long firstId;

        private long secondId;

        /**
         * Construct a gene pair with the specified pair of IDs and count
         * 
         * @param id1 - ID of first gene
         * @param id2 - ID of second gene
         */
        public GenePair( long id1, long id2 ) {
            this.firstId = id1;
            this.secondId = id2;
        }

        public long getFirstId() {
            return firstId;
        }

        public void setFirstId( long firstId ) {
            this.firstId = firstId;
        }

        public long getSecondId() {
            return secondId;
        }

        public void setSecondId( long secondId ) {
            this.secondId = secondId;
        }
    }

    public class CoexpressionMatrices {
        private DenseDoubleMatrix3DNamed correlationMatrix;
        private DenseDoubleMatrix3DNamed sampleSizeMatrix;

        public CoexpressionMatrices( Collection<ExpressionExperiment> ees, Collection<Gene> queryGenes,
                Collection<Gene> targetGenes ) {
            List eeNames = getEeNames( ees );
            List queryGeneNames = getGeneNames( queryGenes );
            List targetGeneNames = getGeneNames( targetGenes );

            correlationMatrix = new DenseDoubleMatrix3DNamed( eeNames, queryGeneNames, targetGeneNames );
            sampleSizeMatrix = new DenseDoubleMatrix3DNamed( eeNames, queryGeneNames, targetGeneNames );
        }

        private List getEeNames( Collection<ExpressionExperiment> ees ) {
            List eeNames = new ArrayList();
            for ( ExpressionExperiment ee : ees ) {
                eeNames.add( ee.getId() );
            }
            return eeNames;
        }

        private List getGeneNames( Collection<Gene> genes ) {
            List geneNames = new ArrayList();
            for ( Gene gene : genes ) {
                geneNames.add( gene.getId() );
            }
            return geneNames;
        }

        public DenseDoubleMatrix3DNamed getCorrelationMatrix() {
            return correlationMatrix;
        }

        public void setCorrelationMatrix( DenseDoubleMatrix3DNamed correlationMatrix ) {
            this.correlationMatrix = correlationMatrix;
        }

        public DenseDoubleMatrix3DNamed getSampleSizeMatrix() {
            return sampleSizeMatrix;
        }

        public void setSampleSizeMatrix( DenseDoubleMatrix3DNamed sampleSizeMatrix ) {
            this.sampleSizeMatrix = sampleSizeMatrix;
        }
    }

    private class ExpressionProfilePair implements Comparable<ExpressionProfilePair> {
        ExpressionProfile ep1;
        ExpressionProfile ep2;
        double correlation;
        double expressionLevel;
        int sampleSize;

        public ExpressionProfilePair( ExpressionProfile ep1, ExpressionProfile ep2 ) {
            this.ep1 = ep1;
            this.ep2 = ep2;
            double[] v1 = new double[ep1.getExpressionLevels().length];
            double[] v2 = new double[ep2.getExpressionLevels().length];
            for ( int i = 0; i < v1.length; i++ )
                v1[i] = ep1.getExpressionLevels()[i];
            for ( int i = 0; i < v2.length; i++ )
                v2[i] = ep2.getExpressionLevels()[i];
            this.correlation = CorrelationStats.correl( v1, v2 );
            this.sampleSize = ep1.getExpressionLevels().length;
        }

        public int compareTo( ExpressionProfilePair epPair ) {
            return correlation > epPair.correlation ? 1 : -1;
        }
    }

    public void setDedvService( DesignElementDataVectorService dedvService ) {
        this.dedvService = dedvService;
    }

    public void setGeneService(GeneService geneService) {
		this.geneService = geneService;
	}

	public ArrayDesignService getAdService() {
        return adService;
    }

    public void setAdService( ArrayDesignService adService ) {
        this.adService = adService;
    }
}
