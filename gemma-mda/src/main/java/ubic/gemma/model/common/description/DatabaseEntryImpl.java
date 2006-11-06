/*
 * The Gemma project.
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common.description;

/**
 * @see ubic.gemma.model.common.description.DatabaseEntry
 * @author pavlidis
 * @version $Id$
 */
public class DatabaseEntryImpl extends ubic.gemma.model.common.description.DatabaseEntry {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 2482685192599172941L;

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntry#toString()
     */
    @Override
    public java.lang.String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( this.getAccession() + " " );
        buf.append( this.getExternalDatabase() == null ? "[no external database]" : this.getExternalDatabase()
                .getName() );
        buf.append( this.getId() == null ? "" : " (Id=" + this.getId() + ")" );
        return buf.toString();
    }

}