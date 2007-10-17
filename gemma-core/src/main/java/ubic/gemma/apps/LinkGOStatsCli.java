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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.CompressedNamedBitMatrix;
import ubic.gemma.analysis.linkAnalysis.CommandLineToolUtilService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoImpl.ProbeLink;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

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
public class LinkGOStatsCli extends AbstractGeneExpressionExperimentManipulatingCLI {

    private final static int GO_MAXIMUM_COUNT = 50;
    private final static int MAXIMUM_LINK_NUM = 20;
    private final static int ITERATION_NUM = 100;
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
            log.info( watch.getTime() / 1000 );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private Probe2ProbeCoexpressionService p2pService = null;
    private CommandLineToolUtilService linkAnalysisUtilService = null;
    private CompressedNamedBitMatrix linkCount = null;
    private int[][] realStats = null;
    private int[][] simulatedStats = null;
    private Map<Long, Integer> eeIndexMap = new HashMap<Long, Integer>();
    private Map<Long, Gene> geneMap = new HashMap<Long, Gene>();
    private Collection<Gene> coveredGenes = new HashSet<Gene>();

    private int[] goTermsDistribution = null;

    /**
     * 
     */
    private void counting() {
        int rows = linkCount.rows();
        int cols = linkCount.columns();
        for ( int i = 0; i < rows; i++ ) {
            if ( i % 1000 == 0 ) System.err.println( "Current Row: " + i );
            int[] bits = linkCount.getRowBitCount( i );
            for ( int j = i + 1; j < cols; j++ ) {
                int bit = bits[j];
                if ( bit > 0 ) {
                    if ( bit >= MAXIMUM_LINK_NUM ) bit = MAXIMUM_LINK_NUM - 1;
                    Gene gene1 = geneMap.get( ( Long ) linkCount.getRowName( i ) );
                    Gene gene2 = geneMap.get( ( Long ) linkCount.getRowName( j ) );
                    if ( gene1 == null || gene2 == null ) {
                        log.info( "Wrong setting for gene" + ( Long ) linkCount.getRowName( i ) + "\t"
                                + ( Long ) linkCount.getRowName( j ) );
                        continue;
                    }
                    int goOverlap = linkAnalysisUtilService.computeGOOverlap( gene1, gene2 );
                    if ( !coveredGenes.contains( gene1 ) ) coveredGenes.add( gene1 );
                    if ( !coveredGenes.contains( gene2 ) ) coveredGenes.add( gene2 );
                    if ( goOverlap >= GO_MAXIMUM_COUNT )
                        realStats[bit][GO_MAXIMUM_COUNT - 1]++;
                    else
                        realStats[bit][goOverlap]++;
                }
            }
        }

    }

    private void counting( Gene[] genes, Gene[] shuffledGenes, int iterationIndex ) {
        for ( int i = 0; i < genes.length; i++ ) {
            if ( genes[i].getId() == shuffledGenes[i].getId() ) continue;
            int goOverlap = linkAnalysisUtilService.computeGOOverlap( genes[i], shuffledGenes[i] );
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
            for ( int j = 0; j < GO_MAXIMUM_COUNT; j++ )
                out.write( "\t" + j );
            out.write( "\n" );
            // double culmulative = 0.0;
            int culmulative = 0;
            for ( int i = ITERATION_NUM / CHUNK_NUM; i < ITERATION_NUM / CHUNK_NUM + 1; i++ ) {
                for ( int j = 0; j < GO_MAXIMUM_COUNT; j++ ) {
                    // culmulative = culmulative + (double)simulatedStats[i][j]/(double)totalSimulatedLinks[i];
                    culmulative = culmulative + simulatedStats[i][j];
                    out.write( "\t" + culmulative );
                }
            }
            out.write( "\n" );
            for ( int i = 1; i < MAXIMUM_LINK_NUM; i++ ) {
                culmulative = 0;
                out.write( "Link_" + i );
                for ( int j = 0; j < GO_MAXIMUM_COUNT; j++ ) {
                    // culmulative = culmulative + (double)realStats[i][j]/(double)totalRealLinks[i];
                    culmulative = culmulative + realStats[i][j];
                    out.write( culmulative + "\t" );
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
            out.write( "Go Terms Distribution for" + coveredGenes.size() + ":\n" );
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

    @Override
    protected void buildOptions() {
        // no-op.
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Shuffle Links ", args );
        if ( err != null ) {
            return err;
        }

        Collection<ExpressionExperiment> ees = null;
        if ( this.experimentListFile != null ) {
            try {
                ees = readExpressionExperimentListFile( this.experimentListFile );
            } catch ( IOException e ) {
                return e;
            }
        } else if ( taxon != null ) {
            ees = eeService.findByTaxon( taxon );
        } else {
            log.error( "You must provide either the taxon or a list of expression experiments in a file" );
            bail( ErrorCode.MISSING_OPTION );
        }

        Collection<Gene> allGenes = linkAnalysisUtilService.loadKnownGenes( taxon );
        log.info( "Loaded " + allGenes.size() + " genes" );

        for ( Gene gene : allGenes ) {
            geneMap.put( gene.getId(), gene );
        }
        int index = 0;
        for ( ExpressionExperiment ee : ees ) {
            eeIndexMap.put( ee.getId(), index );
            index++;
        }
        linkCount = new CompressedNamedBitMatrix( allGenes.size(), allGenes.size(), ees.size() );
        for ( Gene geneIter : allGenes ) {
            linkCount.addRowName( geneIter.getId() );
            linkCount.addColumnName( geneIter.getId() );
        }
        for ( ExpressionExperiment ee : ees ) {
            log.info( "Shuffling " + ee.getShortName() );
            Collection<ProbeLink> links = p2pService.getProbeCoExpression( ee, this.taxon.getCommonName(), true );
            if ( links == null || links.size() == 0 ) continue;
            fillingMatrix( links, ee );
        }
        log.info( "Counting the GO terms for real links" );
        counting();
        index = 0;
        Gene[] genes = new Gene[coveredGenes.size()];
        for ( Gene gene : coveredGenes ) {
            genes[index++] = gene;
            int goTermsNum = linkAnalysisUtilService.getGOTerms( gene ).size();
            if ( goTermsNum >= GO_MAXIMUM_COUNT ) goTermsNum = GO_MAXIMUM_COUNT - 1;
            goTermsDistribution[goTermsNum]++;
        }
        for ( int i = 0; i < ITERATION_NUM; i++ ) {
            log.info( "Current Iteration: " + i );
            Gene[] shuffledGenes = shuffling( genes );
            counting( genes, shuffledGenes, ( int ) ( i / CHUNK_NUM ) );
        }
        log.info( "Output" );
        output();

        return null;
    }

    protected void processOptions() {
        super.processOptions();

        p2pService = ( Probe2ProbeCoexpressionService ) this.getBean( "probe2ProbeCoexpressionService" );
        linkAnalysisUtilService = ( CommandLineToolUtilService ) this.getBean( "linkAnalysisUtilService" );
        realStats = new int[MAXIMUM_LINK_NUM][GO_MAXIMUM_COUNT];
        simulatedStats = new int[ITERATION_NUM / CHUNK_NUM + 1][GO_MAXIMUM_COUNT];
        goTermsDistribution = new int[GO_MAXIMUM_COUNT];
    }

}
