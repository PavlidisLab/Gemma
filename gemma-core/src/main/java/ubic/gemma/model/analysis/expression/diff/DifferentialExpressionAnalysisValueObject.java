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

import com.fasterxml.jackson.annotation.JsonInclude;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.analysis.AnalysisValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Summary of a differential expression analysis
 *
 * @author paul
 */
@SuppressWarnings("unused") // Used in frontend
public class DifferentialExpressionAnalysisValueObject extends AnalysisValueObject<DifferentialExpressionAnalysis>
        implements Serializable {

    public static final double DEFAULT_THRESHOLD = 0.05; // should be one of the values stored in the HitListSizes
    private static final long serialVersionUID = 622877438067070041L;

    private Map<Long, Collection<FactorValueValueObject>> factorValuesUsed = new HashMap<>();
    private Collection<DiffExResultSetSummaryValueObject> resultSets = new HashSet<>();
    private Collection<Long> arrayDesignsUsed = null;
    private Long bioAssaySetId;
    private Long sourceExperiment;
    private ExperimentalFactorValueObject subsetFactor = null;
    private FactorValueValueObject subsetFactorValue = null;

    /**
     * Does not populate the resultSets.
     *
     * @param analysis the analysis to read the values from
     */
    public DifferentialExpressionAnalysisValueObject( DifferentialExpressionAnalysis analysis ) {
        super( analysis );
        this.bioAssaySetId = analysis.getExperimentAnalyzed().getId();
        if ( analysis.getSubsetFactorValue() != null ) {
            this.subsetFactorValue = new FactorValueValueObject( analysis.getSubsetFactorValue() );
            this.subsetFactor = new ExperimentalFactorValueObject(
                    analysis.getSubsetFactorValue().getExperimentalFactor() );
            // fill in the factorValuesUsed separately, needs access to details of the subset.
        }
    }

    public Collection<Long> getArrayDesignsUsed() {
        return arrayDesignsUsed;
    }

    public void setArrayDesignsUsed( Collection<Long> arrayDesignsUsed ) {
        this.arrayDesignsUsed = arrayDesignsUsed;
    }

    public Long getBioAssaySetId() {
        return bioAssaySetId;
    }

    public void setBioAssaySetId( Long bioAssaySetId ) {
        this.bioAssaySetId = bioAssaySetId;
    }

    /**
     * @return Map of ExperimentalFactor IDs to FactorValues that were actually used in the analysis. If this is a NOT a
     * subset analysis, then this won't be important (so it may not be populated), but for subset analyses
     * (subsetFactor != null), only the factor values present in the subset are relevant.
     */
    public Map<Long, Collection<FactorValueValueObject>> getFactorValuesUsed() {
        return factorValuesUsed;
    }

    public void setFactorValuesUsed( Map<Long, Collection<FactorValueValueObject>> factorValuesUsed ) {
        this.factorValuesUsed = factorValuesUsed;
    }

    public Collection<DiffExResultSetSummaryValueObject> getResultSets() {
        return resultSets;
    }

    public void setResultSets( Collection<DiffExResultSetSummaryValueObject> resultSets ) {
        this.resultSets = resultSets;
    }

    public Long getSourceExperiment() {
        return sourceExperiment;
    }

    /**
     * If this is a subset analysis
     *
     * @param id the source experiment id
     */
    public void setSourceExperiment( Long id ) {
        this.sourceExperiment = id;
    }

    public ExperimentalFactorValueObject getSubsetFactor() {
        return subsetFactor;
    }

    public void setSubsetFactor( ExperimentalFactorValueObject subsetFactor ) {
        this.subsetFactor = subsetFactor;
    }

    public FactorValueValueObject getSubsetFactorValue() {
        return subsetFactorValue;
    }

    public void setSubsetFactorValue( FactorValueValueObject subsetFactorValue ) {

        this.subsetFactorValue = subsetFactorValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        DifferentialExpressionAnalysisValueObject other = ( DifferentialExpressionAnalysisValueObject ) obj;
        if ( id == null ) {
            return other.id == null;
        } else
            return id.equals( other.id );
    }

    @Override
    public String toString() {
        return "DiffExAnalysisVO [id=" + id + ", bioAssaySetId=" + bioAssaySetId + "]";
    }

    public boolean isSubset() {
        return this.subsetFactor != null;
    }

}
