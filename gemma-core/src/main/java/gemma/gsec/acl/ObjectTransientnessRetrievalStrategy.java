package gemma.gsec.acl;

/**
 * A strategy to determine if a given domain object is transient.
 * @author poirigui
 */
public interface ObjectTransientnessRetrievalStrategy {

    /**
     * Determine if the given object is transient.
     */
    boolean isObjectTransient( Object domainObject );
}
