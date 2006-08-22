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

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ltd.getahead.dwr.WebContextFactory;

/**
 * <hr
 * <p>
 * This class is exposed on the client web side for ajax call backs. Notice this object has no member variables or state
 * information. DWR does not gaurantee that the same "object" will be called everytime from the client. In fact, dwr
 * seams to create a new instance of the server side object everytime it makes a call back from the client side.
 * 
 * @author klc
 * @version $Id$
 */

public class HttpProgressMonitor {

    protected static final Log logger = LogFactory.getLog( HttpProgressMonitor.class );
    public static final String PROGRESS_ATTRIBUTE = "progressInfo";

    public ProgressData getProgressStatus() {

        HttpSession session = WebContextFactory.get().getSession();
        HttpProgressObserver po = ( HttpProgressObserver ) session.getAttribute( PROGRESS_ATTRIBUTE );

        //if observer not in the session means 1st time through.  Add to session and move on. 
        if ( po == null ) {
            po = new HttpProgressObserver();
            session.setAttribute( PROGRESS_ATTRIBUTE, po );
        }

        ProgressData pd = po.getProgressData();

        //if progress has finished need to remove observer from session and get make sure observer cleans up after itself.
        if ( pd.isDone() ) {
            po.finished();
            session.removeAttribute( PROGRESS_ATTRIBUTE );
        }

        return pd;

    }

}
