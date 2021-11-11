package ubic.gemma.model.common.search;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchSettingsTest {

    @Test
    public void testSetQueryWhenQueryContainsBlankThenTrimAccordingly() {
        SearchSettings searchSettings = SearchSettings.builder().build();
        searchSettings.setQuery( " " );
        assertThat( searchSettings.getQuery() ).isEqualTo( "" );
        assertThat( searchSettings.getRawQuery() ).isEqualTo( " " );
    }

    @Test
    public void testSetQueryWhenQueryIsNull() {
        SearchSettings searchSettings = SearchSettings.builder().build();
        searchSettings.setQuery( null );
        assertThat( searchSettings.getQuery() ).isNull();
        assertThat( searchSettings.getRawQuery() ).isNull();
    }

    @Test
    public void testSetQueryWhenQueryIsATermUri() {
        SearchSettings searchSettings = SearchSettings.builder().build();
        searchSettings.setQuery( "http://example.ca/" );
        assertThat( searchSettings.getQuery() ).isEqualTo( "http://example.ca/" );
        assertThat( searchSettings.getRawQuery() ).isEqualTo( "http://example.ca/" );
        assertThat( searchSettings.isTermQuery() );
        assertThat( searchSettings.getTermUri() ).isEqualTo( "http://example.ca/" );
    }

    @Test
    public void testSetQueryWhenQueryIsATermUriWithTrailingBlanks() {
        SearchSettings searchSettings = SearchSettings.builder().build();
        searchSettings.setQuery( " http://example.ca/ " );
        assertThat( searchSettings.getQuery() ).isEqualTo( "http://example.ca/" );
        assertThat( searchSettings.getRawQuery() ).isEqualTo( " http://example.ca/ " );
        assertThat( searchSettings.isTermQuery() );
        assertThat( searchSettings.getTermUri() ).isEqualTo( "http://example.ca/" );
    }


    @Test
    public void testSetTermUriWhenUriIsBlank() {
        SearchSettings searchSettings = SearchSettings.builder().build();
        searchSettings.setTermUri( "" );
        assertThat( searchSettings.isTermQuery() ).isFalse();
    }

    @Test
    public void testSetTermUriWhenUriIsNull() {
        SearchSettings searchSettings = SearchSettings.builder().build();
        searchSettings.setTermUri( null );
        assertThat( searchSettings.isTermQuery() ).isFalse();
    }
}