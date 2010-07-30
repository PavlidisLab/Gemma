/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.analysis.expression.diff;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.util.r.RClient;
import ubic.basecode.util.r.RConnectionFactory;
import ubic.basecode.util.r.RServeClient;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.util.ConfigUtils;

/**
 * An abstract analyzer to be extended by analyzers which will make use of R.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractAnalyzer {

    protected RClient rc = null;

    @Autowired
    protected ExpressionDataMatrixService expressionDataMatrixService = null;

    /**
     * Connect to R.
     */
    public void connectToR() {

        if ( rc != null && !rc.isConnected() && rc instanceof RServeClient ) {
            ( ( RServeClient ) rc ).connect();
        } else {
            String hostname = ConfigUtils.getString( "gemma.rserve.hostname", "localhost" );
            rc = RConnectionFactory.getRConnection( hostname );
            if ( rc == null ) throw new RuntimeException( "R connection was not established" );
        }

        assert rc != null;
    }

    /**
     * Disconnect from R, clean up objects. Good idea to call this at the end of any methods using R commands.
     */
    public void disconnectR() {
        if ( rc == null || !rc.isConnected() ) {
            return;
        }
        rc.voidEval( "rm(list = ls())" ); // this probably doesn't do much, but doesn't hurt.
        if ( rc != null && rc instanceof RServeClient ) {
            ( ( RServeClient ) rc ).disconnect();
        }
        rc = null;
    }

    /**
     * @param expressionDataMatrixService
     */
    public void setExpressionDataMatrixService( ExpressionDataMatrixService expressionDataMatrixService ) {
        this.expressionDataMatrixService = expressionDataMatrixService;
    }

}
