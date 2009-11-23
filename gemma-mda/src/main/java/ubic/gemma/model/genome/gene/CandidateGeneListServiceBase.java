/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.genome.gene;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.genome.gene.CandidateGeneListService</code>, provides access to
 * all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.genome.gene.CandidateGeneListService
 */
public abstract class CandidateGeneListServiceBase implements ubic.gemma.model.genome.gene.CandidateGeneListService {

    @Autowired
    private ubic.gemma.model.genome.gene.GeneService geneService;

    @Autowired
    private ubic.gemma.model.genome.gene.CandidateGeneListDao candidateGeneListDao;

    @Autowired
    private ubic.gemma.model.genome.gene.CandidateGeneDao candidateGeneDao;

    @Autowired
    private ubic.gemma.model.genome.GeneDao geneDao;

    @Autowired
    private ubic.gemma.model.common.auditAndSecurity.AuditEventDao auditEventDao;

    @Autowired
    private ubic.gemma.model.common.auditAndSecurity.AuditTrailDao auditTrailDao;

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListService#createByName(java.lang.String)
     */
    public ubic.gemma.model.genome.gene.CandidateGeneList createByName( final java.lang.String newName ) {
        try {
            return this.handleCreateByName( newName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.CandidateGeneListServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.CandidateGeneListService.createByName(java.lang.String newName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListService#findAll()
     */
    public java.util.Collection findAll() {
        try {
            return this.handleFindAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.CandidateGeneListServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.CandidateGeneListService.findAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListService#findByContributer(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    public java.util.Collection findByContributer( final ubic.gemma.model.common.auditAndSecurity.Person person ) {
        try {
            return this.handleFindByContributer( person );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.CandidateGeneListServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.CandidateGeneListService.findByContributer(ubic.gemma.model.common.auditAndSecurity.Person person)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListService#findByGeneOfficialName(java.lang.String)
     */
    public java.util.Collection findByGeneOfficialName( final java.lang.String geneName ) {
        try {
            return this.handleFindByGeneOfficialName( geneName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.CandidateGeneListServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.CandidateGeneListService.findByGeneOfficialName(java.lang.String geneName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListService#findByID(java.lang.Long)
     */
    public ubic.gemma.model.genome.gene.CandidateGeneList findByID( final java.lang.Long id ) {
        try {
            return this.handleFindByID( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.CandidateGeneListServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.CandidateGeneListService.findByID(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListService#findByListOwner(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    public java.util.Collection findByListOwner( final ubic.gemma.model.common.auditAndSecurity.Person owner ) {
        try {
            return this.handleFindByListOwner( owner );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.CandidateGeneListServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.CandidateGeneListService.findByListOwner(ubic.gemma.model.common.auditAndSecurity.Person owner)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListService#removeCandidateGeneList(ubic.gemma.model.genome.gene.CandidateGeneList)
     */
    public void removeCandidateGeneList( final ubic.gemma.model.genome.gene.CandidateGeneList candidateGeneList ) {
        try {
            this.handleRemoveCandidateGeneList( candidateGeneList );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.CandidateGeneListServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.CandidateGeneListService.removeCandidateGeneList(ubic.gemma.model.genome.gene.CandidateGeneList candidateGeneList)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListService#saveCandidateGeneList(ubic.gemma.model.genome.gene.CandidateGeneList)
     */
    public ubic.gemma.model.genome.gene.CandidateGeneList saveCandidateGeneList(
            final ubic.gemma.model.genome.gene.CandidateGeneList candidateGeneList ) {
        try {
            return this.handleSaveCandidateGeneList( candidateGeneList );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.CandidateGeneListServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.CandidateGeneListService.saveCandidateGeneList(ubic.gemma.model.genome.gene.CandidateGeneList candidateGeneList)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListService#setActor(ubic.gemma.model.common.auditAndSecurity.User)
     */
    public void setActor( final ubic.gemma.model.common.auditAndSecurity.User actor ) {
        try {
            this.handleSetActor( actor );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.CandidateGeneListServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.CandidateGeneListService.setActor(ubic.gemma.model.common.auditAndSecurity.User actor)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>auditEvent</code>'s DAO.
     */
    public void setAuditEventDao( ubic.gemma.model.common.auditAndSecurity.AuditEventDao auditEventDao ) {
        this.auditEventDao = auditEventDao;
    }

    /**
     * Sets the reference to <code>auditTrail</code>'s DAO.
     */
    public void setAuditTrailDao( ubic.gemma.model.common.auditAndSecurity.AuditTrailDao auditTrailDao ) {
        this.auditTrailDao = auditTrailDao;
    }

    /**
     * Sets the reference to <code>candidateGene</code>'s DAO.
     */
    public void setCandidateGeneDao( ubic.gemma.model.genome.gene.CandidateGeneDao candidateGeneDao ) {
        this.candidateGeneDao = candidateGeneDao;
    }

    /**
     * Sets the reference to <code>candidateGeneList</code>'s DAO.
     */
    public void setCandidateGeneListDao( ubic.gemma.model.genome.gene.CandidateGeneListDao candidateGeneListDao ) {
        this.candidateGeneListDao = candidateGeneListDao;
    }

    /**
     * Sets the reference to <code>gene</code>'s DAO.
     */
    public void setGeneDao( ubic.gemma.model.genome.GeneDao geneDao ) {
        this.geneDao = geneDao;
    }

    /**
     * Sets the reference to <code>geneService</code>.
     */
    public void setGeneService( ubic.gemma.model.genome.gene.GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @see ubic.gemma.model.genome.gene.CandidateGeneListService#updateCandidateGeneList(ubic.gemma.model.genome.gene.CandidateGeneList)
     */
    public void updateCandidateGeneList( final ubic.gemma.model.genome.gene.CandidateGeneList candidateList ) {
        try {
            this.handleUpdateCandidateGeneList( candidateList );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.gene.CandidateGeneListServiceException(
                    "Error performing 'ubic.gemma.model.genome.gene.CandidateGeneListService.updateCandidateGeneList(ubic.gemma.model.genome.gene.CandidateGeneList candidateList)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>auditEvent</code>'s DAO.
     */
    protected ubic.gemma.model.common.auditAndSecurity.AuditEventDao getAuditEventDao() {
        return this.auditEventDao;
    }

    /**
     * Gets the reference to <code>auditTrail</code>'s DAO.
     */
    protected ubic.gemma.model.common.auditAndSecurity.AuditTrailDao getAuditTrailDao() {
        return this.auditTrailDao;
    }

    /**
     * Gets the reference to <code>candidateGene</code>'s DAO.
     */
    protected ubic.gemma.model.genome.gene.CandidateGeneDao getCandidateGeneDao() {
        return this.candidateGeneDao;
    }

    /**
     * Gets the reference to <code>candidateGeneList</code>'s DAO.
     */
    protected ubic.gemma.model.genome.gene.CandidateGeneListDao getCandidateGeneListDao() {
        return this.candidateGeneListDao;
    }

    /**
     * Gets the reference to <code>gene</code>'s DAO.
     */
    protected ubic.gemma.model.genome.GeneDao getGeneDao() {
        return this.geneDao;
    }

    /**
     * Gets the reference to <code>geneService</code>.
     */
    protected ubic.gemma.model.genome.gene.GeneService getGeneService() {
        return this.geneService;
    }

    /**
     * Performs the core logic for {@link #createByName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.genome.gene.CandidateGeneList handleCreateByName( java.lang.String newName )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findAll()}
     */
    protected abstract java.util.Collection handleFindAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByContributer(ubic.gemma.model.common.auditAndSecurity.Person)}
     */
    protected abstract java.util.Collection handleFindByContributer(
            ubic.gemma.model.common.auditAndSecurity.Person person ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGeneOfficialName(java.lang.String)}
     */
    protected abstract java.util.Collection handleFindByGeneOfficialName( java.lang.String geneName )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByID(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.genome.gene.CandidateGeneList handleFindByID( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByListOwner(ubic.gemma.model.common.auditAndSecurity.Person)}
     */
    protected abstract java.util.Collection handleFindByListOwner( ubic.gemma.model.common.auditAndSecurity.Person owner )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #removeCandidateGeneList(ubic.gemma.model.genome.gene.CandidateGeneList)}
     */
    protected abstract void handleRemoveCandidateGeneList(
            ubic.gemma.model.genome.gene.CandidateGeneList candidateGeneList ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #saveCandidateGeneList(ubic.gemma.model.genome.gene.CandidateGeneList)}
     */
    protected abstract ubic.gemma.model.genome.gene.CandidateGeneList handleSaveCandidateGeneList(
            ubic.gemma.model.genome.gene.CandidateGeneList candidateGeneList ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #setActor(ubic.gemma.model.common.auditAndSecurity.User)}
     */
    protected abstract void handleSetActor( ubic.gemma.model.common.auditAndSecurity.User actor )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #updateCandidateGeneList(ubic.gemma.model.genome.gene.CandidateGeneList)}
     */
    protected abstract void handleUpdateCandidateGeneList( ubic.gemma.model.genome.gene.CandidateGeneList candidateList )
            throws java.lang.Exception;

}