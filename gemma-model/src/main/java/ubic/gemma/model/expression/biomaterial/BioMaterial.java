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

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * In MAGE, BioMaterial is an abstract class that represents the important substances such as cells, tissues, DNA,
 * proteins, etc... In MAGE, Biomaterial subclasses such as BioSample and BioSource can be related to other biomaterial
 * through a directed acyclic graph (represented by treatment(s)). In our implementation, we don't care so much about
 * the experimental procedures and we just lump all of the BioMaterial into one class.
 */
public abstract class BioMaterial extends ubic.gemma.model.common.Auditable implements gemma.gsec.model.SecuredChild {

    /**
     * Constructs new instances of {@link ubic.gemma.model.expression.biomaterial.BioMaterial}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.biomaterial.BioMaterial}.
         */
        public static ubic.gemma.model.expression.biomaterial.BioMaterial newInstance() {
            return new ubic.gemma.model.expression.biomaterial.BioMaterialImpl();
        }

    }

    private ubic.gemma.model.genome.Taxon sourceTaxon;

    private Collection<FactorValue> factorValues = new HashSet<>();

    private Collection<BioAssay> bioAssaysUsedIn = new HashSet<>();

    private Collection<Treatment> treatments = new HashSet<>();

    private Collection<Characteristic> characteristics = new HashSet<>();

    private ubic.gemma.model.common.description.DatabaseEntry externalAccession;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public BioMaterial() {
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.expression.bioAssay.BioAssay> getBioAssaysUsedIn() {
        return this.bioAssaysUsedIn;
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.common.description.Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    /**
  
     * An optional external refernce for this BioMaterial. In many cases this is the same as the accession for the
     * related BioAssay. We store the information here to help make the data easier to trace. Note that more than one
     * BioMaterial may reference a given external accession.
     * </p>
     */
    public ubic.gemma.model.common.description.DatabaseEntry getExternalAccession() {
        return this.externalAccession;
    }

    /**
     * The values that this BioAssay is associated with for the experiment.
     */
    public Collection<ubic.gemma.model.expression.experiment.FactorValue> getFactorValues() {
        return this.factorValues;
    }

    /**
     * 
     */
    private static final long serialVersionUID = -7892217074183258185L;

    @Override
    public Securable getSecurityOwner() {
        return null;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Taxon getSourceTaxon() {
        return this.sourceTaxon;
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.expression.biomaterial.Treatment> getTreatments() {
        return this.treatments;
    }

    public void setBioAssaysUsedIn( Collection<ubic.gemma.model.expression.bioAssay.BioAssay> bioAssaysUsedIn ) {
        this.bioAssaysUsedIn = bioAssaysUsedIn;
    }

    public void setCharacteristics( Collection<ubic.gemma.model.common.description.Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }

    public void setExternalAccession( ubic.gemma.model.common.description.DatabaseEntry externalAccession ) {
        this.externalAccession = externalAccession;
    }

    public void setFactorValues( Collection<ubic.gemma.model.expression.experiment.FactorValue> factorValues ) {
        this.factorValues = factorValues;
    }

    public void setSourceTaxon( ubic.gemma.model.genome.Taxon sourceTaxon ) {
        this.sourceTaxon = sourceTaxon;
    }

    public void setTreatments( Collection<ubic.gemma.model.expression.biomaterial.Treatment> treatments ) {
        this.treatments = treatments;
    }

}