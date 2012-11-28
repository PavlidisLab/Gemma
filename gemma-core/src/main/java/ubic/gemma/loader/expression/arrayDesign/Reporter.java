/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.loader.expression.arrayDesign;

import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * A "probe" (Affymetrix); for other types of arrays, there is no practical distinction between compositesequences and
 * reporters, and all analysis would take place at the level of CompositeSequences.
 */
public class Reporter extends ubic.gemma.model.common.DescribableImpl {

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Reporter() {
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 3703827938981026012L;

    private java.lang.Integer row;

    /**
     * 
     */
    public java.lang.Integer getRow() {
        return this.row;
    }

    public void setRow( java.lang.Integer row ) {
        this.row = row;
    }

    private java.lang.Integer col;

    /**
     * 
     */
    public java.lang.Integer getCol() {
        return this.col;
    }

    public void setCol( java.lang.Integer col ) {
        this.col = col;
    }

    private java.lang.Long startInBioChar;

    /**
     * 
     */
    public java.lang.Long getStartInBioChar() {
        return this.startInBioChar;
    }

    public void setStartInBioChar( java.lang.Long startInBioChar ) {
        this.startInBioChar = startInBioChar;
    }

    private java.lang.String strand;

    /**
     * <p>
     * Strand on which the reporter aligns to the biologicalCharacteristic. True=plus strand. False=minus strand.
     * </p>
     */
    public java.lang.String getStrand() {
        return this.strand;
    }

    public void setStrand( java.lang.String strand ) {
        this.strand = strand;
    }

    private CompositeSequence compositeSequence;

    /**
     * 
     */
    public CompositeSequence getCompositeSequence() {
        return this.compositeSequence;
    }

    public void setCompositeSequence( CompositeSequence compositeSequence ) {
        this.compositeSequence = compositeSequence;
    }

    private ubic.gemma.model.genome.biosequence.BioSequence immobilizedCharacteristic;

    /**
     * <p>
     * The sequence that is on the array for this reporter.
     * </p>
     */
    public ubic.gemma.model.genome.biosequence.BioSequence getImmobilizedCharacteristic() {
        return this.immobilizedCharacteristic;
    }

    public void setImmobilizedCharacteristic( ubic.gemma.model.genome.biosequence.BioSequence immobilizedCharacteristic ) {
        this.immobilizedCharacteristic = immobilizedCharacteristic;
    }

    /**
     * This entity does not have any identifiers but since it extends the <code>DesignElementImpl</code> class it will
     * simply delegate the call up there.
     * 
     * @see DesignElement#equals(Object)
     */
    @Override
    public boolean equals( Object object ) {
        return super.equals( object );
    }

    /**
     * This entity does not have any identifiers but since it extends the <code>DesignElementImpl</code> class it will
     * simply delegate the call up there.
     * 
     * @see DesignElement#hashCode()
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Constructs new instances of {@link Reporter}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link Reporter}.
         */
        public static Reporter newInstance() {
            return new Reporter();
        }

        /**
         * Constructs a new instance of {@link Reporter}, taking all required and/or read-only properties as arguments.
         */
        public static Reporter newInstance( CompositeSequence compositeSequence ) {
            final Reporter entity = new Reporter();
            entity.setCompositeSequence( compositeSequence );
            return entity;
        }

        /**
         * Constructs a new instance of {@link Reporter}, taking all possible properties (except the identifier(s))as
         * arguments.
         */
        public static Reporter newInstance( java.lang.Integer row, java.lang.Integer col,
                java.lang.Long startInBioChar, java.lang.String strand, java.lang.String name,
                java.lang.String description, CompositeSequence compositeSequence,
                ubic.gemma.model.genome.biosequence.BioSequence immobilizedCharacteristic ) {
            final Reporter entity = new Reporter();
            entity.setRow( row );
            entity.setCol( col );
            entity.setStartInBioChar( startInBioChar );
            entity.setStrand( strand );
            entity.setName( name );
            entity.setDescription( description );
            entity.setCompositeSequence( compositeSequence );
            entity.setImmobilizedCharacteristic( immobilizedCharacteristic );
            return entity;
        }
    }

    // HibernateEntity.vsl merge-point
}