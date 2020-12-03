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
package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import ubic.gemma.core.analysis.sequence.RepeatScan;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentServiceImpl;
import ubic.gemma.core.util.AbstractCLI;
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

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.PLATFORM;
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option fileOption = Option.builder( "f" ).hasArg().argName( ".out file" )
                .desc( "RepeatScan file to use as input" ).longOpt( "file" ).build();
        this.addOption( fileOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'f' ) ) {
            this.inputFileName = this.getOptionValue( 'f' );
        }
    }

    @Override
    public String getCommandName() {
        return "platformRepeatScan";
    }

    @Override
    protected void doWork() throws Exception {
        bsService = this.getBean( BioSequenceService.class );

        Date skipIfLastRunLaterThan = this.getLimitingDate();

        if ( !this.getArrayDesignsToProcess().isEmpty() ) {
            for ( ArrayDesign arrayDesign : this.getArrayDesignsToProcess() ) {

                if ( !this.needToRun( skipIfLastRunLaterThan, arrayDesign, ArrayDesignRepeatAnalysisEvent.class ) ) {
                    AbstractCLI.log.warn( arrayDesign + " was last run more recently than " + skipIfLastRunLaterThan );
                    return;
                }

                arrayDesign = this.thaw( arrayDesign );

                this.processArrayDesign( arrayDesign );
            }
        } else if ( skipIfLastRunLaterThan != null ) {
            AbstractCLI.log.warn( "*** Running Repeatmasker for all Array designs *** " );

            Collection<ArrayDesign> allArrayDesigns = getArrayDesignService().loadAll();
            for ( ArrayDesign design : allArrayDesigns ) {

                if ( !this.needToRun( skipIfLastRunLaterThan, design, ArrayDesignRepeatAnalysisEvent.class ) ) {
                    AbstractCLI.log.warn( design + " was last run more recently than " + skipIfLastRunLaterThan );
                    // not really an error, but nice to get notification.
                    addErrorObject( design, "Skipped because it was last run after " + skipIfLastRunLaterThan );
                    continue;
                }

                if ( this.isSubsumedOrMerged( design ) ) {
                    AbstractCLI.log.warn( design + " is subsumed or merged into another design, it will not be run." );
                    // not really an error, but nice to get notification.
                    addErrorObject( design, "Skipped because it is subsumed by or merged into another design." );
                    continue;
                }

                AbstractCLI.log.info( "============== Start processing: " + design + " ==================" );
                try {
                    design = getArrayDesignService().thaw( design );
                    this.processArrayDesign( design );
                    addSuccessObject( design, "Successfully processed " + design.getName() );
                    this.audit( design, "" );
                } catch ( Exception e ) {
                    addErrorObject( design, e.getMessage(), e );
                }

            }
        } else {
            throw new RuntimeException();
        }
    }

    @Override
    public String getShortDesc() {
        return "Run RepeatMasker on sequences for an Array design";
    }

    private void audit( ArrayDesign arrayDesign, String note ) {
        AuditEventType eventType = ArrayDesignRepeatAnalysisEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    private void processArrayDesign( ArrayDesign design ) {
        ArrayDesign thawed = this.thaw( design );

        // no taxon is passed to this method so all sequences will be retrieved even for multi taxon arrays
        Collection<BioSequence> sequences = ArrayDesignSequenceAlignmentServiceImpl.getSequences( thawed );

        RepeatScan scanner = new RepeatScan();
        Collection<BioSequence> altered;
        if ( this.inputFileName != null ) {
            altered = scanner.processRepeatMaskerOutput( sequences, inputFileName );
        } else {
            altered = scanner.repeatScan( sequences );
        }

        AbstractCLI.log.info( "Saving..." );
        bsService.update( altered );
        if ( this.inputFileName != null ) {
            this.audit( thawed,
                    "Repeat scan data from file: " + inputFileName + ", updated " + altered.size() + " sequences." );
        } else {
            this.audit( thawed, "Repeat scan done, updated " + altered.size() + " sequences." );
        }
        AbstractCLI.log.info( "Done with " + thawed );
    }

}
