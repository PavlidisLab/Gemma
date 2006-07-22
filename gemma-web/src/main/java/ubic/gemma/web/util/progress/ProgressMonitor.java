/* Copyright (c) 2006 University of British Columbia
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

import uk.ltd.getahead.dwr.ExecutionContext;

/**
 * <hr>
 * <p>
 * Copyright (c) 2006 UBC Pavlab
 * 
 * @author klc
 * @version $Id$
 */




public class ProgressMonitor implements ProgressObserver {

    private ProgressData pData;

    public ProgressMonitor() {
        pData = new ProgressData( 0, "Initilizing", false );
        //Not sure the best way to do this.  Perpaps subclassing for different types of monitors...
        //SecurityContextHolder.getContext().getAuthentication.getName();
        ProgressManager.addToNotification( ExecutionContext.get().getHttpServletRequest().getRemoteUser(), this );
    }

    public ProgressData getProgressStatus() {

        return pData;

    }

    public void progressUpdate( ProgressData pd ) {

        pData = pd;

    }

}
