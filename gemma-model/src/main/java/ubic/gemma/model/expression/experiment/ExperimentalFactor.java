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

import java.util.Collection;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.description.Characteristic;

/**
 * ExperimentFactors are the dependent variables of an experiment (e.g., genotype, time, glucose concentration).
 */
public abstract class ExperimentalFactor extends Auditable implements SecuredChild {

    /**
     * Constructs new instances of {@link ubic.gemma.model.expression.experiment.ExperimentalFactor}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.experiment.ExperimentalFactor}.
         */
        public static ubic.gemma.model.expression.experiment.ExperimentalFactor newInstance() {
            return new ubic.gemma.model.expression.experiment.ExperimentalFactorImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 4615731059510436891L;

    private Collection<ubic.gemma.model.common.description.Characteristic> annotations = new java.util.HashSet<ubic.gemma.model.common.description.Characteristic>();

    private ubic.gemma.model.common.description.Characteristic category;
    private ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign;

    private Collection<FactorValue> factorValues = new java.util.HashSet<ubic.gemma.model.expression.experiment.FactorValue>();

    private ExpressionExperiment securityOwner;

    private ubic.gemma.model.expression.experiment.FactorType type;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ExperimentalFactor() {
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.common.description.Characteristic> getAnnotations() {
        return this.annotations;
    }

    /**
     * 
     */
    public Characteristic getCategory() {
        return this.category;
    }

    /**
     * 
     */
    public ExperimentalDesign getExperimentalDesign() {
        return this.experimentalDesign;
    }

    /**
     * The pairing of BioAssay FactorValues with the ExperimentDesign ExperimentFactor.
     */
    public Collection<ubic.gemma.model.expression.experiment.FactorValue> getFactorValues() {
        return this.factorValues;
    }

    @Override
    public Securable getSecurityOwner() {
        return this.securityOwner;
    }

    /**
     * Categorical vs. continuous. Continuous factors must have a 'measurement' associated with the factorvalues,
     * Categorical ones must not.
     */
    public FactorType getType() {
        return this.type;
    }

    public void setAnnotations( Collection<Characteristic> annotations ) {
        this.annotations = annotations;
    }

    public void setCategory( Characteristic category ) {
        this.category = category;
    }

    public void setExperimentalDesign( ExperimentalDesign experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    public void setFactorValues( Collection<FactorValue> factorValues ) {
        this.factorValues = factorValues;
    }

    /**
     * Used to hint the security system about who 'owns' this,
     * 
     * @param factorValues
     */
    public void setSecurityOwner( ExpressionExperiment securityOwner ) {
        this.securityOwner = securityOwner;
    }

    public void setType( ubic.gemma.model.expression.experiment.FactorType type ) {
        this.type = type;
    }

}