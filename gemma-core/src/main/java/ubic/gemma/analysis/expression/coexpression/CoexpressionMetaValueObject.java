/*
 * The Gemma project
 * 
 * Copyright (c) 2008-2010 University of British Columbia
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
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * @author luke
 * @version $Id$
 */
public class CoexpressionMetaValueObject {

    private List<ExpressionExperimentValueObject> datasets;
    private String errorState;
    private String displayInfo;
    private Collection<CoexpressionDatasetValueObject> knownGeneDatasets;
    private Collection<CoexpressionValueObjectExt> knownGeneResults;
    private Collection<CoexpressionValueObjectExt> queryGenesOnlyResults;

    private boolean knownGenesOnly;

    private Collection<CoexpressionDatasetValueObject> predictedGeneDatasets;

    private Collection<CoexpressionValueObjectExt> predictedGeneResults;
    private Collection<CoexpressionDatasetValueObject> probeAlignedRegionDatasets;
    private Collection<CoexpressionValueObjectExt> probeAlignedRegionResults;
    private Collection<GeneValueObject> queryGenes;
    private Map<String, CoexpressionSummaryValueObject> summary;

    public List<ExpressionExperimentValueObject> getDatasets() {
        return datasets;
    }

    public String getErrorState() {
        return errorState;
    }

    public Collection<CoexpressionDatasetValueObject> getKnownGeneDatasets() {
        return knownGeneDatasets;
    }

    public Collection<CoexpressionValueObjectExt> getKnownGeneResults() {
        return knownGeneResults;
    }

    public Collection<CoexpressionDatasetValueObject> getPredictedGeneDatasets() {
        return predictedGeneDatasets;
    }

    public Collection<CoexpressionValueObjectExt> getPredictedGeneResults() {
        return predictedGeneResults;
    }

    public Collection<CoexpressionDatasetValueObject> getProbeAlignedRegionDatasets() {
        return probeAlignedRegionDatasets;
    }

    public Collection<CoexpressionValueObjectExt> getProbeAlignedRegionResults() {
        return probeAlignedRegionResults;
    }

    public Collection<GeneValueObject> getQueryGenes() {
        return queryGenes;
    }

    public Map<String, CoexpressionSummaryValueObject> getSummary() {
        return summary;
    }

    public boolean isKnownGenesOnly() {
        return knownGenesOnly;
    }

    public void setDatasets( List<ExpressionExperimentValueObject> datasets ) {
        this.datasets = datasets;
    }

    public void setErrorState( String errorState ) {
        this.errorState = errorState;
    }

    public void setKnownGeneDatasets( Collection<CoexpressionDatasetValueObject> knownGeneDatasets ) {
        this.knownGeneDatasets = knownGeneDatasets;
    }

    public void setKnownGeneResults( Collection<CoexpressionValueObjectExt> knownGeneResults ) {
        this.knownGeneResults = knownGeneResults;
    }

    public void setKnownGenesOnly( boolean knownGenesOnly ) {
        this.knownGenesOnly = knownGenesOnly;
    }

    public void setPredictedGeneDatasets( Collection<CoexpressionDatasetValueObject> predictedGeneDatasets ) {
        this.predictedGeneDatasets = predictedGeneDatasets;
    }

    public void setPredictedGeneResults( Collection<CoexpressionValueObjectExt> predictedGeneResults ) {
        this.predictedGeneResults = predictedGeneResults;
    }

    public void setProbeAlignedRegionDatasets( Collection<CoexpressionDatasetValueObject> probeAlignedRegionDatasets ) {
        this.probeAlignedRegionDatasets = probeAlignedRegionDatasets;
    }

    public void setProbeAlignedRegionResults( Collection<CoexpressionValueObjectExt> probeAlignedRegionResults ) {
        this.probeAlignedRegionResults = probeAlignedRegionResults;
    }

    public void setQueryGenes( Collection<GeneValueObject> queryGenes ) {
        this.queryGenes = queryGenes;
    }

    public void setSummary( Map<String, CoexpressionSummaryValueObject> summary ) {
        this.summary = summary;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for ( CoexpressionValueObjectExt ecvo : getKnownGeneResults() ) {
            buf.append( ecvo.toString() );
            buf.append( "\n" );
        }
        return buf.toString();
    }

    public void setDisplayInfo( String displayInfo ) {
        this.displayInfo = displayInfo;
    }

    public String getDisplayInfo() {
        return displayInfo;
    }

    public void setQueryGenesOnlyResults( Collection<CoexpressionValueObjectExt> queryGenesOnlyResults ) {
        this.queryGenesOnlyResults = queryGenesOnlyResults;
    }

    public Collection<CoexpressionValueObjectExt> getQueryGenesOnlyResults() {
        return queryGenesOnlyResults;
    }

}
