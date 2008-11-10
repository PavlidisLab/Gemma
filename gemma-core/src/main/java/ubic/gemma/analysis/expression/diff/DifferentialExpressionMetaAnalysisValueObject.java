package ubic.gemma.analysis.expression.diff;

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

    private String sortKey;
    private Double fisherPValue = null;
    private int numSearchedExperiments;
    private int numExperimentsInScope;
    private int numMetThreshold;

    private Collection<ExpressionExperiment> activeExperiments = null;

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

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey() {
        this.sortKey = String.format( "%06f%s", getFisherPValue(), getGene().getOfficialSymbol() );
    }

    public int getNumSearchedExperiments() {
        return numSearchedExperiments;
    }

    public void setNumSearchedExperiments( int numSearchedExperiments ) {
        this.numSearchedExperiments = numSearchedExperiments;
    }

    public int getNumExperimentsInScope() {
        return numExperimentsInScope;
    }

    public void setNumExperimentsInScope( int numExperimentsInScope ) {
        this.numExperimentsInScope = numExperimentsInScope;
    }

    public int getNumMetThreshold() {
        return numMetThreshold;
    }

    public void setNumMetThreshold( int numMetThreshold ) {
        this.numMetThreshold = numMetThreshold;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append( "# MetaP = " + this.getFisherPValue() + "\n" );

        // FIXME add other information here, like the date and details about experiments.

        for ( DifferentialExpressionValueObject result : this.getProbeResults() ) {
            buf.append( result + "\n" );
        }
        return buf.toString();
    }

}
