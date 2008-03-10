/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.analysis.expression.coexpression;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;

/**
 * @author luke
 */
public class CoexpressionMetaValueObject {

    boolean isCannedAnalysis;
    boolean knownGenesOnly;
    List<Gene> queryGenes;
    Map<String, CoexpressionSummaryValueObject> summary;
    List<ExpressionExperimentValueObject> datasets;
    Collection<CoexpressionDatasetValueObject> knownGeneDatasets;
    Collection<CoexpressionValueObjectExt> knownGeneResults;
    Collection<CoexpressionDatasetValueObject> predictedGeneDatasets;
    Collection<CoexpressionValueObjectExt> predictedGeneResults;
    Collection<CoexpressionDatasetValueObject> probeAlignedRegionDatasets;
    Collection<CoexpressionValueObjectExt> probeAlignedRegionResults;

    public void setIsCannedAnalysis( boolean isCannedAnalysis ) {
        this.isCannedAnalysis = isCannedAnalysis;
    }

    public void setKnownGenesOnly( boolean knownGenesOnly ) {
        this.knownGenesOnly = knownGenesOnly;
    }

    public void setQueryGenes( List<Gene> queryGenes ) {
        this.queryGenes = queryGenes;
    }

    public List<ExpressionExperimentValueObject> getDatasets() {
        return datasets;
    }

    public Collection<Gene> getQueryGenes() {
        return queryGenes;
    }

    public void setDatasets( List<ExpressionExperimentValueObject> datasets ) {
        this.datasets = datasets;
    }

    public Collection<CoexpressionValueObjectExt> getKnownGeneResults() {
        return knownGeneResults;
    }

    public void setKnownGeneResults( Collection<CoexpressionValueObjectExt> knownGeneResults ) {
        this.knownGeneResults = knownGeneResults;
    }

    public Collection<CoexpressionValueObjectExt> getPredictedGeneResults() {
        return predictedGeneResults;
    }

    public void setPredictedGeneResults( Collection<CoexpressionValueObjectExt> predictedGeneResults ) {
        this.predictedGeneResults = predictedGeneResults;
    }

    public Collection<CoexpressionValueObjectExt> getProbeAlignedRegionResults() {
        return probeAlignedRegionResults;
    }

    public void setProbeAlignedRegionResults( Collection<CoexpressionValueObjectExt> probeAlignedRegionResults ) {
        this.probeAlignedRegionResults = probeAlignedRegionResults;
    }

    public Collection<CoexpressionDatasetValueObject> getKnownGeneDatasets() {
        return knownGeneDatasets;
    }

    public void setKnownGeneDatasets( Collection<CoexpressionDatasetValueObject> knownGeneDatasets ) {
        this.knownGeneDatasets = knownGeneDatasets;
    }

    public Collection<CoexpressionDatasetValueObject> getPredictedGeneDatasets() {
        return predictedGeneDatasets;
    }

    public void setPredictedGeneDatasets( Collection<CoexpressionDatasetValueObject> predictedGeneDatasets ) {
        this.predictedGeneDatasets = predictedGeneDatasets;
    }

    public Collection<CoexpressionDatasetValueObject> getProbeAlignedRegionDatasets() {
        return probeAlignedRegionDatasets;
    }

    public void setProbeAlignedRegionDatasets( Collection<CoexpressionDatasetValueObject> probeAlignedRegionDatasets ) {
        this.probeAlignedRegionDatasets = probeAlignedRegionDatasets;
    }

    public Map<String, CoexpressionSummaryValueObject> getSummary() {
        return summary;
    }

    public void setSummary( Map<String, CoexpressionSummaryValueObject> summary ) {
        this.summary = summary;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        for ( CoexpressionValueObjectExt ecvo : getKnownGeneResults() ) {
            buf.append( ecvo.toString() );
            buf.append( "\n" );
        }
        return buf.toString();
    }

}
