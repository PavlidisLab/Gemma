package ubic.gemma.web.controller.diff;

import java.util.Collection;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * A value object with meta analysis results.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionMetaAnalysisValueObject {

    private Gene gene = null;

    private Double fisherPValue = null;

    private Collection<ExpressionExperiment> activeExperiments = null;

    private Collection<ExpressionExperiment> supportingExperiments = null;

    private Collection<DifferentialExpressionValueObject> probeResults = null;

    public Double getFisherPValue() {
        return fisherPValue;
    }

    public void setFisherPValue( Double fisherPValue ) {
        this.fisherPValue = fisherPValue;
    }

    public Gene getGene() {
        return gene;
    }

    public void setGene( Gene gene ) {
        this.gene = gene;
    }

    public Collection<ExpressionExperiment> getSupportingExperiments() {
        return supportingExperiments;
    }

    public void setSupportingExperiments( Collection<ExpressionExperiment> supportingExperiments ) {
        this.supportingExperiments = supportingExperiments;
    }

    public Collection<ExpressionExperiment> getActiveExperiments() {
        return activeExperiments;
    }

    public void setActiveExperiments( Collection<ExpressionExperiment> activeExperiments ) {
        this.activeExperiments = activeExperiments;
    }

    public Collection<DifferentialExpressionValueObject> getProbeResults() {
        return probeResults;
    }

    public void setProbeResults( Collection<DifferentialExpressionValueObject> probeResults ) {
        this.probeResults = probeResults;
    }

}
