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
package ubic.gemma.model.association.phenotype;

/**
 * Evidence based on an experiment. This could be provided via a publication, or information known only to the provider.
 */
public abstract class ExperimentalEvidence extends ubic.gemma.model.association.phenotype.PhenotypeAssociation {

    /**
     * Constructs new instances of {@link ubic.gemma.model.association.phenotype.ExperimentalEvidence}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.association.phenotype.ExperimentalEvidence}.
         */
        public static ubic.gemma.model.association.phenotype.ExperimentalEvidence newInstance() {
            return new ubic.gemma.model.association.phenotype.ExperimentalEvidenceImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1310307976449116756L;
    private ubic.gemma.model.analysis.Investigation experiment;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ExperimentalEvidence() {
    }

    /**
     * 
     */
    public ubic.gemma.model.analysis.Investigation getExperiment() {
        return this.experiment;
    }

    public void setExperiment( ubic.gemma.model.analysis.Investigation experiment ) {
        this.experiment = experiment;
    }

}