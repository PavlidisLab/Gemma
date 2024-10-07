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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.auditAndSecurity.eventType.GeeqEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.GeeqService;

/**
 * Generate or update GEEQ scores
 *
 * @author tesar
 */
public class GeeqCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private GeeqService geeqService;

    private GeeqService.ScoreMode mode = GeeqService.ScoreMode.all;

    @Override
    public String getCommandName() {
        return "runGeeq";
    }

    @Override
    public String getShortDesc() {
        return "Generate or update GEEQ scores";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        if ( commandLine.hasOption( 'm' ) ) {
            this.mode = GeeqService.ScoreMode.valueOf( commandLine.getOptionValue( 'm' ) );
        }
    }

    @Override
    protected void buildOptions( Options options ) {

        super.buildOptions( options );
        super.addAutoOption( options, GeeqEvent.class );
        super.addLimitingDateOption( options );
        super.addForceOption( options );

        Option modeOption = Option.builder( "m" ).longOpt( "mode" )
                .desc( "If specified, switches the scoring mode. By default the mode is set to 'all'" //
                        + "\n Possible values are:" //
                        + "\n " + GeeqService.ScoreMode.all.name() + " - runs all scoring" //
                        + "\n " + GeeqService.ScoreMode.batch.name() + "- recalculates batch related scores - info, confound and batch effect" //
                        + "\n " + GeeqService.ScoreMode.reps.name() + " - recalculates score for replicates" //
                        + "\n " + GeeqService.ScoreMode.pub.name() + " - recalculates score for publication" )
                .hasArg().build();
        options.addOption( modeOption );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        if ( this.noNeedToRun( ee, GeeqEvent.class ) ) {
            return;
        }

        geeqService.calculateScore( ee, mode );

        try {
            refreshExpressionExperimentFromGemmaWeb( ee, false, true );
        } catch ( Exception e ) {
            log.warn( "Failed to refresh " + ee + " from Gemma Web", e );
        }
    }
}
