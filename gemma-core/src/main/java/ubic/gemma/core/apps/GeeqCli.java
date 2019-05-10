/*
 * The gemma-core project
 * 
 * Copyright (c) 2018 University of British Columbia
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

import static ubic.gemma.persistence.service.expression.experiment.GeeqService.OPT_MODE_ALL;
import static ubic.gemma.persistence.service.expression.experiment.GeeqService.OPT_MODE_BATCH;
import static ubic.gemma.persistence.service.expression.experiment.GeeqService.OPT_MODE_PUB;
import static ubic.gemma.persistence.service.expression.experiment.GeeqService.OPT_MODE_REPS;

import org.apache.commons.cli.Option;

import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.GeeqEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.GeeqService;

/**
 * 
 * Generate or update GEEQ scores
 * 
 * @author tesar
 */
public class GeeqCli extends ExpressionExperimentManipulatingCLI {
    private GeeqService geeqService;

    private String mode = GeeqService.OPT_MODE_ALL;

    public static void main( String[] args ) {
        GeeqCli p = new GeeqCli();
        AbstractCLIContextCLI.executeCommand( p, args );
    }

    @Override
    public String getShortDesc() {
        return "Generate or update GEEQ scores";
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        eeService = this.getBean( ExpressionExperimentService.class );
        geeqService = this.getBean( GeeqService.class );

        if ( this.hasOption( 'm' ) ) {
            this.mode = this.getOptionValue( 'm' );
        }
    }

    @Override
    public String getCommandName() {
        return "runGeeq";
    }

    @SuppressWarnings("AccessStaticViaInstance")
    @Override
    protected void buildOptions() {

        super.buildOptions();
        super.addAutoOption();
        super.addDateOption();
        this.autoSeekEventType = GeeqEvent.class;
        super.addForceOption();

        Option modeOption = Option.builder( "m" ).longOpt( "mode" )
                .desc( "If specified, switches the scoring mode. By default the mode is set to 'all'" //
                        + "\n Possible values are:" //
                        + "\n " + OPT_MODE_ALL + " - runs all scoring" //
                        + "\n " + OPT_MODE_BATCH + "- recalculates batch related scores - info, confound and batch effect" //
                        + "\n " + OPT_MODE_REPS + " - recalculates score for replicates" //
                        + "\n " + OPT_MODE_PUB + " - recalculates score for publication" )
                .hasArg().build();
        this.addOption( modeOption );
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = super.processCommandLine( args );
        if ( err != null )
            return err; 

        for ( BioAssaySet bioassay : expressionExperiments ) {
            if ( !( bioassay instanceof ExpressionExperiment ) ) {
                log.debug( bioassay.getName() + " is not an ExpressionExperiment" );
                continue;
            }
            ExpressionExperiment ee = ( ExpressionExperiment ) bioassay;

            if ( !force && this.noNeedToRun( ee, GeeqEvent.class ) ) {
                AbstractCLI.log.debug( "Can't or don't need to run " + ee );
                continue;
            }
            
            try {
                geeqService.calculateScore( ee.getId(), mode );
                this.successObjects.add( ee );
            } catch ( Exception e ) {
                log.error( ee + " failed: " + e.getMessage() );
                this.errorObjects.add( ee + ": " + e.getMessage() );
            }
        }

        this.summarizeProcessing();
        return null;
    }
}
