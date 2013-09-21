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

import ubic.gemma.model.genome.gene.GeneProduct;

/**
 * @see ubic.gemma.model.genome.Gene
 * @author pavlidis
 * @version $Id$
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

            boolean bothHaveNcbi = this.getNcbiGeneId() != null && that.getNcbiGeneId() != null;

            if ( bothHaveNcbi ) {
                return this.getNcbiGeneId().equals( that.getNcbiGeneId() );
            }

            boolean bothHaveSymbol = this.getOfficialSymbol() != null && that.getOfficialSymbol() != null;
            boolean bothHaveTaxon = this.getTaxon() != null && that.getTaxon() != null;

            if ( bothHaveTaxon && bothHaveSymbol && this.getTaxon().equals( that.getTaxon() )
                    && this.getOfficialSymbol().equalsIgnoreCase( that.getOfficialSymbol() ) ) {

                boolean bothHaveName = this.getOfficialName() != null && that.getOfficialName() != null;
                boolean bothHavePhysicalLocation = this.getPhysicalLocation() != null
                        && that.getPhysicalLocation() != null;

                if ( bothHaveName ) {
                    return this.getOfficialName().equals( that.getOfficialName() );
                } else if ( bothHavePhysicalLocation ) {
                    /*
                     * The gene must be thawed, which isn't certain, but if the gene is persistent, we _probably_
                     * wouldn't get this far. See bug 1840, which involves code that _shouldn't_ get this far but it
                     * does.
                     */
                    return this.getPhysicalLocation().equals( that.getPhysicalLocation() );
                } else {
                    return false; // can't decide, assume unequal.
                }
            }
            return false; //

        }
        return true;

    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( this.getId() == null ? computeHashCode() : this.getId().hashCode() );
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( this.getClass().getSimpleName().replace( "Impl", "" ) );
        buf.append( this.getId() == null ? " " : " Id:" + this.getId() + " " );
        buf.append( this.getOfficialSymbol() + " " );
        buf.append( this.getOfficialName() == null ? "" : this.getOfficialName() + " " );

        // This causes too many lazy load problems.
        // buf.append( this.getOfficialName() == null && this.getPhysicalLocation() != null ? "["
        // + this.getPhysicalLocation() + "] " : "" );

        buf.append( this.getNcbiGeneId() == null ? "" : " (NCBI " + this.getNcbiGeneId() + ")" );
        return buf.toString();
    }

    /**
     * @return
     */
    private int computeHashCode() {
        int hashCode = 29;

        if ( this.getNcbiGeneId() != null ) {
            hashCode += this.getNcbiGeneId().hashCode();
            return hashCode;
        }

        if ( this.getOfficialSymbol() != null ) {
            hashCode += this.getOfficialSymbol().hashCode();
        }

        if ( this.getTaxon() != null ) {
            hashCode += this.getTaxon().hashCode();
        }

        if ( this.getOfficialName() != null ) {
            hashCode += this.getOfficialName().hashCode();
        } else if ( this.getPhysicalLocation() != null ) {
            hashCode += this.getPhysicalLocation().hashCode();
        } else if ( this.getProducts() != null && this.getProducts().size() > 0 ) {
            GeneProduct gp = this.getProducts().iterator().next();
            hashCode += gp.hashCode();
        }

        hashCode += super.hashCode();

        return hashCode;
    }
}