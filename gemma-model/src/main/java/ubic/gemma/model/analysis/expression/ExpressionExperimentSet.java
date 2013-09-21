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

package ubic.gemma.model.analysis.expression;

import java.util.Collection;

/**
 * A grouping of expression studies.
 */
public abstract class ExpressionExperimentSet extends ubic.gemma.model.common.Auditable implements
        gemma.gsec.model.Securable {

    /**
     * Constructs new instances of {@link ExpressionExperimentSet}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ExpressionExperimentSet}.
         */
        public static ExpressionExperimentSet newInstance() {
            return new ExpressionExperimentSetImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1034074709420077917L;
    private ubic.gemma.model.genome.Taxon taxon;

    private Collection<ubic.gemma.model.expression.experiment.BioAssaySet> experiments = new java.util.HashSet<ubic.gemma.model.expression.experiment.BioAssaySet>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ExpressionExperimentSet() {
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.expression.experiment.BioAssaySet> getExperiments() {
        return this.experiments;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Taxon getTaxon() {
        return this.taxon;
    }

    public void setExperiments( Collection<ubic.gemma.model.expression.experiment.BioAssaySet> experiments ) {
        this.experiments = experiments;
    }

    public void setTaxon( ubic.gemma.model.genome.Taxon taxon ) {
        this.taxon = taxon;
    }

}