package ubic.gemma.model.genome.gene.phenotype.valueObject;

/** CharacteristicValueObject containing a category to a value */
public class CharacteristicValueObject {

    private String category = "";
    private String categoryUri = "";

    private String value = "";
    private String valueUri = "";

    private long occurence = 0;

    public CharacteristicValueObject( String value, String category ) {
        this.category = category;
        this.value = value;
    }

    public CharacteristicValueObject( String value, String category, String valueUri, String categoryUri ) {
        super();
        this.category = category;
        this.categoryUri = categoryUri;
        this.value = value.toLowerCase();
        this.valueUri = valueUri;
    }

    public String getCategoryUri() {
        return categoryUri;
    }

    public void setCategoryUri( String categoryUri ) {
        this.categoryUri = categoryUri;
    }

    public String getValueUri() {
        return valueUri;
    }

    public void Uri( String valueUri ) {
        this.valueUri = valueUri;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory( String category ) {
        this.category = category;
    }

    public String getValue() {
        return value;
    }

    public void setValue( String value ) {
        this.value = value.toLowerCase();
    }

    public long getOccurence() {
        return occurence;
    }

    public void setOccurence( long occurence ) {
        this.occurence = occurence;
    }

}
