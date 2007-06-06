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

/**
 * Calculate the effect size
 * 
 * @author xwan
 */
public class EffectSizeCalculationCli extends AbstractSpringAwareCLI {
    private static final int MINIMUN_SAMPLE_SIZE = 3;
    private DesignElementDataVectorService dedvService;
    private ExpressionExperimentService eeService;
    private CompositeSequenceService csService;
    private GeneService geneService;
    private String geneList = null;
    private String taxonName = null;
    private String outputFile = null;
    private String matrixFile = null;
    private int stringency = 3;
    private ObjectArrayList genePairs = new ObjectArrayList();
    private Map<Long, Gene> geneMap = new HashMap<Long, Gene>();
    private Collection<ExpressionExperiment> allEEs = null;
    private CorrelationEffectMetaAnalysis metaAnalysis = new CorrelationEffectMetaAnalysis( true, false );
    private ByteArrayConverter bac = new ByteArrayConverter();
    private LongArrayList eeIndex = new LongArrayList();
    private Map<Long, ExpressionExperiment> eeMap = new HashMap<Long, ExpressionExperiment>();
    private Map<Long, Integer> eeSampleSizes = new HashMap<Long, Integer>();

    /**
     * Stores a pair of gene IDs and related info. Sort them by effect size in descending order.
     * 
     * @author xwan
     */
    protected class GenePair implements Comparable<GenePair> {
        long firstId = 0;
        long secondId = 0;
        Integer count = 0;
        Double effectSize = 0.0;
        DoubleArrayList correlations = new DoubleArrayList();
        LongArrayList eeIds = new LongArrayList();

        public GenePair( long firstId, long secondId, int count ) {
            this.firstId = firstId;
            this.secondId = secondId;
            this.count = count;
        }

        public int compareTo( GenePair o ) {
            return effectSize.compareTo( o.effectSize ) * ( -1 );
        }
    }

    /**
     * A data vector based on a DesignElementDataVector. Byte array stored as data.
     * 
     * @author xwan
     */
    protected class DataVector {
        DesignElementDataVector dedv = null;
        double[] val = null;
        long id;

        public DataVector( DesignElementDataVector dedv ) {
            this.dedv = dedv;
            this.id = dedv.getId();
            byte[] bytes = dedv.getData();
            val = bac.byteArrayToDoubles( bytes );
        }

