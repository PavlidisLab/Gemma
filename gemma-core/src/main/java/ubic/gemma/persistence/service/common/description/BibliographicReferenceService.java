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
package ubic.gemma.persistence.service.common.description;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentIdAndShortName;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author kelsey
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@ParametersAreNonnullByDefault
public interface BibliographicReferenceService
        extends BaseService<BibliographicReference>, BaseVoEnabledService<BibliographicReference, BibliographicReferenceValueObject> {

    List<BibliographicReference> browse( int start, int limit );

    List<BibliographicReference> browse( int start, int limit, String orderField, boolean descending );

    /**
     * check to see if the object already exists
     *
     * @param bibliographicReference reference
     * @return reference
     */
    @Override
    BibliographicReference find( BibliographicReference bibliographicReference );

    @Override
    @Secured({ "GROUP_USER" })
    BibliographicReference findOrCreate( BibliographicReference BibliographicReference );

    @Override
    @Secured({ "GROUP_USER" })
    BibliographicReference create( BibliographicReference bibliographicReference );

    @Override
    @Secured({ "GROUP_USER" })
    Collection<BibliographicReference> create( Collection<BibliographicReference> entities );

    @Override
    @Secured({ "GROUP_USER" })
    void update( Collection<BibliographicReference> entities );

    @Override
    @Secured({ "GROUP_USER" })
    void update( BibliographicReference entity );

    @Override
    @Secured({ "GROUP_USER" })
    BibliographicReference save( BibliographicReference entity );

    @Override
    @Secured({ "GROUP_USER" })
    Collection<BibliographicReference> save( Collection<BibliographicReference> entities );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( BibliographicReference BibliographicReference );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Collection<BibliographicReference> entities );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Long id );

    @Nullable
    BibliographicReference findByExternalId( DatabaseEntry accession );

    /**
     * Get a reference by the unqualified external id.
     *
     * @param id id
     * @return reference
     */
    @Nullable
    BibliographicReference findByExternalId( java.lang.String id );

    /**
     * Retrieve a reference by identifier, qualified by the database name (such as 'pubmed').
     *
     * @param id           id
     * @param databaseName db name
     * @return reference
     */
    @Nullable
    BibliographicReference findByExternalId( java.lang.String id, java.lang.String databaseName );

    /**
     * <p>
     * Get a reference by the unqualified external id. Searches for pubmed by default
     * </p>
     *
     * @param id id
     * @return reference VO
     */
    @Nullable
    BibliographicReferenceValueObject findVOByExternalId( java.lang.String id );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" }) /* ACLs are applied in the query */
    long countDistinctWithRelatedExperiments();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" }) /* ACLs are applied in the query */
    long countWithRelatedExperiments();

    /**
     * Return all the BibRefs that are linked to ExpressionExperiments.
     *
     * @return all references with EEs
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" }) /* ACLs are applied in the query */
    Map<BibliographicReference, Set<ExpressionExperimentIdAndShortName>> getRelatedExperiments( int offset, int limit );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" }) /* ACLs are applied in the query */
    Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments( Collection<BibliographicReference> records );

    /**
     * @return all the IDs of bibliographic references in the system.
     */
    Collection<Long> listAll();

    @Nullable
    @Secured({ "GROUP_ADMIN" })
    BibliographicReference refresh( String pubMedId );

    List<BibliographicReferenceValueObject> search( String query, boolean searchExperiments, boolean searchBibrefs ) throws SearchException;

    List<BibliographicReferenceValueObject> search( String query ) throws SearchException;

    BibliographicReference thaw( BibliographicReference bibliographicReference );

    Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences );
}
