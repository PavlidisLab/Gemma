package ubic.gemma.core.loader.expression.simple.model;

import lombok.Value;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import java.io.Serializable;

@Value
public class SimpleCharacteristic implements Serializable {
    @Nullable
    String category;
    @Nullable
    String categoryUri;
    @Nullable
    String value;
    @Nullable
    String valueUri;
}
