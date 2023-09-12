package ubic.gemma.rest.util.args;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;

import java.util.List;

public abstract class AbstractEntityArrayArg<O extends Identifiable, S extends FilteringService<O>> extends AbstractArrayArg<String> implements Arg<List<String>> {

    private final Class<? extends AbstractEntityArg<?, O, S>> entityArgClass;

    protected AbstractEntityArrayArg( @SuppressWarnings("rawtypes") Class<? extends AbstractEntityArg> entityArgClass, List<String> values ) {
        super( values );
        //noinspection unchecked
        this.entityArgClass = ( Class<? extends AbstractEntityArg<?, O, S>> ) entityArgClass;
    }

    /**
     * Obtain the argument class used to parse individual arguments in the array.
     */
    Class<? extends AbstractEntityArg<?, O, S>> getEntityArgClass() {
        return entityArgClass;
    }
}