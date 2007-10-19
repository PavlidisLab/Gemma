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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.linkAnalysis.CommandLineToolUtilService;
import ubic.gemma.analysis.linkAnalysis.LinkConfirmationStatistics;
import ubic.gemma.analysis.linkAnalysis.LinkStatistics;
import ubic.gemma.analysis.linkAnalysis.LinkStatisticsService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * Used to count up links and to generate the link(gene pair) background distribution, which could be used to estimate
 * the false positive rates under different levels of confirmation. When shuffling, there are two steps to finish this
 * process. The first step is to prepare the working table. Then the analysis is done using the working table.
 * <p>
 * To create the working table:
 * 
 * <pre>
 * java -Xmx5G   -jar shuffleLinksCli.jar -s -f mouse_brain_dataset.txt  -t mouse -u administrator -p xxxxxx -v 3
 * </pre>
 * 
 * <p>
 * The second step is to do the shuffling using the working table
 * </p>
 * 
 * <pre>
 * java -Xmx5G   -jar shuffleLinksCli.jar -i 100 -f mouse_brain_dataset.txt  -t mouse -u administrator -p xxxxxx -v 3
 * </pre>
 * 
 * <p>
 * Outputs are a file with the real links, and summary statistics to STOUT.
 * <p>
 * Implementation note: The reason to make a temporary table; In Gemma, the link tables store each links twice (the
 * duplicate one with firstDesignElement and secondDesignElement switched) to speed up the online co-expression query.
 * Some huge expression experiments give rise to a large amount of links more than 10M. However, the shuffling need to
 * go through all expression experiments one by one to extract all links for each expression experiment and this process
 * is required to repeat many times (default is 100) to get better estimation on the background distribution. Therefore,
 * to speed up the shuffling process, the first step will create a new table to save the links without redundancy. It
 * could also do some filtering (only save links for known genes). Then the next step will do the shuffling on the
 * working table, which runs much faster.
 * 
 * @author xwan
 * @version $Id$
 */
public class LinkStatisticsCLI extends AbstractGeneExpressionExperimentManipulatingCLI {

    public static void main( String[] args ) {
        LinkStatisticsCLI shuffle = new LinkStatisticsCLI();
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

    private boolean prepared = true;

    private CommandLineToolUtilService linkAnalysisUtilService;

    /*
     * How many shuffled runs to do. 2 or 3 is enough to get a quick idea of what the results will look like; 100 is
     * better for final analysis.
     */
    private int numIterationsToDo = 0;

    private int currentIteration = 0;

    private int linkStringency = 0;

    /*
     * Print out the link details for shuffled data sets. This is only useful for debugging (big files).
     */
    private boolean doShuffledOutput = false;

    /*
     * If false, just do shuffling.
     */
    private boolean doRealAnalysis = true;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option startPreparing = OptionBuilder.withArgName( "Prepare only" ).withDescription(
                "Prepare temorary table for analysis" ).withLongOpt( "prepare" ).create( 's' );
        addOption( startPreparing );

        Option iterationNum = OptionBuilder.hasArg().withArgName( " The number of iteration for shuffling " )
                .withDescription( " The number of iterations for shuffling (default = 0 " )
                .withLongOpt( "iterationNum" ).create( 'i' );
        addOption( iterationNum );

        /*
         * Not sure exactly what this is for.
         */
        Option linkStringency = OptionBuilder.hasArg().withArgName( "Link support threshold (stringency)" )
                .withDescription( "Link Stringency " ).withLongOpt( "linkStringency" ).create( 'l' );
        addOption( linkStringency );
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
        LinkStatisticsService lss = ( LinkStatisticsService ) this.getBean( "linkStatisticsService" );

        if ( !prepared ) {
            lss.prepareDatabase( ees, taxon.getCommonName() );
            return null;
        }

        Collection<Gene> genes = linkAnalysisUtilService.loadKnownGenes( taxon );

        if ( linkStringency != 0 ) {
            //
            // // not sure I understand what this is for.
            // totalLinks = 0;
            // linkCount = lss.getMatrix( ees, genes );
            // negativeLinkCount = lss.getMatrix( ees, genes );
            // System.gc();
            // // doShuffling( candidates );
            // lss.doGeneLevelShuffling( currentIteration, candidates );
            // if ( doShuffledOutput ) {
            // String fileName = "shuffledLinks_" + linkStringency + ".txt";
            // Writer w = new FileWriter( new File( fileName ) );
            // lss.saveMatrix( linkCount, negativeLinkCount, w, genes, this.linkStringency );
            // }
            // log.info( "Total Links " + totalLinks );
            // log.info( "Covered Gene " + geneCoverage.size() );
        } else {

            LinkConfirmationStatistics confStats = null;

            if ( doRealAnalysis ) { // Currently this is really just for debugging purposes, though reading in from a
                // file might be useful.
                LinkStatistics realStats = lss.analyze( ees, genes, taxon.getCommonName(), false );
                confStats = realStats.getLinkConfirmationStats();

                try {
                    Writer linksOut = new BufferedWriter( new FileWriter( new File( "link-data.txt" ) ) );
                    realStats.writeLinks( linksOut, 0 );
                } catch ( IOException e ) {
                    return e;
                }
            }

            List<LinkConfirmationStatistics> shuffleRuns = new ArrayList<LinkConfirmationStatistics>();
            if ( this.numIterationsToDo > 0 ) {
                log.info( "Running shuffled runs" );
                for ( currentIteration = 0; currentIteration < numIterationsToDo + 1; currentIteration++ ) {
                    log.info( "*** Iteration " + currentIteration + " ****" );

                    LinkStatistics sr = lss.analyze( ees, genes, taxon.getCommonName(), true );
                    shuffleRuns.add( sr.getLinkConfirmationStats() );

                }
            }

            Writer out = new PrintWriter( System.out );
            lss.writeStats( out, confStats, shuffleRuns );
        }

        return null;
    }

    /**
     * 
     */
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( 's' ) ) {
            this.prepared = false;
        }
        if ( hasOption( 'i' ) ) {
            this.numIterationsToDo = Integer.valueOf( getOptionValue( 'i' ) );
        }
        if ( hasOption( 'l' ) ) {
            this.linkStringency = Integer.valueOf( getOptionValue( 'l' ) );
        }

        linkAnalysisUtilService = ( CommandLineToolUtilService ) this.getBean( "commandLineToolUtilService" );
    }

}
