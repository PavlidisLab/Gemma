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
package ubic.gemma.model.genome.biosequence;

/**
 * @see ubic.gemma.model.genome.biosequence.BioSequence
 * @author pavlidis
 * @version $Id$
 */
public class BioSequenceImpl extends ubic.gemma.model.genome.biosequence.BioSequence {

    /**
     * 
     */
    private static final long serialVersionUID = -6620431603579954167L;

    @Override
    public boolean equals( Object object ) {

        if ( !( object instanceof BioSequence ) ) {
            return false;
        }
        final BioSequence that = ( BioSequence ) object;
        if ( this.getId() != null && that.getId() != null ) return this.getId().equals( that.getId() );

        // The way this is constructed, ALL of the items must be the same.
        if ( this.getSequenceDatabaseEntry() != null && that.getSequenceDatabaseEntry() != null
                && !this.getSequenceDatabaseEntry().equals( that.getSequenceDatabaseEntry() ) ) return false;

        if ( this.getTaxon() != null && that.getTaxon() != null && !this.getTaxon().equals( that.getTaxon() ) )
            return false;

        if ( this.getName() != null && that.getName() != null && !this.getName().equals( that.getName() ) )
            return false;

        if ( this.getSequence() != null && that.getSequence() != null
                && !this.getSequence().equals( that.getSequence() ) ) return false;

        if ( this.getLength() != null && that.getLength() != null && !this.getLength().equals( that.getLength() ) )
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;

        if ( this.getId() != null ) {
            return 29 * getId().hashCode();
        }
        int nameHash = this.getName() == null ? 0 : getName().hashCode();
        int taxonHash = this.getTaxon() == null ? 0 : getTaxon().hashCode();
        int lengthHash = this.getLength() == null ? 0 : getLength().hashCode();
        int dbHash = this.getSequenceDatabaseEntry() == null ? 0 : getSequenceDatabaseEntry().hashCode();
        int seqHash = 0;
        if ( dbHash == 0 && nameHash == 0 && lengthHash == 0 && this.getSequence() != null )
            seqHash = this.getSequence().hashCode();
        hashCode = 29 * nameHash + seqHash + dbHash + taxonHash + lengthHash;

        return hashCode;
    }
}