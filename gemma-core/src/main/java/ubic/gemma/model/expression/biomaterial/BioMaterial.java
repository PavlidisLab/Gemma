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

package ubic.gemma.model.expression.biomaterial;

import gemma.gsec.model.Securable;
import ubic.gemma.model.common.AbstractAuditable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.Collection;
import java.util.HashSet;

/**
 * In MAGE, BioMaterial is an abstract class that represents the important substances such as cells, tissues, DNA,
 * proteins, etc... In MAGE, Biomaterial subclasses such as BioSample and BioSource can be related to other biomaterial
 * through a directed acyclic graph (represented by treatment(s)). In our implementation, we don't care so much about
 * the experimental procedures and we just lump all of the BioMaterial into one class.
 */
public class BioMaterial extends AbstractAuditable implements gemma.gsec.model.SecuredChild {


    private static final long serialVersionUID = 4374359557498220256L;
    private ubic.gemma.model.genome.Taxon sourceTaxon;
    private Collection<FactorValue> factorValues = new HashSet<>();
    private Collection<BioAssay> bioAssaysUsedIn = new HashSet<>();
    private Collection<Treatment> treatments = new HashSet<>();
    private Collection<Characteristic> characteristics = new HashSet<>();
    private DatabaseEntry externalAccession;

    public Collection<BioAssay> getBioAssaysUsedIn() {
        return this.bioAssaysUsedIn;
    }

    public void setBioAssaysUsedIn( Collection<BioAssay> bioAssaysUsedIn ) {
        this.bioAssaysUsedIn = bioAssaysUsedIn;
    }

    public Collection<Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    public void setCharacteristics( Collection<Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }

    /**
     * An optional external refernce for this BioMaterial. In many cases this is the same as the accession for the
     * related BioAssay. We store the information here to help make the data easier to trace. Note that more than one
     * BioMaterial may reference a given external accession.
     * </p>
     */
    public DatabaseEntry getExternalAccession() {
        return this.externalAccession;
    }

    public void setExternalAccession( DatabaseEntry externalAccession ) {
        this.externalAccession = externalAccession;
    }

    /**
     * The values that this BioAssay is associated with for the experiment.
     */
    public Collection<FactorValue> getFactorValues() {
        return this.factorValues;
    }

    public void setFactorValues( Collection<FactorValue> factorValues ) {
        this.factorValues = factorValues;
    }

    @Override
    public Securable getSecurityOwner() {
        return null;
    }

    public ubic.gemma.model.genome.Taxon getSourceTaxon() {
        return this.sourceTaxon;
    }

    public void setSourceTaxon( ubic.gemma.model.genome.Taxon sourceTaxon ) {
        this.sourceTaxon = sourceTaxon;
    }

    public Collection<Treatment> getTreatments() {
        return this.treatments;
    }

    public void setTreatments( Collection<Treatment> treatments ) {
        this.treatments = treatments;
    }

    /**
     * Constructs new instances of {@link BioMaterial}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link BioMaterial}.
         */
        public static BioMaterial newInstance() {
            return new BioMaterial();
        }

    }

}