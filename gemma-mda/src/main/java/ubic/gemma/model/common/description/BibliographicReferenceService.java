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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author kelsey
 * @version $Id$
 */
public interface BibliographicReferenceService {

    /**
     * <p>
     * Adds a document (in PDF format) for the reference.
     * </p>
     */
    @Secured( { "GROUP_USER" })
    public void addPDF( ubic.gemma.model.common.description.LocalFile pdfFile,
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ubic.gemma.model.common.description.BibliographicReference create(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * <p>
     * check to see if the object already exists
     * </p>
     */
    public ubic.gemma.model.common.description.BibliographicReference find(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * <p>
     * Get a reference by the unqualified external id.
     * </p>
     */
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId( java.lang.String id );

    /**
     * Retrieve a reference by identifier, qualified by the database name (such as 'pubmed').
     */
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId( java.lang.String id,
            java.lang.String databaseName );

    
    /**
     * F
     * @param accession
     * @return
     */
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId( DatabaseEntry accession );
    
    /**
     * 
     */
    public ubic.gemma.model.common.description.BibliographicReference findByTitle( java.lang.String title );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ubic.gemma.model.common.description.BibliographicReference findOrCreate(
            ubic.gemma.model.common.description.BibliographicReference BibliographicReference );

    /**
     * Return all the BibRefs that are linked to ExpressionExperiments.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public java.util.Map<ExpressionExperiment, BibliographicReference> getAllExperimentLinkedReferences();

    /**
     * Get the ExpressionExperiments, if any, that are linked to the given reference.
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperiment> getRelatedExperiments(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * 
     */
    public ubic.gemma.model.common.description.BibliographicReference load( java.lang.Long id );

    /**
     * 
     */
    public java.util.Collection<BibliographicReference> loadMultiple( java.util.Collection<Long> ids );

    /**
     * 
     */
    @Secured( { "GROUP_ADMIN" })
    public void remove( ubic.gemma.model.common.description.BibliographicReference BibliographicReference );

    /**
     * 
     */
    @Secured( { "GROUP_ADMIN" })
    public void update( ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

}
