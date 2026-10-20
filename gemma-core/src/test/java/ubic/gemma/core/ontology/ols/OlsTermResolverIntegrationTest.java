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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live probe against the real EBI OLS service. Excluded from the default run ({@code @Tag("network")});
 * run with {@code -DexcludedGroups=}.
 */
@Tag("network")
@ExtendWith(NetworkAvailableExtension.class)
public class OlsTermResolverIntegrationTest {

    private static final String OLS_BASE = "https://www.ebi.ac.uk/ols4";

    private OlsTermResolverImpl newResolver() {
        OlsTermResolverImpl resolver = new OlsTermResolverImpl();
        ReflectionTestUtils.setField( resolver, "baseUrl", OLS_BASE );
        ReflectionTestUtils.setField( resolver, "timeoutMs", 15000 );
        return resolver;
    }

    @Test
    @NetworkAvailable(url = OLS_BASE)
    public void testResolveRealTerm() throws OlsUnavailableException {
        // MONDO_0004979 = asthma; non-obsolete and carried by five ontologies (mondo defines it, efo/covoc/oba
        // import it), so it is stable in both identity and label.
        //
        // This used to be http://purl.obolibrary.org/obo/EFO_0000270, which failed on two counts. That IRI does
        // not exist -- native EFO identifiers live under http://www.ebi.ac.uk/efo/, not the OBO PURL prefix, so
        // OLS answered 200 with totalElements: 0. And the term behind the correct IRI has been obsoleted in favour
        // of this MONDO term: EFO, the defining ontology, labels it "obsolete_asthma" and sets term_replaced_by to
        // MONDO_0004979. Picking a live OBO PURL term avoids both traps.
        OlsTerm term = newResolver().resolve( "http://purl.obolibrary.org/obo/MONDO_0004979" );
        assertNotNull( term );
        assertTrue( term.isFound() );
        assertEquals( "asthma", term.getLabel().toLowerCase() );
    }

    @Test
    @NetworkAvailable(url = OLS_BASE)
    public void testResolveFabricatedTermReturnsNull() throws OlsUnavailableException {
        // TGEMO_00003 is fabricated (the GSE84876 grounding incident) — not in OLS
        assertNull( newResolver().resolve( "http://purl.obolibrary.org/obo/TGEMO_00003" ) );
    }
}
