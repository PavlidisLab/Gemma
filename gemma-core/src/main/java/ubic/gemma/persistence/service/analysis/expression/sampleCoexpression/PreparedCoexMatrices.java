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

import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionMatrix;

import javax.annotation.Nullable;

/**
 * Helper class for sample coexpression analysis.
 */
public class PreparedCoexMatrices {

    public PreparedCoexMatrices( @Nullable SampleCoexpressionMatrix matrix, @Nullable SampleCoexpressionMatrix regressedMatrix ) {
        this.matrix = matrix;
        this.regressedMatrix = regressedMatrix;
    }

    @Nullable
    SampleCoexpressionMatrix matrix;
    @Nullable
    SampleCoexpressionMatrix regressedMatrix;
}
