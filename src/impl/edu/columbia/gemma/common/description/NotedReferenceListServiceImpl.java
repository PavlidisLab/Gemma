/*
 * The Gemma project.
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
package edu.columbia.gemma.common.description;

/**
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.common.description.NotedReferenceListService
 */
public class NotedReferenceListServiceImpl extends edu.columbia.gemma.common.description.NotedReferenceListServiceBase {

    /**
     * @see edu.columbia.gemma.common.description.NotedReferenceListService#createNewList(String,
     *      edu.columbia.gemma.common.auditAndSecurity.User)
     */
    protected edu.columbia.gemma.common.description.NotedReferenceList handleCreateNewList( String name,
            edu.columbia.gemma.common.auditAndSecurity.User owner ) throws java.lang.Exception {
        NotedReferenceList result = NotedReferenceList.Factory.newInstance();
        result.setUser( owner );
        result.setName( name );
        result = ( NotedReferenceList ) this.getNotedReferenceListDao().create( result );
        return result;
    }

    /**
     * @see edu.columbia.gemma.common.description.NotedReferenceListService#addReferenceToList(edu.columbia.gemma.common.description.NotedReferenceList,
     *      edu.columbia.gemma.common.description.BibliographicReference)
     */
    @SuppressWarnings("unchecked")
    protected void handleAddReferenceToList(
            edu.columbia.gemma.common.description.NotedReferenceList notedReferenceList,
            edu.columbia.gemma.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception {

        NotedReference newRef = NotedReference.Factory.newInstance();
        newRef.setRating( new Integer( 0 ) );
        bibliographicReference = this.getBibliographicReferenceDao().findOrCreate( bibliographicReference );
        newRef.setReference( bibliographicReference );
        notedReferenceList.getReferences().add( newRef );
        this.getNotedReferenceListDao().update( notedReferenceList );

    }

    /**
     * @see edu.columbia.gemma.common.description.NotedReferenceListService#getAllReferencesForList(edu.columbia.gemma.common.description.NotedReferenceList)
     */
    protected java.util.Collection handleGetAllReferencesForList(
            edu.columbia.gemma.common.description.NotedReferenceList notedReferenceList ) throws java.lang.Exception {
        return notedReferenceList.getReferences();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.NotedReferenceListServiceBase#handleSetListDescription(java.lang.String,
     *      edu.columbia.gemma.common.description.NotedReferenceList)
     */
    @Override
    protected void handleSetListDescription( String description, NotedReferenceList notedReferenceList )
            throws Exception {
        notedReferenceList.setDescription( description );
        this.getNotedReferenceListDao().update( notedReferenceList );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.NotedReferenceListServiceBase#handleSetComment(java.lang.String,
     *      edu.columbia.gemma.common.description.NotedReference )
     */
    @Override
    protected void handleSetComment( String comment, NotedReference notedReference ) throws Exception {
        notedReference.setComment( comment );
        this.getNotedReferenceDao().update( notedReference );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.NotedReferenceListServiceBase#handleRemoveList(edu.columbia.gemma.common.description.NotedReferenceList)
     */
    @Override
    protected void handleRemoveList( NotedReferenceList notedReferenceList ) throws Exception {
        this.getNotedReferenceListDao().remove( notedReferenceList );

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.NotedReferenceListServiceBase#handleRemoveReferenceFromList(edu.columbia.gemma.common.description.NotedReference,
     *      edu.columbia.gemma.common.description.NotedReferenceList)
     */
    @Override
    protected void handleRemoveReferenceFromList( NotedReference notedReference, NotedReferenceList notedReferenceList )
            throws Exception {
        notedReferenceList.getReferences().remove( notedReference );
        this.getNotedReferenceListDao().update( notedReferenceList );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.common.description.NotedReferenceListServiceBase#handleSetRating(java.lang.Integer,
     *      edu.columbia.gemma.common.description.NotedReference )
     */
    @Override
    protected void handleSetRating( Integer rating, NotedReference notedReference ) throws Exception {
        notedReference.setRating( rating );
        this.getNotedReferenceDao().update( notedReference );
    }
}