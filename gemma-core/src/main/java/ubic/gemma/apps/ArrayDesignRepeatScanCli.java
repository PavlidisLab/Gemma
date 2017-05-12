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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import ubic.gemma.core.analysis.sequence.RepeatScan;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentServiceImpl;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignRepeatAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;

import java.util.Collection;
import java.util.Date;

/**
 * Runs repeatmasker on array designs.
 *
 * @author pavlidis
 */
public class ArrayDesignRepeatScanCli extends ArrayDesignSequenceManipulatingCli {

    private BioSequenceService bsService;
    private String inputFileName;

    public static void main( String[] args ) {
        ArrayDesignRepeatScanCli p = new ArrayDesignRepeatScanCli();
        tryDoWorkNoExit( p, args );

    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.PLATFORM;
    }

    @Override
    public String getCommandName() {
        return "platformRepeatScan";
    }

    @Override
    public String getShortDesc() {
        return "Run RepeatMasker on sequences for an Array design";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option fileOption = OptionBuilder.hasArg().withArgName( ".out file" )
                .withDescription( "Repeatscan file to use as input" ).withLongOpt( "file" ).create( 'f' );
        addOption( fileOption );
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception exception = processCommandLine( args );
        if ( exception != null )
            return exception;

        bsService = this.getBean( BioSequenceService.class );

        Date skipIfLastRunLaterThan = getLimitingDate();

        if ( !this.arrayDesignsToProcess.isEmpty() ) {
            for ( ArrayDesign arrayDesign : this.arrayDesignsToProcess ) {

                if ( !needToRun( skipIfLastRunLaterThan, arrayDesign, ArrayDesignRepeatAnalysisEvent.class ) ) {
                    log.warn( arrayDesign + " was last run more recently than " + skipIfLastRunLaterThan );
                    return null;
                }

                arrayDesign = unlazifyArrayDesign( arrayDesign );

                processArrayDesign( arrayDesign );
            }
        } else if ( skipIfLastRunLaterThan != null ) {
            log.warn( "*** Running Repeatmasker for all Array designs *** " );

            Collection<ArrayDesign> allArrayDesigns = arrayDesignService.loadAll();
            for ( ArrayDesign design : allArrayDesigns ) {

                if ( !needToRun( skipIfLastRunLaterThan, design, ArrayDesignRepeatAnalysisEvent.class ) ) {
                    log.warn( design + " was last run more recently than " + skipIfLastRunLaterThan );
                    // not really an error, but nice to get notification.
                    errorObjects
                            .add( design + ": " + "Skipped because it was last run after " + skipIfLastRunLaterThan );
                    continue;
                }

                if ( isSubsumedOrMerged( design ) ) {
                    log.warn( design + " is subsumed or merged into another design, it will not be run." );
                    // not really an error, but nice to get notification.
                    errorObjects
                            .add( design + ": " + "Skipped because it is subsumed by or merged into another design." );
                    continue;
                }

                log.info( "============== Start processing: " + design + " ==================" );
                try {
                    design = arrayDesignService.thaw( design );
                    processArrayDesign( design );
                    successObjects.add( design.getName() );
                    audit( design, "" );
                } catch ( Exception e ) {
                    errorObjects.add( design + ": " + e.getMessage() );
                    log.error( "**** Exception while processing " + design + ": " + e.getMessage() + " ****" );
                    log.error( e, e );
                }

            }
            summarizeProcessing();
        } else {
            bail( ErrorCode.MISSING_ARGUMENT );
        }

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'f' ) ) {
            this.inputFileName = this.getOptionValue( 'f' );
        }
    }

    private void audit( ArrayDesign arrayDesign, String note ) {
        AuditEventType eventType = ArrayDesignRepeatAnalysisEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    private void processArrayDesign( ArrayDesign design ) {
        ArrayDesign thawed = unlazifyArrayDesign( design );

        // no taxon is passed to this method so all sequences will be retrieved even for multi taxon arrays
        Collection<BioSequence> sequences = ArrayDesignSequenceAlignmentServiceImpl.getSequences( thawed );

        RepeatScan scanner = new RepeatScan();
        Collection<BioSequence> altered;
        if ( this.inputFileName != null ) {
            altered = scanner.processRepeatMaskerOutput( sequences, inputFileName );
        } else {
            altered = scanner.repeatScan( sequences );
        }

        log.info( "Saving..." );
        bsService.update( altered );
        if ( this.inputFileName != null ) {
            audit( thawed,
                    "Repeat scan data from file: " + inputFileName + ", updated " + altered.size() + " sequences." );
        } else {
            audit( thawed, "Repeat scan done, updated " + altered.size() + " sequences." );
        }
        log.info( "Done with " + thawed );
    }

}
