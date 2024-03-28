package ubic.gemma.core.search;

import ubic.gemma.model.common.Identifiable;

import java.util.Set;

/**
 * Search source that can retrieve results matching specific fields.
 * @author poirigui
 */
public interface FieldAwareSearchSource extends SearchSource {

    /**
     * Obtain a list of fields that can be searched on.
     */
    Set<String> getFields( Class<? extends Identifiable> entityClass );
}
