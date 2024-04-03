package ubic.gemma.model.common.search;

import org.junit.Test;
import ubic.gemma.core.search.DefaultHighlighter;
import ubic.gemma.core.search.SearchException;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.core.search.lucene.LuceneQueryUtils.prepareTermUriQuery;

public class SearchSettingsTest {

    @Test
    public void testSetQueryWhenQueryContainsBlankThenTrimAccordingly() {
        SearchSettings searchSettings = SearchSettings.builder().build();
        searchSettings.setQuery( " " );
        assertThat( searchSettings.getQuery() ).isEqualTo( " " );
    }

    @Test
    public void testSetQueryWhenQueryIsATermUri() throws SearchException {
        SearchSettings searchSettings = SearchSettings.builder().build();
        searchSettings.setQuery( "http://example.ca/" );
        assertThat( searchSettings.getQuery() ).isEqualTo( "http://example.ca/" );
        assertThat( prepareTermUriQuery( searchSettings ) ).isNotNull().hasToString( "http://example.ca/" );
    }

    @Test
    public void testSetQueryWhenQueryIsATermUriWithTrailingBlanks() throws SearchException {
        SearchSettings searchSettings = SearchSettings.builder().build();
        searchSettings.setQuery( " http://example.ca/ " );
        assertThat( searchSettings.getQuery() ).isEqualTo( " http://example.ca/ " );
        assertThat( prepareTermUriQuery( searchSettings ) ).isNotNull().hasToString( "http://example.ca/" );
    }

    @Test
    public void testSettingsWithDifferentHighlighterAreEqual() {
        assertThat( new DefaultHighlighter() ).isNotEqualTo( new DefaultHighlighter() );
        assertThat( SearchSettings.builder().query( "test" ).highlighter( new DefaultHighlighter() ).build() )
                .isEqualTo( SearchSettings.builder().query( "test" ).highlighter( new DefaultHighlighter() ).build() );
    }
}