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
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.ExpressionExperimentPlatformSwitchService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentPlatformSwitchEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import static ubic.gemma.cli.util.EntityOptionsUtils.addPlatformOption;

/**
 * Switch the array design used to the merged one.
 *
 * @author pavlidis
 */
public class ExpressionExperimentPlatformSwitchCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExpressionExperimentPlatformSwitchService serv;

    @Autowired
    private AuditTrailService ats;

    private String arrayDesignName = null;

    @Override
    public String getCommandName() {
        return "switchExperimentPlatform";
    }

    @Override
    public String getShortDesc() {
        return "Switch an experiment to a different array design (usually a merged one)";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        addPlatformOption( options, "a", "array", "Array design short name to be switched to - no need to specify if the platforms used by the EE are merged" );
        this.addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 'a' ) ) {
            this.arrayDesignName = commandLine.getOptionValue( 'a' );
        }
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        expressionExperiment = this.eeService.thawLite( expressionExperiment );
        ArrayDesign ad;
        if ( this.arrayDesignName != null ) {
            ad = entityLocator.locateArrayDesign( this.arrayDesignName );
            serv.switchExperimentToArrayDesign( expressionExperiment, ad );
            ats.addUpdateEvent( expressionExperiment, ExpressionExperimentPlatformSwitchEvent.class, "Switched to use " + ad );
        } else {
            // Identify merged platform automatically; not really recommended as it might not make the optimal choice.
            ad = serv.switchExperimentToMergedPlatform( expressionExperiment );
            ats.addUpdateEvent( expressionExperiment, ExpressionExperimentPlatformSwitchEvent.class, "Switched to use merged platform " + ad );
        }
    }
}
