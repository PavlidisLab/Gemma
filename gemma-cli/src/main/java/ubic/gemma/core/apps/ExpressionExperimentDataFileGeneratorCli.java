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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

/**
 * @author paul
 */
public class ExpressionExperimentDataFileGeneratorCli extends ExpressionExperimentManipulatingCLI {

    private static final String DESCRIPTION = "Generate analysis text files (diff expression)";

    @Autowired
    private ExpressionDataFileService expressionDataFileService;
    @Autowired
    private AuditTrailService ats;

    private boolean forceWrite = false;

    @Override
    public String getCommandName() {
        return "generateDataFile";
    }


    @Override
    public String getShortDesc() {
        return ExpressionExperimentDataFileGeneratorCli.DESCRIPTION;
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        Option forceWriteOption = Option.builder( "w" )
                .desc( "Overwrites existing files if this option is set" ).longOpt( "forceWrite" )
                .build();
        options.addOption( forceWriteOption );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( 'w' ) ) {
            this.forceWrite = true;
        }
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee1 ) {
        getBatchTaskExecutor().submit( () -> {
            log.info( "Processing Experiment: " + ee1.getName() );
            ExpressionExperiment ee = this.eeService.thawLite( ee1 );
            expressionDataFileService.writeOrLocateDiffExpressionDataFiles( ee, forceWrite )
                    .forEach( ExpressionDataFileService.LockedPath::close );
            ats.addUpdateEvent( ee, CommentedEvent.class, "Generated Flat data files for downloading" );
            addSuccessObject( ee, "Success:  generated data file for " + ee.getShortName() + " ID=" + ee.getId() );
            return null;
        } );
    }
}
