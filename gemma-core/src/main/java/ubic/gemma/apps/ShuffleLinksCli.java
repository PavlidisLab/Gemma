package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Format;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import com.ibm.icu.text.NumberFormat;

import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
import ubic.gemma.analysis.linkAnalysis.TreeNode;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoImpl.Link;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGeneImpl;
import ubic.gemma.model.genome.ProbeAlignedRegionImpl;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.AbstractSpringAwareCLI;

public class ShuffleLinksCli extends AbstractSpringAwareCLI {

    private String taxonName = "mouse";
    private boolean prepared = true;
    private String eeNameFile = null;
    // private long eeId = 312;
    private long eeId = 124;
    // private long eeId = -1;
    private Probe2ProbeCoexpressionService p2pService = null;
    private GeneService geneService = null;
    private ExpressionExperimentService eeService = null;
    private Map<Long, Integer> eeMap = null;
    private int iteration_num = 100;
    private final static int LINK_MAXIMUM_COUNT = 100;
    private CompressedNamedBitMatrix linkCount = null;
    private CompressedNamedBitMatrix negativeLinkCount = null;
    private int[][] stats = null;
    private int currentIteration = 0;
    private int linkStringency = 0;
    private int totalLinks = 0;
    private Set<Long> geneCoverage = new HashSet<Long>();

    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub
        Option taxonOption = OptionBuilder.hasArg().withArgName( "Taxon" ).withDescription( "the taxon name" )
                .withLongOpt( "Taxon" ).create( 't' );
        addOption( taxonOption );
        // Option eeId = OptionBuilder.hasArg().withArgName( "Expression Experiment Id" ).withDescription(
        // "Expression Experiment Id" ).withLongOpt( "eeId" ).create( 'e' );
        // addOption( eeId );
        Option eeNameFile = OptionBuilder.hasArg().withArgName( "File having Expression Experiment Names" )
                .withDescription( "File having Expression Experiment Names" ).withLongOpt( "eeFileName" ).create( 'f' );
        addOption( eeNameFile );
        Option startPreparing = OptionBuilder.withArgName( " Starting preparing " ).withDescription(
                " Starting preparing the temppory tables " ).withLongOpt( "startPreparing" ).create( 's' );
        addOption( startPreparing );

        Option iterationNum = OptionBuilder.hasArg().withArgName( " The number of iteration for shuffling " )
                .withDescription( " The number of iteration for shuffling " ).withLongOpt( "iterationNum" )
                .create( 'i' );
        addOption( iterationNum );

