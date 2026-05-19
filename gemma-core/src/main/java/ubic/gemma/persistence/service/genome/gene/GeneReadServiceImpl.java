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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.PhysicalLocationValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.service.genome.GeneDao;

import org.springframework.lang.Nullable;
import java.util.*;
import java.util.Map.Entry;

/**
 * Implementation of {@link GeneReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link GeneService} interface -- this class is unsecured
 * at the AOP boundary on purpose, so intra-{@code gemma-core} callers can bypass
 * duplicate ACL checks once authenticated.
 *
 * @see GeneService
 */
@Service("geneReadService")
public class GeneReadServiceImpl implements GeneReadService {

    private final GeneDao geneDao;

    @Autowired
    public GeneReadServiceImpl( GeneDao geneDao ) {
        this.geneDao = geneDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> find( PhysicalLocation physicalLocation ) {
        return this.geneDao.find( physicalLocation );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene findByAccession( final String accession, @Nullable final ExternalDatabase source ) {
        return this.geneDao.findByAccession( accession, source );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByAlias( final String search ) {
        return this.geneDao.findByAlias( search );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene findByEnsemblId( String exactString ) {
        return this.geneDao.findByEnsemblId( exactString );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene findByNCBIId( Integer accession ) {
        return this.geneDao.findByNcbiId( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneValueObject findByNCBIIdValueObject( Integer accession ) {
        Gene gene = this.findByNCBIId( accession );
        return gene != null ? new GeneValueObject( gene ) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, GeneValueObject> findByNcbiIds( Collection<Integer> ncbiIds ) {
        Map<Integer, GeneValueObject> result = new HashMap<>();
        Map<Integer, Gene> genes = this.geneDao.findByNcbiIds( ncbiIds );
        for ( Entry<Integer, Gene> entry : genes.entrySet() ) {
            result.put( entry.getKey(), new GeneValueObject( entry.getValue() ) );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialName( final String officialName ) {
        return this.geneDao.findByOfficialName( officialName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialNameInexact( String officialName ) {
        return this.geneDao.findByOfficialNameInexact( officialName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialSymbol( final String officialSymbol ) {
        return this.geneDao.findByOfficialSymbol( officialSymbol );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene findByOfficialSymbol( final String symbol, final Taxon taxon ) {
        return this.geneDao.findByOfficialSymbol( symbol, taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> findByOfficialSymbolInexact( final String officialSymbol ) {
        return this.geneDao.findByOfficialSymbolInexact( officialSymbol );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, GeneValueObject> findByOfficialSymbols( Collection<String> query, Long taxonId ) {
        Map<String, GeneValueObject> result = new HashMap<>();
        Map<String, Gene> genes = this.geneDao.findByOfficialSymbols( query, taxonId );
        for ( String q : genes.keySet() ) {
            result.put( q, new GeneValueObject( genes.get( q ) ) );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public long getCompositeSequenceCount( Gene gene, boolean includeDummyProducts ) {
        return this.geneDao.getCompositeSequenceCount( gene, includeDummyProducts );
    }

    @Override
    @Transactional(readOnly = true)
    public long getCompositeSequenceCountById( final Long id, boolean includeDummyProducts ) {
        return this.geneDao.getCompositeSequenceCountById( id, includeDummyProducts );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequences( final Gene gene, final ArrayDesign arrayDesign, boolean includeDummyProducts ) {
        return this.geneDao.getCompositeSequences( gene, arrayDesign, includeDummyProducts );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequences( final Gene gene, boolean includeDummyProducts ) {
        return this.geneDao.getCompositeSequences( gene, includeDummyProducts );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getCompositeSequencesById( Long geneId, boolean includeDummyProducts ) {
        return this.geneDao.getCompositeSequencesById( geneId, includeDummyProducts );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhysicalLocationValueObject> getPhysicalLocationsValueObjects( Gene gene ) {
        if ( gene == null ) {
            return Collections.emptyList();
        }

        gene = this.thaw( gene );

        Collection<GeneProduct> gpCollection = gene.getProducts();
        List<PhysicalLocationValueObject> locations = new LinkedList<>();

        if ( gpCollection == null )
            return null;

        for ( GeneProduct gp : gpCollection ) {

            PhysicalLocation physicalLocation = gp.getPhysicalLocation();

            if ( physicalLocation == null ) {
                continue;
            }
            // Only add if the physical location of the product is different from any we already know.
            PhysicalLocationValueObject vo = new PhysicalLocationValueObject( physicalLocation );
            if ( !locations.contains( vo ) ) {
                locations.add( vo );
            }
        }

        return locations;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneProductValueObject> getProducts( Long geneId ) {
        if ( geneId == null )
            throw new IllegalArgumentException( "Null id for gene" );
        Gene gene = this.geneDao.load( geneId );

        if ( gene == null )
            throw new IllegalArgumentException( "No gene with id " + geneId );

        Collection<GeneProductValueObject> result = new ArrayList<>();
        for ( GeneProduct gp : gene.getProducts() ) {
            result.add( new GeneProductValueObject( gp ) );
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadAll( final Taxon taxon ) {
        return this.geneDao.loadKnownGenes( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadMicroRNAs( final Taxon taxon ) {
        return this.geneDao.getMicroRnaByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadThawed( Collection<Long> ids ) {
        return this.geneDao.loadThawed( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> loadThawedLiter( Collection<Long> ids ) {
        return this.geneDao.loadThawedLiter( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneValueObject loadValueObjectById( Long id ) {
        Gene g = this.geneDao.load( id );
        if ( g == null )
            return null;
        g = this.geneDao.thaw( g );
        return GeneValueObject.convert2ValueObject( g );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeneValueObject> loadValueObjectsByIds( Collection<Long> ids ) {
        List<Gene> g = this.geneDao.loadThawed( ids );
        return this.geneDao.loadValueObjects( g );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneValueObject> loadValueObjectsByIdsLiter( Collection<Long> ids ) {
        Collection<Gene> g = this.geneDao.loadThawedLiter( ids );
        return this.geneDao.loadValueObjects( g );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene thaw( Gene gene ) {
        return this.geneDao.thaw( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene thawAliases( Gene gene ) {
        return this.geneDao.thawAliases( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> thawLite( final Collection<Gene> genes ) {
        return this.geneDao.thawLite( genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene thawLite( Gene gene ) {
        return this.geneDao.thawLite( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Gene thawLiter( Gene gene ) {
        return this.geneDao.thawLiter( gene );
    }
}
