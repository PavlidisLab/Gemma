package ubic.gemma.web.services.rest.util.args;

/**
 * Created by tesarst on 16/05/17.
 * Class created to allow some API methods to allow various parameter types. E.g a taxon can be represented by Long (ID)
 * or a String (scientific/common name, abbreviation)
 */
public abstract class MutableArg<R, S, T, U> {

    /**
     * Should only be used by the implementations of this class, which is why there is no getter and setter for it,
     * as the whole reason behind this class is to delegate the functionality from the web service controllers.
     */
    protected R value;

    /**
     * Calls appropriate backend logic to retrieve the persistent object that this mutable argument represents.
     *
     * @return an object whose identifier matches the value of this mutable argument.
     */
    protected abstract S getPersistentObject( T service );

    /**
     * Calls appropriate backend logic to retreive the value object of the persistent object that this argument represents.
     * @param service
     * @return
     */
    protected abstract U getValueObject(T service);

}
