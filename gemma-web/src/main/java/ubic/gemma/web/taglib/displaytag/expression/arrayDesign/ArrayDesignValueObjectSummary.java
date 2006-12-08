package ubic.gemma.web.taglib.displaytag.expression.arrayDesign;

import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

public class ArrayDesignValueObjectSummary extends ArrayDesignValueObject {

    /**
     * 
     */
    private static final long serialVersionUID = 4317373121919405446L;

    
    private String summaryTable;


    
    public ArrayDesignValueObjectSummary() {
        super();
    }


    public ArrayDesignValueObjectSummary( ArrayDesignValueObject otherBean ) {
        super( otherBean );
    }


    public ArrayDesignValueObjectSummary(  ArrayDesignValueObject otherBean ,String summaryTable ) {
        super(otherBean);
        this.summaryTable = summaryTable;
    }


    /**
     * @return the summaryTable
     */
    public String getSummaryTable() {
        return summaryTable;
    }


    /**
     * @param summaryTable the summaryTable to set
     */
    public void setSummaryTable( String summaryTable ) {
        this.summaryTable = summaryTable;
    }
}
