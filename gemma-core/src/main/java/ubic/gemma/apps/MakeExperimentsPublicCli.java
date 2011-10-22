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

import ubic.gemma.security.SecurityService;

/**
 * Make data sets public. You must be the owner of the experiment to do this.
 * 
 * @author paul
 * @version $Id$
 */
public class MakeExperimentsPublicCli extends ExpressionExperimentManipulatingCLI {

    public static void main( String[] args ) {
        MakeExperimentsPublicCli d = new MakeExperimentsPublicCli();
        d.doWork( args );
    }

    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( "Delete experiments", args );

        SecurityService securityService = ( SecurityService ) this.getBean( "securityService" );

        securityService.makePublic( this.expressionExperiments );

        return null;
    }

}
