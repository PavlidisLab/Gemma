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

import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
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

    private Long geneDifferentialExpressionMetaAnalysisId = null;

    private Double metaPvalue;

    private Double metaQvalue;

    private Double meanLogFoldChange;

    private Double metaPvalueRank;

    private Long geneDifferentialExpressionMetaAnalysisResultId;

    public DiffExpressionEvidenceValueObject() {
        super();
    }

    public DiffExpressionEvidenceValueObject( DifferentialExpressionEvidence differentialExpressionEvidence,
            Long geneDifferentialExpressionMetaAnalysisId ) {
        super( differentialExpressionEvidence );

        this.geneDifferentialExpressionMetaAnalysisId = geneDifferentialExpressionMetaAnalysisId;
        this.metaPvalue = differentialExpressionEvidence.getGeneDifferentialExpressionMetaAnalysisResult()
                .getMetaPvalue();
        this.metaQvalue = differentialExpressionEvidence.getGeneDifferentialExpressionMetaAnalysisResult()
                .getMetaQvalue();
        this.meanLogFoldChange = differentialExpressionEvidence.getGeneDifferentialExpressionMetaAnalysisResult()
                .getMeanLogFoldChange();
        this.metaPvalueRank = differentialExpressionEvidence.getGeneDifferentialExpressionMetaAnalysisResult()
                .getMetaPvalueRank();
        this.geneDifferentialExpressionMetaAnalysisResultId = differentialExpressionEvidence
                .getGeneDifferentialExpressionMetaAnalysisResult().getId();
        this.selectionThreshold = differentialExpressionEvidence.getSelectionThreshold();
    }

    public DiffExpressionEvidenceValueObject(
            GeneDifferentialExpressionMetaAnalysis geneDifferentialExpressionMetaAnalysis,
            GeneDifferentialExpressionMetaAnalysisResult geneDifferentialExpressionMetaAnalysisResult,
            SortedSet<CharacteristicValueObject> phenotypes, String evidenceCode, Double selectionThreshold ) {

        super( geneDifferentialExpressionMetaAnalysisResult.getGene().getNcbiGeneId(), phenotypes,
                geneDifferentialExpressionMetaAnalysis.getDescription(), evidenceCode, false, null );

        this.selectionThreshold = selectionThreshold;
        this.geneDifferentialExpressionMetaAnalysisId = geneDifferentialExpressionMetaAnalysis.getId();
        this.metaPvalue = geneDifferentialExpressionMetaAnalysisResult.getMetaPvalue();
        this.metaQvalue = geneDifferentialExpressionMetaAnalysisResult.getMetaQvalue();
        this.meanLogFoldChange = geneDifferentialExpressionMetaAnalysisResult.getMeanLogFoldChange();
        this.metaPvalueRank = geneDifferentialExpressionMetaAnalysisResult.getMetaPvalueRank();
        this.geneDifferentialExpressionMetaAnalysisResultId = geneDifferentialExpressionMetaAnalysisResult.getId();
    }

    public DiffExpressionEvidenceValueObject( Integer geneNCBI, SortedSet<CharacteristicValueObject> phenotypes,
            String description, String evidenceCode, Long geneDifferentialExpressionMetaAnalysisId,
            Double selectionThreshold,
            GeneDifferentialExpressionMetaAnalysisResult geneDifferentialExpressionMetaAnalysisResult ) {
        super( geneNCBI, phenotypes, description, evidenceCode, false, null );

        this.selectionThreshold = selectionThreshold;
        this.geneDifferentialExpressionMetaAnalysisId = geneDifferentialExpressionMetaAnalysisId;

        this.metaPvalue = geneDifferentialExpressionMetaAnalysisResult.getMetaPvalue();
        this.metaQvalue = geneDifferentialExpressionMetaAnalysisResult.getMetaQvalue();
        this.meanLogFoldChange = geneDifferentialExpressionMetaAnalysisResult.getMeanLogFoldChange();
        this.metaPvalueRank = geneDifferentialExpressionMetaAnalysisResult.getMetaPvalueRank();
        this.geneDifferentialExpressionMetaAnalysisResultId = geneDifferentialExpressionMetaAnalysisResult.getId();
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

    public Double getMetaPvalue() {
        return this.metaPvalue;
    }

    public void setMetaPvalue( Double metaPvalue ) {
        this.metaPvalue = metaPvalue;
    }

    public Double getMetaQvalue() {
        return this.metaQvalue;
    }

    public void setMetaQvalue( Double metaQvalue ) {
        this.metaQvalue = metaQvalue;
    }

    public Double getMeanLogFoldChange() {
        return this.meanLogFoldChange;
    }

    public void setMeanLogFoldChange( Double meanLogFoldChange ) {
        this.meanLogFoldChange = meanLogFoldChange;
    }

    public Double getMetaPvalueRank() {
        return this.metaPvalueRank;
    }

    public void setMetaPvalueRank( Double metaPvalueRank ) {
        this.metaPvalueRank = metaPvalueRank;
    }

    public Long getGeneDifferentialExpressionMetaAnalysisResultId() {
        return this.geneDifferentialExpressionMetaAnalysisResultId;
    }

    public void setGeneDifferentialExpressionMetaAnalysisResultId( Long geneDifferentialExpressionMetaAnalysisResultId ) {
        this.geneDifferentialExpressionMetaAnalysisResultId = geneDifferentialExpressionMetaAnalysisResultId;
    }

}
