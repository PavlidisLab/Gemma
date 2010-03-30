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
package ubic.gemma.job.progress.grid;

import ubic.gemma.job.grid.util.SpacesGenericEntry;
import net.jini.core.event.RemoteEventListener;

/**
 * Used to log information on the compute server. This type of entry is written to the java space, and received by a
 * {@link RemoteEventListener} as a notification.
 * 
 * @author keshav
 * @version $Id$
 */
public class SpacesProgressEntry extends SpacesGenericEntry {

    private static final long serialVersionUID = 1L;

    public SpacesProgressEntry() {
        super();
        this.setFifo( true );
    }
}
