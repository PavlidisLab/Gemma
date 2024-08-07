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

import ubic.gemma.model.common.auditAndSecurity.Securable;
import org.hibernate.search.annotations.*;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * In MAGE, BioMaterial is an abstract class that represents the important substances such as cells, tissues, DNA,
 * proteins, etc... In MAGE, Biomaterial subclasses such as BioSample and BioSource can be related to other biomaterial
 * through a directed acyclic graph (represented by treatment(s)). In our implementation, we don't care so much about
 * the experimental procedures and we just lump all of the BioMaterial into one class.
 */
@Indexed
public class BioMaterial extends AbstractDescribable implements SecuredChild, Serializable {

    private static final long serialVersionUID = 4374359557498220256L;
    private Taxon sourceTaxon;
    private Set<FactorValue> factorValues = new HashSet<>();
    private Set<BioAssay> bioAssaysUsedIn = new HashSet<>();
    private Set<Treatment> treatments = new HashSet<>();
    private Set<Characteristic> characteristics = new HashSet<>();
    @Nullable
    private DatabaseEntry externalAccession;

    @Override
    public int hashCode() {
        return Objects.hash( getName() );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof BioMaterial ) )
            return false;
        final BioMaterial that = ( BioMaterial ) object;
        if ( this.getId() != null && that.getId() != null )
            return this.getId().equals( that.getId() );
        return Objects.equals( getName(), that.getName() );
    }

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    @Field
    public String getName() {
        return super.getName();
    }

    @ContainedIn
    public Set<BioAssay> getBioAssaysUsedIn() {
        return this.bioAssaysUsedIn;
    }

    public void setBioAssaysUsedIn( Set<BioAssay> bioAssaysUsedIn ) {
        this.bioAssaysUsedIn = bioAssaysUsedIn;
    }

    @IndexedEmbedded
    public Set<Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    public void setCharacteristics( Set<Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }

    /**
     * @return An optional external reference for this BioMaterial. In many cases this is the same as the accession for
     *         the
     *         related BioAssay. We store the information here to help make the data easier to trace. Note that more
     *         than one
     *         BioMaterial may reference a given external accession.
     */
    @Nullable
    @IndexedEmbedded
    public DatabaseEntry getExternalAccession() {
        return this.externalAccession;
    }

    public void setExternalAccession( @Nullable DatabaseEntry externalAccession ) {
        this.externalAccession = externalAccession;
    }

    /**
     * @return The values that this BioAssay is associated with for the experiment.
     */
    public Set<FactorValue> getFactorValues() {
        return this.factorValues;
    }

    public void setFactorValues( Set<FactorValue> factorValues ) {
        this.factorValues = factorValues;
    }

    @Transient
    @Override
    public Securable getSecurityOwner() {
        return null;
    }

    public Taxon getSourceTaxon() {
        return this.sourceTaxon;
    }

    public void setSourceTaxon( Taxon sourceTaxon ) {
        this.sourceTaxon = sourceTaxon;
    }

    public Set<Treatment> getTreatments() {
        return this.treatments;
    }

    public void setTreatments( Set<Treatment> treatments ) {
        this.treatments = treatments;
    }

    public static final class Factory {
        public static BioMaterial newInstance() {
            return new BioMaterial();
        }

        public static BioMaterial newInstance( String name ) {
            BioMaterial bm = new BioMaterial();
            bm.setName( name );
            return bm;
        }

        public static BioMaterial newInstance( String name, Taxon taxon ) {
            BioMaterial bm = new BioMaterial();
            bm.setName( name );
            bm.setSourceTaxon( taxon );
            return bm;
        }
    }

}