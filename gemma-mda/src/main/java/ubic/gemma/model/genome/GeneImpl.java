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

            boolean bothHaveNcbi = this.getNcbiId() != null && that.getNcbiId() != null;

            if ( bothHaveNcbi ) {
                return this.getNcbiId().equals( that.getNcbiId() );
            }

            boolean bothHaveSymbol = this.getOfficialSymbol() != null && that.getOfficialSymbol() != null;
            boolean bothHaveTaxon = this.getTaxon() != null && that.getTaxon() != null;

            if ( bothHaveTaxon && bothHaveSymbol && this.getTaxon().equals( that.getTaxon() )
                    && this.getOfficialSymbol().equalsIgnoreCase( that.getOfficialSymbol() ) ) {

                boolean bothHaveName = this.getOfficialName() != null && that.getOfficialName() != null;
                boolean bothHavePhysicalLocation = this.getPhysicalLocation() != null
                        && that.getPhysicalLocation() != null;
                boolean bothHaveGeneProducts = this.getProducts() != null && this.getProducts().size() > 0
                        && that.getProducts() != null && that.getProducts().size() > 0;

                if ( bothHaveName ) {
                    return this.getOfficialName().equals( that.getOfficialName() );
                } else if ( bothHavePhysicalLocation ) {
                    return this.getPhysicalLocation().equals( that.getPhysicalLocation() );
                } else if ( bothHaveGeneProducts ) {
                    // fastest check: they must any given gene product in common.
                    GeneProduct thisGeneProduct = this.getProducts().iterator().next();
                    for ( GeneProduct thatGeneProduct : that.getProducts() ) {
                        if ( thisGeneProduct.equals( thatGeneProduct ) ) {
                            return true;
                        }
                    }
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

    private int computeHashCode() {
        int hashCode = 29;

        if ( this.getNcbiId() != null ) {
            hashCode += this.getNcbiId().hashCode();
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

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( this.getClass().getSimpleName() );
        buf.append( this.getId() == null ? " " : " Id:" + this.getId() + " " );
        buf.append( this.getOfficialSymbol() + " " );
        buf.append( this.getOfficialName() == null ? "" : this.getOfficialName() + " " );
        buf.append( this.getOfficialName() == null && this.getPhysicalLocation() != null ? "["
                + this.getPhysicalLocation() + "] " : "" );
        buf.append( this.getNcbiId() == null ? "" : " (NCBI " + this.getNcbiId() + ")" );
        return buf.toString();
    }
}