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

import java.util.Collection;

import ubic.gemma.model.common.description.Characteristic;

/**
 * 
 */
public abstract class ExperimentalDesign extends ubic.gemma.model.common.Auditable implements
        gemma.gsec.model.SecuredChild {

    /**
     * 
     */
    private static final long serialVersionUID = 1734101852541885497L;

    /**
     * Constructs new instances of {@link ubic.gemma.model.expression.experiment.ExperimentalDesign}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.experiment.ExperimentalDesign}.
         */
        public static ExperimentalDesign newInstance() {
            return new ExperimentalDesignImpl();
        }

    }

    private String replicateDescription;

    private String qualityControlDescription;

    private String normalizationDescription;

    private Collection<ExperimentalFactor> experimentalFactors = new java.util.HashSet<>();

    private Collection<Characteristic> types = new java.util.HashSet<>();

    /**
     * The description of the factors (TimeCourse, Dosage, etc.) that group the BioAssays.
     */
    public Collection<ExperimentalFactor> getExperimentalFactors() {
        return this.experimentalFactors;
    }

    /**
     * 
     */
    public String getNormalizationDescription() {
        return this.normalizationDescription;
    }

    /**
     * 
     */
    public String getQualityControlDescription() {
        return this.qualityControlDescription;
    }

    /**
     * 
     */
    public String getReplicateDescription() {
        return this.replicateDescription;
    }

    @Override
    public Securable getSecurityOwner() {
        return null;
    }

    /**
     * 
     */
    public Collection<Characteristic> getTypes() {
        return this.types;
    }

    public void setExperimentalFactors( Collection<ExperimentalFactor> experimentalFactors ) {
        this.experimentalFactors = experimentalFactors;
    }

    public void setNormalizationDescription( String normalizationDescription ) {
        this.normalizationDescription = normalizationDescription;
    }

    public void setQualityControlDescription( String qualityControlDescription ) {
        this.qualityControlDescription = qualityControlDescription;
    }

    public void setReplicateDescription( String replicateDescription ) {
        this.replicateDescription = replicateDescription;
    }

    public void setTypes( Collection<Characteristic> types ) {
        this.types = types;
    }

}