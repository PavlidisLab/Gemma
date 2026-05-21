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

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.DescribableUtils;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;

import org.springframework.lang.Nullable;
import jakarta.persistence.Transient;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * ExperimentFactors are the dependent variables of an experiment (e.g., genotype, time, glucose concentration).
 * <p>
 * Hibernate Search 7 mapping: indexed root and embedded contributor via
 * {@code ExperimentalDesign.experimentalFactors}; pulls its {@link #getCategory()} characteristic
 * and the deep {@link #getFactorValues()} chain into the EE document.
 *
 * @author Paul
 */
@Indexed
public class ExperimentalFactor extends AbstractDescribable implements SecuredChild<ExpressionExperiment> {

    public static Comparator<ExperimentalFactor> COMPARATOR = Comparator.comparing( ExperimentalFactor::getName )
            .thenComparing( ExperimentalFactor::getCategory, Comparator.nullsLast( Comparator.naturalOrder() ) )
            .thenComparing( ExperimentalFactor::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    private FactorType type;
    @Nullable
    private Characteristic category;
    private ExperimentalDesign experimentalDesign;
    private Set<FactorValue> factorValues = new HashSet<>();
    @Deprecated
    private Set<Characteristic> annotations = new HashSet<>();
    private ExpressionExperiment securityOwner;

    /**
     * Curator/agent hint about whether this factor warrants picking a baseline factor value. Mirrors the
     * curation-ui {@code Factor.baseline_relevance} field. Allowed values: {@code "required"},
     * {@code "not_applicable"}, {@code "uncertain"}. {@code null} when not set (legacy data,
     * factors not yet visited by the proposer pipeline).
     */
    @Nullable
    private String baselineRelevance;

    /**
     * Free-text rationale for the baselineRelevance value. {@code null} when not set.
     */
    @Nullable
    private String baselineRelevanceReason;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public ExperimentalFactor() {
    }

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

    @Transient
    @Override
    public ExpressionExperiment getSecurityOwner() {
        return this.securityOwner;
    }

    public void setSecurityOwner( ExpressionExperiment securityOwner ) {
        this.securityOwner = securityOwner;
    }

    /**
     * @return Categorical vs. continuous. Continuous factors must have a 'measurement' associated with the
     * factorvalues,
     * Categorical ones must not.
     */
    public FactorType getType() {
        return this.type;
    }

    public void setType( FactorType type ) {
        this.type = type;
    }

    /**
     * Obtain the category of this experimental factor.
     *
     * @return the category or null if annotated automatically from GEO or used as a dummy.
     */
    @Nullable
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public Characteristic getCategory() {
        return this.category;
    }

    public void setCategory( @Nullable Characteristic category ) {
        this.category = category;
    }

    public ExperimentalDesign getExperimentalDesign() {
        return this.experimentalDesign;
    }

    public void setExperimentalDesign( ExperimentalDesign experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    /**
     * @return The pairing of BioAssay FactorValues with the ExperimentDesign ExperimentFactor.
     */
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @IndexedEmbedded
    public Set<FactorValue> getFactorValues() {
        return this.factorValues;
    }

    public void setFactorValues( Set<FactorValue> factorValues ) {
        this.factorValues = factorValues;
    }

    @Deprecated
    public Set<Characteristic> getAnnotations() {
        return this.annotations;
    }

    @Deprecated
    public void setAnnotations( Set<Characteristic> annotations ) {
        this.annotations = annotations;
    }

    @Nullable
    public String getBaselineRelevance() {
        return baselineRelevance;
    }

    public void setBaselineRelevance( @Nullable String baselineRelevance ) {
        this.baselineRelevance = baselineRelevance;
    }

    @Nullable
    public String getBaselineRelevanceReason() {
        return baselineRelevanceReason;
    }

    public void setBaselineRelevanceReason( @Nullable String baselineRelevanceReason ) {
        this.baselineRelevanceReason = baselineRelevanceReason;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( !( obj instanceof ExperimentalFactor ) )
            return false;
        ExperimentalFactor other = ( ExperimentalFactor ) obj;
        if ( getId() != null && other.getId() != null )
            return getId().equals( other.getId() );
        return Objects.equals( getCategory(), other.getCategory() )
                && DescribableUtils.equalsByName( this, other )
                && Objects.equals( getDescription(), other.getDescription() );
    }

    @Override
    public String toString() {
        return super.toString() + ( type != null ? " Type=" + type : "" );
    }

    public static final class Factory {

        public static ExperimentalFactor newInstance() {
            return new ExperimentalFactor();
        }

        public static ExperimentalFactor newInstance( String name, FactorType factorType ) {
            ExperimentalFactor experimentalFactor = newInstance();
            experimentalFactor.setName( name );
            experimentalFactor.setType( factorType );
            return experimentalFactor;
        }

        public static ExperimentalFactor newInstance( String name, FactorType factorType, Category category ) {
            ExperimentalFactor experimentalFactor = newInstance( name, factorType );
            experimentalFactor.setCategory( Characteristic.Factory.newInstance( category ) );
            return experimentalFactor;
        }

        public static ExperimentalFactor newInstance( ExperimentalDesign ed, String name, FactorType factorType ) {
            ExperimentalFactor experimentalFactor = newInstance();
            experimentalFactor.setExperimentalDesign( ed );
            experimentalFactor.setName( name );
            experimentalFactor.setType( factorType );
            return experimentalFactor;
        }
    }
}