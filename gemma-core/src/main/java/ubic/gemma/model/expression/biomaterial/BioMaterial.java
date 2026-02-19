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

import org.hibernate.search.annotations.*;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.DescribableUtils;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static ubic.gemma.persistence.service.expression.biomaterial.BioMaterialUtils.visitBioMaterials;

/**
 * In MAGE, BioMaterial is an abstract class that represents the important substances such as cells, tissues, DNA,
 * proteins, etc... In MAGE, Biomaterial subclasses such as BioSample and BioSource can be related to other biomaterial
 * through a directed acyclic graph (represented by treatment(s)). In our implementation, we don't care so much about
 * the experimental procedures and we just lump all of the BioMaterial into one class.
 * <p>
 * BioMaterial can be organized in a hierarchy via {@link #getSourceBioMaterial()}. When that is the case,
 * sub-biomaterials inherit characteristics, factors and treatments from their source biomaterials.
 */
@Indexed
public class BioMaterial extends AbstractDescribable implements SecuredChild<ExpressionExperiment> {

    public static final int MAX_NAME_LENGTH = 255;

    public static Comparator<BioMaterial> COMPARATOR = Comparator
            .comparing( BioMaterial::getName, DescribableUtils.NAME_COMPARATOR )
            .thenComparing( BioMaterial::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    @Nullable
    private BioMaterial sourceBioMaterial;
    private Taxon sourceTaxon;
    private Set<FactorValue> factorValues = new HashSet<>();
    private Set<BioAssay> bioAssaysUsedIn = new HashSet<>();
    private Set<Treatment> treatments = new HashSet<>();
    private Set<Characteristic> characteristics = new HashSet<>();
    @Nullable
    private DatabaseEntry externalAccession;

    @Nullable
    private ExpressionExperiment securityOwner;

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

    /**
     * Parent biomaterial or null if this is a top-level biomaterial.
     * <p>
     * This is used to represent a sample derived from another sample. For example, you could have a bulk tissue sample
     * that has been sorted per cell type. Each cell type would constitute a biomaterial with the bulk tissue as parent.
     */
    @Nullable
    public BioMaterial getSourceBioMaterial() {
        return this.sourceBioMaterial;
    }

    public void setSourceBioMaterial( @Nullable BioMaterial sourceBioMaterial ) {
        this.sourceBioMaterial = sourceBioMaterial;
    }

    @ContainedIn
    public Set<BioAssay> getBioAssaysUsedIn() {
        return this.bioAssaysUsedIn;
    }

    public void setBioAssaysUsedIn( Set<BioAssay> bioAssaysUsedIn ) {
        this.bioAssaysUsedIn = bioAssaysUsedIn;
    }

    /**
     * Obtain all the assays used in the hierarchy of biomaterials via {@link #getSourceBioMaterial()}.
     *
     * @see BioMaterial#getBioAssaysUsedIn()
     */
    public Set<BioAssay> getAllBioAssaysUsedIn() {
        Set<BioAssay> assays = new HashSet<>();
        visitBioMaterials( this, bm -> {
            assays.addAll( bm.getBioAssaysUsedIn() );
        } );
        return unmodifiableSet( assays );
    }

    @IndexedEmbedded
    public Set<Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    public void setCharacteristics( Set<Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }

    /**
     * Obtain all the {@link Characteristic} associated to this biomaterial, including those inherited from its ancestors
     * via {@link #getSourceBioMaterial()}.
     *
     * @see #getCharacteristics()
     */
    @Transient
    public Set<Characteristic> getAllCharacteristics() {
        Set<Characteristic> result = new HashSet<>( this.characteristics );
        visitBioMaterials( this, bm -> result.addAll( bm.getCharacteristics() ) );
        return unmodifiableSet( result );
    }

    /**
     * @return An optional external reference for this BioMaterial. In many cases this is the same as the accession for
     * the
     * related BioAssay. We store the information here to help make the data easier to trace. Note that more
     * than one
     * BioMaterial may reference a given external accession.
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
     * Obtain the values that this BioAssay is associated with for the experiment.
     */
    public Set<FactorValue> getFactorValues() {
        return this.factorValues;
    }

    public void setFactorValues( Set<FactorValue> factorValues ) {
        this.factorValues = factorValues;
    }

    /**
     * Obtain all the {@link FactorValue} associated to this biomaterial, including those inherited from its ancestors
     * via {@link #getSourceBioMaterial()}.
     */
    @Transient
    public Set<FactorValue> getAllFactorValues() {
        Set<FactorValue> result = new HashSet<>( this.factorValues );
        visitBioMaterials( this, bm -> result.addAll( bm.getFactorValues() ) );
        return unmodifiableSet( result );
    }

    @Transient
    @Nullable
    @Override
    public ExpressionExperiment getSecurityOwner() {
        return this.securityOwner;
    }

    public void setSecurityOwner( @Nullable ExpressionExperiment securityOwner ) {
        this.securityOwner = securityOwner;
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

    /**
     * Obtain all treatments, including those inherited from its ancestors via {@link #getSourceBioMaterial()}.
     */
    @Transient
    public Set<Treatment> getAllTreatments() {
        Set<Treatment> result = new HashSet<>();
        visitBioMaterials( this, bm -> result.addAll( bm.getTreatments() ) );
        return unmodifiableSet( result );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof BioMaterial ) )
            return false;
        final BioMaterial that = ( BioMaterial ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        }
        return DescribableUtils.equalsByName( this, that );
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