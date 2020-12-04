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
package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.core.loader.genome.BlatResultParser;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.*;

/**
 * Command line interface to run blat on the sequences for a microarray; the results are persisted in the DB. You must
 * start the BLAT server first before using this.
 *
 * @author pavlidis
 */
public class ArrayDesignBlatCli extends ArrayDesignSequenceManipulatingCli {

    private Taxon taxon;
    private ArrayDesignSequenceAlignmentService arrayDesignSequenceAlignmentService;
    private String blatResultFile = null;
    private Double blatScoreThreshold = Blat.DEFAULT_BLAT_SCORE_THRESHOLD;
    private boolean sensitive = false;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PLATFORM;
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option blatResultOption = Option.builder( "b" ).hasArg().argName( "PSL file" ).desc(
                "Blat result file in PSL format (if supplied, BLAT will not be run; will not work with settings that indicate "
                        + "multiple platforms to run); -t option overrides" )
                .longOpt( "blatfile" )
                .build();

        Option blatScoreThresholdOption = Option.builder( "s" ).hasArg().argName( "Blat score threshold" )
                .desc(
                        "Threshold (0-1.0) for acceptance of BLAT alignments [Default = " + this.blatScoreThreshold + "]" )
                .longOpt( "scoreThresh" )
                .build();

        this.addOption( Option.builder( "sensitive" ).desc( "Run on more sensitive server, if available" ).build() );

        Option taxonOption = Option.builder( "t" ).hasArg().argName( "taxon" ).desc(
                "Taxon common name (e.g., human); if platform name not given (analysis will be "
                        + "restricted to sequences on that platform for taxon given), blat "
                        + "will be run for all ArrayDesigns from that taxon (overrides -a and -b)" )
                .build();

        this.addOption( taxonOption );
        this.addThreadsOption();
        this.addOption( blatScoreThresholdOption );
        this.addOption( blatResultOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( this.hasOption( "sensitive" ) ) {
            this.sensitive = true;
        }

        if ( this.hasOption( 'b' ) ) {
            this.blatResultFile = this.getOptionValue( 'b' );
        }

        if ( this.hasOption( AbstractCLI.THREADS_OPTION ) ) {
            this.numThreads = this.getIntegerOptionValue( "threads" );
        }

        if ( this.hasOption( 's' ) ) {
            this.blatScoreThreshold = this.getDoubleOptionValue( 's' );
        }

        TaxonService taxonService = this.getBean( TaxonService.class );

        if ( this.hasOption( 't' ) ) {
            String taxonName = this.getOptionValue( 't' );
            this.taxon = taxonService.findByCommonName( taxonName );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No taxon named " + taxonName );
            }
        }

        arrayDesignSequenceAlignmentService = this.getBean( ArrayDesignSequenceAlignmentService.class );

    }

    @Override
    public String getCommandName() {
        return "blatPlatform";
    }

    @Override
    protected void doWork() throws Exception {
        final Date skipIfLastRunLaterThan = this.getLimitingDate();

        if ( !this.getArrayDesignsToProcess().isEmpty() ) {

            if ( this.blatResultFile != null && this.getArrayDesignsToProcess().size() > 1 ) {
                throw new IllegalArgumentException(
                        "Cannot provide a blat result file when multiple arrays are being analyzed" );
            }

            for ( ArrayDesign arrayDesign : this.getArrayDesignsToProcess() ) {
                if ( !this.shouldRun( skipIfLastRunLaterThan, arrayDesign, ArrayDesignSequenceAnalysisEvent.class ) ) {
                    AbstractCLI.log.warn( arrayDesign + " was last run more recently than " + skipIfLastRunLaterThan );
                    return;
                }

                arrayDesign = this.thaw( arrayDesign );
                Collection<BlatResult> persistedResults;
                try {
                    if ( this.blatResultFile != null ) {
                        Collection<BlatResult> blatResults = this.getBlatResultsFromFile( arrayDesign );

                        if ( blatResults == null || blatResults.size() == 0 ) {
                            throw new IllegalStateException( "No blat results in file!" );
                        }

                        AbstractCLI.log.info( "Got " + blatResults.size() + " blat records" );
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
                    AbstractCLI.log.info( "Persisted " + persistedResults.size() + " results" );
                } catch ( IOException e ) {
                    addErrorObject( null, e.getMessage(), e );
                }
            }

        } else if ( taxon != null ) {

            Collection<ArrayDesign> allArrayDesigns = getArrayDesignService().findByTaxon( taxon );
            AbstractCLI.log.warn( "*** Running BLAT for all " + taxon.getCommonName() + " Array designs *** ["
                    + allArrayDesigns.size() + " items]" );

            final SecurityContext context = SecurityContextHolder.getContext();

            // split over multiple threads so we can multiplex. Put the array designs in a queue.
            Collection<Callable<Void>> arrayDesigns = new ArrayList<>( allArrayDesigns.size() );
            for ( ArrayDesign arrayDesign : allArrayDesigns ) {
                arrayDesigns.add( new ProcessArrayDesign( arrayDesign, skipIfLastRunLaterThan ) );
            }

            executeBatchTasks( arrayDesigns );

        } else {
            throw new RuntimeException();
        }
    }

    @Override
    public String getShortDesc() {
        return "Run BLAT on the sequences for a platform; the results are persisted in the DB.";
    }

    private void audit( ArrayDesign arrayDesign, String note ) {
        getArrayDesignReportService().generateArrayDesignReport( arrayDesign.getId() );
        AuditEventType eventType = ArrayDesignSequenceAnalysisEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
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

        AbstractCLI.log.info( "Reading blat results in from " + f.getAbsolutePath() );
        BlatResultParser parser = new BlatResultParser();
        parser.setScoreThreshold( this.blatScoreThreshold );
        parser.setTaxon( arrayDesignTaxon );
        parser.parse( f );
        return parser.getResults();
    }

    private void processArrayDesign( ArrayDesign design ) {

        AbstractCLI.log.info( "============== Start processing: " + design + " ==================" );
        try {
            // thaw is already done.
            arrayDesignSequenceAlignmentService.processArrayDesign( design, this.sensitive );
            addSuccessObject( design, "Successfully processed " + design.getName() );
            this.audit( design, "Part of a batch job; BLAT score threshold was " + this.blatScoreThreshold );
            this.updateMergedOrSubsumed( design );

        } catch ( Exception e ) {
            addErrorObject( design, e.getMessage(), e );
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
            AbstractCLI.log.info( "Marking subsumed or merged design as completed, updating report: " + ad );
            this.audit( ad, "Parent design was processed (merged or subsumed by this)" );
            getArrayDesignReportService().generateArrayDesignReport( ad.getId() );
        }
    }

    /*
     * Here is our task runner.
     */
    private class ProcessArrayDesign implements Callable<Void> {

        private ArrayDesign arrayDesign;
        private Date skipIfLastRunLaterThan;

        private ProcessArrayDesign( ArrayDesign arrayDesign, Date skipIfLastRunLaterThan ) {
            this.arrayDesign = arrayDesign;
            this.skipIfLastRunLaterThan = skipIfLastRunLaterThan;
        }

        @Override
        public Void call() {
            if ( !ArrayDesignBlatCli.this.shouldRun( skipIfLastRunLaterThan, arrayDesign, ArrayDesignSequenceAnalysisEvent.class ) ) {
                return null;
            }
            arrayDesign = getArrayDesignService().thaw( arrayDesign );
            ArrayDesignBlatCli.this.processArrayDesign( arrayDesign );
            addSuccessObject( arrayDesign, "Processed " + arrayDesign.getShortName() );
            return null;
        }
    }
}
