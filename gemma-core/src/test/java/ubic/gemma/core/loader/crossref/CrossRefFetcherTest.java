/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ubic.gemma.core.loader.crossref;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.ExternalDatabases;

import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fast, network-free test of the CrossRef JSON &rarr; {@link BibliographicReference} mapping.
 */
class CrossRefFetcherTest {

    private final CrossRefFetcher fetcher = new CrossRefFetcher();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void mapsBiorxivPreprintMessage() throws Exception {
        // trimmed shape of api.crossref.org/works/{doi} "message" for a bioRxiv preprint
        String json = "{"
                + "\"DOI\":\"10.1101/2025.01.02.634567\","
                + "\"type\":\"posted-content\","
                + "\"subtype\":\"preprint\","
                + "\"title\":[\"A very interesting preprint\"],"
                + "\"author\":[{\"given\":\"Jane\",\"family\":\"Smith\"},{\"given\":\"Ada\",\"family\":\"Doe\"}],"
                + "\"institution\":[{\"name\":\"bioRxiv\"}],"
                + "\"posted\":{\"date-parts\":[[2025,1,2]]},"
                + "\"published\":{\"date-parts\":[[2025,1,2]]},"
                + "\"URL\":\"https://doi.org/10.1101/2025.01.02.634567\","
                + "\"abstract\":\"<jats:p>Some <jats:italic>abstract</jats:italic> text.</jats:p>\","
                + "\"publisher\":\"Cold Spring Harbor Laboratory\""
                + "}";
        JsonNode message = mapper.readTree( json );

        BibliographicReference ref = fetcher.toBibliographicReference( "10.1101/2025.01.02.634567", message );

        assertThat( ref.getTitle() ).isEqualTo( "A very interesting preprint" );
        assertThat( ref.getAuthorList() ).isEqualTo( "Smith, Jane; Doe, Ada; " );
        assertThat( ref.getPublication() ).isEqualTo( "bioRxiv" );
        assertThat( ref.getAbstractText() ).isEqualTo( "Some abstract text." );
        assertThat( ref.getFullTextUri() ).isEqualTo( "https://doi.org/10.1101/2025.01.02.634567" );

        // stored under the DOI namespace, keyed by the DOI, so re-adds are idempotent
        assertThat( ref.getPubAccession() ).isNotNull();
        assertThat( ref.getPubAccession().getAccession() ).isEqualTo( "10.1101/2025.01.02.634567" );
        assertThat( ref.getPubAccession().getExternalDatabase().getName() ).isEqualTo( ExternalDatabases.DOI );

        Calendar c = Calendar.getInstance();
        c.setTime( ref.getPublicationDate() );
        assertThat( c.get( Calendar.YEAR ) ).isEqualTo( 2025 );
        assertThat( c.get( Calendar.MONTH ) ).isEqualTo( Calendar.JANUARY );
        assertThat( c.get( Calendar.DAY_OF_MONTH ) ).isEqualTo( 2 );
    }

    @Test
    void mapsJournalArticleWithContainerTitle() throws Exception {
        String json = "{"
                + "\"DOI\":\"10.1000/xyz123\","
                + "\"type\":\"journal-article\","
                + "\"title\":[\"A journal paper\"],"
                + "\"author\":[{\"given\":\"John\",\"family\":\"Roe\"}],"
                + "\"container-title\":[\"Journal of Examples\"],"
                + "\"issued\":{\"date-parts\":[[2024]]},"
                + "\"volume\":\"12\",\"issue\":\"3\",\"page\":\"100-110\""
                + "}";
        JsonNode message = mapper.readTree( json );

        BibliographicReference ref = fetcher.toBibliographicReference( "10.1000/xyz123", message );

        assertThat( ref.getTitle() ).isEqualTo( "A journal paper" );
        assertThat( ref.getPublication() ).isEqualTo( "Journal of Examples" );
        assertThat( ref.getVolume() ).isEqualTo( "12" );
        assertThat( ref.getIssue() ).isEqualTo( "3" );
        assertThat( ref.getPages() ).isEqualTo( "100-110" );
        // year-only date-parts -> defaults month/day to January / 1st
        Calendar c = Calendar.getInstance();
        c.setTime( ref.getPublicationDate() );
        assertThat( c.get( Calendar.YEAR ) ).isEqualTo( 2024 );
    }
}
