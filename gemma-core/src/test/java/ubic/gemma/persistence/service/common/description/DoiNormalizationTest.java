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
package ubic.gemma.persistence.service.common.description;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fast unit test for {@link BibliographicReferenceServiceImpl#normalizeDoi(String)}. Normalization is
 * idempotency-critical: the same DOI pasted any which way must reduce to one canonical key so it stores and
 * looks up as a single reference.
 */
class DoiNormalizationTest {

    private static final String CANONICAL = "10.1101/2025.01.02.634567";

    @Test
    void stripsResolverPrefixesAndLowerCases() {
        assertThat( BibliographicReferenceServiceImpl.normalizeDoi( "10.1101/2025.01.02.634567" ) ).isEqualTo( CANONICAL );
        assertThat( BibliographicReferenceServiceImpl.normalizeDoi( "https://doi.org/10.1101/2025.01.02.634567" ) ).isEqualTo( CANONICAL );
        assertThat( BibliographicReferenceServiceImpl.normalizeDoi( "http://doi.org/10.1101/2025.01.02.634567" ) ).isEqualTo( CANONICAL );
        assertThat( BibliographicReferenceServiceImpl.normalizeDoi( "https://dx.doi.org/10.1101/2025.01.02.634567" ) ).isEqualTo( CANONICAL );
        assertThat( BibliographicReferenceServiceImpl.normalizeDoi( "doi:10.1101/2025.01.02.634567" ) ).isEqualTo( CANONICAL );
        assertThat( BibliographicReferenceServiceImpl.normalizeDoi( "  10.1101/2025.01.02.634567  " ) ).isEqualTo( CANONICAL );
        // DOIs are case-insensitive; canonicalize to lower case
        assertThat( BibliographicReferenceServiceImpl.normalizeDoi( "10.1101/ABC.def" ) ).isEqualTo( "10.1101/abc.def" );
    }
}
