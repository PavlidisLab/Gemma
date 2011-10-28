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
package ubic.gemma.model.genome.gene;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.genome.gene.GeneProduct
 */
public class GeneProductImpl extends ubic.gemma.model.genome.gene.GeneProduct {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8414732389521430535L;

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof GeneProduct ) ) {
            return false;
        }
        final GeneProduct that = ( GeneProduct ) object;

        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {

            boolean bothHaveNcbiGi = this.getNcbiGi() != null && that.getNcbiGi() != null;

            if ( bothHaveNcbiGi ) {
                return this.getNcbiGi().equals( that.getNcbiGi() );
            }

            boolean bothHaveGene = this.getGene() != null && that.getGene() != null;
            boolean bothHaveSymbol = this.getName() != null && that.getName() != null;

            if ( bothHaveSymbol && bothHaveGene && this.getName().equals( that.getName() )
                    && this.getGene().equals( that.getGene() ) ) {
                return true;
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

        buf.append( this.getClass().getSimpleName() );

        if ( this.getId() != null ) {
            buf.append( " Id=" + this.getId() + " " );
        } else {
            buf.append( " " );
        }

        if ( this.getNcbiGi() != null ) {

        }

        buf.append( this.getName() + ( this.getName() == null ? "" : " GI:" + this.getNcbiGi() ) );
        buf.append( " [Gene = " + this.getGene() + "]" );

        return buf.toString();

    }

    private int computeHashCode() {
        int hashCode = 0;

        if ( this.getNcbiGi() != null ) {
            hashCode += this.getNcbiGi().hashCode();
        } else if ( this.getName() != null && this.getGene() != null ) {
            hashCode += this.getName().hashCode();
            hashCode += this.getGene().hashCode();
        }

        return hashCode;
    }

}