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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.sequence.Blat;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.core.loader.genome.BlatResultParser;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import static ubic.gemma.cli.util.EntityOptionsUtils.addTaxonOption;

/**
 * Command line interface to run blat on the sequences for a microarray; the results are persisted in the DB. You must
 * start the BLAT server first before using this.
 *
 * @author pavlidis
 */
public class ArrayDesignBlatCli extends ArrayDesignSequenceManipulatingCli {

    @Autowired
    private ArrayDesignSequenceAlignmentService arrayDesignSequenceAlignmentService;
    @Autowired
    private TaxonService taxonService;

    private Taxon taxon;
    private String blatResultFile = null;
    private Double blatScoreThreshold = Blat.DEFAULT_BLAT_SCORE_THRESHOLD;
    private boolean sensitive = false;

    @Override
    public String getCommandName() {
        return "blatPlatform";
    }

    @Override
    public String getShortDesc() {
        return "Run BLAT on the sequences for a platform; the results are persisted in the DB.";
    }

    @Override
    protected void buildArrayDesignOptions( Options options ) {

        Option blatResultOption = Option.builder( "b" ).hasArg().argName( "PSL file" ).desc(
                        "Blat result file in PSL format (if supplied, BLAT will not be run; will not work with settings that indicate "
                                + "multiple platforms to run); -t option overrides" )
                .longOpt( "blatfile" )
                .build();

        Option blatScoreThresholdOption = Option.builder( "s" ).hasArg().argName( "Blat score threshold" )
                .desc(
                        "Threshold (0-1.0) for acceptance of BLAT alignments [Default = " + this.blatScoreThreshold + "]" )
                .longOpt( "scoreThresh" )
                .type( Number.class )
                .build();

        options.addOption( Option.builder( "sensitive" ).desc( "Run on more sensitive server, if available" ).build() );

        addTaxonOption( options, "t", "taxon", "Taxon identifier (e.g., human); if platform name not given (analysis will be "
                + "restricted to sequences on that platform for taxon given), blat "
                + "will be run for all ArrayDesigns from that taxon (overrides -a and -b)" );

        // this.addThreadsOption( options );
        options.addOption( blatScoreThresholdOption );
        options.addOption( blatResultOption );
    }

    @Override
    protected void processArrayDesignOptions( CommandLine commandLine ) throws ParseException {

        if ( commandLine.hasOption( "sensitive" ) ) {
            this.sensitive = true;
        }

        if ( commandLine.hasOption( 'b' ) ) {
            this.blatResultFile = commandLine.getOptionValue( 'b' );
        }

//        if ( commandLine.hasOption( AbstractCLI.THREADS_OPTION ) ) {
//            this.numThreads = this.getIntegerOptionValue( commandLine, "threads" );
//        }

        if ( commandLine.hasOption( 's' ) ) {
            this.blatScoreThreshold = ( ( Number ) commandLine.getParsedOptionValue( 's' ) ).doubleValue();
        }

        if ( commandLine.hasOption( 't' ) ) {
            String taxonName = commandLine.getOptionValue( 't' );
            this.taxon = entityLocator.locateTaxon( taxonName );
        }
    }

    @Override
    protected void processArrayDesigns( Collection<ArrayDesign> arrayDesignsToProcess ) {
        final Date skipIfLastRunLaterThan = this.getLimitingDate();

        if ( !arrayDesignsToProcess.isEmpty() ) {

            if ( this.blatResultFile != null && arrayDesignsToProcess.size() > 1 ) {
                throw new IllegalArgumentException(
                        "Cannot provide a blat result file when multiple arrays are being analyzed" );
            }

            for ( ArrayDesign arrayDesign : arrayDesignsToProcess ) {
                if ( !this.shouldRun( skipIfLastRunLaterThan, arrayDesign, ArrayDesignSequenceAnalysisEvent.class ) ) {
                    log.warn( arrayDesign + " does not meet criteria to be processed" );
                    return;
                }

                arrayDesign = arrayDesignService.thaw( arrayDesign );
                log.info( "============== Start processing: " + arrayDesign.getShortName() + " ==================" );
                Collection<BlatResult> persistedResults;
                try {
                    if ( this.blatResultFile != null ) {
                        Collection<BlatResult> blatResults = this.getBlatResultsFromFile( arrayDesign );

                        if ( blatResults == null || blatResults.isEmpty() ) {
                            throw new IllegalStateException( "No blat results in file!" );
                        }

                        log.info( "Got " + blatResults.size() + " blat records" );
                        persistedResults = arrayDesignSequenceAlignmentService
                                .processArrayDesign( arrayDesign, taxon, blatResults );
                        this.audit( arrayDesign, "BLAT results read from file: " + blatResultFile );
                        this.updateMergedOrSubsumed( arrayDesign );

                    } else {
                        // Run blat from scratch.
                        persistedResults = arrayDesignSequenceAlignmentService
                                .processArrayDesign( arrayDesign, this.sensitive );
                        this.audit( arrayDesign, "Based on a fresh alignment analysis; BLAT score threshold was "
                                + this.blatScoreThreshold + "; sensitive mode was " + this.sensitive );
                        this.updateMergedOrSubsumed( arrayDesign );
                    }
                    log.info( "Persisted " + persistedResults.size() + " results" );
                } catch ( IOException e ) {
                    addErrorObject( arrayDesign, e );
                }
            }

        } else if ( taxon != null ) {

            Collection<ArrayDesign> allArrayDesigns = arrayDesignService.findByTaxon( taxon );
            log.warn( "*** Running BLAT for all " + taxon.getCommonName() + " Array designs *** ["
                    + allArrayDesigns.size() + " items]" );

            // split over multiple threads so we can multiplex. Put the array designs in a queue.
            for ( ArrayDesign arrayDesign : allArrayDesigns ) {
                getBatchTaskExecutor().submit( new ProcessArrayDesign( arrayDesign, skipIfLastRunLaterThan ) );
            }
        } else {
            throw new RuntimeException();
        }
    }

