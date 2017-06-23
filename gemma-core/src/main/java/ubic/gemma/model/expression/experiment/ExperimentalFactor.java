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

import gemma.gsec.model.Securable;
import gemma.gsec.model.SecuredChild;
import ubic.gemma.model.common.AbstractAuditable;
import ubic.gemma.model.common.description.Characteristic;

import java.util.Collection;

/**
 * ExperimentFactors are the dependent variables of an experiment (e.g., genotype, time, glucose concentration).
 *
 * @author Paul
 */
public class ExperimentalFactor extends AbstractAuditable implements SecuredChild {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 4615731059510436891L;
    private Collection<ubic.gemma.model.common.description.Characteristic> annotations = new java.util.HashSet<>();
    private ubic.gemma.model.common.description.Characteristic category;
    private ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign;
    private Collection<FactorValue> factorValues = new java.util.HashSet<>();
    private ExpressionExperiment securityOwner;
    private ubic.gemma.model.expression.experiment.FactorType type;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public ExperimentalFactor() {
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

        if ( getId() == null ) {
            if ( other.getId() != null )
                return false;
        } else if ( !getId().equals( other.getId() ) )
            return false;

        if ( this.getCategory() == null ) {
            if ( other.getCategory() != null )
                return false;
        } else if ( !this.getCategory().equals( other.getCategory() ) )
            return false;

        if ( getName() == null ) {
            if ( other.getName() != null )
                return false;
        } else if ( !getName().equals( other.getName() ) )
            return false;

        if ( this.getDescription() == null ) {
            if ( other.getDescription() != null )
                return false;
        } else if ( !getDescription().equals( other.getDescription() ) )
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null ) {
            return super.hashCode();
        }

        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( this.getName() == null ) ? 0 : this.getName().hashCode() );
        result = prime * result + ( ( this.getDescription() == null ) ? 0 : this.getDescription().hashCode() );
        return result;
    }



    @Override
    public Securable getSecurityOwner() {
        return this.securityOwner;
    }

    /**
     * Used to hint the security system about who 'owns' this,
     */
    public void setSecurityOwner( ExpressionExperiment securityOwner ) {
        this.securityOwner = securityOwner;
    }

    public Collection<ubic.gemma.model.common.description.Characteristic> getAnnotations() {
        return this.annotations;
    }

    public void setAnnotations( Collection<Characteristic> annotations ) {
        this.annotations = annotations;
    }

    public Characteristic getCategory() {
        return this.category;
    }

    public void setCategory( Characteristic category ) {
        this.category = category;
    }

    public ExperimentalDesign getExperimentalDesign() {
        return this.experimentalDesign;
    }

    public void setExperimentalDesign( ExperimentalDesign experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    /**
     * The pairing of BioAssay FactorValues with the ExperimentDesign ExperimentFactor.
     */
    public Collection<ubic.gemma.model.expression.experiment.FactorValue> getFactorValues() {
        return this.factorValues;
    }

    public void setFactorValues( Collection<FactorValue> factorValues ) {
        this.factorValues = factorValues;
    }

    /**
     * Categorical vs. continuous. Continuous factors must have a 'measurement' associated with the factorvalues,
     * Categorical ones must not.
     */
    public FactorType getType() {
        return this.type;
    }

    public void setType( ubic.gemma.model.expression.experiment.FactorType type ) {
        this.type = type;
    }



    /**
     * Constructs new instances of {@link ubic.gemma.model.expression.experiment.ExperimentalFactor}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.experiment.ExperimentalFactor}.
         */
        public static ubic.gemma.model.expression.experiment.ExperimentalFactor newInstance() {
            return new ubic.gemma.model.expression.experiment.ExperimentalFactor();
        }

    }

}