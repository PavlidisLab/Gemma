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

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import ubic.gemma.model.analysis.AnalysisResult;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

import java.util.HashSet;
import java.util.Set;

@Entity
public abstract class FactorAssociatedAnalysisResultSet<R extends AnalysisResult> extends AnalysisResultSet<R> {

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "EXPERIMENTAL_FACTORS2FACTOR_ASSOCIATED_ANALYSIS_RESULT_SETS",
            joinColumns = @JoinColumn(name = "FACTOR_ASSOCIATED_ANALYSIS_RESULT_SETS_FK", columnDefinition = "BIGINT"),
            inverseJoinColumns = @JoinColumn(name = "EXPERIMENTAL_FACTORS_FK", columnDefinition = "BIGINT"),
            foreignKey = @ForeignKey(name = "EXPERIMENTAL_FACTOR_FACTOR_ASSOCIATED_ANALYSIS_RESULT_SETS_C"))
    private Set<ExperimentalFactor> experimentalFactors = new HashSet<>();

    public Set<ExperimentalFactor> getExperimentalFactors() {
        return this.experimentalFactors;
    }

    public void setExperimentalFactors( Set<ExperimentalFactor> experimentalFactors ) {
        this.experimentalFactors = experimentalFactors;
    }
}
