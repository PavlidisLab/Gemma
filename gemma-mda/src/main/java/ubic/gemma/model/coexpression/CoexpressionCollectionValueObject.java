package ubic.gemma.model.coexpression;

import java.util.Collection;
import java.util.HashSet;

public class CoexpressionCollectionValueObject {
    private int linkCount;
    private int stringencyLinkCount;
    private Collection<CoexpressionValueObject> coexpressionData;
    
    public CoexpressionCollectionValueObject() {
        linkCount = 0;
        stringencyLinkCount = 0;
        coexpressionData = new HashSet<CoexpressionValueObject>();
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
    
}
