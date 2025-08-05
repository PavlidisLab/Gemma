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

import org.hibernate.search.annotations.*;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * ExperimentFactors are the dependent variables of an experiment (e.g., genotype, time, glucose concentration).
 *
 * @author Paul
 */
@Indexed
public class ExperimentalFactor extends AbstractDescribable implements SecuredChild {

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
    @Field
    public String getName() {
        return super.getName();
    }

    @Override
    @Field(store = Store.YES)
    public String getDescription() {
        return super.getDescription();
    }

    @Transient
    @Override
    public Securable getSecurityOwner() {
        return this.securityOwner;
    }

    public void setSecurityOwner( ExpressionExperiment securityOwner ) {
        this.securityOwner = securityOwner;
    }

    /**
     * @return Categorical vs. continuous. Continuous factors must have a 'measurement' associated with the
     *         factorvalues,
     *         Categorical ones must not.
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
                && Objects.equals( getName(), other.getName() )
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
    }
}