package ubic.gemma.model.coexpression;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

public class CoexpressionCollectionValueObject {
    private int linkCount;
    private int stringencyLinkCount;
    private Collection<ExpressionExperimentValueObject> expressionExperiments;
    private Collection<CoexpressionValueObject> coexpressionData;
    
    public CoexpressionCollectionValueObject() {
        linkCount = 0;
        stringencyLinkCount = 0;
        coexpressionData = new HashSet<CoexpressionValueObject>();
        expressionExperiments = new HashSet<ExpressionExperimentValueObject>();
    }
    
    /**
     * @return the coexpressionData
     */
    public Collection<CoexpressionValueObject> getCoexpressionData() {
        return coexpressionData;
    }
    /**
     * @param coexpressionData the coexpressionData to set
     */
    public void setCoexpressionData( Collection<CoexpressionValueObject> coexpressionData ) {
        this.coexpressionData = coexpressionData;
    }
    /**
     * @return the linkCount
     */
    public int getLinkCount() {
        return linkCount;
    }
    /**
     * @param linkCount the linkCount to set
     */
    public void setLinkCount( int linkCount ) {
        this.linkCount = linkCount;
    }
    /**
     * @return the stringencyLinkCount
     */
    public int getStringencyLinkCount() {
        return stringencyLinkCount;
    }
    /**
     * @param stringencyLinkCount the stringencyLinkCount to set
     */
    public void setStringencyLinkCount( int stringencyLinkCount ) {
        this.stringencyLinkCount = stringencyLinkCount;
    }

    /**
     * @return the expressionExperiments
     */
    public Collection<ExpressionExperimentValueObject> getExpressionExperiments() {
        return expressionExperiments;
    }

    /**
     * @param expressionExperiments the expressionExperiments to set
     */
    public void setExpressionExperiments( Collection<ExpressionExperimentValueObject> expressionExperiments ) {
        this.expressionExperiments = expressionExperiments;
    }
    
    /**
     * Add an expression experiment to the list
     * @param vo
     */
    public void addExpressionExperiment( ExpressionExperimentValueObject vo) {
        this.expressionExperiments.add( vo );
    }
    
    /**
     * Add a collection of expression experiment to the list
     * @param vo
     */
    public void addExpressionExperiment( Collection<ExpressionExperimentValueObject> vos) {
        this.expressionExperiments.addAll( vos );
    }
    
}
