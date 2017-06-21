package ubic.gemma.model.common;

/**
 * Created by tesarst on 31/05/17.
 * Absolutely basic interface for any object that can be stored in the database.
 * This is used in the value objects, services, and dao structures.
 */
public interface Identifiable {

    Long getId();
}
