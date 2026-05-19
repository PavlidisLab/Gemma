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

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

import org.springframework.lang.Nullable;
import javax.annotation.CheckReturnValue;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Read-only retrieval service for {@link Gene}.
 * <p>
 * Phase 3 of the {@link GeneService} decomposition (strangler fig). This service houses
 * the DAO-bound read cluster previously implemented directly on the
 * {@code GeneServiceImpl} facade: the {@code find...}, {@code getCompositeSequence...},
 * {@code getProducts}, {@code getPhysicalLocationsValueObjects}, {@code loadAll(Taxon)},
 * {@code loadMicroRNAs}, {@code loadThawed*}, {@code loadValueObject*}, and {@code thaw*}
 * methods. All methods delegate to {@link ubic.gemma.persistence.service.genome.GeneDao}
 * (with simple VO-conversion where appropriate) and orchestrate no other collaborators.
 * <p>
 * Methods that orchestrate other services -- {@code findGOTerms}, {@code searchGenes},
 * {@code loadFullyPopulatedValueObject}, {@code populateAssociatedExperimentCount} --
 * remain on the {@link GeneService} facade because they pull in {@code SearchService},
 * {@code TaxonService}, {@code Gene2GOAssociationService}, {@code GeneSetSearch}, the
 * {@code HomologeneService} future, and {@code CharacteristicService}, and warrant their
 * own service slices later.
 * <p>
 * Callers should generally keep using {@link GeneService} as the facade -- the facade
 * delegates to this service. Direct injection is appropriate where a class would
 * otherwise create a Spring construction cycle through the heavier facade.
 * <p>
 * ACL / {@code @Secured} annotations live on {@link GeneService} (the caller-facing
 * facade interface); enforcement happens at the facade proxy boundary, so this interface
 * is intentionally unsecured.
 *
 * @see GeneService
 */
public interface GeneReadService {

    /**
     * @see ubic.gemma.persistence.service.genome.GeneDao#find(PhysicalLocation)
     */
    Collection<Gene> find( PhysicalLocation physicalLocation );

    @Nullable
    Gene findByAccession( String accession, @Nullable ExternalDatabase source );

    Collection<Gene> findByAlias( String search );

    @Nullable
    Gene findByEnsemblId( String exactString );

    @Nullable
    Gene findByNCBIId( Integer accession );

    @Nullable
    GeneValueObject findByNCBIIdValueObject( Integer accession );

    Map<Integer, GeneValueObject> findByNcbiIds( Collection<Integer> ncbiIds );

    Collection<Gene> findByOfficialName( String officialName );

    Collection<Gene> findByOfficialNameInexact( String officialName );

    Collection<Gene> findByOfficialSymbol( String officialSymbol );

    @Nullable
    Gene findByOfficialSymbol( String symbol, Taxon taxon );

    Collection<Gene> findByOfficialSymbolInexact( String officialSymbol );

    Map<String, GeneValueObject> findByOfficialSymbols( Collection<String> query, Long taxonId );

    long getCompositeSequenceCount( Gene gene, boolean includeDummyProducts );

    long getCompositeSequenceCountById( Long id, boolean includeDummyProducts );

    Collection<CompositeSequence> getCompositeSequences( Gene gene, ArrayDesign arrayDesign, boolean includeDummyProducts );

    Collection<CompositeSequence> getCompositeSequences( Gene gene, boolean includeDummyProducts );

    Collection<CompositeSequence> getCompositeSequencesById( Long geneId, boolean includeDummyProducts );

    List<PhysicalLocationValueObject> getPhysicalLocationsValueObjects( Gene gene );

    Collection<GeneProductValueObject> getProducts( Long geneId );

    Collection<Gene> loadAll( Taxon taxon );

    Collection<Gene> loadMicroRNAs( Taxon taxon );

    Collection<Gene> loadThawed( Collection<Long> ids );

    Collection<Gene> loadThawedLiter( Collection<Long> ids );

    @Nullable
    GeneValueObject loadValueObjectById( Long id );

    List<GeneValueObject> loadValueObjectsByIds( Collection<Long> ids );

    Collection<GeneValueObject> loadValueObjectsByIdsLiter( Collection<Long> ids );

    @CheckReturnValue
    Gene thaw( Gene gene );

    @CheckReturnValue
    Gene thawAliases( Gene gene );

    @CheckReturnValue
    Collection<Gene> thawLite( Collection<Gene> genes );

    @CheckReturnValue
    Gene thawLite( Gene gene );

    @CheckReturnValue
    Gene thawLiter( Gene gene );
}
