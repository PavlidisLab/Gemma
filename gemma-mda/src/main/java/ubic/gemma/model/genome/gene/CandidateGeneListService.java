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

import org.springframework.security.access.annotation.Secured;

/**
 * @author kelsey
 * @version $Id$
 */
public interface CandidateGeneListService {

    /**
     * @param newName
     * @return
     */
    @Secured( { "GROUP_USER" })
    public ubic.gemma.model.genome.gene.CandidateGeneList createByName( java.lang.String newName );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<CandidateGeneList> findAll();

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<CandidateGeneList> findByContributer(
            ubic.gemma.model.common.auditAndSecurity.Person person );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<CandidateGeneList> findByGeneOfficialName( java.lang.String geneName );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ubic.gemma.model.genome.gene.CandidateGeneList findByID( java.lang.Long id );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<CandidateGeneList> findByListOwner(
            ubic.gemma.model.common.auditAndSecurity.Person owner );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void removeCandidateGeneList( ubic.gemma.model.genome.gene.CandidateGeneList candidateGeneList );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public ubic.gemma.model.genome.gene.CandidateGeneList saveCandidateGeneList(
            ubic.gemma.model.genome.gene.CandidateGeneList candidateGeneList );

    /**
     * 
     */
    public void setActor( ubic.gemma.model.common.auditAndSecurity.User actor );

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void updateCandidateGeneList( ubic.gemma.model.genome.gene.CandidateGeneList candidateList );

}
