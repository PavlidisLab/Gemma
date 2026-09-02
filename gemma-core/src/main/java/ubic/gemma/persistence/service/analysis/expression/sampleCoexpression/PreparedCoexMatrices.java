/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.analysis.expression.sampleCoexpression;

import ubic.gemma.core.security.audit.payload.SampleCorrelationAnalysisPayload;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix;

import org.springframework.lang.Nullable;

/**
 * Helper class for sample coexpression analysis.
 */
public class PreparedCoexMatrices {

    public PreparedCoexMatrices( @Nullable SampleCoexpressionMatrix matrix, @Nullable SampleCoexpressionMatrix regressedMatrix ) {
        this( matrix, regressedMatrix, null );
    }

    public PreparedCoexMatrices( @Nullable SampleCoexpressionMatrix matrix, @Nullable SampleCoexpressionMatrix regressedMatrix,
            @Nullable SampleCorrelationAnalysisPayload filterAttrition ) {
        this.matrix = matrix;
        this.regressedMatrix = regressedMatrix;
        this.filterAttrition = filterAttrition;
    }

    @Nullable
    SampleCoexpressionMatrix matrix;
    @Nullable
    SampleCoexpressionMatrix regressedMatrix;

    /**
     * Per-stage row and column attrition from the filter that produced {@link #matrix}, carried here so
     * {@link SampleCoexpressionAnalysisServiceImpl#compute} can record it on the audit event. Null when the
     * unregressed matrix could not be built, since there is then no filter run to describe.
     * <p>
     * 🛑 Do NOT serialise this object itself onto the audit row -- it holds the correlation matrices. Only
     * {@link #getFilterAttrition()} belongs in an audit payload.
     */
    @Nullable
    SampleCorrelationAnalysisPayload filterAttrition;

    @Nullable
    public SampleCorrelationAnalysisPayload getFilterAttrition() {
        return filterAttrition;
    }
}
