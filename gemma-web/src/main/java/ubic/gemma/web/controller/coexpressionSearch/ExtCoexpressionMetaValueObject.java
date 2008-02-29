package ubic.gemma.web.controller.coexpressionSearch;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;

/**
 * @author luke
 */
public class ExtCoexpressionMetaValueObject {

    boolean isCannedAnalysis;
    boolean knownGenesOnly;
    List<Gene> queryGenes;
    Map<String, ExtCoexpressionSummaryValueObject> summary;
    List<ExpressionExperimentValueObject> datasets;
    Collection<ExtCoexpressionDatasetValueObject> knownGeneDatasets;
    Collection<ExtCoexpressionValueObject> knownGeneResults;
    Collection<ExtCoexpressionDatasetValueObject> predictedGeneDatasets;
    Collection<ExtCoexpressionValueObject> predictedGeneResults;
    Collection<ExtCoexpressionDatasetValueObject> probeAlignedRegionDatasets;
    Collection<ExtCoexpressionValueObject> probeAlignedRegionResults;

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

    public Collection<ExtCoexpressionValueObject> getKnownGeneResults() {
        return knownGeneResults;
    }

    public void setKnownGeneResults( Collection<ExtCoexpressionValueObject> knownGeneResults ) {
        this.knownGeneResults = knownGeneResults;
    }

    public Collection<ExtCoexpressionValueObject> getPredictedGeneResults() {
        return predictedGeneResults;
    }

    public void setPredictedGeneResults( Collection<ExtCoexpressionValueObject> predictedGeneResults ) {
        this.predictedGeneResults = predictedGeneResults;
    }

    public Collection<ExtCoexpressionValueObject> getProbeAlignedRegionResults() {
        return probeAlignedRegionResults;
    }

    public void setProbeAlignedRegionResults( Collection<ExtCoexpressionValueObject> probeAlignedRegionResults ) {
        this.probeAlignedRegionResults = probeAlignedRegionResults;
    }

    public Collection<ExtCoexpressionDatasetValueObject> getKnownGeneDatasets() {
        return knownGeneDatasets;
    }

    public void setKnownGeneDatasets( Collection<ExtCoexpressionDatasetValueObject> knownGeneDatasets ) {
        this.knownGeneDatasets = knownGeneDatasets;
    }

    public Collection<ExtCoexpressionDatasetValueObject> getPredictedGeneDatasets() {
        return predictedGeneDatasets;
    }

    public void setPredictedGeneDatasets( Collection<ExtCoexpressionDatasetValueObject> predictedGeneDatasets ) {
        this.predictedGeneDatasets = predictedGeneDatasets;
    }

    public Collection<ExtCoexpressionDatasetValueObject> getProbeAlignedRegionDatasets() {
        return probeAlignedRegionDatasets;
    }

    public void setProbeAlignedRegionDatasets( Collection<ExtCoexpressionDatasetValueObject> probeAlignedRegionDatasets ) {
        this.probeAlignedRegionDatasets = probeAlignedRegionDatasets;
    }

    public Map<String, ExtCoexpressionSummaryValueObject> getSummary() {
        return summary;
    }
    
    public void setSummary( Map<String, ExtCoexpressionSummaryValueObject> summary ) {
        this.summary = summary;
    }
    
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for ( ExtCoexpressionValueObject ecvo : getKnownGeneResults() ) {
            buf.append( ecvo.toString() );
            buf.append( "\n" );
        }
        return buf.toString();
    }
    
}
