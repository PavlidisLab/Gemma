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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.core.analysis.sequence.RepeatScan;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentServiceImpl;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignRepeatAnalysisEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;

/**
 * Runs repeatmasker on array designs.
 *
 * @author pavlidis
 */
public class ArrayDesignRepeatScanCli extends ArrayDesignSequenceManipulatingCli {

    @Autowired
    private BioSequenceService bsService;

    @Nullable
    private Path inputFileName;

    @Value("${repeatMasker.exe}")
    private String repeatMaskerExe;

    @Override
    protected void buildArrayDesignOptions( Options options ) {
        Option fileOption = Option.builder( "f" ).hasArg().argName( ".out file" )
                .desc( "RepeatScan file to use as input" ).longOpt( "file" ).type( Path.class )
                .get();
        options.addOption( fileOption );
    }

    @Override
    protected void processArrayDesignOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( 'f' ) ) {
            this.inputFileName = commandLine.getParsedOptionValue( 'f' );
        }
    }

    @Override
    public String getCommandName() {
        return "platformRepeatScan";
    }

    @Override
    protected void processArrayDesigns( Collection<ArrayDesign> arrayDesignsToProcess ) {
        Date skipIfLastRunLaterThan = this.getLimitingDate();

        if ( !arrayDesignsToProcess.isEmpty() ) {
            for ( ArrayDesign arrayDesign : arrayDesignsToProcess ) {

                if ( !this.needToRun( skipIfLastRunLaterThan, arrayDesign, ArrayDesignRepeatAnalysisEvent.class ) ) {
                    log.warn( arrayDesign + " was last run more recently than " + skipIfLastRunLaterThan );
                    return;
                }

                arrayDesign = arrayDesignService.thaw( arrayDesign );

                this.processArrayDesign( arrayDesign );
            }
        } else if ( skipIfLastRunLaterThan != null ) {
            log.warn( "*** Running Repeatmasker for all Array designs *** " );

            Collection<ArrayDesign> allArrayDesigns = arrayDesignService.loadAll();
            for ( ArrayDesign design : allArrayDesigns ) {

                if ( !this.needToRun( skipIfLastRunLaterThan, design, ArrayDesignRepeatAnalysisEvent.class ) ) {
                    log.warn( design + " was last run more recently than " + skipIfLastRunLaterThan );
                    // not really an error, but nice to get notification.
                    addErrorObject( design, "Skipped because it was last run after " + skipIfLastRunLaterThan );
                    continue;
                }

                if ( this.isSubsumedOrMerged( design ) ) {
                    log.warn( design + " is subsumed or merged into another design, it will not be run." );
                    // not really an error, but nice to get notification.
                    addErrorObject( design, "Skipped because it is subsumed by or merged into another design." );
                    continue;
                }

                log.info( "============== Start processing: " + design + " ==================" );
                try {
                    design = arrayDesignService.thaw( design );
                    this.processArrayDesign( design );
                    addSuccessObject( design );
                    this.audit( design, "" );
                } catch ( Exception e ) {
                    addErrorObject( design, e );
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

    @Override
    protected void processArrayDesign( ArrayDesign design ) {
        ArrayDesign thawed = arrayDesignService.thaw( design );

        // no taxon is passed to this method so all sequences will be retrieved even for multi taxon arrays
        Collection<BioSequence> sequences = ArrayDesignSequenceAlignmentServiceImpl.getSequences( thawed );

        RepeatScan scanner = new RepeatScan( repeatMaskerExe );
        Collection<BioSequence> altered;
        if ( this.inputFileName != null ) {
            altered = scanner.processRepeatMaskerOutput( sequences, inputFileName );
        } else {
            altered = scanner.repeatScan( sequences );
        }

        log.info( "Saving..." );
        bsService.update( altered );
        if ( this.inputFileName != null ) {
            this.audit( thawed,
                    "Repeat scan data from file: " + inputFileName + ", updated " + altered.size() + " sequences." );
        } else {
            this.audit( thawed, "Repeat scan done, updated " + altered.size() + " sequences." );
        }
        log.info( "Done with " + thawed );
    }

    private void audit( ArrayDesign arrayDesign, String note ) {
        auditTrailService.addUpdateEvent( arrayDesign, ArrayDesignRepeatAnalysisEvent.class, note );
    }
}
