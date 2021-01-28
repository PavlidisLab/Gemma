/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.model.analysis.expression.diff;

import ubic.gemma.model.IdentifiableValueObject;

public class DifferentialExpressionAnalysisResultValueObject extends IdentifiableValueObject<DifferentialExpressionAnalysisResult> {

    private final Double pValue;
    private final Double correctedPvalue;
    private final Double rank;

    public DifferentialExpressionAnalysisResultValueObject( DifferentialExpressionAnalysisResult result ) {
        super( result.getId() );
        this.pValue = result.getPvalue();
        this.correctedPvalue = result.getCorrectedPvalue();
        this.rank = result.getRank();
    }

    public Double getPvalue() {
        return pValue;
    }

    public Double getCorrectedPvalue() {
        return correctedPvalue;
    }

    public Double getRank() {
        return rank;
    }
}
