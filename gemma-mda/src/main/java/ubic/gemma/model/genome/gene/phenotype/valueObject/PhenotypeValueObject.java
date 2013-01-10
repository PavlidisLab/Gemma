package ubic.gemma.model.genome.gene.phenotype.valueObject;

public class PhenotypeValueObject {

    private String value = "";
    private String valueUri = "";

    public PhenotypeValueObject( String value, String valueUri ) {
        super();
        this.value = value;
        this.valueUri = valueUri;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public String getValueUri() {
        return this.valueUri;
    }

    public void setValueUri( String valueUri ) {
        this.valueUri = valueUri;
    }

}
