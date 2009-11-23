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
package ubic.gemma.model.common.description;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.description.NotedReferenceListService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.NotedReferenceListService
 */
public abstract class NotedReferenceListServiceBase implements
        ubic.gemma.model.common.description.NotedReferenceListService {

    @Autowired
    private ubic.gemma.model.common.description.NotedReferenceDao notedReferenceDao;

    @Autowired
    private ubic.gemma.model.common.description.NotedReferenceListDao notedReferenceListDao;

    @Autowired
    private ubic.gemma.model.common.description.BibliographicReferenceDao bibliographicReferenceDao;

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceListService#addReferenceToList(ubic.gemma.model.common.description.NotedReferenceList,
     *      ubic.gemma.model.common.description.BibliographicReference)
     */
    public void addReferenceToList( final ubic.gemma.model.common.description.NotedReferenceList notedReferenceList,
            final ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {
        try {
            this.handleAddReferenceToList( notedReferenceList, bibliographicReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.NotedReferenceListServiceException(
                    "Error performing 'ubic.gemma.model.common.description.NotedReferenceListService.addReferenceToList(ubic.gemma.model.common.description.NotedReferenceList notedReferenceList, ubic.gemma.model.common.description.BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceListService#createNewList(java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.User)
     */
    public ubic.gemma.model.common.description.NotedReferenceList createNewList( final java.lang.String name,
            final ubic.gemma.model.common.auditAndSecurity.User owner ) {
        try {
            return this.handleCreateNewList( name, owner );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.NotedReferenceListServiceException(
                    "Error performing 'ubic.gemma.model.common.description.NotedReferenceListService.createNewList(java.lang.String name, ubic.gemma.model.common.auditAndSecurity.User owner)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceListService#getAllReferencesForList(ubic.gemma.model.common.description.NotedReferenceList)
     */
    public java.util.Collection getAllReferencesForList(
            final ubic.gemma.model.common.description.NotedReferenceList notedReferenceList ) {
        try {
            return this.handleGetAllReferencesForList( notedReferenceList );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.NotedReferenceListServiceException(
                    "Error performing 'ubic.gemma.model.common.description.NotedReferenceListService.getAllReferencesForList(ubic.gemma.model.common.description.NotedReferenceList notedReferenceList)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceListService#removeList(ubic.gemma.model.common.description.NotedReferenceList)
     */
    public void removeList( final ubic.gemma.model.common.description.NotedReferenceList notedReferenceList ) {
        try {
            this.handleRemoveList( notedReferenceList );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.NotedReferenceListServiceException(
                    "Error performing 'ubic.gemma.model.common.description.NotedReferenceListService.removeList(ubic.gemma.model.common.description.NotedReferenceList notedReferenceList)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceListService#removeReferenceFromList(ubic.gemma.model.common.description.NotedReference,
     *      ubic.gemma.model.common.description.NotedReferenceList)
     */
    public void removeReferenceFromList( final ubic.gemma.model.common.description.NotedReference notedReference,
            final ubic.gemma.model.common.description.NotedReferenceList notedReferenceList ) {
        try {
            this.handleRemoveReferenceFromList( notedReference, notedReferenceList );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.NotedReferenceListServiceException(
                    "Error performing 'ubic.gemma.model.common.description.NotedReferenceListService.removeReferenceFromList(ubic.gemma.model.common.description.NotedReference notedReference, ubic.gemma.model.common.description.NotedReferenceList notedReferenceList)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>bibliographicReference</code>'s DAO.
     */
    public void setBibliographicReferenceDao(
            ubic.gemma.model.common.description.BibliographicReferenceDao bibliographicReferenceDao ) {
        this.bibliographicReferenceDao = bibliographicReferenceDao;
    }

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceListService#setComment(java.lang.String,
     *      ubic.gemma.model.common.description.NotedReference)
     */
    public void setComment( final java.lang.String comment,
            final ubic.gemma.model.common.description.NotedReference notedReference ) {
        try {
            this.handleSetComment( comment, notedReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.NotedReferenceListServiceException(
                    "Error performing 'ubic.gemma.model.common.description.NotedReferenceListService.setComment(java.lang.String comment, ubic.gemma.model.common.description.NotedReference notedReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceListService#setListDescription(java.lang.String,
     *      ubic.gemma.model.common.description.NotedReferenceList)
     */
    public void setListDescription( final java.lang.String description,
            final ubic.gemma.model.common.description.NotedReferenceList notedReferenceList ) {
        try {
            this.handleSetListDescription( description, notedReferenceList );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.NotedReferenceListServiceException(
                    "Error performing 'ubic.gemma.model.common.description.NotedReferenceListService.setListDescription(java.lang.String description, ubic.gemma.model.common.description.NotedReferenceList notedReferenceList)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>notedReference</code>'s DAO.
     */
    public void setNotedReferenceDao( ubic.gemma.model.common.description.NotedReferenceDao notedReferenceDao ) {
        this.notedReferenceDao = notedReferenceDao;
    }

    /**
     * Sets the reference to <code>notedReferenceList</code>'s DAO.
     */
    public void setNotedReferenceListDao(
            ubic.gemma.model.common.description.NotedReferenceListDao notedReferenceListDao ) {
        this.notedReferenceListDao = notedReferenceListDao;
    }

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceListService#setRating(java.lang.Integer,
     *      ubic.gemma.model.common.description.NotedReference)
     */
    public void setRating( final java.lang.Integer rating,
            final ubic.gemma.model.common.description.NotedReference notedReference ) {
        try {
            this.handleSetRating( rating, notedReference );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.NotedReferenceListServiceException(
                    "Error performing 'ubic.gemma.model.common.description.NotedReferenceListService.setRating(java.lang.Integer rating, ubic.gemma.model.common.description.NotedReference notedReference)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>bibliographicReference</code>'s DAO.
     */
    protected ubic.gemma.model.common.description.BibliographicReferenceDao getBibliographicReferenceDao() {
        return this.bibliographicReferenceDao;
    }

    /**
     * Gets the reference to <code>notedReference</code>'s DAO.
     */
    protected ubic.gemma.model.common.description.NotedReferenceDao getNotedReferenceDao() {
        return this.notedReferenceDao;
    }

    /**
     * Gets the reference to <code>notedReferenceList</code>'s DAO.
     */
    protected ubic.gemma.model.common.description.NotedReferenceListDao getNotedReferenceListDao() {
        return this.notedReferenceListDao;
    }

    /**
     * Performs the core logic for
     * {@link #addReferenceToList(ubic.gemma.model.common.description.NotedReferenceList, ubic.gemma.model.common.description.BibliographicReference)}
     */
    protected abstract void handleAddReferenceToList(
            ubic.gemma.model.common.description.NotedReferenceList notedReferenceList,
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #createNewList(java.lang.String, ubic.gemma.model.common.auditAndSecurity.User)}
     */
    protected abstract ubic.gemma.model.common.description.NotedReferenceList handleCreateNewList(
            java.lang.String name, ubic.gemma.model.common.auditAndSecurity.User owner ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getAllReferencesForList(ubic.gemma.model.common.description.NotedReferenceList)}
     */
    protected abstract java.util.Collection handleGetAllReferencesForList(
            ubic.gemma.model.common.description.NotedReferenceList notedReferenceList ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #removeList(ubic.gemma.model.common.description.NotedReferenceList)}
     */
    protected abstract void handleRemoveList( ubic.gemma.model.common.description.NotedReferenceList notedReferenceList )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #removeReferenceFromList(ubic.gemma.model.common.description.NotedReference, ubic.gemma.model.common.description.NotedReferenceList)}
     */
    protected abstract void handleRemoveReferenceFromList(
            ubic.gemma.model.common.description.NotedReference notedReference,
            ubic.gemma.model.common.description.NotedReferenceList notedReferenceList ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #setComment(java.lang.String, ubic.gemma.model.common.description.NotedReference)}
     */
    protected abstract void handleSetComment( java.lang.String comment,
            ubic.gemma.model.common.description.NotedReference notedReference ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #setListDescription(java.lang.String, ubic.gemma.model.common.description.NotedReferenceList)}
     */
    protected abstract void handleSetListDescription( java.lang.String description,
            ubic.gemma.model.common.description.NotedReferenceList notedReferenceList ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #setRating(java.lang.Integer, ubic.gemma.model.common.description.NotedReference)}
     */
    protected abstract void handleSetRating( java.lang.Integer rating,
            ubic.gemma.model.common.description.NotedReference notedReference ) throws java.lang.Exception;

}