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
 * @see ubic.gemma.model.genome.Chromosome
 */
public class ChromosomeImpl extends ubic.gemma.model.genome.Chromosome {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -8353766718193697363L;

    /**
     * This method is only familiar with chromosomes as named in the UCSC GoldenPath database and would have to be
     * modified if the 'rules' are broken by some other source used.
     * 
     * @return true if this is a regular chromosome, not a separate assembly for e.g. a haplotype (6_cox_hap2 for
     *         example).
     */
    public static boolean isCanonical( Chromosome c ) {
        String name = c.getName();

        /*
         * X|Un, 1|2|3|4,
         */
        if ( name.contains( "|" ) ) return false;

        /*
         * ??
         */
        if ( name.equals( "-" ) ) return false;

        /*
         * ???
         */
        if ( name.contains( "_random" ) ) return false;

        return true;

    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Chromosome ) ) {
            return false;
        }
        final Chromosome that = ( Chromosome ) object;

        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {
            return this.getTaxon().equals( that.getTaxon() ) && this.getName().equals( that.getName() );
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;

        assert this.getName() != null;
        assert this.getTaxon() != null;

        hashCode = 29
                * hashCode
                + ( this.getId() == null ? this.getName().hashCode() + this.getTaxon().hashCode() : this.getId()
                        .hashCode() );

        return hashCode;
    }

    @Override
    public String toString() {
        return this.getTaxon().getScientificName() + " Chromosome " + this.getName();
    }

}