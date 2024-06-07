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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;

import static org.junit.Assert.assertNotNull;

/**
 * This class tests the bibliographic reference data access object. It is also used to test some of the Hibernate
 * features.
 * 
 * @author pavlidis
 *
 */
public class BibliographicReferenceServiceTest extends BaseSpringContextTest {

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;
    private DatabaseEntry de = null;
    private BibliographicReference testBibRef = null;

    /*
     * Call to create should persist the BibliographicReference and DatabaseEntry (cascade=all).
     */
    @Before
    public void setUp() throws Exception {

        testBibRef = BibliographicReference.Factory.newInstance();

        de = this.getTestPersistentDatabaseEntry( "PubMed" );

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

}