package ubic.gemma.analysis.linkAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMap;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.metaanalysis.CorrelationEffectMetaAnalysis;
import ubic.gemma.analysis.coexpression.GeneCoExpressionAnalysis;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import cern.colt.list.DoubleArrayList;

/**
 * Effect size calculation service
 * 
 * @spring.bean id="effectSizeService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 * @author Raymond
 */
public class EffectSizeService {
    private ExpressionExperimentService eeService;

    private GeneService geneService;

    private CorrelationEffectMetaAnalysis metaAnalysis;

    private ByteArrayConverter bac;

    private static Log log = LogFactory.getLog( EffectSizeService.class.getName() );

    private static final int MINIMUM_SAMPLE_SIZE = 3;

    private static final int CHUNK_SIZE = 500;
    
    public EffectSizeService() {
        metaAnalysis = new CorrelationEffectMetaAnalysis(true, false);
        bac = new ByteArrayConverter();
    }

    /**
     * Read in gene pair IDs from the geneList into genePairs if they meet the stringency requirements, i.e. if the
     * pairing is seen in enough other datasets.
     * 
     * @param geneListFile - gene list file name
     * @param stringency - minimum stringency required for a gene pair
     * @return list of gene pairs
     */
    public Collection<GenePair> readGenePairsByID( String geneListFile, int stringency ) throws IOException {
        log.info( "Reading gene pairs by ID from " + geneListFile );
        Collection<GenePair> genePairs = new HashSet<GenePair>();
        BufferedReader in = new BufferedReader( new FileReader( new File( geneListFile ) ) );
        String row = null;
        while ( ( row = in.readLine() ) != null && !row.startsWith( "#" ) ) {
            row = row.trim();
            if ( StringUtils.isBlank( row ) ) continue;
            String[] subItems = row.split( "\t" );
            long firstId = Long.valueOf( subItems[0] );
            long secondId = Long.valueOf( subItems[1] );
            int count = Integer.valueOf( subItems[2] );
            if ( count >= stringency ) {
                genePairs.add( new GenePair( firstId, secondId ) );
            }
        }
        in.close();
        return genePairs;
    }

    /**
     * Read a file containing a pair of genes (official symbols) per line
     * 
     * @param geneListFile - gene list file name
     * @return a collection (hash set) of gene pairs
     * @throws IOException
     */
    public Collection<GenePair> readGenePairsByOfficialSymbol( String geneListFile ) throws IOException {
        log.info( "Reading gene pairs by official symbol from " + geneListFile );
        Collection<GenePair> genePairs = new HashSet<GenePair>();
        BufferedReader in = new BufferedReader( new FileReader( geneListFile ) );
        String line = null;
        while ( ( line = in.readLine() ) != null ) {
            if ( line.startsWith( "#" ) ) {
                continue;
            }
            String[] symbols = line.trim().split( "\t" );
            if ( symbols.length < 2 ) {
                continue;
            }
            long id1 = ( ( Gene ) geneService.findByOfficialSymbol( symbols[0] ).iterator().next() ).getId();
            long id2 = ( ( Gene ) geneService.findByOfficialSymbol( symbols[1] ).iterator().next() ).getId();
            genePairs.add( new GenePair( id1, id2 ) );
        }
        log.info( genePairs.size() + " gene pairs read" );
        return genePairs;
    }

    /**
     * Calculate the effect size for each gene pair from the specified expression experiments
     * 
     * @param EEs - expression experiments
     * @param genePairs - gene pairs
     */
    public void calculateEffectSize( Collection<ExpressionExperiment> EEs, Collection<GenePair> genePairs ) {
        Map<Long, Integer> eeSampleSizeMap = calculateCorrelations( EEs, genePairs );

        StopWatch watch = new StopWatch();
        watch.start();
        log.info( "Start computing effect size for " + genePairs.size() + " gene pairs" );
        for ( GenePair pair : genePairs ) {
            DoubleArrayList correlations = new DoubleArrayList();
            DoubleArrayList sampleSizes = new DoubleArrayList();
            for ( ExpressionExperiment ee : EEs ) {
                Integer sampleSize = eeSampleSizeMap.get( ee.getId() );
                Double corr = pair.getCorrelation( ee.getId() );
                if ( sampleSize != null && corr != null ) {
                    sampleSizes.add( sampleSize );
                    correlations.add( corr );
                }
            }
            metaAnalysis.run( correlations, sampleSizes );
            double effectSize = metaAnalysis.getE();
            pair.setEffectSize( effectSize );
        }
        watch.stop();
        log.info( "Finished computing effect size: " + watch.getTime() / 1000 + " seconds" );

    }
    
