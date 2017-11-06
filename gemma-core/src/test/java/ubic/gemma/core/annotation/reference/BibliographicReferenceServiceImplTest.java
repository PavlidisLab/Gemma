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
package ubic.gemma.core.annotation.reference;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.testing.BaseSpringContextTest;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import static org.easymock.EasyMock.*;

/**
 * @author pavlidis
 */
public class BibliographicReferenceServiceImplTest extends BaseSpringContextTest {

    @Autowired
    private PhenotypeAssociationService pas;

    @Autowired
    private ExpressionExperimentService ees;

    private BibliographicReferenceServiceImpl svc = null;
    private BibliographicReferenceDao brdao = null;
    private DatabaseEntry de = null;
    private ExternalDatabase extDB = null;

    @Before
    public void setUp() throws Exception {

        brdao = createMock( BibliographicReferenceDao.class );

        svc = new BibliographicReferenceServiceImpl( brdao, pas, ees );

        extDB = ExternalDatabase.Factory.newInstance();
        extDB.setName( "PUBMED" );

        de = DatabaseEntry.Factory.newInstance();
        de.setAccession( "12345" );
        de.setExternalDatabase( extDB );
    }

    @Test
    public final void testFindByExternalId() {

        BibliographicReference mockBR = BibliographicReference.Factory.newInstance();
        mockBR.setPubAccession( de );
        mockBR.setTitle( "My Title" );
        brdao.findByExternalId( "12345", "PUBMED" );
        expectLastCall().andReturn( mockBR );

        replay( brdao );
        svc.findByExternalId( "12345", "PUBMED" );
        verify( brdao );
    }

}