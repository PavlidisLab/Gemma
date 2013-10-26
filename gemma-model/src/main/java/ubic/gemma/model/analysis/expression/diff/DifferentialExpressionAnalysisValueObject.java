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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;

/**
 * Summary of a differential expression analysis
 * 
 * @author paul
 * @version $Id$
 */
public class DifferentialExpressionAnalysisValueObject implements Serializable {

    public static final double DEFAULT_THRESHOLD = 0.05; // should be one of the values stored in the HitListSizes

    private static final long serialVersionUID = 622877438067070041L;

    private Long bioAssaySetId;

    // DO NOT change the key type without first checking all places it is used!
    private Map<Long, Collection<FactorValueValueObject>> factorValuesUsed = new HashMap<>();

    private Long id;

    private Collection<DifferentialExpressionSummaryValueObject> resultSets = new HashSet<>();

    private ExperimentalFactorValueObject subsetFactor = null;

    private FactorValueValueObject subsetFactorValue = null;

    /**
     * Does not populate the resultSets.
     * 
     * @param analysis
     */
    public DifferentialExpressionAnalysisValueObject( DifferentialExpressionAnalysis analysis ) {
        this.id = analysis.getId();
        this.bioAssaySetId = analysis.getExperimentAnalyzed().getId();
        if ( analysis.getSubsetFactorValue() != null ) {
            this.subsetFactorValue = new FactorValueValueObject( analysis.getSubsetFactorValue() );
            this.subsetFactor = new ExperimentalFactorValueObject( analysis.getSubsetFactorValue()
                    .getExperimentalFactor() );
            // fill in the factorValuesUsed separately, needs access to details of the subset.
        }
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        DifferentialExpressionAnalysisValueObject other = ( DifferentialExpressionAnalysisValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null ) return false;
        } else if ( !id.equals( other.id ) ) return false;
        return true;
    }

    public Long getBioAssaySetId() {
        return bioAssaySetId;
    }

    /**
     * @return Map of ExperimentalFactor IDs to FactorValues that were actually used in the analysis. If this is a NOT a
     *         subset analysis, then this won't be important (so it may not be populated), but for subset analyses
     *         (subsetFactor != null), only the factorvalues present in the subset are relevant.
     */
    public Map<Long, Collection<FactorValueValueObject>> getFactorValuesUsed() {
        return factorValuesUsed;
    }

    public Long getId() {
        return id;
    }

    public Collection<DifferentialExpressionSummaryValueObject> getResultSets() {
        return resultSets;
    }

    public ExperimentalFactorValueObject getSubsetFactor() {
        return subsetFactor;
    }

    public FactorValueValueObject getSubsetFactorValue() {
        return subsetFactorValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    public void setBioAssaySetId( Long bioAssaySetId ) {
        this.bioAssaySetId = bioAssaySetId;
    }

    public void setFactorValuesUsed( Map<Long, Collection<FactorValueValueObject>> factorValuesUsed ) {
        this.factorValuesUsed = factorValuesUsed;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setResultSets( Collection<DifferentialExpressionSummaryValueObject> resultSets ) {
        this.resultSets = resultSets;
    }

    public void setSubsetFactor( ExperimentalFactorValueObject subsetFactor ) {
        this.subsetFactor = subsetFactor;
    }

    public void setSubsetFactorValue( FactorValueValueObject subsetFactorValue ) {
        this.subsetFactorValue = subsetFactorValue;
    }

}