    /**
     * Calculate correlations for the specified gene pairs in the specified EEs.
     * 
     * @param EEs - expression experiments
     * @param genePairs - gene pair list
     * @param geneMap - gene ID to gene map
     * @return expression experiment ID to sample size map
     */
    public Map<Long, Integer> calculateCorrelations( Collection<ExpressionExperiment> EEs,
            Collection<GenePair> genePairs ) {
        if ( genePairs.size() < CHUNK_SIZE ) {
            return calculateCorrelationsChunk( EEs, genePairs );
        }
        Collection<GenePair> oneChunkGenePairs = new HashSet<GenePair>();
        Map<Long, Integer> eeSampleSizeMap = new HashMap<Long, Integer>();
        int count = 0;
        for ( GenePair genePair : genePairs ) {
            oneChunkGenePairs.add( genePair );
            count++;
            if ( count % CHUNK_SIZE == 0 || count == genePairs.size() ) {
                eeSampleSizeMap.putAll( calculateCorrelationsChunk( EEs, oneChunkGenePairs ) );
                oneChunkGenePairs.clear();
            }
        }
        return eeSampleSizeMap;
    }


    /**
     * Save the gene pairs as a figure to the specified file name
     * 
     * @param figureFileName - file name
     * @param numGenePairsToSave - number of gene pairs to save
     * @param genePairs - gene pair list
     * @param EEs - expression experiment list
     * @throws IOException
     */
    public void saveToFigures( String figureFileName, int numGenePairsToSave, Collection<GenePair> genePairs,
            Collection<ExpressionExperiment> EEs ) throws IOException {
        Collection<Long> geneIDs = new HashSet<Long>();
        for ( GenePair genePair : genePairs ) {
            geneIDs.add( genePair.getFirstId() );
            geneIDs.add( genePair.getSecondId() );
        }
        Map<Long, Gene> geneMap = getGeneMap( geneIDs );
        double[][] data = new double[numGenePairsToSave][EEs.size()];
        List<String> rowLabels = new ArrayList<String>();
        List<String> colLabels = new ArrayList<String>();
        List<GenePair> outputGenePairs = new ArrayList<GenePair>();
        for ( GenePair genePair : genePairs ) {
            if ( genePair.getCorrelations().size() > 100 ) outputGenePairs.add( genePair );
        }
        Collections.sort( outputGenePairs );
        for ( ExpressionExperiment ee : EEs ) {
            colLabels.add( ee.getShortName() );
        }
        for ( int i = 0; i < outputGenePairs.size(); i++ ) {
            GenePair genePair = outputGenePairs.get( i );
            rowLabels.add( geneMap.get( genePair.getFirstId() ).getName() + "_"
                    + geneMap.get( genePair.getSecondId() ).getName() );

            if ( i == numGenePairsToSave / 2 - 1 ) i = genePairs.size() - numGenePairsToSave / 2 - 1;
        }
        DoubleMatrixNamed dataMatrix = new DenseDoubleMatrix2DNamed( data );
        dataMatrix.setRowNames( rowLabels );
        dataMatrix.setColumnNames( colLabels );

        for ( int i = 0; i < outputGenePairs.size(); i++ ) {
            GenePair genePair = outputGenePairs.get( i );
            String rowName = geneMap.get( genePair.getFirstId() ).getName() + "_"
                    + geneMap.get( genePair.getSecondId() ).getName();
            int rowIndex = dataMatrix.getRowIndexByName( rowName );
            for ( ExpressionExperiment ee : EEs ) {
                String colName = ee.getShortName();
                int colIndex = dataMatrix.getColIndexByName( colName );
                dataMatrix.setQuick( rowIndex, colIndex, genePair.getCorrelation( ee.getId() ) );
            }

            if ( i == numGenePairsToSave / 2 - 1 ) i = genePairs.size() - numGenePairsToSave / 2 - 1;
        }
        ColorMatrix dataColorMatrix = new ColorMatrix( dataMatrix );
        dataColorMatrix.setColorMap( ColorMap.GREENRED_COLORMAP );
        JMatrixDisplay dataMatrixDisplay = new JMatrixDisplay( dataColorMatrix );
        dataMatrixDisplay.saveImage( figureFileName, true );
    }

