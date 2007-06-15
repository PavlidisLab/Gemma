package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMap;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.math.CorrelationStats;
import ubic.basecode.math.metaanalysis.CorrelationEffectMetaAnalysis;
import ubic.gemma.analysis.coexpression.GeneCoExpressionAnalysis;
import ubic.gemma.analysis.linkAnalysis.MetaLinkFinder;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.LongArrayList;
import cern.colt.list.ObjectArrayList;
import cern.colt.map.AbstractLongObjectMap;
import cern.colt.map.OpenLongObjectHashMap;

/**
 * Calculate the effect size
 * 
 * @author xwan
 */
public class EffectSizeCalculationCli extends AbstractSpringAwareCLI {
    private static final int MINIMUM_SAMPLE_SIZE = 3;
    // private DesignElementDataVectorService dedvService;
    private ExpressionExperimentService eeService;
    // private CompositeSequenceService csService;
    private GeneService geneService;
    private String geneList = null;
    private String taxonName = null;
    private String outputFile = null;
    private String matrixFile = null;
    private int stringency = 3;
    /**
     * Stores candidate gene pairs (to evaluate effect size)
     */
    /**
     * Maps gene IDs to genes
     */
    // private Map<Long, Gene> geneMap = new HashMap<Long, Gene>();
    // private Collection<ExpressionExperiment> allEEs = null;
    private CorrelationEffectMetaAnalysis metaAnalysis = new CorrelationEffectMetaAnalysis( true, false );
    private ByteArrayConverter bac = new ByteArrayConverter();

