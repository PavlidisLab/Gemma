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
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author pavlidis
 */
public class BibliographicReferenceServiceImplTest extends BaseSpringContextTest {

    private BibliographicReferenceServiceImpl svc = null;
    private BibliographicReferenceReadServiceImpl readSvc = null;
    private DatabaseEntry de = null;

    @Mock
    private BibliographicReferenceDao brdao;
    @Mock
    private SearchService searchService;
    @Mock
    private ExpressionExperimentService expressionExperimentService;

    @Before
    public void setUp() throws Exception {

        readSvc = new BibliographicReferenceReadServiceImpl( brdao, searchService, expressionExperimentService );
        svc = new BibliographicReferenceServiceImpl( brdao );
        // facade @Autowired fields aren't wired for hand-constructed instances
        ReflectionTestUtils.setField( svc, "bibliographicReferenceReadService", readSvc );
        ReflectionTestUtils.setField( svc, "expressionExperimentService", expressionExperimentService );

        ExternalDatabase extDB = ExternalDatabase.Factory.newInstance();
        extDB.setName( ExternalDatabases.PUBMED );

        de = DatabaseEntry.Factory.newInstance();
        de.setAccession( "12345" );
        de.setExternalDatabase( extDB );
    }

    @Test
    public final void testFindByExternalId() {

        BibliographicReference mockBR = BibliographicReference.Factory.newInstance();
        mockBR.setPubAccession( de );
        mockBR.setTitle( "My Title" );
        when( brdao.findByExternalId( "12345", ExternalDatabases.PUBMED ) ).thenReturn( mockBR );

        svc.findByExternalId( "12345", ExternalDatabases.PUBMED );
        verify( brdao ).findByExternalId( "12345", ExternalDatabases.PUBMED );
    }

}