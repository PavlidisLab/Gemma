/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
import ubic.gemma.analysis.linkAnalysis.GeneLink;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoImpl.ProbeLink;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.GoMetric;
import ubic.gemma.ontology.GoMetric.Metric;

/**
 * <p>
 * Collect the statistics of GO similarities of the computed linkss from compared to random generated links.
 * </p>
 * 
 * <pre>
 * java -Xmx5G  -jar linkGOStats.jar -f mouse_brain_dataset.txt  -t mouse -u administrator -p xxxxx -v 3
 * </pre>
 * 
 * <p>
 * FIXME this class reproduces code in the ShuffleLinksCli. This should also load links from a file.
 * 
 * @author xwan
 * @version $Id$
 */
public class LinkGOStatsCli extends ExpressionExperimentManipulatingCLI {

    private final static int GO_MAXIMUM_COUNT = 50;
    private final static int MAXIMUM_LINK_NUM = 20;
    private final static int ITERATION_NUM = 1;
    private final static int CHUNK_NUM = 20;

    /**
     * @param args
     */
    public static void main( String[] args ) {

        LinkGOStatsCli goStats = new LinkGOStatsCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = goStats.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Finished: elapsed time=" + watch.getTime() / 1000 );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private Probe2ProbeCoexpressionService p2pService = null;
    private GoMetric goMetricService;
    private CompressedNamedBitMatrix linkCount = null;
    private int[][] realStats = null;
    private int[][] simulatedStats = null;
    private Map<Long, Integer> eeIndexMap = new HashMap<Long, Integer>();
    private Map<Long, Gene> geneMap = new HashMap<Long, Gene>();
    private Collection<Gene> coveredGenes = new HashSet<Gene>();

    private int[] goTermsDistribution = null;
    private GeneOntologyService geneOntologyService;
    private String linkFile = null;

    /**
     * Do count directly from a link matrix.
     */
    private void counting() {
        int rows = linkCount.rows();
        int cols = linkCount.columns();
        for ( int i = 0; i < rows; i++ ) {
            if ( i % 1000 == 0 ) System.err.println( "Current Row: " + i );
            int[] bits = linkCount.getRowBitCount( i );
            for ( int j = i + 1; j < cols; j++ ) {
                int support = bits[j];
                if ( support > 0 ) {
                    if ( support >= MAXIMUM_LINK_NUM ) support = MAXIMUM_LINK_NUM - 1;
                    Gene gene1 = geneMap.get( linkCount.getRowName( i ) );
                    Gene gene2 = geneMap.get( linkCount.getRowName( j ) );
                    if ( gene1 == null || gene2 == null ) {
                        log.info( "Wrong setting for gene" + linkCount.getRowName( i ) + "\t"
                                + linkCount.getRowName( j ) );
                        continue;
                    }
                    // int goOverlap = linkAnalysisUtilService.computeGOOverlap( gene1, gene2 );
                    int goOverlap = goMetricService.computeSimilarity( gene1, gene2, null, Metric.simple ).intValue();
                    if ( !coveredGenes.contains( gene1 ) ) coveredGenes.add( gene1 );
                    if ( !coveredGenes.contains( gene2 ) ) coveredGenes.add( gene2 );
                    if ( goOverlap >= GO_MAXIMUM_COUNT ) {
                        realStats[support][GO_MAXIMUM_COUNT - 1]++;
                    } else {
                        realStats[support][goOverlap]++;
                    }
                }
            }
        }

    }

    private void count( Collection<GeneLink> links ) {
        int count = 0;
        int numToDo = links.size();
        for ( GeneLink link : links ) {
            Gene gene1 = geneMap.get( link.getFirstGene() );
            Gene gene2 = geneMap.get( link.getSecondGene() );

            assert gene1 != null;
            assert gene2 != null;

            int goOverlap = goMetricService.computeSimilarity( gene1, gene2, null, Metric.simple ).intValue();
            int support = link.getScore().intValue();
            assert support > 0; // should have filtered these out by now.
            if ( support >= MAXIMUM_LINK_NUM ) support = MAXIMUM_LINK_NUM - 1;
            if ( goOverlap >= GO_MAXIMUM_COUNT ) {
                realStats[support][GO_MAXIMUM_COUNT - 1]++;
            } else {
                realStats[support][goOverlap]++;
            }
            if ( ++count % 5e4 == 0 ) {
                log.info( "Counted GO similarity for " + count + "/" + numToDo + " links ... " );
            }
        }
        log.info( "Counted GO similarity for " + count + " links." );
    }

    private void counting( Gene[] genes, Gene[] shuffledGenes, int iterationIndex ) {
        for ( int i = 0; i < genes.length; i++ ) {
            if ( genes[i].getId() == shuffledGenes[i].getId() ) continue;
            int goOverlap = goMetricService.computeSimilarity( genes[i], shuffledGenes[i], null, Metric.simple )
                    .intValue();

            if ( goOverlap >= GO_MAXIMUM_COUNT ) {
                simulatedStats[iterationIndex][GO_MAXIMUM_COUNT - 1]++;
                simulatedStats[ITERATION_NUM / CHUNK_NUM][GO_MAXIMUM_COUNT - 1]++;
            } else {
                simulatedStats[iterationIndex][goOverlap]++;
                simulatedStats[ITERATION_NUM / CHUNK_NUM][goOverlap]++;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void fillingMatrix( Collection<ProbeLink> links, ExpressionExperiment ee ) {
        Collection<Long> csIds = new HashSet<Long>();
        for ( ProbeLink link : links ) {
            csIds.add( link.getFirstDesignElementId() );
            csIds.add( link.getSecondDesignElementId() );
        }
        // FIXME this used to only return known genes.
        Map<Long, Collection<Long>> cs2genes = geneService.getCS2GeneMap( csIds );
        int eeIndex = eeIndexMap.get( ee.getId() );
        for ( ProbeLink link : links ) {
            Collection<Long> firstGeneIds = cs2genes.get( link.getFirstDesignElementId() );
            Collection<Long> secondGeneIds = cs2genes.get( link.getSecondDesignElementId() );
            if ( firstGeneIds == null || secondGeneIds == null ) {
                log.info( " Preparation is not correct (get null genes) " + link.getFirstDesignElementId() + ","
                        + link.getSecondDesignElementId() );
                continue;
            }
            // if(firstGeneIds.size() != 1 || secondGeneIds.size() != 1){
            // log.info(" Preparation is not correct (get non-specific genes)" + link.getFirst_design_element_fk() + ","
            // + link.getSecond_design_element_fk());
            // System.exit(0);
            // }
            for ( Long firstGeneId : firstGeneIds ) {
                for ( Long secondGeneId : secondGeneIds ) {
                    firstGeneId = firstGeneIds.iterator().next();
                    secondGeneId = secondGeneIds.iterator().next();
                    try {
                        int rowIndex = linkCount.getRowIndexByName( firstGeneId );
                        int colIndex = linkCount.getColIndexByName( secondGeneId );
                        linkCount.set( rowIndex, colIndex, eeIndex );
                        linkCount.set( colIndex, rowIndex, eeIndex );
                    } catch ( Exception e ) {
                        log.info( " No Gene Definition " + firstGeneId + "," + secondGeneId );
                        // Aligned Region and Predicted Gene
                        continue;
                    }
                }
            }
        }
    }

    private void outputRealLinks() {
        try {
            FileWriter out = new FileWriter( new File( "analysis.txt" ) );

            out.write( "Overlap" );
            for ( int j = 0; j < GO_MAXIMUM_COUNT; j++ )
                out.write( "\t" + j );
            out.write( "\n" );

            for ( int i = 1; i < realStats.length; i++ ) {
                out.write( "Support=" + i );
                for ( int j = 0; j < realStats[i].length; j++ ) {
                    out.write( "\t" + realStats[i][j] );
                }
                out.write( "\n" );
            }

            // output( realStats, "realGODist.png" );

            out.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    // /**
    // * @param stats
    // * @param imageName
    // * @throws Exception
    // */
    // private void output( int[][] stats, String imageName ) throws Exception {
    // List<String> rowLabels = new ArrayList<String>();
    // List<String> colLabels = new ArrayList<String>();
    // for ( int i = 0; i < GO_MAXIMUM_COUNT; i++ ) {
    // colLabels.add( Integer.toString( i ) );
    // }
    // // double culmulative = 0.0;
    // int culmulatives[] = new int[stats.length];
    // for ( int i = 0; i < stats.length; i++ ) {
    // for ( int j = 0; j < GO_MAXIMUM_COUNT; j++ ) {
    // culmulatives[i] = culmulatives[i] + stats[i][j];
    // }
    // }
    // for ( ExpressionExperiment ee : eeIndexMap.keySet() ) {
    // if ( noLinkEEs.contains( ee ) ) continue;
    // rowLabels.add( ee.getShortName() );
    // }
    // double data[][] = new double[stats.length - noLinkEEs.size()][GO_MAXIMUM_COUNT];
    // int dataIndex = 0;
    // for ( ExpressionExperiment ee : eeIndexMap.keySet() ) {
    // if ( noLinkEEs.contains( ee ) ) continue;
    // int eeIndex = eeIndexMap.get( ee );
    // for ( int j = 0; j < GO_MAXIMUM_COUNT; j++ ) {
    // data[dataIndex][j] = ( double ) stats[eeIndex][j] / ( double ) culmulatives[eeIndex];
    // }
    // dataIndex++;
    // }
    // DoubleMatrixNamed dataMatrix = new DenseDoubleMatrix2DNamed( data );
    // dataMatrix.setRowNames( rowLabels );
    // dataMatrix.setColumnNames( colLabels );
    //
    // ColorMatrix dataColorMatrix = new ColorMatrix( dataMatrix );
    // // dataColorMatrix.setColorMap( ColorMap.GREENRED_COLORMAP );
    // dataColorMatrix.setColorMap( ColorMap.BLACKBODY_COLORMAP );
    // JMatrixDisplay dataMatrixDisplay = new JMatrixDisplay( dataColorMatrix );
    // dataMatrixDisplay.saveImage( imageName, true );
    //
    // }

    /**
     * 
     *
     */
    private void output() {
        int totalSimulatedLinks[] = new int[ITERATION_NUM / CHUNK_NUM + 1], totalRealLinks[] = new int[MAXIMUM_LINK_NUM];
        for ( int i = 0; i < ITERATION_NUM / CHUNK_NUM + 1; i++ ) {
            for ( int j = 0; j < GO_MAXIMUM_COUNT; j++ ) {
                totalSimulatedLinks[i] = totalSimulatedLinks[i] + simulatedStats[i][j];
            }
        }
        for ( int i = 1; i < MAXIMUM_LINK_NUM; i++ ) {
            for ( int j = 0; j < GO_MAXIMUM_COUNT; j++ ) {
                totalRealLinks[i] = totalRealLinks[i] + realStats[i][j];
            }
        }
        try {
            FileWriter out = new FileWriter( new File( "goSimilarity.txt" ) );
            out.write( "GoTerms" );
            for ( int j = 0; j < GO_MAXIMUM_COUNT; j++ )
                out.write( "\t" + j );
            out.write( "\n" );
            // double culmulative = 0.0;
            int cumulative = 0;
            for ( int i = ITERATION_NUM / CHUNK_NUM; i < ITERATION_NUM / CHUNK_NUM + 1; i++ ) {
                for ( int j = 0; j < GO_MAXIMUM_COUNT; j++ ) {
                    // culmulative = culmulative + (double)simulatedStats[i][j]/(double)totalSimulatedLinks[i];
                    cumulative = cumulative + simulatedStats[i][j];
                    out.write( "\t" + cumulative );
                }
            }
            out.write( "\n" );
            for ( int i = 1; i < MAXIMUM_LINK_NUM; i++ ) {
                cumulative = 0;
                out.write( "Link_" + i );
                for ( int j = 0; j < GO_MAXIMUM_COUNT; j++ ) {
                    // culmulative = culmulative + (double)realStats[i][j]/(double)totalRealLinks[i];
                    cumulative = cumulative + realStats[i][j];
                    out.write( cumulative + "\t" );
                }
                out.write( "\n" );
            }
            out.write( "\nRandom Pair Generation Distribution:\n" );
            double densityCulmulative = 0.0;
            for ( int i = 0; i < ITERATION_NUM / CHUNK_NUM + 1; i++ ) {
                densityCulmulative = 0.0;
                for ( int j = 0; j < GO_MAXIMUM_COUNT; j++ ) {
                    densityCulmulative = densityCulmulative + ( double ) simulatedStats[i][j]
                            / ( double ) totalSimulatedLinks[i];
                    out.write( densityCulmulative + "\t" );
                }
                out.write( "\n" );
            }

            int total = 0;
            for ( int i = 0; i < GO_MAXIMUM_COUNT; i++ )
                total = total + goTermsDistribution[i];
            out.write( "Go Terms Distribution for " + coveredGenes.size() + " genes:\n" );
            for ( int i = 0; i < GO_MAXIMUM_COUNT; i++ ) {
                out.write( ( double ) goTermsDistribution[i] / ( double ) total + "\t" );
            }
            out.write( "\n" );
            out.close();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    private Gene[] shuffling( Gene[] genes ) {
        Gene[] shuffledGenes = new Gene[genes.length];
        System.arraycopy( genes, 0, shuffledGenes, 0, genes.length );
        Random random = new Random();
        for ( int i = genes.length - 1; i >= 0; i-- ) {
            int pos = random.nextInt( i + 1 );
            Gene tmp = shuffledGenes[pos];
            shuffledGenes[pos] = shuffledGenes[i];
            shuffledGenes[i] = tmp;
        }
        return shuffledGenes;
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option linkFileOption = OptionBuilder.hasArg().withArgName( "Link file path" ).withDescription(
                "File with list of links (output of LinkStatisticsCLI)" ).create( "linkfile" );
        addOption( linkFileOption );
    }

    /**
     * FIXME This is duplicated code from LinkStatisticsCLI.
     * 
     * @return collection of known genes for the taxon selected on the command line. Known genes basically means NCBI
     *         genes (not PARs and not "predicted").
     */
    @SuppressWarnings("unchecked")
    private Collection<Gene> getKnownGenes() {
        log.info( "Loading genes ..." );
        Collection<Gene> genes = geneService.getGenesByTaxon( taxon );
        Collection<Gene> knownGenes = new HashSet<Gene>();
        for ( Gene g : genes ) {
            // FIXME this should be optional, though the number of all genes together is really big.
            if ( !( g instanceof ProbeAlignedRegion ) && !( g instanceof PredictedGene ) ) {
                knownGenes.add( g );
            }
        }
        log.info( "Using " + knownGenes.size() + " 'known genes' for analysis" );
        return knownGenes;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Link GO stats ", args );
        if ( err != null ) {
            return err;
        }

        geneOntologyService.init( true );

        while ( !this.geneOntologyService.isReady() ) {
            log.info( "Waiting for GO to load ..." );
            try {
                Thread.sleep( 1000 );
                System.err.print( "." );
            } catch ( InterruptedException e ) {
                return e;
            }
        }

        Collection<GeneLink> linksFromFile = null;
        if ( this.linkFile != null ) {

            try {
                linksFromFile = loadLinks( linkFile );
            } catch ( IOException e ) {
                return e;
            }

        }

        if ( linkFile != null ) {
            count( linksFromFile );
        } else {
            Collection<Gene> allGenes = getKnownGenes();
            log.info( "Loaded " + allGenes.size() + " genes" );

            for ( Gene gene : allGenes ) {
                geneMap.put( gene.getId(), gene );
            }
            int index = 0;
            for ( ExpressionExperiment ee : expressionExperiments ) {
                eeIndexMap.put( ee.getId(), index );
                index++;
            }
            linkCount = new CompressedNamedBitMatrix( allGenes.size(), allGenes.size(), expressionExperiments.size() );
            for ( Gene geneIter : allGenes ) {
                linkCount.addRowName( geneIter.getId() );
                linkCount.addColumnName( geneIter.getId() );
            }
            for ( ExpressionExperiment ee : expressionExperiments ) {
                log.info( "Shuffling " + ee.getShortName() );
                Collection<ProbeLink> links = p2pService.getProbeCoExpression( ee, this.taxon.getCommonName(), true );
                if ( links == null || links.size() == 0 ) continue;
                fillingMatrix( links, ee );
            }

            log.info( "Counting the GO terms for real links" );
            counting();
        }

        // Summarize
        // int index = 0;
        // Gene[] genes = new Gene[coveredGenes.size()];
        // for ( Gene gene : coveredGenes ) {
        // genes[index++] = gene;
        // int goTermsNum = geneOntologyService.getGOTerms( gene ).size();
        // if ( goTermsNum >= GO_MAXIMUM_COUNT ) goTermsNum = GO_MAXIMUM_COUNT - 1;
        // goTermsDistribution[goTermsNum]++;
        // }

        // log.info( "Go analysis for random links" ); // THISIS WRONG
        // for ( int i = 0; i < ITERATION_NUM; i++ ) {
        // log.info( "Current Iteration: " + i );
        // Gene[] shuffledGenes = shuffling( genes );
        // counting( genes, shuffledGenes, ( int ) ( i / CHUNK_NUM ) );
        // }
        // log.info( "Output" );
        outputRealLinks();
        // output();

        return null;
    }

    /**
     * @param filepath
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private Collection<GeneLink> loadLinks( String filepath ) throws IOException {

        File f = new File( filepath );
        if ( !f.canRead() ) {
            throw new IOException( "Cannot read from " + filepath );
        }
        log.info( "Loading links from " + filepath );
        BufferedReader in = new BufferedReader( new FileReader( f ) );

        Collection<GeneLink> links = new HashSet<GeneLink>();

        Map<String, Gene> geneCache = new HashMap<String, Gene>();

        int count = 0;
        while ( in.ready() ) {
            String line = in.readLine().trim();
            if ( line.startsWith( "#" ) ) {
                continue;
            }

            String[] strings = StringUtils.split( line );
            String g1 = strings[0];
            String g2 = strings[1];

            // skip any self links.
            if ( g1.equals( g2 ) ) continue;

            String support = strings[2];// positive only!

            if ( support.equals( "0" ) ) continue;

            Gene gene1 = null;
            Gene gene2 = null;

            if ( geneCache.containsKey( g1 ) ) {
                gene1 = geneCache.get( g1 );
            } else {
                Collection<Gene> genes = geneService.findByOfficialSymbol( g1 );
                for ( Gene gene : genes ) {
                    if ( gene.getTaxon().equals( taxon ) ) {
                        geneCache.put( g1, gene );
                        gene1 = gene;
                        coveredGenes.add( gene1 );
                        this.geneMap.put( gene1.getId(), gene1 );
                        break;
                    }
                }

            }
            if ( geneCache.containsKey( g2 ) ) {
                gene2 = geneCache.get( g2 );
            } else {
                Collection<Gene> genes = geneService.findByOfficialSymbol( g2 );
                for ( Gene gene : genes ) {
                    if ( gene.getTaxon().equals( taxon ) ) {
                        geneCache.put( g2, gene );
                        gene2 = gene;
                        coveredGenes.add( gene2 );
                        this.geneMap.put( gene2.getId(), gene2 );
                        break;
                    }
                }
            }

            if ( gene1 == null || gene2 == null ) {
                log.error( "Could not locate one or both of '" + g1 + "' or '" + g2 + "' for " + taxon.getCommonName() );
                continue;
            }

            GeneLink geneLink = new GeneLink( gene1.getId(), gene2.getId(), new Double( support ) );
            links.add( geneLink );
            if ( ++count % 5e5 == 0 ) {
                log.info( "Loaded " + count + " links" );
            }
        }
        in.close();
        log.info( "Loaded " + count + " links" );
        return links;
    }

    protected void processOptions() {
        super.processOptions();

        if ( hasOption( "linkfile" ) ) {
            this.linkFile = this.getOptionValue( "linkfile" );
        }

        p2pService = ( Probe2ProbeCoexpressionService ) this.getBean( "probe2ProbeCoexpressionService" );
        this.goMetricService = ( GoMetric ) this.getBean( "goMetric" );
        this.geneOntologyService = ( GeneOntologyService ) this.getBean( "geneOntologyService" );
        realStats = new int[MAXIMUM_LINK_NUM][GO_MAXIMUM_COUNT];
        simulatedStats = new int[ITERATION_NUM / CHUNK_NUM + 1][GO_MAXIMUM_COUNT];
        goTermsDistribution = new int[GO_MAXIMUM_COUNT];
    }

}
