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

import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResult;

/**
 * Evidence documented by a differential expression result stored in the system
 */
public abstract class DifferentialExpressionEvidence extends DataAnalysisEvidence {

    /**
     * Constructs new instances of {@link DifferentialExpressionEvidence}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link DifferentialExpressionEvidence}.
         */
        public static DifferentialExpressionEvidence newInstance() {
            return new DifferentialExpressionEvidenceImpl();
        }

    }

    private Double selectionThreshold;

    private GeneDifferentialExpressionMetaAnalysisResult geneDifferentialExpressionMetaAnalysisResult;

    /**
     * 
     */
    public GeneDifferentialExpressionMetaAnalysisResult getGeneDifferentialExpressionMetaAnalysisResult() {
        return this.geneDifferentialExpressionMetaAnalysisResult;
    }

    /**
     * The q-value threshold that was used to select genes from the meta-analysis.
     */
    public Double getSelectionThreshold() {
        return this.selectionThreshold;
    }

    public void setGeneDifferentialExpressionMetaAnalysisResult(
            GeneDifferentialExpressionMetaAnalysisResult geneDifferentialExpressionMetaAnalysisResult ) {
        this.geneDifferentialExpressionMetaAnalysisResult = geneDifferentialExpressionMetaAnalysisResult;
    }

    public void setSelectionThreshold( Double selectionThreshold ) {
        this.selectionThreshold = selectionThreshold;
    }

}