package ubic.gemma.model.genome.gene.phenotype.valueObject;

/** CharacteristicValueObject containing a category to a value */
public class CharacteristicValueObject {

    private String category = "";

    private String value = "";

    public CharacteristicValueObject( String value, String category ) {
        this.category = category;
        this.value = value;
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
        this.value = value;
    }
}
