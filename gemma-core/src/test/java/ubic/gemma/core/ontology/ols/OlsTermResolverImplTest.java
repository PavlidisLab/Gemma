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
package ubic.gemma.core.ontology.ols;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Offline tests for the OLS response parser. The live call is exercised by
 * {@code OlsTermResolverIntegrationTest} ({@code @Tag("network")}).
 */
public class OlsTermResolverImplTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OlsTerm parse( String json, String iri ) throws IOException {
        JsonNode root = objectMapper.readTree( json );
        return OlsTermResolverImpl.parseTerm( root, iri );
    }

    @Test
    public void testParsePrefersDefiningOntology() throws IOException {
        // same IRI returned from two ontologies; the defining one wins
        String json = "{\"_embedded\":{\"terms\":["
                + "{\"iri\":\"http://purl.obolibrary.org/obo/EFO_0000270\",\"label\":\"imported label\",\"is_defining_ontology\":false},"
                + "{\"iri\":\"http://purl.obolibrary.org/obo/EFO_0000270\",\"label\":\"asthma\",\"is_defining_ontology\":true}"
                + "]}}";
        OlsTerm term = parse( json, "http://purl.obolibrary.org/obo/EFO_0000270" );
        assertNotNull( term );
        assertTrue( term.isFound() );
        assertEquals( "asthma", term.getLabel() );
    }

    @Test
    public void testParseFallsBackToFirstWhenNoDefining() throws IOException {
        String json = "{\"_embedded\":{\"terms\":["
                + "{\"iri\":\"http://x/1\",\"label\":\"first\"},"
                + "{\"iri\":\"http://x/1\",\"label\":\"second\"}"
                + "]}}";
        OlsTerm term = parse( json, "http://x/1" );
        assertNotNull( term );
        assertEquals( "first", term.getLabel() );
    }

    @Test
    public void testParseNoMatchReturnsNull() throws IOException {
        // OLS returns a page envelope with no _embedded when the IRI is unknown
        String json = "{\"page\":{\"size\":20,\"totalElements\":0,\"totalPages\":0,\"number\":0}}";
        assertNull( parse( json, "http://purl.obolibrary.org/obo/TGEMO_00003" ) );
    }

    @Test
    public void testParseEmptyTermsReturnsNull() throws IOException {
        assertNull( parse( "{\"_embedded\":{\"terms\":[]}}", "http://x/1" ) );
    }

    @Test
    public void testParseTermWithoutLabelReturnsNull() throws IOException {
        String json = "{\"_embedded\":{\"terms\":[{\"iri\":\"http://x/1\"}]}}";
        assertNull( parse( json, "http://x/1" ) );
    }

    @Test
    public void testNotFoundFactory() {
        OlsTerm nf = OlsTerm.notFound( "http://x/1" );
        assertFalse( nf.isFound() );
        assertNull( nf.getLabel() );
        assertEquals( "http://x/1", nf.getIri() );
    }
}
