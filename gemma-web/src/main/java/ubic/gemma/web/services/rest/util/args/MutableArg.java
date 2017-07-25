package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseVoEnabledService;

/**
 * Created by tesarst on 16/05/17.
 * Class created to allow some API methods to allow various parameter types. E.g a taxon can be represented by Long (ID)
 * or a String (scientific/common name, abbreviation)
 *
 * @param <A>  the type that the argument is expected to mutate to.
 * @param <O>  the persistent object type.
 * @param <S>  the service for the object type.
 * @param <VO> the value object type.
 */
public abstract class MutableArg<A, O extends Identifiable, S extends BaseVoEnabledService<O, VO>, VO extends IdentifiableValueObject<O>> {
    /**
     * Should only be used by the implementations of this class, which is why there is no setter for it,
     * as the whole reason behind this class is to delegate the functionality from the web service controllers.
     */
    protected A value;

    String nullCause = "No cause specified.";

    /**
     * @return the reason that the object represented by the argument was null.
     */
    public final String getNullCause() {
        return nullCause;
    }

    /**
     * Calls appropriate backend logic to retrieve the value object of the persistent object that this argument represents.
     *
     * @param service the service to use for the value object retrieval.
     * @return the value object whose identifier matches the value of this mutable argument.
     */
    public final VO getValueObject( S service ) {
        O object = this.value == null ? null : this.getPersistentObject( service );
        return object == null ? null : service.loadValueObject( object );
    }

    /**
     * Calls appropriate backend logic to retrieve the persistent object that this mutable argument represents.
     *
     * @param service the service to use for the value object retrieval.
     * @return an object whose identifier matches the value of this mutable argument.
     */
    public abstract O getPersistentObject( S service );

}
