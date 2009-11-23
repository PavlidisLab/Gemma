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

import org.springframework.security.access.annotation.Secured;

/**
 * @author kelsey
 * @version $Id$
 */
public interface NotedReferenceListService {

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void addReferenceToList( ubic.gemma.model.common.description.NotedReferenceList notedReferenceList,
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public ubic.gemma.model.common.description.NotedReferenceList createNewList( java.lang.String name,
            ubic.gemma.model.common.auditAndSecurity.User owner );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<NotedReference> getAllReferencesForList(
            ubic.gemma.model.common.description.NotedReferenceList notedReferenceList );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void removeList( ubic.gemma.model.common.description.NotedReferenceList notedReferenceList );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void removeReferenceFromList( ubic.gemma.model.common.description.NotedReference notedReference,
            ubic.gemma.model.common.description.NotedReferenceList notedReferenceList );

    /**
     * <p>
     * Set the comment for a reference.
     * </p>
     */
    @Secured( { "GROUP_USER" })
    public void setComment( java.lang.String comment, ubic.gemma.model.common.description.NotedReference notedReference );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void setListDescription( java.lang.String description,
            ubic.gemma.model.common.description.NotedReferenceList notedReferenceList );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public void setRating( java.lang.Integer rating, ubic.gemma.model.common.description.NotedReference notedReference );

}
