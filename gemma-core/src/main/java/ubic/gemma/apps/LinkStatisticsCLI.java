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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.linkAnalysis.LinkAnalysisUtilService;
import ubic.gemma.analysis.linkAnalysis.LinkConfirmationStatistics;
import ubic.gemma.analysis.linkAnalysis.LinkStatistics;
import ubic.gemma.analysis.linkAnalysis.LinkStatisticsService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.AbstractSpringAwareCLI;

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
public class LinkStatisticsCLI extends AbstractSpringAwareCLI {

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

    private String taxonName = "mouse";
    private boolean prepared = true;
    private String eeNameFile = null;

    private ExpressionExperimentService eeService = null;
    private LinkAnalysisUtilService linkAnalysisUtilService = null;
    private int numIterationsToDo = 0;

    private int currentIteration = 0;
    private int linkStringency = 0;
    private boolean doShuffledOutput = false;

    /**
     * @param fileName Contains list of EE short names to use (essentially filters)
     * @param ees All the EEs for the chosen taxon.
     * @return
     * @throws IOException
     */
    private Collection<ExpressionExperiment> loadExpressionExperiments( String fileName,
            Collection<ExpressionExperiment> ees ) throws IOException {
        if ( fileName == null ) return ees;
        Collection<ExpressionExperiment> desiredEes = new HashSet<ExpressionExperiment>();
        Collection<String> eeNames = new HashSet<String>();

        InputStream is = new FileInputStream( fileName );
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
        String shortName = null;
        while ( ( shortName = br.readLine() ) != null ) {
            if ( StringUtils.isBlank( shortName ) ) continue;
            eeNames.add( shortName.trim().toUpperCase() );
        }

        for ( ExpressionExperiment ee : ees ) {
            shortName = ee.getShortName();
            if ( eeNames.contains( shortName.trim().toUpperCase() ) ) desiredEes.add( ee );
        }
        return desiredEes;
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option taxonOption = OptionBuilder.hasArg().withArgName( "Taxon" ).withDescription(
                "the taxon name (default=mouse)s" ).withLongOpt( "Taxon" ).create( 't' );
        addOption( taxonOption );

        Option eeNameFile = OptionBuilder.hasArg().withArgName( "File having Expression Experiment Names" )
                .withDescription( "File having Expression Experiment Names" ).withLongOpt( "eeFileName" ).create( 'f' );
        addOption( eeNameFile );
        Option startPreparing = OptionBuilder.withArgName( " Starting preparing " ).withDescription(
                " Starting preparing the temporary tables " ).withLongOpt( "startPreparing" ).create( 's' );
        addOption( startPreparing );

        Option iterationNum = OptionBuilder.hasArg().withArgName( " The number of iteration for shuffling " )
                .withDescription( " The number of iterations for shuffling (default = 100 " ).withLongOpt(
                        "iterationNum" ).create( 'i' );
        addOption( iterationNum );

        /*
         * Not sure what this does.
         */
        Option linkStringency = OptionBuilder.hasArg().withArgName( " The Link Stringency " ).withDescription(
                " The link Stringency " ).withLongOpt( "linkStringency" ).create( 'l' );
        addOption( linkStringency );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Shuffle Links ", args );
        if ( err != null ) {
            return err;
        }

        Taxon taxon = linkAnalysisUtilService.getTaxon( taxonName );
        Collection<ExpressionExperiment> eesForTaxon = eeService.findByTaxon( taxon );
        Collection<ExpressionExperiment> eesToAnalyze;
        try {
            eesToAnalyze = loadExpressionExperiments( this.eeNameFile, eesForTaxon );
        } catch ( IOException e ) {
            return e;
        }
        LinkStatisticsService lss = ( LinkStatisticsService ) this.getBean( "linkStatisticsService" );

        if ( !prepared ) {
            lss.prepareDatabase( eesToAnalyze, taxonName );
            return null;
        }

        Collection<Gene> genes = linkAnalysisUtilService.loadGenes( taxon );

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

            LinkStatistics realStats = lss.analyze( eesToAnalyze, genes, taxonName, false );
            LinkConfirmationStatistics confStats = realStats.getLinkConfirmationStats();

            try {
                Writer linksOut = new FileWriter( new File( "link-data.txt" ) );
                realStats.writeLinks( linksOut, 0 );
            } catch ( IOException e ) {
                return e;
            }

            List<LinkConfirmationStatistics> shuffleRuns = new ArrayList<LinkConfirmationStatistics>();
            if ( this.numIterationsToDo > 0 ) {
                log.info( "Running shuffled runs" );
                for ( currentIteration = 0; currentIteration < numIterationsToDo + 1; currentIteration++ ) {
                    log.info( "*** Iteration " + currentIteration + " ****" );

                    LinkStatistics sr = lss.analyze( eesForTaxon, genes, taxonName, true );
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
        if ( hasOption( 't' ) ) {
            this.taxonName = getOptionValue( 't' );
        }
        if ( hasOption( 'f' ) ) {
            this.eeNameFile = getOptionValue( 'f' );
        }
        if ( hasOption( 's' ) ) {
            this.prepared = false;
        }
        if ( hasOption( 'i' ) ) {
            this.numIterationsToDo = Integer.valueOf( getOptionValue( 'i' ) );
        }
        if ( hasOption( 'l' ) ) {
            this.linkStringency = Integer.valueOf( getOptionValue( 'l' ) );
        }

        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        linkAnalysisUtilService = ( LinkAnalysisUtilService ) this.getBean( "linkAnalysisUtilService" );
    }

}