        Option linkStringency = OptionBuilder.hasArg().withArgName( " The Link Stringency " ).withDescription(
                " The link Stringency " ).withLongOpt( "linkStringency" ).create( 'l' );
        addOption( linkStringency );
    }

    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 't' ) ) {
            this.taxonName = getOptionValue( 't' );
        }
        if ( hasOption( 'f' ) ) {
            this.eeNameFile = getOptionValue( 'f' );
        }
        // if ( hasOption( 'e' ) ) {
        // this.eeId = Long.valueOf(getOptionValue( 'e' ));
        // }
        if ( hasOption( 's' ) ) {
            this.prepared = false;
        }
        if ( hasOption( 'i' ) ) {
            this.iteration_num = Integer.valueOf( getOptionValue( 'i' ) );
        }
        if ( hasOption( 'l' ) ) {
            this.linkStringency = Integer.valueOf( getOptionValue( 'l' ) );
        }

        p2pService = ( Probe2ProbeCoexpressionService ) this.getBean( "probe2ProbeCoexpressionService" );
        geneService = ( GeneService ) this.getBean( "geneService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        stats = new int[iteration_num + 1][LINK_MAXIMUM_COUNT];
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

    private Collection<Gene> loadGenes( Taxon taxon ) {
        Collection<Gene> allGenes = geneService.getGenesByTaxon( taxon );
        Collection<Gene> genes = new HashSet<Gene>();
        for ( Gene gene : allGenes ) {
            if ( !( gene instanceof PredictedGeneImpl ) && !( gene instanceof ProbeAlignedRegionImpl ) ) {
                genes.add( gene );
            }
        }
        log.info( "Get " + genes.size() + " genes" );
        return genes;
    }

    private Collection<ExpressionExperiment> getCandidateEE( String fileName, Collection<ExpressionExperiment> ees ) {
        if ( fileName == null ) return ees;
        Collection<ExpressionExperiment> candidates = new HashSet<ExpressionExperiment>();
        Collection<String> eeNames = new HashSet<String>();
        try {
            InputStream is = new FileInputStream( fileName );
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
            String shortName = null;
            while ( ( shortName = br.readLine() ) != null ) {
                if ( StringUtils.isBlank( shortName ) ) continue;
                eeNames.add( shortName.trim().toUpperCase() );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            return candidates;
        }
        for ( ExpressionExperiment ee : ees ) {
            String shortName = ee.getShortName();
            if ( eeNames.contains( shortName.trim().toUpperCase() ) ) candidates.add( ee );
        }
        return candidates;
    }

    private void shuffleLinks( Collection<Link> links ) {
        // Do shuffling
        Random random = new Random();
        Object[] linksInArray = links.toArray();
        for ( int i = linksInArray.length - 1; i >= 0; i-- ) {
            int pos = random.nextInt( i + 1 );
            Long tmpId = ( ( Link ) linksInArray[pos] ).getSecond_design_element_fk();
            ( ( Link ) linksInArray[pos] ).setSecond_design_element_fk( ( ( Link ) linksInArray[i] )
                    .getSecond_design_element_fk() );
            ( ( Link ) linksInArray[i] ).setSecond_design_element_fk( tmpId );
        }
    }

    private CompressedNamedBitMatrix getMatrix( Collection<ExpressionExperiment> ees, Collection<Gene> genes ) {
        CompressedNamedBitMatrix linkCount = new CompressedNamedBitMatrix( genes.size(), genes.size(), ees.size() );
        for ( Gene geneIter : genes ) {
            linkCount.addRowName( geneIter.getId() );
        }
        for ( Gene geneIter : genes ) {
            linkCount.addColumnName( geneIter.getId() );
        }
        return linkCount;
    }

    private void fillingMatrix( Collection<Link> links, ExpressionExperiment ee ) {
        Set<Long> csIds = new HashSet<Long>();
        for ( Link link : links ) {
            csIds.add( link.getFirst_design_element_fk() );
            csIds.add( link.getSecond_design_element_fk() );
        }
        Map<Long, Collection<Long>> cs2genes = geneService.getCS2GeneMap( csIds );
        int eeIndex = eeMap.get( ee.getId() );
        for ( Link link : links ) {
            Collection<Long> firstGeneIds = cs2genes.get( link.getFirst_design_element_fk() );
            Collection<Long> secondGeneIds = cs2genes.get( link.getSecond_design_element_fk() );
            if ( firstGeneIds == null || secondGeneIds == null ) {
                log.info( " Preparation is not correct (get null genes) " + link.getFirst_design_element_fk() + ","
                        + link.getSecond_design_element_fk() );
                continue;
            }
            // if(firstGeneIds.size() != 1 || secondGeneIds.size() != 1){
            // log.info(" Preparation is not correct (get non-specific genes)" + link.getFirst_design_element_fk() + ","
            // + link.getSecond_design_element_fk());
            // System.exit(0);
            // }
            for ( Long firstGeneId : firstGeneIds ) {
                for ( Long secondGeneId : secondGeneIds ) {
                    geneCoverage.add( firstGeneId );
                    geneCoverage.add( secondGeneId );
                    try {
                        int rowIndex = linkCount.getRowIndexByName( firstGeneId );
                        int colIndex = linkCount.getColIndexByName( secondGeneId );
                        if ( link.getScore() > 0 ) {
                            linkCount.set( rowIndex, colIndex, eeIndex );
                        } else {
                            negativeLinkCount.set( rowIndex, colIndex, eeIndex );
                        }
                    } catch ( Exception e ) {
                        log.info( " No Gene Definition " + firstGeneId + "," + secondGeneId );
                        // Aligned Region and Predicted Gene
                        continue;
                    }
                }
            }
            totalLinks++;
        }
    }

    private void counting() {
        int rows = linkCount.rows();
        int cols = linkCount.columns();
        // The filling process only filled one item. So the matrix is not symetric
        for ( int i = 0; i < rows; i++ ) {
            int[] positiveBits = new int[cols];
            positiveBits = linkCount.getRowBits( i, positiveBits );
            int[] negativeBits = new int[cols];
            negativeBits = negativeLinkCount.getRowBits( i, negativeBits );
            for ( int j = 0; j < cols; j++ ) {
                int positiveBit = positiveBits[j];
                int negativeBit = negativeBits[j];
                if ( positiveBit > 0 ) {
                    stats[currentIteration][positiveBit]++;
                }
                if ( negativeBit > 0 ) {
                    stats[currentIteration][negativeBit]++;
                }
            }
        }

    }

    private void doShuffling( Collection<ExpressionExperiment> ees ) {
        int total = 0;
        for ( ExpressionExperiment ee : ees ) {
            log.info( "Shuffling " + ee.getShortName() );
            Collection<Link> links = p2pService.getProbeCoExpression( ee, this.taxonName, true );
            if ( links == null || links.size() == 0 ) continue;
            if ( currentIteration != 0 ) {
                total = total + links.size();
                shuffleLinks( links );
            }
            fillingMatrix( links, ee );
        }
        counting();
        log.info( " Shuffled " + total + " links" );
    }

    private void saveMatrix( String outFile, Collection<Gene> genes ) {
        Map<Long, String> geneId2Name = new HashMap<Long, String>();
        for ( Gene gene : genes ) {
            geneId2Name.put( gene.getId(), gene.getName() );
        }
        try {
            FileWriter out = new FileWriter( new File( outFile ) );
            int rows = linkCount.rows();
            int cols = linkCount.columns();
            // The filling process only filled one item. So the matrix is not symetric
            for ( int i = 0; i < rows; i++ ) {
                int[] positiveBits = new int[cols];
                positiveBits = linkCount.getRowBits( i, positiveBits );
                int[] negativeBits = new int[cols];
                negativeBits = negativeLinkCount.getRowBits( i, negativeBits );

                for ( int j = 0; j < cols; j++ ) {
                    int positiveBit = positiveBits[j];
                    int negativeBit = negativeBits[j];
                    if ( this.linkStringency > 0 ) {
                        if ( positiveBit >= this.linkStringency ) {
                            out.write( geneId2Name.get( linkCount.getRowName( i ) ) + "\t"
                                    + geneId2Name.get( linkCount.getColName( j ) ) + "\t" + positiveBit + "\t" + "+"
                                    + "\n" );
                        }
                        if ( negativeBit >= this.linkStringency ) {
                            out.write( geneId2Name.get( linkCount.getRowName( i ) ) + "\t"
                                    + geneId2Name.get( linkCount.getColName( j ) ) + "\t" + negativeBit + "\t" + "-"
                                    + "\n" );
                        }
                    } else {
                        if ( positiveBit != 0 || negativeBit != 0 ) {
                            out.write( geneId2Name.get( linkCount.getRowName( i ) ) + "\t"
                                    + geneId2Name.get( linkCount.getColName( j ) ) + "\t" + positiveBit + "\t"
                                    + negativeBit + "\n" );
                        }
                    }
                }
            }
            out.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    private void saveStats( String outFile ) {
        try {
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits( 3 );

            FileWriter out = new FileWriter( new File( outFile ) );
            for ( int i = 0; i < iteration_num + 1; i++ ) {
                for ( int j = LINK_MAXIMUM_COUNT - 2; j >= 0; j-- ) {
                    stats[i][j] = stats[i][j] + stats[i][j + 1];
                }
            }
            int maxBits = 0;
            for ( int j = LINK_MAXIMUM_COUNT - 1; j >= 0; j-- ) {
                if ( stats[0][j] != 0 ) {
                    maxBits = j;
                    break;
                }
            }
            for ( int j = 1; j <= maxBits; j++ )
                out.write( "Link" + j + "\t" );
            out.write( "\n" );
            out.write( "Link Distribution:\n" );
            for ( int j = 1; j <= maxBits; j++ ) {
                out.write( stats[0][j] + "\t" );
            }
            out.write( "\n" );
            out.write( "Average False Positive Rate\n" );
            double[] falsePositiveRates = new double[maxBits + 1];
            for ( int i = 1; i < iteration_num + 1; i++ ) {
                for ( int j = 1; j <= maxBits; j++ ) {
                    if ( stats[0][j] != 0 )
                        falsePositiveRates[j] = falsePositiveRates[j] + ( double ) stats[i][j] / ( double ) stats[0][j];
                }
            }
            for ( int j = 1; j < maxBits; j++ ) {
                out.write( nf.format( falsePositiveRates[j] / iteration_num ) + "\t" );
            }
            out.write( "\n" );
            out.write( "All shuffled false positive rates:\n" );
            for ( int i = 1; i < iteration_num + 1; i++ ) {
                for ( int j = 1; j <= maxBits; j++ ) {
                    if ( stats[0][j] == 0 )
                        out.write( "\t" );
                    else
                        out.write( nf.format( ( double ) stats[i][j] / ( double ) stats[0][j] ) + "\t" );
                }
                out.write( "\n" );
            }
            out.close();
        } catch ( Exception e ) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    protected Exception doWork( String[] args ) {
        // TODO Auto-generated method stub
        Exception err = processCommandLine( "Shuffle Links ", args );
        if ( err != null ) {
            return err;
        }
        Taxon taxon = getTaxon( taxonName );
        Collection<ExpressionExperiment> ees = eeService.findByTaxon( taxon );
        Collection<ExpressionExperiment> candidates = getCandidateEE( this.eeNameFile, ees );
        if ( !prepared ) {
            log.info( " Create intermediate tables for shuffling " );
            StopWatch watch = new StopWatch();
            watch.start();
            p2pService.prepareForShuffling( candidates, taxonName );
            watch.stop();
            log.info( " Spent " + watch.getTime() / 1000 + " to finish the preparation " );
            System.exit( 0 );
        }
        Collection<Gene> genes = loadGenes( taxon );
        eeMap = new HashMap<Long, Integer>();
        int index = 0;
        for ( ExpressionExperiment eeIter : candidates ) {
            eeMap.put( eeIter.getId(), new Integer( index ) );
            index++;
        }
        if ( linkStringency != 0 ) {
            totalLinks = 0;
            linkCount = getMatrix( ees, genes );
            negativeLinkCount = getMatrix( ees, genes );
            System.gc();
            doShuffling( candidates );
            saveMatrix( "matrix_" + linkStringency + ".txt", genes );
            log.info( "Total Links " + totalLinks );
            log.info( "Covered Gene " + geneCoverage.size() );
        } else {
            // The first iteration doesn't do the shuffling and only read the real data and do the counting
            for ( currentIteration = 0; currentIteration < iteration_num + 1; currentIteration++ ) {
                totalLinks = 0;
                linkCount = getMatrix( ees, genes );
                negativeLinkCount = getMatrix( ees, genes );
                System.gc();
                doShuffling( candidates );
                saveMatrix( "matrix" + currentIteration + ".txt", genes );
            }
            saveStats( "stats.txt" );
        }
        // if(this.eeId > 0){
        // ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        // ee.setId(this.eeId);
        // p2pService.shuffle(ee, taxonName);
        // }
        // else{
        // p2pService.shuffle(taxonName);
        // }
        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub
        ShuffleLinksCli shuffle = new ShuffleLinksCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = shuffle.doWork( args );
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
