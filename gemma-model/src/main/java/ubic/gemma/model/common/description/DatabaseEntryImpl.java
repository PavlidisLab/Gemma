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

    @Override
    public boolean equals( Object object ) {
        if ( !( object instanceof DatabaseEntry ) ) return false;

        DatabaseEntry that = ( DatabaseEntry ) object;

        if ( this.getId() != null && that.getId() != null ) return super.equals( object );

        if ( this.getAccession() != null && that.getAccession() != null
                && !this.getAccession().equals( that.getAccession() ) ) return false;

        if ( this.getAccessionVersion() != null && that.getAccessionVersion() != null
                && !this.getAccessionVersion().equals( that.getAccessionVersion() ) ) return false;

        if ( this.getExternalDatabase() != null && that.getExternalDatabase() != null
                && !this.getExternalDatabase().equals( that.getExternalDatabase() ) ) return false;

        return true;
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null ) return super.hashCode();

        int hashCode = 0;
        if ( this.getAccession() != null ) hashCode = 29 * this.getAccession().hashCode();

        if ( this.getAccessionVersion() != null ) hashCode += this.getAccessionVersion().hashCode();

        if ( this.getExternalDatabase() != null ) hashCode += this.getExternalDatabase().hashCode();

        return hashCode;
    }

    /**
     * @see ubic.gemma.model.common.description.DatabaseEntry#toString()
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( this.getAccession() + " " );
        buf.append( this.getExternalDatabase() == null ? "[no external database]" : this.getExternalDatabase()
                .getName() );
        buf.append( this.getId() == null ? "" : " (Id=" + this.getId() + ")" );
        return buf.toString();
    }

}