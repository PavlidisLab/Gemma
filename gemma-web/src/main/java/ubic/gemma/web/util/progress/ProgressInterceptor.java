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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.MethodBeforeAdvice;

import uk.ltd.getahead.dwr.ExecutionContext;

/**
 * <hr>
 * <p>
 * Copyright (c) 2006 UBC Pavlab
 * 
 * @author klc
 * @version $Id$
 */
public abstract class ProgressInterceptor implements MethodBeforeAdvice {

    private HttpServletRequest req;
    protected static final Log logger = LogFactory.getLog( PersistProgressInterceptor.class );
    protected int finishingValue; // used to determine the end of the progress metre
    protected int progress; // just a count of the progress made
    protected int percent;
    protected String description;

    public ProgressInterceptor(String progressDescription) {
        super();
        description = progressDescription;
        progress = 0;
        finishingValue = 0;
        percent = 0;
        req = ExecutionContext.get().getHttpServletRequest();
    }

    // updates the session info with the new percentage.
    protected void updateSession( int newPercent ) {

        if ( newPercent == 100 ) {
            req.getSession().setAttribute( "ProgessInfo",
                    new ProgressData( newPercent, finishingValue, description, Boolean.TRUE ) );
        } else if ( newPercent > percent ) {
            req.getSession().setAttribute( "ProgessInfo",
                    new ProgressData( newPercent, finishingValue, description, Boolean.FALSE ) );
        }

    }

}
