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
package ubic.gemma.persistence.service.genome.gene;

import org.springframework.lang.Nullable;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.SessionBoundGeneSetValueObject;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Read-only retrieval service for {@link GeneSet}.
 * <p>
 * Phase 3 of the {@link GeneSetService} decomposition (strangler fig). This service
 * houses the DAO-bound read cluster previously implemented directly on the
 * {@code GeneSetServiceImpl} facade: {@code loadWithMembers}, {@code findByGene},
 * {@code loadValueObjectByIdLite}, {@code loadValueObjectsByIdsLite},
 * {@code findByName} (x2), {@code loadAll(Taxon)}, {@code loadMyGeneSets} (x2),
 * {@code loadMySharedGeneSets}, {@code getGenesInGroup}, {@code getGeneIdsInGroup},
 * {@code getSize}, {@code getTaxonVOforGeneSetVO}, {@code getTaxon}, and
 * {@code getTaxa}. All methods delegate to {@link GeneSetDao} (with simple
 * {@link org.hibernate.Hibernate#initialize(Object)} wrapping where appropriate) and
 * the {@link GeneService} where needed for taxon resolution.
 * <p>
 * Methods that orchestrate across collaborators -- {@code findGeneSetsByGene},
 * {@code findGeneSetsByName}, {@code getUsersGeneGroups},
 * {@code getUsersGeneGroupsValueObjects} -- stay on the facade because they
 * depend on {@code GeneSetSearch}, {@code GeneSetValueObjectHelper}, and
 * {@code SecurityService} and so are out of scope for a DAO-bound read slice.
 * <p>
 * Write-side methods ({@code createDatabaseEntity}, {@code updateDatabaseEntity*},
 * {@code deleteDatabaseEntity*}, {@code removeAll}, plus the inherited
 * {@code BaseService} mutators) stay on the {@link GeneSetService} facade.
 * <p>
 * Callers should generally keep using {@link GeneSetService} as the facade --
 * the facade delegates to this service. Direct injection is appropriate where
 * a class is logically read-only AND the read method carries no ACL filter-chain
 * annotation. The facade interface declares
 * {@code @PostFilter("hasPermission(...)")} on several entity-returning reads
 * ({@code loadWithMembers}, {@code findByGene}, {@code findByName}, {@code loadAll}),
 * {@code AFTER_ACL_FILTER_MY_DATA} on the {@code loadMyGeneSets*} cluster, and
 * {@code AFTER_ACL_VALUE_OBJECT_*} on the VO-returning reads. Those checks fire at
 * the facade proxy boundary; intra-{@code gemma-core} callers that inject this
 * service directly bypass the duplicate ACL check and must therefore be reads
 * that don't require permission filtering (e.g. {@code getTaxon}, {@code getTaxa},
 * {@code getTaxonVOforGeneSetVO}, which carry no {@code @Secured} on the facade).
 *
 * @see GeneSetService
 */
public interface GeneSetReadService {

    Collection<GeneSet> loadWithMembers( Collection<Long> ids );

    Collection<GeneSet> findByGene( Gene gene );

    @Nullable
    DatabaseBackedGeneSetValueObject loadValueObjectByIdLite( Long id );

    List<DatabaseBackedGeneSetValueObject> loadValueObjectsByIdsLite( Collection<Long> geneSetIds );

    Collection<GeneSet> findByName( String name );

    Collection<GeneSet> findByName( String name, Taxon taxon );

    Collection<GeneSet> loadAll( @Nullable Taxon tax );

    Collection<GeneSet> loadMyGeneSets();

    Collection<GeneSet> loadMyGeneSets( Taxon tax );

    Collection<GeneSet> loadMySharedGeneSets( Taxon tax );

    Collection<GeneValueObject> getGenesInGroup( GeneSetValueObject object );

    Collection<Long> getGeneIdsInGroup( GeneSetValueObject geneSetVO );

    int getSize( GeneSetValueObject geneSetVO );

    TaxonValueObject getTaxonVOforGeneSetVO( SessionBoundGeneSetValueObject geneSetVO );

    @Nullable
    Taxon getTaxon( GeneSet geneSet );

    Set<Taxon> getTaxa( GeneSet geneSet );
}
