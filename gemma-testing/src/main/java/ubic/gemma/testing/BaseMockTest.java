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
package ubic.gemma.testing;

import static org.easymock.EasyMock.createMock;
import junit.framework.TestCase;
import ubic.gemma.model.common.auditAndSecurity.UserDao;
import ubic.gemma.model.common.auditAndSecurity.UserRoleDao;
import ubic.gemma.model.common.description.BibliographicReferenceDao;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.TaxonDao;
import ubic.gemma.model.genome.gene.CandidateGeneDao;
import ubic.gemma.model.genome.gene.CandidateGeneListDao;

/**
 * Subclass this test if you need mock domain objects.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class BaseMockTest extends TestCase {

    private CandidateGeneDao candidateGeneDao;
    private CandidateGeneListDao candidateGeneListDao;
    private BibliographicReferenceDao bibliographicReferenceDao;
    private ExternalDatabaseDao externalDatabaseDao;
    private UserRoleDao userRoleDao;
    private ArrayDesignDao arrayDesignDao;
    private GeneDao geneDao;
    private UserDao userDao;
    private TaxonDao taxonDao;

    public BaseMockTest() {
        userDao = createMock( UserDao.class );
        taxonDao = createMock( TaxonDao.class );
        geneDao = createMock( GeneDao.class );
        candidateGeneDao = createMock( CandidateGeneDao.class );
        candidateGeneListDao = createMock( CandidateGeneListDao.class );
        arrayDesignDao = createMock( ArrayDesignDao.class );
        bibliographicReferenceDao = createMock( BibliographicReferenceDao.class );
        userRoleDao = createMock( UserRoleDao.class );
        externalDatabaseDao = createMock( ExternalDatabaseDao.class );
    }

    /**
     * @return Returns the arrayDesignDao.
     */
    public ArrayDesignDao getArrayDesignDao() {
        return this.arrayDesignDao;
    }

    /**
     * @return Returns the bibliographicReferenceDao.
     */
    public BibliographicReferenceDao getBibliographicReferenceDao() {
        return this.bibliographicReferenceDao;
    }

    /**
     * @return Returns the candidateGeneDao.
     */
    public CandidateGeneDao getCandidateGeneDao() {
        return this.candidateGeneDao;
    }

    /**
     * @return Returns the candidateGeneListDao.
     */
    public CandidateGeneListDao getCandidateGeneListDao() {
        return this.candidateGeneListDao;
    }

    /**
     * @return Returns the externalDatabaseDao.
     */
    public ExternalDatabaseDao getExternalDatabaseDao() {
        return this.externalDatabaseDao;
    }

    /**
     * @return Returns the geneDao.
     */
    public GeneDao getGeneDao() {
        return this.geneDao;
    }

    /**
     * @return Returns the taxonDao.
     */
    public TaxonDao getTaxonDao() {
        return this.taxonDao;
    }

    /**
     * @return Returns the userDao.
     */
    public UserDao getUserDao() {
        return this.userDao;
    }

    /**
     * @return Returns the userRoleDao.
     */
    public UserRoleDao getUserRoleDao() {
        return this.userRoleDao;
    }

}
