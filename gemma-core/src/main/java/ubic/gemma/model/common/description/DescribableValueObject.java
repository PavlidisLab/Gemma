package ubic.gemma.model.common.description;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.IdentifiableValueObject;

import javax.annotation.Nullable;

/**
 * @param <T>
 * @author poirigui
 */
@Getter
@Setter
public abstract class DescribableValueObject<T extends Describable> extends IdentifiableValueObject<T> implements Describable {

    private String name;
    @Nullable
    private String description;

    protected DescribableValueObject() {

    }

    protected DescribableValueObject( T entity ) {
        super( entity );
        this.name = entity.getName();
        this.description = entity.getDescription();
    }
}
