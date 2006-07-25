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

import java.io.Serializable;

/**
 * <hr>
 * <p>
 * Copyright (c) 2006 UBC Pavlab
 * 
 * @author klc
 * @version $Id$
 */

public class ProgressData implements Serializable {

    /** The serial version UID of this class. Needed for serialization. */
    private static final long serialVersionUID = -1618654342446645213L; // TODO: This needs to be generated... currently
                                                                        // just one number off off UserRoleImpl.

    private int percent;
    private String description;
    private boolean done;

    public ProgressData( int per, String descrip, boolean finished ) {
        percent = per;
        description = descrip;
        done = finished;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone( boolean done ) {
        this.done = done;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent( int percent ) {
        this.percent = percent;
    }

}