    // private LongArrayList eeIndex = new LongArrayList();
    // private Map<Long, ExpressionExperiment> eeMap = new HashMap<Long, ExpressionExperiment>();
    // private Map<Long, Integer> eeSampleSizes = new HashMap<Long, Integer>();

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option geneFileOption = OptionBuilder.hasArg().isRequired().withArgName( "geneFile" ).withDescription(
                "Short names of the genes to analyze" ).withLongOpt( "geneFile" ).create( 'g' );
        addOption( geneFileOption );
        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "Taxon" ).withDescription(
                "the taxon of the genes to analyze" ).withLongOpt( "Taxon" ).create( 't' );
        addOption( taxonOption );
        Option matrixFile = OptionBuilder.hasArg().withArgName( "Bit Matrixfile" ).isRequired().withDescription(
                "The file for saving bit matrix" ).withLongOpt( "matrixfile" ).create( 'm' );
        addOption( matrixFile );
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFile" ).withDescription(
                "File for saving the correlation data" ).withLongOpt( "outFile" ).create( 'o' );
        addOption( outputFileOption );
        Option stringencyFileOption = OptionBuilder.hasArg().withArgName( "stringency" ).withDescription(
                "The stringency for the number of co-expression link(Default 3)" ).withLongOpt( "stringency" ).create(
                's' );
        addOption( stringencyFileOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'g' ) ) {
            this.geneList = getOptionValue( 'g' );
        }
        if ( hasOption( 't' ) ) {
            this.taxonName = getOptionValue( 't' );
        }
        // if ( hasOption( 'o' ) ) {
        // this.outputFile = getOptionValue( 'o' );
        // }
        if ( hasOption( 's' ) ) {
            this.stringency = Integer.parseInt( getOptionValue( 's' ) );
        }
        // if ( hasOption( 'm' ) ) {
        // this.matrixFile = getOptionValue( 'm' );
        // }
        // dedvService = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        geneService = ( GeneService ) this.getBean( "geneService" );
        // csService = ( CompositeSequenceService ) this.getBean( "compositeSequenceService" );
    }

    private Taxon getTaxon( String name ) {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( name );
        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
        taxon = taxonService.find( taxon );
        if ( taxon == null ) {
            log.info( "No Taxon found!" );
        }
        return taxon;
    }

    /**
     * Write out gene pairs or links that meet the stringency requirements
     * 
     * @param outFile
     */
    private void saveGenePairs( String outFile ) {
        int count = 0;
        try {
            FileWriter out = new FileWriter( new File( outFile ) );
            for ( int i = 0; i < MetaLinkFinder.linkCount.rows(); i++ )
                for ( int j = i + 1; j < MetaLinkFinder.linkCount.columns(); j++ ) {
                    int bitCount = MetaLinkFinder.linkCount.bitCount( i, j );
                    if ( bitCount >= stringency ) {
                        // linkCount.getRowName(i) and linkCount.getColName(j) will get the gene ids.
                        out.write( MetaLinkFinder.linkCount.getRowName( i ) + "\t"
                                + MetaLinkFinder.linkCount.getRowName( j ) + "\t" + bitCount + "\n" );
                        count++;
                    }
                }
            out.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( "Total Links " + count );
    }

    /**
     * Read in gene pair IDs from the geneList (CLI specified file) into genePairs if they meet the stringency
     * requirements, i.e. if the pairing is seen in enough other datasets.
     * 
     * @return list of gene pairs
     */
    private GenePairList readGenePairs( String geneListFile ) {
        GenePairList genePairs = new GenePairList();
        try {
            BufferedReader in = new BufferedReader( new FileReader( new File( geneListFile ) ) );
            String row = null;
            while ( ( row = in.readLine() ) != null ) {
                row = row.trim();
                if ( StringUtils.isBlank( row ) ) continue;
                String[] subItems = row.split( "\t" );
                long firstId = Long.valueOf( subItems[0] );
                long secondId = Long.valueOf( subItems[1] );
                int count = Integer.valueOf( subItems[2] );
                if ( count >= this.stringency ) {
                    genePairs.add( new GenePair( firstId, secondId, count ) );
                }
            }
            in.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        // load data in chunks
        return genePairs;
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
        Map<Long, Collection<Long>> gene2cs = new HashMap<Long, Collection<Long>>();
        int count = 0;
        int total = genes.size();
        final int CHUNK_LIMIT = 500;
        Collection<Gene> genesInOneChunk = new HashSet<Gene>();
        for ( Gene gene : genes ) {
            genesInOneChunk.add( gene );
            count++;
            total--;
            if ( count == CHUNK_LIMIT || total == 0 ) {
                gene2cs.putAll( geneService.getCompositeSequenceMap( genesInOneChunk ) );
                count = 0;
                genesInOneChunk.clear();
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
     * Retrieve an expression profile for a set of genes, i.e. a map of design element data vectors to the set of genes
     * that it represents.
     * 
     * @param cs2gene - a probe to gene map
     * @param qt - the quantitation type of the expression profile desired
     * @param ee - the expression experiment
     * @return map of design element data vectors to its set of genes
     */
    Map<DesignElementDataVector, Collection<Gene>> getDesignElementDataVectors( Map<Long, Collection<Gene>> cs2gene,
            QuantitationType qt, ExpressionExperiment ee ) {
        Map<DesignElementDataVector, Collection<Gene>> dedv2genes = eeService.getDesignElementDataVectors( cs2gene, qt );
        for ( DesignElementDataVector dedv : dedv2genes.keySet() ) {
            dedv.setExpressionExperiment( ee );
        }
        return dedv2genes;
    }

    /**
     * Calculate the correlation between the two specified expression profiles
     * 
     * @param ep1 - expression profile
     * @param ep2 - expression profile
     * @return the correlation coefficient
     */
    private double correlation( ExpressionProfile ep1, ExpressionProfile ep2 ) {
        double[] ival = ep1.val, jval = ep2.val;

        if ( ival.length != jval.length ) {
            // System.err.print("Error in Dimension " + devI.getId()+ " " + ival.length + " (" +
            // devI.getExpressionExperiment().getId() + ") ");
            // System.err.println(devJ.getId() + " " + jval.length + " (" + devJ.getExpressionExperiment().getId() + ")
            // ");
            return Double.NaN;
        }
        if ( ival.length < GeneCoExpressionAnalysis.MINIMUM_SAMPLE ) return Double.NaN;
        if ( ep1.getId() == ep2.getId() ) {
            // System.err.println("Error in " + devI.getExpressionExperiment().getId());
            return Double.NaN;
        }
        return CorrelationStats.correl( ival, jval );
    }

    /**
     * Calculate the median correlation between the source and the target genes
     * 
     * @param source - a collection of data vectors
     * @param target - a collection of data vectors
     * @return the median correlation between the source and the target
     */
    private double medianCorrelation( Collection<ExpressionProfile> source, Collection<ExpressionProfile> target ) {
        DoubleArrayList sortedData = new DoubleArrayList();
        for ( ExpressionProfile ep1 : source ) {
            for ( ExpressionProfile ep2 : target ) {
                double corr = correlation( ep1, ep2 );
                if ( !Double.isNaN( corr ) ) {
                    sortedData.add( corr );
                }
            }
        }
        Double medianCorr = sortedData.get( sortedData.size() / 2 );
        return ( sortedData.size() > 0 ) ? medianCorr : 0.0;
    }

    /**
     * Get a gene ID to expression profiles map for an expression experiment (specified by the quantitation type)
     * 
     * @param geneMap - gene ID to gene map
     * @param qt - quantitation type of the expression experiment
     * @return gene ID to expression profile map
     */
    private Map<Long, Collection<ExpressionProfile>> getGeneID2EPsMap( Map<Long, Gene> geneMap, QuantitationType qt ) {
        Map<Long, Collection<Long>> gene2cs = getGene2CSMap( geneMap.values() );
        Map<Long, Collection<Gene>> cs2gene = getCS2GeneMap( gene2cs, geneMap );
        System.err.println( "Loaded " + gene2cs.keySet().size() + " genes and " + cs2gene.keySet().size() + " CSs" );

        Map<Long, Collection<ExpressionProfile>> geneID2EPs = new HashMap<Long, Collection<ExpressionProfile>>();
        Map<DesignElementDataVector, Collection<Gene>> dedv2genes = eeService.getDesignElementDataVectors( cs2gene, qt );
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
    private Map<Long, Integer> calculateCorrelations( Collection<ExpressionExperiment> EEs, GenePairList genePairs,
            Map<Long, Gene> geneMap ) {
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
            Map<Long, Collection<ExpressionProfile>> geneID2EPs = getGeneID2EPsMap( geneMap, qt );

            for ( int i = 0; i < genePairs.size(); i++ ) {
                GenePair genePair = genePairs.get( i );
                Collection<ExpressionProfile> source = geneID2EPs.get( genePair.firstId );
                Collection<ExpressionProfile> target = geneID2EPs.get( genePair.secondId );
                if ( source != null && target != null ) {
                    double corr = 0.0;
                    corr = medianCorrelation( source, target );
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
     * Calculate the effect size for each gene pair from the specified expression experiments
     */
    private void calculateEffectSize( Collection<ExpressionExperiment> EEs, GenePairList genePairs,
            Map<Long, Gene> geneMap, Map<Long, Integer> eeSampleSizeMap ) {
        StopWatch watch = new StopWatch();
        watch.start();
        System.err.println( "Start Computing Effect Size for " + genePairs.size() + " gene pairs" );
        for ( int i = 0; i < genePairs.size(); i++ ) {
            GenePair pair = genePairs.get( i );
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
        System.err.println( "Finished in " + watch.getTime() / 1000 + " seconds" );

    }

    /**
     * Save the gene pairs to the specified file name
     * 
     * @param figureFileName - file name
     * @param numGenePairsToSave - number of gene pairs to save
     * @param genePairs - gene pair list
     * @param EEs - expression experiment list
     * @param geneMap - gene ID to gene map
     * @throws IOException
     */
    private void saveToFigures( String figureFileName, int numGenePairsToSave, GenePairList genePairs,
            Collection<ExpressionExperiment> EEs, Map<Long, Gene> geneMap ) throws IOException {
        double[][] data = new double[numGenePairsToSave][EEs.size()];
        List<String> rowLabels = new ArrayList<String>();
        List<String> colLabels = new ArrayList<String>();
        GenePairList outputGenePairs = new GenePairList();
        for ( int i = 0; i < genePairs.size(); i++ ) {
            GenePair genePair = genePairs.get( i );
            if ( genePair.getCorrelations().size() > 100 ) outputGenePairs.add( genePair );
        }
        genePairs = outputGenePairs;
        genePairs.sort();
        for ( ExpressionExperiment ee : EEs ) {
            colLabels.add( ee.getShortName() );
        }
        for ( int i = 0; i < genePairs.size(); i++ ) {
            GenePair genePair = genePairs.get( i );
            rowLabels
                    .add( geneMap.get( genePair.firstId ).getName() + "_" + geneMap.get( genePair.secondId ).getName() );

            if ( i == numGenePairsToSave / 2 - 1 ) i = genePairs.size() - numGenePairsToSave / 2 - 1;
        }
        DoubleMatrixNamed dataMatrix = new DenseDoubleMatrix2DNamed( data );
        dataMatrix.setRowNames( rowLabels );
        dataMatrix.setColumnNames( colLabels );

        for ( int i = 0; i < genePairs.size(); i++ ) {
            GenePair genePair = genePairs.get( i );
            String rowName = geneMap.get( genePair.firstId ).getName() + "_"
                    + geneMap.get( genePair.secondId ).getName();
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

    @Override
    protected Exception doWork( String[] args ) {
        Exception exc = processCommandLine( "EffectSizeCalculation ", args );
        if ( exc != null ) {
            return exc;
        }
        MetaLinkFinder linkFinder = new MetaLinkFinder();
        linkFinder.setGeneService( geneService );
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            linkFinder.fromFile( this.matrixFile, null );
        } catch ( IOException e ) {
            log.info( "Couldn't load the data from the files " );
            return e;
        }
        watch.stop();
        log.info( "Spend " + watch.getTime() / 1000 + " to load the data matrix" );

        Taxon taxon = getTaxon( this.taxonName );

        // First time using the following function to save candidate gene pair into a file
        // saveGenePairs("genepairs.txt");
        GenePairList genePairs = readGenePairs( geneList );
        Map<Long, Gene> geneMap = getGeneMap( genePairs.getGeneIDs() );
        Collection<ExpressionExperiment> EEs = eeService.findByTaxon( taxon );
        Map<Long, Integer> eeSampleSizeMap = calculateCorrelations( EEs, genePairs, geneMap );
        calculateEffectSize( EEs, genePairs, geneMap, eeSampleSizeMap );

        try {
            saveToFigures( "correlationData.png", 2700, genePairs, EEs, geneMap );
        } catch ( IOException e ) {
            return e;
        }
        return null;
    }

    /**
     * Calculate effect size
     * 
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub
        EffectSizeCalculationCli compute = new EffectSizeCalculationCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = compute.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( watch.getTime() / 1000 );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * Stores a list of gene pairs using <code>ObjectArrayList</code> as a delegate
     * 
     * @author Raymond
     */
    protected class GenePairList {
        private ObjectArrayList genePairs;
        private Set<Long> geneIDList;

        /**
         * Constructs a new gene pair list
         */
        public GenePairList() {
            genePairs = new ObjectArrayList();
            geneIDList = new HashSet<Long>();
        }

        /**
         * Add a gene pair
         * 
         * @param id1
         * @param id2
         * @param count
         */
        public void add( long id1, long id2, int count ) {
            GenePair genePair = new GenePair( id1, id2, count );
            genePairs.add( genePair );
            geneIDList.add( id1 );
            geneIDList.add( id2 );
        }

        /**
         * Add a gene pair
         * 
         * @param genePair
         */
        public void add( GenePair genePair ) {
            genePairs.add( genePair );
            geneIDList.add( genePair.firstId );
            geneIDList.add( genePair.secondId );
        }

        /**
         * Add all of the specified gene pairs
         * 
         * @param collection
         */
        public void addAllOf( Collection<GenePair> collection ) {
            genePairs.addAllOf( collection );
        }

        /**
         * Returns true if the receiver contains the specified element. Tests for equality or identity as specified by
         * testForEquality.
         * 
         * @param elem - element to search for
         * @param testForEquality - if true, test for equality, otherwise identity
         * @return true if receiver contains specified element
         */
        public boolean contains( GenePair elem, boolean testForEquality ) {
            return genePairs.contains( elem, testForEquality );
        }

        /**
         * Get the gene pair at the specified index
         * 
         * @param index
         * @return gene pair
         */
        public GenePair get( int index ) {
            return ( GenePair ) genePairs.get( index );
        }

        /**
         * Checks if the gene pair list is empty
         * 
         * @return true if list is empty
         */
        public boolean isEmpty() {
            return genePairs.isEmpty();
        }

        /**
         * Remove the gene pair at the specified index
         * 
         * @param index
         */
        public void remove( int index ) {
            genePairs.remove( index );
        }

        /**
         * Size of the gene pair list
         * 
         * @return size
         */
        public int size() {
            return genePairs.size();
        }

        /**
         * Get the list of gene IDs
         * 
         * @return set of gene IDs
         */
        public Collection<Long> getGeneIDs() {
            return geneIDList;
        }

        /**
         * Sort the gene pairs
         */
        public void sort() {
            genePairs.sort();
        }
    }

    /**
     * Stores a pair of gene IDs and related info. Sort them by effect size in descending order.
     * 
     * @author xwan
     * @author Raymond (refactoring)
     */
    public class GenePair implements Comparable<GenePair> {
        private long firstId = 0;
        private long secondId = 0;
        private Integer count = 0;
        private Double effectSize = 0.0;
        private AbstractLongObjectMap eeCorrelationMap;

        /**
         * Construct a gene pair with the specified pair of IDs and count
         * 
         * @param id1 - ID of first gene
         * @param id2 - ID of second gene
         * @param count - number of expression experiments gene pair is observed in
         */
        public GenePair( long id1, long id2, int count ) {
            this.firstId = id1;
            this.secondId = id2;
            this.count = count;
            eeCorrelationMap = new OpenLongObjectHashMap();
        }

        /**
         * Add a correlation
         * 
         * @param eeID - expression experiment ID
         * @param correlation
         */
        public void addCorrelation( long eeID, double correlation ) {
            eeCorrelationMap.put( eeID, new Double( correlation ) );
        }

        /**
         * Get a correlation for a specified expression experiment
         * 
         * @param eeID - expression experiment ID
         * @return correlation of the expression experiment
         */
        public Double getCorrelation( long eeID ) {
            return ( Double ) eeCorrelationMap.get( eeID );
        }

        /**
         * Get the of correlations
         * 
         * @return list of correlations (Double)
         */
        public ObjectArrayList getCorrelations() {
            return eeCorrelationMap.values();
        }

        /**
         * Get the list of expression experiment IDs
         * 
         * @return list of expression experiment IDs
         */
        public LongArrayList getEEIDs() {
            return eeCorrelationMap.keys();
        }

        public int compareTo( GenePair o ) {
            return -effectSize.compareTo( o.effectSize );
        }

        public Integer getCount() {
            return count;
        }

        public void setCount( Integer count ) {
            this.count = count;
        }

        public Double getEffectSize() {
            return effectSize;
        }

        public void setEffectSize( Double effectSize ) {
            this.effectSize = effectSize;
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

    /**
     * Stores the expression profile data.
     * 
     * @author xwan
     * @author raymond (refactoring)
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

}
