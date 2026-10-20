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
package ubic.gemma.model.expression.experiment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.springframework.lang.Nullable;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.description.Characteristic;

import java.util.Set;

/**
 * Hibernate Search 7 mapping: indexed root and embedded contributor to
 * {@link ExpressionExperiment#getExperimentalDesign()}. The deep
 * {@code experimentalFactors.factorValues.characteristics.{value,valueUri}} path on EE
 * runs through this entity.
 */
@Entity
@Table(name = "EXPERIMENTAL_DESIGN")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Indexed
public class ExperimentalDesign extends AbstractDescribable implements SecuredChild<ExpressionExperiment> {

    @Column(name = "REPLICATE_DESCRIPTION", columnDefinition = "VARCHAR(255)")
    private String replicateDescription;
    @Column(name = "QUALITY_CONTROL_DESCRIPTION", columnDefinition = "VARCHAR(255)")
    private String qualityControlDescription;
    @Column(name = "NORMALIZATION_DESCRIPTION", columnDefinition = "VARCHAR(255)")
    private String normalizationDescription;
    @OneToMany(mappedBy = "experimentalDesign", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<ExperimentalFactor> experimentalFactors = new java.util.HashSet<>();
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "EXPERIMENTAL_DESIGN_FK", columnDefinition = "BIGINT", foreignKey = @ForeignKey(name = "CHARACTERISTIC_EXPERIMENTAL_DESIGN_FKC"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Characteristic> types = new java.util.HashSet<>();

    @Nullable
    @Transient
    private ExpressionExperiment securityOwner;

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    @FullTextField
    public String getName() {
        return super.getName();
    }

    @Override
    @FullTextField(projectable = Projectable.YES)
    public String getDescription() {
        return super.getDescription();
    }

    /**
     * @return The description of the factors (TimeCourse, Dosage, etc.) that group the BioAssays.
     */
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public Set<ExperimentalFactor> getExperimentalFactors() {
        return this.experimentalFactors;
    }

    public void setExperimentalFactors( Set<ExperimentalFactor> experimentalFactors ) {
        this.experimentalFactors = experimentalFactors;
    }

    public String getNormalizationDescription() {
        return this.normalizationDescription;
    }

    public void setNormalizationDescription( String normalizationDescription ) {
        this.normalizationDescription = normalizationDescription;
    }

    public String getQualityControlDescription() {
        return this.qualityControlDescription;
    }

    public void setQualityControlDescription( String qualityControlDescription ) {
        this.qualityControlDescription = qualityControlDescription;
    }

    public String getReplicateDescription() {
        return this.replicateDescription;
    }

    public void setReplicateDescription( String replicateDescription ) {
        this.replicateDescription = replicateDescription;
    }

    @Nullable
    @Override
    public ExpressionExperiment getSecurityOwner() {
        return securityOwner;
    }

    public void setSecurityOwner( @Nullable ExpressionExperiment securityOwner ) {
        this.securityOwner = securityOwner;
    }

    public Set<Characteristic> getTypes() {
        return this.types;
    }

    public void setTypes( Set<Characteristic> types ) {
        this.types = types;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof ExperimentalDesign ) )
            return false;
        ExperimentalDesign that = ( ExperimentalDesign ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return false;
        }
    }

    public static final class Factory {

        public static ExperimentalDesign newInstance() {
            return new ExperimentalDesign();
        }

    }

}