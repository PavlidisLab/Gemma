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

package ubic.gemma.model.analysis;

import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import javax.persistence.Transient;

/**
 * An analysis of a single experiment or subset.
 * @param <T> the type of experiment analyzed, usually {@link ubic.gemma.model.expression.experiment.ExpressionExperiment},
 *            but you can use {@link BioAssaySet} to also allow {@link ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet}.
 */
public abstract class SingleExperimentAnalysis<T extends BioAssaySet> extends ExpressionAnalysis implements SecuredChild<ExpressionExperiment> {

    private T experimentAnalyzed;
    private Integer numberOfElementsAnalyzed;

    public T getExperimentAnalyzed() {
        return this.experimentAnalyzed;
    }

    public void setExperimentAnalyzed( T experimentAnalyzed ) {
        this.experimentAnalyzed = experimentAnalyzed;
    }

    /**
     * @return The number of probes or genes (or other elements) used in the analysis. The exact meaning is determined by the
     * subclass + implementation.
     */
    public Integer getNumberOfElementsAnalyzed() {
        return this.numberOfElementsAnalyzed;
    }

    public void setNumberOfElementsAnalyzed( Integer numberOfElementsAnalyzed ) {
        this.numberOfElementsAnalyzed = numberOfElementsAnalyzed;
    }

    /**
     * Experiment analysis are always owned by the experiment (or source experiment, if a subset).
     */
    @Transient
    @Override
    public ExpressionExperiment getSecurityOwner() {
        if ( experimentAnalyzed instanceof ExpressionExperiment ) {
            return ( ExpressionExperiment ) experimentAnalyzed;
        } else {
            return ( ( ExpressionExperimentSubSet ) experimentAnalyzed ).getSourceExperiment();
        }
    }
}