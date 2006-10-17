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
package ubic.gemma.model.genome;

/**
 * @see ubic.gemma.model.genome.Gene
 */
public class GeneImpl extends ubic.gemma.model.genome.Gene {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -557590340503789274L;

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Gene ) ) {
            return false;
        }
        final Gene that = ( Gene ) object;

        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {

            // to be unambiguous need NCBI id OR (symbol + taxon + (name OR physical location))

            boolean bothHaveNcbi = this.getNcbiId() != null && that.getNcbiId() != null;

            if ( bothHaveNcbi ) {
                return this.getNcbiId().equals( that.getNcbiId() );
            }

            boolean bothHaveSymbol = this.getOfficialSymbol() != null && that.getOfficialSymbol() != null;
            boolean bothHaveTaxon = this.getTaxon() != null && that.getTaxon() != null;

            if ( bothHaveTaxon && bothHaveSymbol && this.getTaxon().equals( that.getTaxon() )
                    && this.getOfficialSymbol().equals( that.getOfficialSymbol() ) ) {

                boolean bothHaveName = this.getOfficialName() != null && that.getOfficialName() != null;
                boolean bothHavePhysicalLocation = this.getPhysicalLocation() != null
                        && that.getPhysicalLocation() != null;

                if ( bothHaveName ) {
                    return this.getOfficialName().equals( that.getOfficialName() );
                } else if ( bothHavePhysicalLocation ) {
                    return this.getPhysicalLocation().nearlyEquals( that.getPhysicalLocation() );
                } else {
                    return false; // can't decide, assume unequal.
                }
            } else {
                return false; // 
            }
        }
        return true;

    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( this.getId() == null ? computeHashCode() : this.getId().hashCode() );
        return hashCode;
    }

    private int computeHashCode() {
        int hashCode = 0;

        if ( this.getNcbiId() != null ) {
            hashCode += this.getNcbiId().hashCode();
        } else if ( this.getOfficialSymbol() != null && this.getOfficialName() != null && this.getTaxon() != null ) {
            hashCode += this.getOfficialSymbol().hashCode();
            hashCode += this.getOfficialName().hashCode();
            hashCode += this.getTaxon().hashCode();
        } else if ( this.getOfficialSymbol() != null && this.getPhysicalLocation() != null && this.getTaxon() != null ) {
            hashCode += this.getOfficialSymbol().hashCode();
            hashCode += this.getPhysicalLocation().hashCode();
            hashCode += this.getTaxon().hashCode();
        } else {
            hashCode += super.hashCode();
        }

        return hashCode;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + ( this.getId() == null ? " " : " Id:" + this.getId() )
                + this.getOfficialSymbol()
                + ( this.getOfficialName() == null ? " " : this.getOfficialName() + " " )
                + ( this.getOfficialName() == null && this.getPhysicalLocation() == null ? " " : this
                        .getPhysicalLocation()
                        + " " ) + ( this.getNcbiId() == null ? " " : " (NCBI " + this.getNcbiId() + ")" );
    }
}