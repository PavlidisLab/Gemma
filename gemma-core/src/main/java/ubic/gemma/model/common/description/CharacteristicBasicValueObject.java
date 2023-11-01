package ubic.gemma.model.common.description;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.IdentifiableValueObject;

import javax.annotation.Nullable;

@Data
@EqualsAndHashCode(callSuper = true)
public class CharacteristicBasicValueObject extends IdentifiableValueObject<Characteristic> {

    private String value;
    @Nullable
    private String valueUri;
    private String category;
    @Nullable
    private String categoryUri;

    /**
     * Required when using the class as a spring bean.
     */
    public CharacteristicBasicValueObject() {
        super();
    }

    public CharacteristicBasicValueObject( String value, @Nullable String valueUri, String category,
            @Nullable String categoryUri ) {
        super( ( Long ) null );
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
