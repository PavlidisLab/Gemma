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
 * Evidence documented by a differential expression result stored in the system
 */
public abstract class DifferentialExpressionEvidence extends DataAnalysisEvidence {

    /**
     * Constructs new instances of {@link ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence}.
         */
        public static ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence newInstance() {
            return new ubic.gemma.model.association.phenotype.DifferentialExpressionEvidenceImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 2281091076210841860L;
    private Double selectionThreshold;

    private ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResult geneDifferentialExpressionMetaAnalysisResult;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public DifferentialExpressionEvidence() {
    }

    /**
     * 
     */
    public ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResult getGeneDifferentialExpressionMetaAnalysisResult() {
        return this.geneDifferentialExpressionMetaAnalysisResult;
    }

    /**
     * The q-value threshold that was used to select genes from the meta-analysis.
     */
    public Double getSelectionThreshold() {
        return this.selectionThreshold;
    }

    public void setGeneDifferentialExpressionMetaAnalysisResult(
            ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResult geneDifferentialExpressionMetaAnalysisResult ) {
        this.geneDifferentialExpressionMetaAnalysisResult = geneDifferentialExpressionMetaAnalysisResult;
    }

    public void setSelectionThreshold( Double selectionThreshold ) {
        this.selectionThreshold = selectionThreshold;
    }

}