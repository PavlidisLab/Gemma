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
package ubic.gemma.model.genome;

/**
 * @see ubic.gemma.model.genome.Taxon
 */
public class TaxonImpl extends ubic.gemma.model.genome.Taxon {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 6920404001701683807L;

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Taxon ) ) {
            return false;
        }
        final Taxon that = ( Taxon ) object;

        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {

            // use ncbi id OR scientific name.

            if ( this.getNcbiId() != null && that.getNcbiId() != null && !this.getNcbiId().equals( that.getNcbiId() ) )
                return false;

            if ( this.getSecondaryNcbiId() != null && that.getSecondaryNcbiId() != null
                    && !this.getSecondaryNcbiId().equals( that.getSecondaryNcbiId() ) ) return false;

            if ( this.getScientificName() != null && that.getScientificName() != null
                    && !this.getScientificName().equals( that.getScientificName() ) ) return false;

        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( this.getId() == null ? computeHashCode() : this.getId().hashCode() );

        return hashCode;
    }

    /**
     * @see ubic.gemma.model.genome.Taxon#toString()
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( "Taxon:" );
        if ( this.getId() != null ) {
            buf.append( " Id = " + this.getId() );
        }

        if ( this.getScientificName() != null ) {
            buf.append( " " + this.getScientificName() );
        }
        if ( this.getCommonName() != null ) {
            buf.append( " (" + this.getCommonName() + ")" );
        }
        if ( this.getNcbiId() != null ) {
            buf.append( " NCBI id=" + this.getNcbiId() );
        }
        if ( this.getAbbreviation() != null ) {
            buf.append( " Abbreviation =" + this.getAbbreviation() );
        }
        return buf.toString();
    }

    private int computeHashCode() {
        int hashCode = 0;
        if ( this.getNcbiId() != null ) {
            hashCode += this.getNcbiId().hashCode();
        } else if ( this.getScientificName() != null ) {
            hashCode += this.getScientificName().hashCode();
        } else {
            hashCode += super.hashCode();
        }

        return hashCode;
    }

}