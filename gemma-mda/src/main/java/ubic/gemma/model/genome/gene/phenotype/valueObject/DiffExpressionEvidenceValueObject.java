/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.model.genome.gene.phenotype.valueObject;

import java.util.SortedSet;

import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResult;
import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;

/**
 * TODO Document Me
 * 
 * @author stgeorgn
 * @version $Id$
 */
public class DiffExpressionEvidenceValueObject extends EvidenceValueObject {

    private Double selectionThreshold;

    private GeneDifferentialExpressionMetaAnalysisResult geneDifferentialExpressionMetaAnalysisResult = null;

    private Long geneDifferentialExpressionMetaAnalysisId = null;

    public DiffExpressionEvidenceValueObject() {
        super();
    }

    public DiffExpressionEvidenceValueObject( DifferentialExpressionEvidence differentialExpressionEvidence,
            Double selectionThreshold, Long geneDifferentialExpressionMetaAnalysisId ) {
        super( differentialExpressionEvidence );
        this.selectionThreshold = selectionThreshold;
        this.geneDifferentialExpressionMetaAnalysisResult = differentialExpressionEvidence
                .getGeneDifferentialExpressionMetaAnalysisResult();
        this.geneDifferentialExpressionMetaAnalysisId = geneDifferentialExpressionMetaAnalysisId;
    }

    public DiffExpressionEvidenceValueObject( Integer geneNCBI, SortedSet<CharacteristicValueObject> phenotypes,
            String description, String evidenceCode, boolean isNegativeEvidence,
            EvidenceSourceValueObject evidenceSource,
            GeneDifferentialExpressionMetaAnalysisResult geneDifferentialExpressionMetaAnalysisResult,
            Double selectionThreshold ) {
        super( geneNCBI, phenotypes, description, evidenceCode, isNegativeEvidence, evidenceSource );

        this.selectionThreshold = selectionThreshold;

        this.geneDifferentialExpressionMetaAnalysisResult = geneDifferentialExpressionMetaAnalysisResult;
    }

    public GeneDifferentialExpressionMetaAnalysisResult getGeneDifferentialExpressionMetaAnalysisResult() {
        return this.geneDifferentialExpressionMetaAnalysisResult;
    }

    public void setGeneDifferentialExpressionMetaAnalysisResult(
            GeneDifferentialExpressionMetaAnalysisResult geneDifferentialExpressionMetaAnalysisResult ) {
        this.geneDifferentialExpressionMetaAnalysisResult = geneDifferentialExpressionMetaAnalysisResult;
    }

    public Double getSelectionThreshold() {
        return this.selectionThreshold;
    }

    public void setSelectionThreshold( Double selectionThreshold ) {
        this.selectionThreshold = selectionThreshold;
    }

    public Long getGeneDifferentialExpressionMetaAnalysisId() {
        return this.geneDifferentialExpressionMetaAnalysisId;
    }

    public void setGeneDifferentialExpressionMetaAnalysisId( Long geneDifferentialExpressionMetaAnalysisId ) {
        this.geneDifferentialExpressionMetaAnalysisId = geneDifferentialExpressionMetaAnalysisId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime
                * result
                + ( ( this.geneDifferentialExpressionMetaAnalysisId == null ) ? 0
                        : this.geneDifferentialExpressionMetaAnalysisId.hashCode() );
        result = prime
                * result
                + ( ( this.geneDifferentialExpressionMetaAnalysisResult == null ) ? 0
                        : this.geneDifferentialExpressionMetaAnalysisResult.hashCode() );
        result = prime * result + ( ( this.selectionThreshold == null ) ? 0 : this.selectionThreshold.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        return false;
    }

}
