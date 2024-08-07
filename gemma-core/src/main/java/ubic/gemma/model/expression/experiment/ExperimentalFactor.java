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

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import org.hibernate.search.annotations.*;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * ExperimentFactors are the dependent variables of an experiment (e.g., genotype, time, glucose concentration).
 *
 * @author Paul
 */
@Indexed
public class ExperimentalFactor extends AbstractDescribable implements SecuredChild, Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 4615731059510436891L;

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

    @Transient
    @Override
    public Securable getSecurityOwner() {
        return this.securityOwner;
    }

    /**
     * @param securityOwner Used to hint the security system about who 'owns' this,
     */
    public void setSecurityOwner( ExpressionExperiment securityOwner ) {
        this.securityOwner = securityOwner;
    }

    @Override
    public int hashCode() {
        return Objects.hash( getType(), getName() );
    }

    @Override
    public boolean equals( Object obj ) {
        if ( obj == null )
            return false;
        if ( this == obj )
            return true;

        if ( !( obj instanceof ExperimentalFactor ) )
            return false;

        ExperimentalFactor other = ( ExperimentalFactor ) obj;

        if ( this.getId() == null ) {
            if ( other.getId() != null )
                return false;
        } else if ( !this.getId().equals( other.getId() ) )
            return false;

        if ( this.getCategory() == null ) {
            if ( other.getCategory() != null )
                return false;
        } else if ( !this.getCategory().equals( other.getCategory() ) )
            return false;

        if ( this.getName() == null ) {
            if ( other.getName() != null )
                return false;
        } else if ( !this.getName().equals( other.getName() ) )
            return false;

        if ( this.getDescription() == null ) {
            return other.getDescription() == null;
        }
        return this.getDescription().equals( other.getDescription() );
    }

    @Override
    public String toString() {
        return super.toString()
                + ( type != null ? " Type=" + type.toString().toUpperCase() : "" )
                + ( category != null ? " Category=" + category : "" );
    }

    public static final class Factory {

        public static ExperimentalFactor newInstance() {
            return new ExperimentalFactor();
        }

        public static ExperimentalFactor newInstance( FactorType type, String name ) {
            ExperimentalFactor ef = new ExperimentalFactor();
            ef.setType( type );
            ef.setName( name );
            return ef;
        }
    }
}