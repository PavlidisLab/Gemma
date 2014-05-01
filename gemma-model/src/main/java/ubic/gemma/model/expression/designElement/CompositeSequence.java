/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.expression.designElement;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * A "Probe set" (Affymetrix) or a "Probe" (other types of arrays). The sequence referred to is a "target sequence"
 * (Affymetrix), oligo (oligo arrays) or cDNA clone/EST (cDNA arrays)
 */
public abstract class CompositeSequence extends ubic.gemma.model.common.Describable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.expression.designElement.CompositeSequence}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.designElement.CompositeSequence}.
         */
        public static ubic.gemma.model.expression.designElement.CompositeSequence newInstance() {
            return new ubic.gemma.model.expression.designElement.CompositeSequenceImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -3859507822452159349L;
    private ubic.gemma.model.genome.biosequence.BioSequence biologicalCharacteristic;

    private ArrayDesign arrayDesign;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public CompositeSequence() {
    }

    /**
     * 
     */
    public ArrayDesign getArrayDesign() {
        return this.arrayDesign;
    }

    /**
     * The sequence for this composite sequence.
     */
    public ubic.gemma.model.genome.biosequence.BioSequence getBiologicalCharacteristic() {
        return this.biologicalCharacteristic;
    }

    public void setArrayDesign( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    public void setBiologicalCharacteristic( ubic.gemma.model.genome.biosequence.BioSequence biologicalCharacteristic ) {
        this.biologicalCharacteristic = biologicalCharacteristic;
    }

}