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
package ubic.gemma.core.annotation.reference;

import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author kelsey
 */
public interface BibliographicReferenceService extends BaseVoEnabledService<BibliographicReference, BibliographicReferenceValueObject> {

    /**
     * Adds a document (in PDF format) for the reference.
     */
    @Secured({ "GROUP_USER" })
    void addPDF( LocalFile pdfFile, BibliographicReference bibliographicReference );

    List<BibliographicReference> browse( Integer start, Integer limit );

    List<BibliographicReference> browse( Integer start, Integer limit, String orderField, boolean descending );

    @Secured({ "GROUP_USER" })
    BibliographicReference create( BibliographicReference bibliographicReference );

    /**
     * check to see if the object already exists
     */
    BibliographicReference find( BibliographicReference bibliographicReference );

    BibliographicReference findByExternalId( DatabaseEntry accession );

    /**
     * Get a reference by the unqualified external id.
     */
    BibliographicReference findByExternalId( java.lang.String id );

    /**
     * Retrieve a reference by identifier, qualified by the database name (such as 'pubmed').
     */
    BibliographicReference findByExternalId( java.lang.String id, java.lang.String databaseName );

    @Secured({ "GROUP_USER" })
    BibliographicReference findOrCreate( BibliographicReference BibliographicReference );

    /**
     * <p>
     * Get a reference by the unqualified external id. Searches for pubmed by default
     * </p>
     */
    BibliographicReferenceValueObject findVOByExternalId( java.lang.String id );

    /**
     * Return all the BibRefs that are linked to ExpressionExperiments.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    java.util.Map<ExpressionExperiment, BibliographicReference> getAllExperimentLinkedReferences();

    /**
     * Get the ExpressionExperiments, if any, that are linked to the given reference.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    java.util.Collection<ExpressionExperiment> getRelatedExperiments( BibliographicReference bibliographicReference );

    Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments(
            Collection<BibliographicReference> records );

    /**
     * @return all the IDs of bibliographic references in the system.
     */
    Collection<Long> listAll();

    @Secured({ "GROUP_ADMIN" })
    BibliographicReference refresh( String pubMedId );

    @Secured({ "GROUP_ADMIN" })
    void remove( BibliographicReference BibliographicReference );

    List<BibliographicReferenceValueObject> search( SearchSettingsValueObject settings );

    List<BibliographicReferenceValueObject> search( String query );

    @Secured({ "GROUP_ADMIN" })
    void update( BibliographicReference bibliographicReference );

}