    private void audit( ArrayDesign arrayDesign, String note ) {
        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        auditTrailService.addUpdateEvent( arrayDesign, ArrayDesignSequenceAnalysisEvent.class, note );
    }

    /**
     * Process blat file which must be for one taxon.
     */
    private Collection<BlatResult> getBlatResultsFromFile( ArrayDesign arrayDesign ) throws IOException {
        Taxon arrayDesignTaxon;
        File f = new File( blatResultFile );
        if ( !f.canRead() ) {
            throw new RuntimeException( "Cannot read from " + blatResultFile );
        }
        // check being running for just one taxon
        arrayDesignTaxon = arrayDesignSequenceAlignmentService.validateTaxaForBlatFile( arrayDesign, taxon );

        log.info( "Reading blat results in from " + f.getAbsolutePath() );
        BlatResultParser parser = new BlatResultParser();
        parser.setScoreThreshold( this.blatScoreThreshold );
        parser.setTaxon( arrayDesignTaxon );
        parser.parse( f );
        return parser.getResults();
    }

    @Override
    protected void processArrayDesign( ArrayDesign design ) {

        log.info( "============== Start processing: " + design.getShortName() + " ==================" );
        try {
            // thaw is already done.
            arrayDesignSequenceAlignmentService.processArrayDesign( design, this.sensitive );
            addSuccessObject( design );
            this.audit( design, "Part of a batch job; BLAT score threshold was " + this.blatScoreThreshold );
            this.updateMergedOrSubsumed( design );

        } catch ( Exception e ) {
            addErrorObject( design, e );
        }
    }

    /**
     * When we analyze a platform that has mergees or subsumed platforms, we can treat them as if they were analyzed as
     * well. We simply add an audit event, and update the report for the platform.
     *
     * @param design platform
     */
    private void updateMergedOrSubsumed( ArrayDesign design ) {
        /*
         * Update merged or subsumed platforms.
         */

        Collection<ArrayDesign> toUpdate = this.getRelatedDesigns( design );
        for ( ArrayDesign ad : toUpdate ) {
            log.info( "Marking subsumed or merged design as completed, updating report: " + ad );
            this.audit( ad, "Parent design was processed (merged or subsumed by this)" );
            arrayDesignReportService.generateArrayDesignReport( ad.getId() );
        }
    }

    /*
     * Here is our task runner.
     */
    private class ProcessArrayDesign implements Runnable {

        private ArrayDesign arrayDesign;
        private final Date skipIfLastRunLaterThan;

        private ProcessArrayDesign( ArrayDesign arrayDesign, Date skipIfLastRunLaterThan ) {
            this.arrayDesign = arrayDesign;
            this.skipIfLastRunLaterThan = skipIfLastRunLaterThan;
        }

        @Override
        public void run() {
            if ( !ArrayDesignBlatCli.this.shouldRun( skipIfLastRunLaterThan, arrayDesign, ArrayDesignSequenceAnalysisEvent.class ) ) {
                return;
            }
            arrayDesign = arrayDesignService.thaw( arrayDesign );
            ArrayDesignBlatCli.this.processArrayDesign( arrayDesign );
            addSuccessObject( arrayDesign );
        }
    }
}
