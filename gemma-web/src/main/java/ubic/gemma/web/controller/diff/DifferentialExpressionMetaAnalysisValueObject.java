package ubic.gemma.web.controller.diff;

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
    
    private Integer numSearchedDataSets;
    
    private Integer numSupportingDataSets;

    public Integer getNumSearchedDataSets() {
        return numSearchedDataSets;
    }

    public void setNumSearchedDataSets( Integer numSearchedDataSets ) {
        this.numSearchedDataSets = numSearchedDataSets;
    }

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

    public Integer getNumSupportingDataSets() {
        return numSupportingDataSets;
    }

    public void setNumSupportingDataSets( Integer numSupportingDataSets ) {
        this.numSupportingDataSets = numSupportingDataSets;
    }

}
