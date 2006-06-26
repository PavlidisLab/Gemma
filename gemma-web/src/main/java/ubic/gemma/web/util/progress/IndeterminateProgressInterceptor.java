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

package ubic.gemma.web.util.progress;

/**
 * <hr>
 * <p>
 * Copyright (c) 2006 UBC Pavlab
 * 
 * @author klc
 * @version $Id$
 */
public abstract class IndeterminateProgressInterceptor extends ProgressInterceptor {

    final int RECALCULATE = 10;
    int recalculateEndpoint; // Used for determning when we want to try and recalculate the endpoint

    public IndeterminateProgressInterceptor( String progressDescription ) {
        super( progressDescription );
        this.recalculateEndpoint = 0;

    }

    // uses some kind of information to determine the endpoint of the process
    protected abstract void estimateEndPoint();

    // This could be problematic for the browser as the new percent could be much smaller or much larger.
    protected void updateSession( int newPercent ) {

        recalculateEndpoint++;
        if ( this.recalculateEndpoint == RECALCULATE ) this.estimateEndPoint();

        super.updateSession( newPercent );

    }
}
