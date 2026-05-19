/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.common.description;

import org.springframework.lang.Nullable;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentIdAndShortName;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only retrieval service for {@link BibliographicReference}.
 * <p>
 * Phase 3 of the {@link BibliographicReferenceService} decomposition (strangler fig). This
 * service houses the read cluster previously implemented directly on the
 * {@code BibliographicReferenceServiceImpl} facade: {@code browse}, {@code findByExternalId},
 * {@code findVOByExternalId}, {@code countDistinctWithRelatedExperiments},
 * {@code countWithRelatedExperiments}, {@code getRelatedExperiments}, {@code listAll},
 * {@code search}, and {@code thaw}. Methods delegate to {@link BibliographicReferenceDao}
 * (plus {@code SearchService} and {@code ExpressionExperimentService} where the original
 * facade methods needed them to populate related-experiment VOs).
 * <p>
 * Write-side methods ({@code create}, {@code save}, {@code update}, {@code remove},
 * {@code refresh}, {@code findOrCreate}, plus the inherited {@code BaseService} mutators)
 * stay on the {@link BibliographicReferenceService} facade.
 * <p>
 * Callers should generally keep using {@link BibliographicReferenceService} as the facade --
 * the facade delegates to this service. Direct injection is appropriate where a class is
 * logically read-only (REST endpoints, CLIs, browser controllers, intra-core readers).
 * <p>
 * ACL / {@code @Secured} annotations live on {@link BibliographicReferenceService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary,
 * so this interface is intentionally unsecured.
 *
 * @see BibliographicReferenceService
 */
@ParametersAreNonnullByDefault
public interface BibliographicReferenceReadService {

    List<BibliographicReference> browse( int start, int limit );

    List<BibliographicReference> browse( int start, int limit, String orderField, boolean descending );

    @Nullable
    BibliographicReference findByExternalId( DatabaseEntry accession );

    /**
     * Get a reference by the unqualified external id. Searches for pubmed by default.
     */
    @Nullable
    BibliographicReference findByExternalId( String id );

    /**
     * Retrieve a reference by identifier, qualified by the database name (such as 'pubmed').
     */
    @Nullable
    BibliographicReference findByExternalId( String id, String databaseName );

    /**
     * Get a reference VO by the unqualified external id. Searches for pubmed by default.
     */
    @Nullable
    BibliographicReferenceValueObject findVOByExternalId( String id );

    long countDistinctWithRelatedExperiments();

    long countWithRelatedExperiments();

    Map<BibliographicReference, Set<ExpressionExperimentIdAndShortName>> getRelatedExperiments( int offset, int limit );

    Map<BibliographicReference, Collection<ExpressionExperiment>> getRelatedExperiments( Collection<BibliographicReference> records );

    /**
     * @return all the IDs of bibliographic references in the system.
     */
    Collection<Long> listAll();

    List<BibliographicReferenceValueObject> search( String query, boolean searchExperiments, boolean searchBibrefs ) throws SearchException;

    List<BibliographicReferenceValueObject> search( String query ) throws SearchException;

    BibliographicReference thaw( BibliographicReference bibliographicReference );

    Collection<BibliographicReference> thaw( Collection<BibliographicReference> bibliographicReferences );
}
