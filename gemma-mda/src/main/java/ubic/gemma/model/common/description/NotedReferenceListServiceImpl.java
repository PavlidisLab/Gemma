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
package ubic.gemma.model.common.description;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.common.description.NotedReferenceListService
 */
public class NotedReferenceListServiceImpl extends ubic.gemma.model.common.description.NotedReferenceListServiceBase {

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceListService#createNewList(String,
     *      ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    protected ubic.gemma.model.common.description.NotedReferenceList handleCreateNewList( String name,
            ubic.gemma.model.common.auditAndSecurity.User owner ) throws java.lang.Exception {
        NotedReferenceList result = NotedReferenceList.Factory.newInstance();
        result.setUser( owner );
        result.setName( name );
        result = ( NotedReferenceList ) this.getNotedReferenceListDao().create( result );
        return result;
    }

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceListService#addReferenceToList(ubic.gemma.model.common.description.NotedReferenceList,
     *      ubic.gemma.model.common.description.BibliographicReference)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void handleAddReferenceToList( ubic.gemma.model.common.description.NotedReferenceList notedReferenceList,
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception {

        NotedReference newRef = NotedReference.Factory.newInstance();
        newRef.setRating( new Integer( 0 ) );
        bibliographicReference = this.getBibliographicReferenceDao().findOrCreate( bibliographicReference );
        newRef.setReference( bibliographicReference );
        notedReferenceList.getReferences().add( newRef );
        this.getNotedReferenceListDao().update( notedReferenceList );

    }

    /**
     * @see ubic.gemma.model.common.description.NotedReferenceListService#getAllReferencesForList(ubic.gemma.model.common.description.NotedReferenceList)
     */
    @Override
    protected java.util.Collection handleGetAllReferencesForList(
            ubic.gemma.model.common.description.NotedReferenceList notedReferenceList ) throws java.lang.Exception {
        return notedReferenceList.getReferences();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.NotedReferenceListServiceBase#handleSetListDescription(java.lang.String,
     *      ubic.gemma.model.common.description.NotedReferenceList)
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
     * @see ubic.gemma.model.common.description.NotedReferenceListServiceBase#handleSetComment(java.lang.String,
     *      ubic.gemma.model.common.description.NotedReference )
     */
    @Override
    protected void handleSetComment( String comment, NotedReference notedReference ) throws Exception {
        notedReference.setComment( comment );
        this.getNotedReferenceDao().update( notedReference );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.NotedReferenceListServiceBase#handleRemoveList(ubic.gemma.model.common.description.NotedReferenceList)
     */
    @Override
    protected void handleRemoveList( NotedReferenceList notedReferenceList ) throws Exception {
        this.getNotedReferenceListDao().remove( notedReferenceList );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.NotedReferenceListServiceBase#handleRemoveReferenceFromList(ubic.gemma.model.common.description.NotedReference,
     *      ubic.gemma.model.common.description.NotedReferenceList)
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
     * @see ubic.gemma.model.common.description.NotedReferenceListServiceBase#handleSetRating(java.lang.Integer,
     *      ubic.gemma.model.common.description.NotedReference )
     */
    @Override
    protected void handleSetRating( Integer rating, NotedReference notedReference ) throws Exception {
        notedReference.setRating( rating );
        this.getNotedReferenceDao().update( notedReference );
    }
}