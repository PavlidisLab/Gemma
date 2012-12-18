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
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisSummaryValueObject;
import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;

/**
 * TODO Document Me
 * 
 * @author stgeorgn
 * @version $Id$
 */
public class DiffExpressionEvidenceValueObject extends EvidenceValueObject {

    private Double selectionThreshold = 0D;

    private Double metaPvalue = 0D;

    private Double metaQvalue = 0D;

    private Double meanLogFoldChange = 0D;

    private Double metaPvalueRank = 0D;

    private Boolean upperTail = false;

    private Long geneDifferentialExpressionMetaAnalysisId = 0L;

    private Long geneDifferentialExpressionMetaAnalysisResultId = 0L;

    private GeneDifferentialExpressionMetaAnalysisSummaryValueObject geneDifferentialExpressionMetaAnalysisSummaryValueObject = null;

    public DiffExpressionEvidenceValueObject() {
        super();
    }

    public DiffExpressionEvidenceValueObject(
            DifferentialExpressionEvidence differentialExpressionEvidence,
            GeneDifferentialExpressionMetaAnalysisSummaryValueObject geneDifferentialExpressionMetaAnalysisSummaryValueObject ) {
        super( differentialExpressionEvidence );

        this.metaPvalue = differentialExpressionEvidence.getGeneDifferentialExpressionMetaAnalysisResult()
                .getMetaPvalue();
        this.metaQvalue = differentialExpressionEvidence.getGeneDifferentialExpressionMetaAnalysisResult()
                .getMetaQvalue();
        this.meanLogFoldChange = differentialExpressionEvidence.getGeneDifferentialExpressionMetaAnalysisResult()
                .getMeanLogFoldChange();
        this.metaPvalueRank = differentialExpressionEvidence.getGeneDifferentialExpressionMetaAnalysisResult()
                .getMetaPvalueRank();
        this.upperTail = differentialExpressionEvidence.getGeneDifferentialExpressionMetaAnalysisResult()
                .getUpperTail();
        this.geneDifferentialExpressionMetaAnalysisId = geneDifferentialExpressionMetaAnalysisSummaryValueObject
                .getId();
        this.geneDifferentialExpressionMetaAnalysisResultId = differentialExpressionEvidence
                .getGeneDifferentialExpressionMetaAnalysisResult().getId();
        this.selectionThreshold = differentialExpressionEvidence.getSelectionThreshold();
        this.geneDifferentialExpressionMetaAnalysisSummaryValueObject = geneDifferentialExpressionMetaAnalysisSummaryValueObject;
        if ( this.geneDifferentialExpressionMetaAnalysisSummaryValueObject != null ) {
            this.geneDifferentialExpressionMetaAnalysisSummaryValueObject.setDiffExpressionEvidence( this );
        }
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
        this.upperTail = geneDifferentialExpressionMetaAnalysisResult.getUpperTail();
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

    public Long getGeneDifferentialExpressionMetaAnalysisResultId() {
        return this.geneDifferentialExpressionMetaAnalysisResultId;
    }

    public void setGeneDifferentialExpressionMetaAnalysisResultId( Long geneDifferentialExpressionMetaAnalysisResultId ) {
        this.geneDifferentialExpressionMetaAnalysisResultId = geneDifferentialExpressionMetaAnalysisResultId;
    }

    public GeneDifferentialExpressionMetaAnalysisSummaryValueObject getGeneDifferentialExpressionMetaAnalysisSummaryValueObject() {
        return this.geneDifferentialExpressionMetaAnalysisSummaryValueObject;
    }

    public void setGeneDifferentialExpressionMetaAnalysisSummaryValueObject(
            GeneDifferentialExpressionMetaAnalysisSummaryValueObject geneDifferentialExpressionMetaAnalysisSummaryValueObject ) {
        this.geneDifferentialExpressionMetaAnalysisSummaryValueObject = geneDifferentialExpressionMetaAnalysisSummaryValueObject;
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

    public Boolean getUpperTail() {
        return this.upperTail;
    }

    public void setUpperTail( Boolean upperTail ) {
        this.upperTail = upperTail;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime
                * result
                + ( ( this.geneDifferentialExpressionMetaAnalysisId == null ) ? 0 : this.geneDifferentialExpressionMetaAnalysisId
                        .hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( !super.equals( obj ) ) return false;
        if ( getClass() != obj.getClass() ) return false;
        DiffExpressionEvidenceValueObject other = ( DiffExpressionEvidenceValueObject ) obj;
        if ( this.geneDifferentialExpressionMetaAnalysisId == null ) {
            if ( other.geneDifferentialExpressionMetaAnalysisId != null ) return false;
        } else if ( !this.geneDifferentialExpressionMetaAnalysisId.equals( other.geneDifferentialExpressionMetaAnalysisId ) )
            return false;
        return true;
    }
    
    

}
