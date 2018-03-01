package ubic.gemma.model.genome.gene.phenotype.valueObject;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.Characteristic;

@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class CharacteristicBasicValueObject extends IdentifiableValueObject<Characteristic> {
    protected String value;
    protected String valueUri;
    protected String category;
    protected String categoryUri;

    /**
     * Required when using the class as a spring bean.
     */
    public CharacteristicBasicValueObject() {
    }

    public CharacteristicBasicValueObject( Long id ) {
        super( id );
    }

    public CharacteristicBasicValueObject( Long id, String value, String valueUri, String category,
            String categoryUri ) {
        super( id );
        this.value = value;
        this.valueUri = valueUri;
        this.category = category;
        this.categoryUri = categoryUri;
    }

    public CharacteristicBasicValueObject( Characteristic c ) {
        super( c.getId() );
        this.value = c.getValue();
        this.valueUri = c.getValueUri();
        this.category = c.getCategory();
        this.categoryUri = c.getCategoryUri();
    }

    public String getValue() {
        return value;
    }

    public String getValueUri() {
        return valueUri;
    }

    public String getCategory() {
        return category;
    }

    public String getCategoryUri() {
        return categoryUri;
    }
}
