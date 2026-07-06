/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
 *
 */
package ubic.gemma.persistence.service.common.description;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabases;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This class tests the bibliographic reference data access object. It is also used to test some of the Hibernate
 * features.
 *
 * @author pavlidis
 *
 */
public class BibliographicReferenceServiceTest extends BaseSpringContextTest5 {

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;
    private DatabaseEntry de = null;
    private BibliographicReference testBibRef = null;

    /*
     * Call to create should persist the BibliographicReference and DatabaseEntry (cascade=all).
     */
    @BeforeEach
    public void setUp() throws Exception {

        testBibRef = BibliographicReference.Factory.newInstance();

        de = this.getTestPersistentDatabaseEntry( ExternalDatabases.PUBMED );

        /* Set the DatabaseEntry. */
        testBibRef.setPubAccession( de );

        bibliographicReferenceService.create( testBibRef );
    }

    @Test
    public final void testfind() {
        BibliographicReference result = this.bibliographicReferenceService.find( testBibRef );
        assertNotNull( result );
    }

    /*
     * Class under test for Object findByExternalId(int, java.lang.String)
     */
    @Test
    public final void testFindByExternalIdentString() {
        testBibRef = bibliographicReferenceService.findByExternalId( de );
        assertNotNull( testBibRef );
    }

    /**
     * Prod has duplicate BibliographicReference rows for the same PubMed accession; the external-id
     * lookups must return one (the lowest-id) rather than throwing {@code NonUniqueResultException} (which
     * surfaced as a 500 on {@code PUT /datasets/{id}/publications}), and {@code find} must too so
     * {@code findOrCreate} doesn't add yet another duplicate.
     */
    @Test
    public final void testFindByExternalIdToleratesDuplicates() {
        String accession = de.getAccession();
        BibliographicReference dupRef = BibliographicReference.Factory.newInstance();
        dupRef.setPubAccession( DatabaseEntry.Factory.newInstance( accession, de.getExternalDatabase() ) );
        bibliographicReferenceService.create( dupRef );

        assertNotNull( bibliographicReferenceService.findByExternalId( accession, ExternalDatabases.PUBMED ) );
        assertNotNull( bibliographicReferenceService.findByExternalId( de ) );
        assertNotNull( bibliographicReferenceService.find( testBibRef ) );
    }

    @Test
    public void testGetRelatedExperiments() {
        assertThat( bibliographicReferenceService.getRelatedExperiments( Collections.singleton( testBibRef ) ) )
                .isEmpty();
        bibliographicReferenceService.getRelatedExperiments( 0, 10 );
        bibliographicReferenceService.countWithRelatedExperiments();
    }
}