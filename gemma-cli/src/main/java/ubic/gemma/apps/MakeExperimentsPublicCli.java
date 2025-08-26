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
package ubic.gemma.apps;

import gemma.gsec.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.auditAndSecurity.eventType.MakePublicEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Make data sets public. You must be the owner of the experiment to do this.
 *
 * @author paul
 */
public class MakeExperimentsPublicCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private SecurityService securityService;

    @Override
    public String getCommandName() {
        return "makePublic";
    }

    @Override
    public String getShortDesc() {
        return "Make experiments public";
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        securityService.makePublic( ee );
        this.auditTrailService.addUpdateEvent( ee, MakePublicEvent.class, "Made public from command line" );
        addSuccessObject( ee, "Experiment was made public." );
    }
}
