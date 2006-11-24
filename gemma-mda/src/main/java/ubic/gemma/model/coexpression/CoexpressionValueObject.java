package ubic.gemma.model.coexpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

public class CoexpressionValueObject {

    private String geneName;
    private Long geneId;
    private String geneOfficialName;
    private Map<Long,ExpressionExperimentValueObject> expressionExperimentValueObjects;

    public CoexpressionValueObject() {
        geneName = "";
        geneId = null;
        geneOfficialName = null;
        expressionExperimentValueObjects = new HashMap<Long, ExpressionExperimentValueObject>();
    }
    
    /**
     * @return the expressionExperimentValueObjects
     */
    public Collection getExpressionExperimentValueObjects() {
        return expressionExperimentValueObjects.values();
    }

    /**
     * @param expressionExperimentValueObjects the expressionExperimentValueObjects to set
     */
    public void addExpressionExperimentValueObject(ExpressionExperimentValueObject expressionExperimentValueObject ) {
        this.expressionExperimentValueObjects.put( new Long(expressionExperimentValueObject.getId()), expressionExperimentValueObject);    
    }
    /**
     * @return the geneId
     */
    public Long getGeneId() {
        return geneId;
    }
    /**
     * @param geneId the geneId to set
     */
    public void setGeneId( Long geneId ) {
        this.geneId = geneId;
    }
    /**
     * @return the geneName
     */
    public String getGeneName() {
        return geneName;
    }
    /**
     * @param geneName the geneName to set
     */
    public void setGeneName( String geneName ) {
        this.geneName = geneName;
    }
    /**
     * @return the geneOfficialName
     */
    public String getGeneOfficialName() {
        return geneOfficialName;
    }
    /**
     * @param geneOfficialName the geneOfficialName to set
     */
    public void setGeneOfficialName( String geneOfficialName ) {
        this.geneOfficialName = geneOfficialName;
    }

}