    /**
     * Create and return a gene ID to gene map
     * 
     * @param ids - gene IDs
     * @return gene ID to gene map
     */
    private Map<Long, Gene> getGeneMap( Collection<Long> ids ) {
        Map<Long, Gene> geneMap = new HashMap<Long, Gene>();
        int count = 0;
        int total = ids.size();
        final int CHUNK_LIMIT = 100;
        Collection<Long> idsInOneChunk = new HashSet<Long>();
        Collection<Gene> allGenes = new HashSet<Gene>();
        log.info( "Start loading genes" );
        StopWatch qWatch = new StopWatch();
        qWatch.start();
        for ( Long geneID : ids ) {
            idsInOneChunk.add( geneID );
            count++;
            total--;
            if ( count == CHUNK_LIMIT || total == 0 ) {
                allGenes.addAll( geneService.load( idsInOneChunk ) );
                count = 0;
                idsInOneChunk.clear();
                System.out.print( "." );
            }
        }
        System.err.println();
        qWatch.stop();
        log.info( "Query takes " + qWatch.getTime() + " to load " + ids.size() + " genes" );

        for ( Gene gene : allGenes ) {
            if ( ids.contains( gene.getId() ) ) {
                geneMap.put( gene.getId(), gene );
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
    private Map<Long, Collection<Long>> getGene2CSMap( Collection<Gene> genes ) {
        log.info( "Loading composite sequences for " + genes.size() + " genes");
        if ( genes.size() <= CHUNK_SIZE ) {
            return geneService.getCompositeSequenceMap( genes );
        }
        Map<Long, Collection<Long>> gene2cs = new HashMap<Long, Collection<Long>>();
        int count = 0;
        Collection<Gene> genesChunk = new HashSet<Gene>();
        for ( Gene gene : genes ) {
            genesChunk.add( gene );
            count++;
            if ( count % CHUNK_SIZE == 0 || count == genes.size() ) {
                gene2cs.putAll( geneService.getCompositeSequenceMap( genesChunk ) );
                genesChunk.clear();
            }
        }
        System.err.println( "Get " + gene2cs.keySet().size() + " cs" );
        return gene2cs;
    }

    /**
     * Inverts the specified gene to probe (cs) map for the specified gene to produce a probe to gene(s) map
     * 
     * @param geneId - ID of gene that probes map to
     * @param gene2cs - gene to probe map
     * @return probe to gene map
     */
    private Map<Long, Collection<Gene>> getCS2GeneMap( Map<Long, Collection<Long>> gene2cs, Map<Long, Gene> geneMap ) {
        Map<Long, Collection<Gene>> cs2gene = new HashMap<Long, Collection<Gene>>();
        for ( Long geneId : gene2cs.keySet() ) {
            Collection<Long> csIds = gene2cs.get( geneId );
            for ( Long csId : csIds ) {
                Collection<Gene> genes = cs2gene.get( csId );
                if ( genes == null ) {
                    genes = new HashSet<Gene>();
                    cs2gene.put( csId, genes );
                }
                genes.add( geneMap.get( geneId ) );
            }
        }
        return cs2gene;
    }

    /**
     * Retrieve an expression profile for a set of genes, i.e. a map of design element data vectors to the set of genes
     * that it represents.
     * 
     * @param cs2gene - a probe to gene map
     * @param qt - the quantitation type of the expression profile desired
     * @param ee - the expression experiment
     * @return map of design element data vectors to its set of genes
     */
    private Map<DesignElementDataVector, Collection<Gene>> getDesignElementDataVectors(
            Map<Long, Collection<Gene>> cs2gene, QuantitationType qt, ExpressionExperiment ee ) {
        Map<DesignElementDataVector, Collection<Gene>> dedv2genes = eeService.getDesignElementDataVectors( cs2gene, qt );
        for ( DesignElementDataVector dedv : dedv2genes.keySet() ) {
            dedv.setExpressionExperiment( ee );
        }
        return dedv2genes;
    }

    /**
     * Get the preferred quantitation type for the expression experiment
     * 
     * @param ee expression experiment
     * @return preferred quantitation type
     */
    private QuantitationType getPreferredQT( ExpressionExperiment ee ) {
        Collection<QuantitationType> qts = eeService.getQuantitationTypes( ee );
        for ( QuantitationType qt : qts ) {
            if ( qt.getIsPreferred() ) return qt;
        }
        return null;
    }

    /**
     * Calculate the median correlation between the source and the target expression profiles 
     * 
     * @param source - a collection of data vectors
     * @param target - a collection of data vectors
     * @return the median correlation between the source and the target
     */
    private double medianEPCorrelation( Collection<ExpressionProfile> source, Collection<ExpressionProfile> target ) {
        DoubleArrayList data = new DoubleArrayList();
        for ( ExpressionProfile ep1 : source ) {
            for ( ExpressionProfile ep2 : target ) {
                if (ep1.val.length == ep2.val.length && ep1.val.length > GeneCoExpressionAnalysis.MINIMUM_SAMPLE) {
                    data.add(CorrelationStats.correl( ep1.val, ep2.val ));
                } 
            }
        }
        data.sort();
        return ( data.size() > 0 ) ? data.get( data.size() / 2 ) : 0.0;
    }

    /**
     * Get a gene ID to expression profiles map for an expression experiment (specified by the quantitation type)
     * 
     * @param genes - genes to map
     * @param qt - quantitation type of the expression experiment
     * @return gene ID to expression profile map
     */
    private Map<Long, Collection<ExpressionProfile>> getGeneID2EPsMap( Collection<Long> geneIDs, QuantitationType qt, ExpressionExperiment ee) {
        Map<Long, Gene> geneMap = getGeneMap( geneIDs );
        Map<Long, Collection<Long>> gene2cs = getGene2CSMap( geneMap.values() );
        Map<Long, Collection<Gene>> cs2gene = getCS2GeneMap( gene2cs, geneMap );
        System.err.println( "Loaded " + gene2cs.keySet().size() + " genes and " + cs2gene.keySet().size() + " CSs" );

        Map<Long, Collection<ExpressionProfile>> geneID2EPs = new HashMap<Long, Collection<ExpressionProfile>>();
        Map<DesignElementDataVector, Collection<Gene>> dedv2genes = getDesignElementDataVectors( cs2gene, qt, ee );
        for ( DesignElementDataVector dedv : dedv2genes.keySet() ) {
            Collection<Gene> genes = dedv2genes.get( dedv );
            for ( Gene gene : genes ) {
                Long id = gene.getId();
                Collection<ExpressionProfile> eps = geneID2EPs.get( id );
                if ( eps == null ) {
                    eps = new HashSet<ExpressionProfile>();
                    geneID2EPs.put( id, eps );
                }
                eps.add( new ExpressionProfile( dedv ) );
            }
        }
        return geneID2EPs;
    }

    /**
     * Calculate correlations for the specified gene pairs in the specified EEs.
     * 
     * @param EEs - expression experiments
     * @param genePairs - gene pair list
     * @param geneMap - gene ID to gene map
     * @return expression experiment ID to sample size map
     */
    private Map<Long, Integer> calculateCorrelationsChunk( Collection<ExpressionExperiment> EEs,
            Collection<GenePair> genePairs ) {
        Map<Long, Integer> eeSampleSizeMap = new HashMap<Long, Integer>();
        int total = 0;
        for ( ExpressionExperiment ee : EEs ) {
            StopWatch watch = new StopWatch();
            watch.start();
            // quantitation type is tied to its EE
            QuantitationType qt = getPreferredQT( ee );
            if ( qt == null ) {
                continue;
            }
            Collection<Long> geneIDs = new HashSet<Long>();
            for ( GenePair genePair : genePairs ) {
                geneIDs.add( genePair.getFirstId() );
                geneIDs.add( genePair.getSecondId() );
            }
            Map<Long, Collection<ExpressionProfile>> geneID2EPs = getGeneID2EPsMap( geneIDs, qt, ee );

            for ( GenePair genePair : genePairs ) {
                Collection<ExpressionProfile> source = geneID2EPs.get( genePair.getFirstId() );
                Collection<ExpressionProfile> target = geneID2EPs.get( genePair.getSecondId() );
                if ( source != null && target != null ) {
                    double corr = medianEPCorrelation( source, target );
                    if ( corr != 0.0 ) {
                        int eeSampleSize = source.iterator().next().val.length;
                        if ( eeSampleSize > MINIMUM_SAMPLE_SIZE ) {
                            genePair.addCorrelation( ee.getId(), corr );
                            eeSampleSizeMap.put( ee.getId(), eeSampleSize );
                        }
                    }
                }
            }
            // eeIndex.add( ee.getId() );
            // eeMap.put( ee.getId(), ee );
            total += geneID2EPs.values().size();
            System.err.println( ee.getId() + "----->" + geneID2EPs.values().size() + " expression profiles" + " in "
                    + watch.getTime() / 1000 + " seconds" );
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

        double[] val = null;

        long id;

        /**
         * Construct an ExpressionProfile from the specified DesignElementDataVector
         * 
         * @param dedv - vector to convert
         */
        public ExpressionProfile( DesignElementDataVector dedv ) {
            this.dedv = dedv;
            this.id = dedv.getId();
            byte[] bytes = dedv.getData();
            val = bac.byteArrayToDoubles( bytes );
        }

        /**
         * Get the ID of the vector
         * 
         * @return the vector ID
         */
        public long getId() {
            return this.id;
        }
    }

    public void setEeService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }
}
