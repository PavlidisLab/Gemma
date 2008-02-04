package ubic.gemma.web.controller.coexpressionSearch;

import java.util.Collection;
import java.util.List;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author luke
 */
public class ExtCoexpressionMetaValueObject {

    List<ExpressionExperiment> datasets;
    Collection<ExtCoexpressionValueObject> knownGeneResults;
    Collection<ExtCoexpressionValueObject> predictedGeneResults;
    Collection<ExtCoexpressionValueObject> probeAlignedRegionResults;
    

    public List<ExpressionExperiment> getDatasets() {
        return datasets;
    }

    public void setDatasets( List<ExpressionExperiment> datasets ) {
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
    
}
