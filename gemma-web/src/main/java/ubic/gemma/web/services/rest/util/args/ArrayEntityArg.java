package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Array of identifiers of an Identifiable entity
 */
public abstract class ArrayEntityArg extends ArrayStringArg {

    ArrayEntityArg( List<String> values ) {
        super( values );
    }

    ArrayEntityArg( String errorMessage, Exception exception ) {
        super( errorMessage, exception );
    }

    /**
     * Combines the given filters with the properties in this array to create a final filter to be used for VO retrieval.
     *
     * @param service the service used to guess the type and name of the property that this arrayEntityArg represents.
     * @param filters the filters list to add the new filter to. Can be null.
     * @return the same array list as given, with a new added element, or a new ArrayList, in case the given filters
     * was null.
     */
    public abstract ArrayList<ObjectFilter[]> combineFilters( ArrayList<ObjectFilter[]> filters,
            BaseVoEnabledService service );

}
