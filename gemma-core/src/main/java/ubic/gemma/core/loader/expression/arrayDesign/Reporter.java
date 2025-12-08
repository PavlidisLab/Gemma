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
package ubic.gemma.core.loader.expression.arrayDesign;

import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.DescribableUtils;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.io.Serializable;

/**
 * A "probe" (Affymetrix); for other types of arrays, there is no practical distinction between compositesequences and
 * reporters, and all analysis would take place at the level of CompositeSequences.
 * For a brief time this was part of our core data model; but currently this is used only transiently during parsing
 * etc. of Affymetrix platform sequence files.
 */
@SuppressWarnings("unused") // Possible external use
public class Reporter extends AbstractDescribable implements Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 3703827938981026012L;
    private Integer row;
    private Integer col;
    private Long startInBioChar;
    private String strand;
    private CompositeSequence compositeSequence;
    private ubic.gemma.model.genome.biosequence.BioSequence immobilizedCharacteristic;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    @SuppressWarnings("WeakerAccess") // Required by Spring
    public Reporter() {
    }

    public Integer getRow() {
        return this.row;
    }

    public void setRow( Integer row ) {
        this.row = row;
    }

    public Integer getCol() {
        return this.col;
    }

    public void setCol( Integer col ) {
        this.col = col;
    }

    public Long getStartInBioChar() {
        return this.startInBioChar;
    }

    public void setStartInBioChar( Long startInBioChar ) {
        this.startInBioChar = startInBioChar;
    }

    /**
     * @return Strand on which the reporter aligns to the biologicalCharacteristic. True=plus strand. False=minus strand.
     */
    public String getStrand() {
        return this.strand;
    }

    public void setStrand( String strand ) {
        this.strand = strand;
    }

    public CompositeSequence getCompositeSequence() {
        return this.compositeSequence;
    }

    public void setCompositeSequence( CompositeSequence compositeSequence ) {
        this.compositeSequence = compositeSequence;
    }

    /**
     * @return The sequence that is on the array for this reporter.
     */
    public ubic.gemma.model.genome.biosequence.BioSequence getImmobilizedCharacteristic() {
        return this.immobilizedCharacteristic;
    }

    public void setImmobilizedCharacteristic(
            ubic.gemma.model.genome.biosequence.BioSequence immobilizedCharacteristic ) {
        this.immobilizedCharacteristic = immobilizedCharacteristic;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof Reporter ) )
            return false;
        Reporter that = ( Reporter ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        }
        return DescribableUtils.equalsByName( this, that );
    }

    public static final class Factory {

        public static Reporter newInstance() {
            return new Reporter();
        }

    }

}