        public long getId() {
            return this.id;
        }
    }

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
                "File for saving the corelation data" ).withLongOpt( "outFile" ).create( 'o' );
        addOption( outputFileOption );
        Option stringencyFileOption = OptionBuilder.hasArg().withArgName( "stringency" ).withDescription(
                "The stringency for the number of co-expression link(Default 3)" ).withLongOpt( "stringency" ).create(
                's' );
        addOption( stringencyFileOption );
    }

    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'g' ) ) {
            this.geneList = getOptionValue( 'g' );
        }
        if ( hasOption( 't' ) ) {
            this.taxonName = getOptionValue( 't' );
        }
        if ( hasOption( 'o' ) ) {
            this.outputFile = getOptionValue( 'o' );
        }
        if ( hasOption( 's' ) ) {
            this.stringency = Integer.parseInt( getOptionValue( 's' ) );
        }
        if ( hasOption( 'm' ) ) {
            this.matrixFile = getOptionValue( 'm' );
        }
        dedvService = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        geneService = ( GeneService ) this.getBean( "geneService" );
        csService = ( CompositeSequenceService ) this.getBean( "compositeSequenceService" );
    }

    private Taxon getTaxon( String name ) {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setCommonName( name );
        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );
        taxon = taxonService.find( taxon );
        if ( taxon == null ) {
            log.info( "NO Taxon found!" );
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.err.println( "Total Links " + count );
    }

    /**
     * Read in gene pair IDs from the geneList (CLI specified file) into geneMap
     */
    private void readGenesPairs() {
        Collection<Long> geneIds = new HashSet<Long>();
        try {
            BufferedReader in = new BufferedReader( new FileReader( new File( this.geneList ) ) );
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
                    if ( !geneIds.contains( firstId ) ) geneIds.add( firstId );
                    if ( !geneIds.contains( secondId ) ) geneIds.add( secondId );
                }
            }
            in.close();
        } catch ( Exception e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // load data in chunks
        int count = 0;
        int total = geneIds.size();
        int CHUNK_LIMIT = 100;
        Collection<Long> idsInOneChunk = new HashSet<Long>();
        Collection<Gene> allGenes = new HashSet<Gene>();
        log.info( "Start loading genes" );
        StopWatch qWatch = new StopWatch();
        qWatch.start();
        for ( Long geneId : geneIds ) {
            idsInOneChunk.add( geneId );
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
        log.info( "Query takes " + qWatch.getTime() + " to load " + geneIds.size() + " genes" );

        for ( Gene gene : allGenes ) {
            if ( geneIds.contains( gene.getId() ) ) {
                geneMap.put( gene.getId(), gene );
            }
        }
    }

    /**
     * Build and return the gene to probe (composite sequences) map
     * 
     * @return gene to probe map
     */
    private Map<Long, Collection<Long>> getGene2CSMap() {
        // Get cs2Gene Map
        Map<Long, Collection<Long>> gene2cs = new HashMap<Long, Collection<Long>>();
        int count = 0;
        int total = geneMap.keySet().size();
        int CHUNK_LIMIT = 500;
        Collection<Gene> genesInOneChunk = new HashSet<Gene>();
        for ( Gene gene : geneMap.values() ) {
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
     * Build and return the probe (cs) to gene map
     * 
     * @param geneId
     * @param gene2cs
     * @return
     */
    private Map<Long, Collection<Gene>> getCs2GeneMap( Long geneId, Map<Long, Collection<Long>> gene2cs ) {
        Map<Long, Collection<Gene>> cs2gene = new HashMap<Long, Collection<Gene>>();
        Collection<Long> csIds = gene2cs.get( geneId );
        for ( Long csId : csIds ) {
            Collection<Gene> genes = cs2gene.get( csId );
            if ( genes == null ) {
                genes = new HashSet<Gene>();
                cs2gene.put( csId, genes );
            }
            genes.add( geneMap.get( geneId ) );
        }
        return cs2gene;
    }

    /**
     * Get the preferred quantitation type for the expression experiment
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
     * 
     * @param cs2gene
     * @param qt
     * @param ee
     * @return
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
     * @param devI
     * @param devJ
     * @return
     */
    private double coRelation( DataVector dedvI, DataVector dedvJ ) throws Exception {
        double corr = 0;

        double[] ival = dedvI.val, jval = dedvJ.val;

        if ( ival.length != jval.length ) {
            // System.err.print("Error in Dimension " + devI.getId()+ " " + ival.length + " (" +
            // devI.getExpressionExperiment().getId() + ") ");
            // System.err.println(devJ.getId() + " " + jval.length + " (" + devJ.getExpressionExperiment().getId() + ")
            // ");
            return Double.NaN;
        }
        if ( ival.length < GeneCoExpressionAnalysis.MINIMUM_SAMPLE ) return Double.NaN;
        if ( dedvI.getId() == dedvJ.getId() ) {
            // System.err.println("Error in " + devI.getExpressionExperiment().getId());
            return Double.NaN;
        }
        return CorrelationStats.correl( ival, jval );
    }

    private double correlation( Collection<DataVector> source, Collection<DataVector> target ) throws Exception {
        Object[] dedvI = source.toArray();
        Object[] dedvJ = source.toArray();
        DoubleArrayList sortedData = new DoubleArrayList();
        ;
        for ( int ii = 0; ii < dedvI.length; ii++ )
            for ( int jj = 0; jj < dedvJ.length; jj++ ) {
                double corr = coRelation( ( DataVector ) dedvI[ii], ( DataVector ) dedvJ[jj] );
                if ( !Double.isNaN( corr ) ) sortedData.add( corr );
            }
        if ( sortedData.size() > 0 ) {
            Double medianCorr = sortedData.get( sortedData.size() / 2 );
            return medianCorr;
        }
        return 0.0;
    }

    // For slow version
    private double effectSize( Collection<DataVector> source, Collection<DataVector> target ) {
        if ( source == null || target == null ) return 0.0;
        Map<Long, Collection<DataVector>> sourceMap = new HashMap<Long, Collection<DataVector>>();
        Map<Long, Collection<DataVector>> targetMap = new HashMap<Long, Collection<DataVector>>();
        DoubleArrayList correlations = new DoubleArrayList();
        DoubleArrayList sampleSizes = new DoubleArrayList();

        for ( DataVector dv : source ) {
            Long key = dv.dedv.getExpressionExperiment().getId();
            Collection<DataVector> dvs = sourceMap.get( key );
            if ( dvs == null ) {
                dvs = new HashSet<DataVector>();
                sourceMap.put( key, dvs );
            }
            dvs.add( dv );
        }

        for ( DataVector dv : target ) {
            Long key = dv.dedv.getExpressionExperiment().getId();
            Collection<DataVector> dvs = targetMap.get( key );
            if ( dvs == null ) {
                dvs = new HashSet<DataVector>();
                targetMap.put( key, dvs );
            }
            dvs.add( dv );
        }
        for ( Long eeId : sourceMap.keySet() ) {
            Collection<DataVector> dedvsI = sourceMap.get( eeId );
            Collection<DataVector> dedvsJ = targetMap.get( eeId );
            double corr = 0.0;
            try {
                corr = correlation( dedvsI, dedvsJ );
            } catch ( Exception e ) {
                continue;
            }
            if ( corr == 0.0 ) continue;
            Integer eeSampleSize = eeSampleSizes.get( eeId );
            if ( eeSampleSize == null ) {
                eeSampleSize = dedvsI.iterator().next().val.length;
                eeSampleSizes.put( eeId, eeSampleSize );
            }
            if ( eeSampleSize > MINIMUN_SAMPLE_SIZE ) {
                correlations.add( corr );
                sampleSizes.add( eeSampleSize );
            }
        }
        metaAnalysis.run( correlations, sampleSizes );
        double effectSize = Math.abs( metaAnalysis.getE() );
        return effectSize;
    }

    /**
     * Calculate the effect size for one chunk of the data
     * @param start
     * @param end
     * @param cs2gene
     */
    private void calculateEffectSizeForOneChunk( int start, int end, Map<Long, Collection<Gene>> cs2gene ) {
        Map<DesignElementDataVector, Collection<Gene>> dedv2genes = new HashMap<DesignElementDataVector, Collection<Gene>>();
        StopWatch watch = new StopWatch();
        watch.start();
        for ( ExpressionExperiment ee : allEEs ) {
            QuantitationType qt = getPreferredQT( ee );
            if ( qt == null ) continue;
            Map<DesignElementDataVector, Collection<Gene>> dedvs = getDesignElementDataVectors( cs2gene, qt, ee );
            dedv2genes.putAll( dedvs );
        }
        watch.stop();
        System.err.println( "Took " + watch.getTime() / 1000 + " to retrieve data" );
        watch.reset();
        watch.start();
        Map<Long, Collection<DataVector>> geneId2dvs = new HashMap<Long, Collection<DataVector>>();
        for ( DesignElementDataVector dedv : dedv2genes.keySet() ) {
            Collection<Gene> genes = dedv2genes.get( dedv );
            for ( Gene gene : genes ) {
                Long id = gene.getId();
                Collection<DataVector> dvs = geneId2dvs.get( id );
                if ( dvs == null ) {
                    dvs = new HashSet<DataVector>();
                    geneId2dvs.put( id, dvs );
                }
                dvs.add( new DataVector( dedv ) );
            }
        }
        for ( int i = start; i < end; i++ ) {
            GenePair genePair = ( GenePair ) genePairs.get( i );
            genePair.effectSize = effectSize( geneId2dvs.get( genePair.firstId ), geneId2dvs.get( genePair.secondId ) );
        }
        watch.stop();
        System.err.println( "Took " + watch.getTime() / 1000 + " to calculation correlations" );
    }

    /**
     * 
     *
     */
    private void calculateEffectSizeForHughData() {
        Map<Long, Collection<Long>> gene2cs = getGene2CSMap();
        Map<Long, Collection<Gene>> cs2gene = new HashMap<Long, Collection<Gene>>();
        Collection<Long> geneIds = new HashSet<Long>();

        int CHUNK_LIMIT = 3000;
        int count = 0;
        StopWatch watch = new StopWatch();
        watch.start();
        System.err.println( "Start Computing Effect Size for " + genePairs.size() + " gene pairs" );
        genePairs.sort();
        for ( int i = 0; i < genePairs.size(); i++ ) {
            GenePair onePair = ( GenePair ) genePairs.get( i );
            if ( !geneIds.contains( onePair.firstId ) ) {
                cs2gene.putAll( getCs2GeneMap( onePair.firstId, gene2cs ) );
                geneIds.add( onePair.firstId );
            }
            if ( !geneIds.contains( onePair.secondId ) ) {
                cs2gene.putAll( getCs2GeneMap( onePair.secondId, gene2cs ) );
                geneIds.add( onePair.secondId );
            }
            count++;
            if ( geneIds.size() == CHUNK_LIMIT || ( i + 1 ) == genePairs.size() ) {
                calculateEffectSizeForOneChunk( ( i + 1 ) - count, i + 1, cs2gene );
                count = 0;
                geneIds.clear();
                cs2gene.clear();
                System.err.print( i + "( " + watch.getTime() / 1000 + " ) " );
            }
        }
        watch.stop();
        System.err.println( "Finished in " + watch.getTime() / 1000 + " seconds" );
    }

    // For fast version
    private double effectSize( DoubleArrayList correlations, LongArrayList eeIds ) {
        DoubleArrayList sampleSizes = new DoubleArrayList();
        for ( int i = 0; i < eeIds.size(); i++ ) {
            sampleSizes.add( eeSampleSizes.get( eeIds.get( i ) ) );
        }
        metaAnalysis.run( correlations, sampleSizes );
        double effectSize = metaAnalysis.getE();
        return effectSize;
    }

    private void calculateCorrelations( Map<DesignElementDataVector, Collection<Gene>> dedv2genes,
            Map<Long, Collection<Gene>> cs2gene, long eeId ) {
        Map<Long, Collection<DataVector>> geneId2dvs = new HashMap<Long, Collection<DataVector>>();
        for ( DesignElementDataVector dedv : dedv2genes.keySet() ) {
            Collection<Gene> genes = dedv2genes.get( dedv );
            for ( Gene gene : genes ) {
                Long id = gene.getId();
                Collection<DataVector> dvs = geneId2dvs.get( id );
                if ( dvs == null ) {
                    dvs = new HashSet<DataVector>();
                    geneId2dvs.put( id, dvs );
                }
                dvs.add( new DataVector( dedv ) );
            }
        }
        for ( int i = 0; i < genePairs.size(); i++ ) {
            GenePair genePair = ( GenePair ) genePairs.get( i );
            Collection<DataVector> source = geneId2dvs.get( genePair.firstId );
            Collection<DataVector> target = geneId2dvs.get( genePair.secondId );
            if ( source != null && target != null ) {
                double corr = 0.0;
                try {
                    corr = correlation( source, target );
                } catch ( Exception e ) {
                }
                if ( corr != 0.0 ) {
                    int eeSampleSize = source.iterator().next().val.length;
                    if ( !eeSampleSizes.containsKey( eeId ) ) {
                        eeSampleSizes.put( eeId, eeSampleSize );
                    }
                    if ( eeSampleSize > MINIMUN_SAMPLE_SIZE ) {
                        genePair.correlations.add( corr );
                        genePair.eeIds.add( eeId );
                    }
                }
            }

        }
    }

    /**
     * Calculate the effect size for each gene pair
     *
     */
    private void calculateEffectSize() {
        Map<Long, Collection<Long>> gene2cs = getGene2CSMap();
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
        System.err.println( " Got " + gene2cs.keySet().size() + " genes and " + cs2gene.keySet().size() + " CSs" );
        StopWatch watch = new StopWatch();
        watch.start();
        System.err.println( "Start Computing Effect Size for " + genePairs.size() + " gene pairs" );
        int total = 0;
        for ( ExpressionExperiment ee : allEEs ) {
            QuantitationType qt = getPreferredQT( ee );
            if ( qt == null ) continue;
            Map<DesignElementDataVector, Collection<Gene>> dedv2genes = eeService.getDesignElementDataVectors( cs2gene,
                    qt );
            calculateCorrelations( dedv2genes, cs2gene, ee.getId() );
            eeIndex.add( ee.getId() );
            eeMap.put( ee.getId(), ee );
            total = total + dedv2genes.keySet().size();
            System.err.println( ee.getId() + "----->" + dedv2genes.keySet().size() + " DEDVs" + " in "
                    + watch.getTime() / 1000 + " seconds" );
        }
        for ( int i = 0; i < genePairs.size(); i++ ) {
            GenePair onePair = ( GenePair ) genePairs.get( i );
            onePair.effectSize = effectSize( onePair.correlations, onePair.eeIds );
        }
        watch.stop();
        System.err.println( "Finished in " + watch.getTime() / 1000 + " seconds" );

    }

    private void saveToFigures( String figureFileName, int numOfGenePairs ) throws IOException {
        double[][] data = new double[numOfGenePairs][eeMap.keySet().size()];
        List<String> rowLabels = new ArrayList<String>();
        List<String> colLabels = new ArrayList<String>();
        ObjectArrayList outputGenePairs = new ObjectArrayList();
        for ( int i = 0; i < genePairs.size(); i++ ) {
            GenePair genePair = ( GenePair ) genePairs.get( i );
            if ( genePair.correlations.size() > 100 ) outputGenePairs.add( genePair );
        }
        genePairs = outputGenePairs;
        genePairs.sort();
        for ( int i = 0; i < eeIndex.size(); i++ ) {
            long eeId = eeIndex.get( i );
            colLabels.add( eeMap.get( eeId ).getShortName() );
        }
        for ( int i = 0; i < genePairs.size(); i++ ) {
            GenePair genePair = ( GenePair ) genePairs.get( i );
            rowLabels
                    .add( geneMap.get( genePair.firstId ).getName() + "_" + geneMap.get( genePair.secondId ).getName() );

            if ( i == numOfGenePairs / 2 - 1 ) i = genePairs.size() - numOfGenePairs / 2 - 1;
        }
        DoubleMatrixNamed dataMatrix = new DenseDoubleMatrix2DNamed( data );
        dataMatrix.setRowNames( rowLabels );
        dataMatrix.setColumnNames( colLabels );

        for ( int i = 0; i < genePairs.size(); i++ ) {
            GenePair genePair = ( GenePair ) genePairs.get( i );
            String rowName = geneMap.get( genePair.firstId ).getName() + "_"
                    + geneMap.get( genePair.secondId ).getName();
            int rowIndex = dataMatrix.getRowIndexByName( rowName );
            for ( int j = 0; j < genePair.eeIds.size(); j++ ) {
                long eeId = eeIndex.get( j );
                String colName = eeMap.get( eeId ).getShortName();
                int colIndex = dataMatrix.getColIndexByName( colName );
                dataMatrix.setQuick( rowIndex, colIndex, genePair.correlations.get( j ) );
            }

            if ( i == numOfGenePairs / 2 - 1 ) i = genePairs.size() - numOfGenePairs / 2 - 1;
        }
        ColorMatrix dataColorMatrix = new ColorMatrix( dataMatrix );
        dataColorMatrix.setColorMap( ColorMap.GREENRED_COLORMAP );
        JMatrixDisplay dataMatrixDisplay = new JMatrixDisplay( dataColorMatrix );
        dataMatrixDisplay.saveImage( figureFileName, true );
    }

    @Override
    protected Exception doWork( String[] args ) {
        // TODO Auto-generated method stub
        Exception err = processCommandLine( "EffectSizeCalculation ", args );
        if ( err != null ) {
            return err;
        }
        MetaLinkFinder linkFinder = new MetaLinkFinder();
        linkFinder.setGeneService( geneService );
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            linkFinder.fromFile( this.matrixFile, null );
        } catch (IOException e) {
            log.info( "Couldn't load the data from the files " );
            return e;
        }
        watch.stop();
        log.info( "Spend " + watch.getTime() / 1000 + " to load the data matrix" );

        Taxon taxon = getTaxon( this.taxonName );
        allEEs = eeService.findByTaxon( taxon );

        // First time using the following function to save candidate gene pair into a file
        // saveGenePairs("genepairs.txt");
        readGenesPairs();
        calculateEffectSize();
        try {
            saveToFigures( "correlationData.png", 2700 );
        } catch ( Exception e ) {
            return e;
        }
        return null;
    }

    /**
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

}
