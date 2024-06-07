package ubic.gemma.core.search;

import org.apache.lucene.queryParser.QueryParser;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.search.SearchSettings;

import java.util.Set;

/**
 * Search source that can retrieve results matching specific fields.
 * @author poirigui
 * @see ubic.gemma.core.search.lucene.LuceneQueryUtils#parseSafely(SearchSettings, QueryParser)
 */
public interface FieldAwareSearchSource extends SearchSource {

    /**
     * Obtain a list of fields that can be searched on.
     */
    Set<String> getFields( Class<? extends Identifiable> entityClass );
}
