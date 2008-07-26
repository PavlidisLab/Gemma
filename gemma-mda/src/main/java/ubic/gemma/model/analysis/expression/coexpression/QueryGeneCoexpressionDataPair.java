package ubic.gemma.model.analysis.expression.coexpression;

import ubic.gemma.model.genome.Gene;

/**
 * @author luke
 */
public class QueryGeneCoexpressionDataPair {

    private Gene queryGene;
    private CoexpressionValueObject coexpressionData;

    public QueryGeneCoexpressionDataPair( Gene queryGene, CoexpressionValueObject coexpressionData ) {
        this.queryGene = queryGene;
        this.coexpressionData = coexpressionData;
    }

    /**
     * @return the queryGene
     */
    public Gene getQueryGene() {
        return queryGene;
    }

    /**
     * @return the coexpressionData
     */
    public CoexpressionValueObject getCoexpressionData() {
        return coexpressionData;
    }
}
