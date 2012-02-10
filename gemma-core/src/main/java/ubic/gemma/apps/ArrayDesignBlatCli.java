/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.loader.genome.BlatResultParser;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * Command line interface to run blat on the sequences for a microarray; the results are persisted in the DB. You must
 * start the BLAT server first before using this.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignBlatCli extends ArrayDesignSequenceManipulatingCli {

    public static void main( String[] args ) {
        ArrayDesignBlatCli p = new ArrayDesignBlatCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private ArrayDesignSequenceAlignmentService arrayDesignSequenceAlignmentService;

    private String blatResultFile = null;

    private Double blatScoreThreshold = Blat.DEFAULT_BLAT_SCORE_THRESHOLD;

    private TaxonService taxonService;

    private String taxonName;

    Taxon taxon;

    private boolean sensitive = false;

    @Override
    public String getShortDesc() {
        return "Run BLAT on the sequences for a microarray; the results are persisted in the DB.";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option blatResultOption = OptionBuilder
                .hasArg()
                .withArgName( "PSL file" )
                .withDescription(
                        "Blat result file in PSL format (if supplied, BLAT will not be run; will not work with settings that indidate multiple array designs to run); -t option overrides" )
                .withLongOpt( "blatfile" ).create( 'b' );

        Option blatScoreThresholdOption = OptionBuilder.hasArg().withArgName( "Blat score threshold" ).withDescription(
                "Threshold (0-1.0) for acceptance of BLAT alignments [Default = " + this.blatScoreThreshold + "]" )
                .withLongOpt( "scoreThresh" ).create( 's' );

        this.addOption( OptionBuilder.withDescription( "Run on more sensitive server, if available" ).create(
                "sensitive" ) );

        Option taxonOption = OptionBuilder
                .hasArg()
                .withArgName( "taxon" )
                .withDescription(
                        "Taxon common name (e.g., human); if array design name not given (analysis will be restricted to sequences on that array for taxon given), blat will be run for all ArrayDesigns from that taxon (overrides -a and -b)" )
                .create( 't' );

        addOption( taxonOption );
        addThreadsOption();
        addOption( blatScoreThresholdOption );
        addOption( blatResultOption );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine(
                "Array design sequence BLAT - only works if server is already started or if a PSL file is provided!",
                args );
        if ( err != null ) return err;

        final Date skipIfLastRunLaterThan = getLimitingDate();

        if ( !this.arrayDesignsToProcess.isEmpty() ) {

            if ( this.blatResultFile != null && this.arrayDesignsToProcess.size() > 1 ) {
                throw new IllegalArgumentException(
                        "Cannot provide a blat result file when multiple arrays are being analyzed" );
            }

            for ( ArrayDesign arrayDesign : this.arrayDesignsToProcess ) {
                if ( !needToRun( skipIfLastRunLaterThan, arrayDesign, ArrayDesignSequenceAnalysisEvent.class ) ) {
                    log.warn( arrayDesign + " was last run more recently than " + skipIfLastRunLaterThan );
                    return null;
                }

                arrayDesign = unlazifyArrayDesign( arrayDesign );
                Collection<BlatResult> persistedResults;
                try {
                    if ( this.blatResultFile != null ) {
                        Collection<BlatResult> blatResults = getBlatResultsFromFile( arrayDesign );

                        if ( blatResults == null || blatResults.size() == 0 ) {
                            throw new IllegalStateException( "No blat results in file!" );
                        }

                        log.info( "Got " + blatResults.size() + " blat records" );
                        persistedResults = arrayDesignSequenceAlignmentService.processArrayDesign( arrayDesign, taxon,
                                blatResults );
                        audit( arrayDesign, "BLAT results read from file: " + blatResultFile );
                    } else {
                        // Run blat from scratch.
                        persistedResults = arrayDesignSequenceAlignmentService.processArrayDesign( arrayDesign,
                                this.sensitive );
                        audit( arrayDesign, "Based on a fresh alignment analysis; BLAT score threshold was "
                                + this.blatScoreThreshold + "; sensitive mode was " + this.sensitive );
                    }
                    log.info( "Persisted " + persistedResults.size() + " results" );
                } catch ( FileNotFoundException e ) {
                    this.errorObjects.add( e );
                } catch ( IOException e ) {
                    this.errorObjects.add( e );
                }
            }

        } else if ( taxon != null ) {

            Collection<ArrayDesign> allArrayDesigns = arrayDesignService.findByTaxon( taxon );
            log.warn( "*** Running BLAT for all " + taxon.getCommonName() + " Array designs *** ["
                    + allArrayDesigns.size() + " items]" );

            final SecurityContext context = SecurityContextHolder.getContext();

            // split over multiple threads so we can multiplex. Put the array designs in a queue.

            /*
             * Here is our task runner.
             */
            class Consumer implements Runnable {
                private final BlockingQueue<ArrayDesign> queue;

                public Consumer( BlockingQueue<ArrayDesign> q ) {
                    queue = q;
                }

                @Override
                public void run() {
                    SecurityContextHolder.setContext( context );
                    while ( true ) {
                        ArrayDesign ad = queue.poll();
                        if ( ad == null ) {
                            break;
                        }
                        consume( ad );
                    }
                }

                void consume( ArrayDesign x ) {

                    x = arrayDesignService.thaw( x );

                    processArrayDesign( skipIfLastRunLaterThan, x );

                }
            }

            BlockingQueue<ArrayDesign> arrayDesigns = new ArrayBlockingQueue<ArrayDesign>( allArrayDesigns.size() );
            for ( ArrayDesign ad : allArrayDesigns ) {
                arrayDesigns.add( ad );
            }

            /*
             * Start the threads
             */
            Collection<Thread> threads = new ArrayList<Thread>();
            for ( int i = 0; i < this.numThreads; i++ ) {
                Consumer c1 = new Consumer( arrayDesigns );
                Thread k = new Thread( c1 );
                threads.add( k );
                k.start();
            }

            waitForThreadPoolCompletion( threads );

            /*
             * All done
             */
            summarizeProcessing();

        } else {
            bail( ErrorCode.MISSING_ARGUMENT );
        }

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( "sensitive" ) ) {
            this.sensitive = true;
        }

        if ( hasOption( 'b' ) ) {
            this.blatResultFile = this.getOptionValue( 'b' );
        }

        if ( hasOption( THREADS_OPTION ) ) {
            this.numThreads = this.getIntegerOptionValue( "threads" );
        }

        if ( hasOption( 's' ) ) {
            this.blatScoreThreshold = this.getDoubleOptionValue( 's' );
        }

        this.taxonService = ( TaxonService ) this.getBean( "taxonService" );

        if ( this.hasOption( 't' ) ) {
            this.taxonName = this.getOptionValue( 't' );
            this.taxon = taxonService.findByCommonName( this.taxonName );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No taxon named " + taxonName );
            }
        }

        arrayDesignSequenceAlignmentService = ( ArrayDesignSequenceAlignmentService ) this
                .getBean( "arrayDesignSequenceAlignmentService" );

    }

    /**
     * @param skipIfLastRunLaterThan
     * @param design
     */
    void processArrayDesign( Date skipIfLastRunLaterThan, ArrayDesign design ) {
        if ( !needToRun( skipIfLastRunLaterThan, design, ArrayDesignSequenceAnalysisEvent.class ) ) {
            log.warn( design + " was last run more recently than " + skipIfLastRunLaterThan );
            // not really an error, but nice to get notification.
            errorObjects.add( design + ": " + "Skipped because it was last run after " + skipIfLastRunLaterThan );
            return;
        }

        if ( isSubsumedOrMerged( design ) ) {
            log.warn( design + " is subsumed or merged into another design, it will not be run." );
            // not really an error, but nice to get notification.
            errorObjects.add( design + ": " + "Skipped because it is subsumed by or merged into another design." );
            return;
        }

        log.info( "============== Start processing: " + design + " ==================" );
        try {
            // thaw is already done.
            arrayDesignSequenceAlignmentService.processArrayDesign( design, this.sensitive );
            successObjects.add( design.getName() );
            audit( design, "Part of a batch job; BLAT score threshold was " + this.blatScoreThreshold );
        } catch ( Exception e ) {
            errorObjects.add( design + ": " + e.getMessage() );
            log.error( "**** Exception while processing " + design + ": " + e.getMessage() + " ****" );
            log.error( e, e );
        }
    }

    /**
     * @param arrayDesign
     */
    private void audit( ArrayDesign arrayDesign, String note ) {
        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        AuditEventType eventType = ArrayDesignSequenceAnalysisEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    /**
     * Process blat file which must be for one taxon.
     * 
     * @param arrayDesign
     * @return
     * @throws IOException
     */
    private Collection<BlatResult> getBlatResultsFromFile( ArrayDesign arrayDesign ) throws IOException {
        Taxon arrayDesignTaxon = null;
        File f = new File( blatResultFile );
        if ( !f.canRead() ) {
            log.error( "Cannot read from " + blatResultFile );
            bail( ErrorCode.INVALID_OPTION );
        }
        // check being running for just one taxon
        arrayDesignTaxon = arrayDesignSequenceAlignmentService.validateTaxaForBlatFile( arrayDesign, taxon );

        log.info( "Reading blat results in from " + f.getAbsolutePath() );
        BlatResultParser parser = new BlatResultParser();
        parser.setScoreThreshold( this.blatScoreThreshold );
        parser.setTaxon( arrayDesignTaxon );
        parser.parse( f );
        Collection<BlatResult> blatResults = parser.getResults();
        return blatResults;
    }

}
