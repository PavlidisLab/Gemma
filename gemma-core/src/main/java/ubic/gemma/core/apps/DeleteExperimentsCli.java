/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.apps;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

/**
 * Delete one or more experiments from the system.
 *
 * @author paul
 */
public class DeleteExperimentsCli extends ExpressionExperimentManipulatingCLI {

    private List<String> platformAccs = null;

    @Override
    public String getCommandName() {
        return "deleteExperiments";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        options.addOption(
                Option.builder( "a" ).longOpt( "array" )
                        .desc( "Delete platform(s) instead; you must delete associated experiments first; other options are ignored" )
                        .argName( "comma-delimited list of platform short names" ).hasArg().build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        super.processOptions( commandLine );
        if ( commandLine.hasOption( 'a' ) ) {
            this.platformAccs = Arrays.asList( StringUtils.split( commandLine.getOptionValue( 'a' ), "," ) );
        }
    }

    @Override
    protected void doWork() throws Exception {
        this.force = true;

        if ( platformAccs != null ) {
            ArrayDesignService ads = this.getBean( ArrayDesignService.class );

            for ( String p : platformAccs ) {

                ArrayDesign a = ads.findByShortName( p );

                if ( a == null ) {
                    log.info( "No such platform " + p );
                    addErrorObject( p, "No such platform " );
                    continue;
                }

                if ( !ads.getExpressionExperiments( a ).isEmpty() ) {
                    log.info( "Platform still has experiments that must be deleted first: " + p );
                    addErrorObject( p, "Experiments still exist for platform" );
                    continue;
                }

                if ( !ads.getSwitchedExperiments( a.getId() ).isEmpty() ) {
                    log.info( "Platform still has experiments (switched to anther platform) that must be deleted first: " + p );
                    addErrorObject( p, "Experiments  (switched to anther platform) still exist for platform" );
                    continue;
                }

                try {
                    log.info( "--------- Deleting " + a + " --------" );
                    ads.remove( a );
                    addSuccessObject( a, "Successfully deleted " + a );
                } catch ( Exception e ) {
                    addErrorObject( a, e.getMessage(), e );
                }
            }
            return;
        }

        for ( BioAssaySet bas : this.expressionExperiments ) {
            try {
                log.info( "--------- Deleting " + bas + " --------" );
                this.eeService.remove( ( ExpressionExperiment ) bas );
                addSuccessObject( bas, "Successfully deleted " + bas );
                log.info( "--------- Finished Deleting " + bas + " -------" );
            } catch ( Exception ex ) {
                addErrorObject( bas, ex.getMessage(), ex );
            }
        }
    }

    @Override
    public String getShortDesc() {
        return "Delete experiments or platforms from the system";
    }

}
