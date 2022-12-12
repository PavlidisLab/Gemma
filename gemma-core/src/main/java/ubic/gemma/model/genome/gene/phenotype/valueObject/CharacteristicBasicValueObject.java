package ubic.gemma.model.genome.gene.phenotype.valueObject;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.Characteristic;

@Getter
@Setter
public class CharacteristicBasicValueObject extends IdentifiableValueObject<Characteristic> {
    protected String value;
    protected String valueUri;
    protected String category;
    protected String categoryUri;

    /**
     * Required when using the class as a spring bean.
     */
    public CharacteristicBasicValueObject() {
        super();
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
        super( c );
        this.value = c.getValue();
        this.valueUri = c.getValueUri();
        this.category = c.getCategory();
        this.categoryUri = c.getCategoryUri();
    }
}
