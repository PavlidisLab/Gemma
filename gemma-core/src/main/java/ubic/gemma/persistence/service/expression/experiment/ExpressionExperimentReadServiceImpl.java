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
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.Thaws;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link ExpressionExperimentReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link ExpressionExperimentService} interface — this class
 * is unsecured at the AOP boundary on purpose, so that intra-{@code gemma-core} callers
 * that hold an authenticated session can bypass duplicate ACL checks.
 *
 * @see ExpressionExperimentService
 */
@Service("expressionExperimentReadService")
public class ExpressionExperimentReadServiceImpl implements ExpressionExperimentReadService {

    private final ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    public ExpressionExperimentReadServiceImpl( ExpressionExperimentDao expressionExperimentDao ) {
        this.expressionExperimentDao = expressionExperimentDao;
    }

    @Override
    @Nonnull
    @Transactional(readOnly = true)
    public ExpressionExperiment loadReference( Long id ) {
        return expressionExperimentDao.loadReference( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadReferences( Collection<Long> ids ) {
        return expressionExperimentDao.loadReference( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadAllReferences() {
        return expressionExperimentDao.loadReference( expressionExperimentDao.loadIds( null, null ) );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithAuditTrail( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            Hibernate.initialize( ee.getAuditTrail() );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> loadTroubledIds() {
        return expressionExperimentDao.loadTroubledIds();
    }

    @Override
    @Transactional(readOnly = true)
    public SortedMap<String, String> loadAllIdentifiersAndName( boolean includeNames ) {
        List<ExpressionExperimentDao.Identifiers> allIds = expressionExperimentDao.loadAllIdentifiers();
        TreeMap<String, String> finalIds = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
        populateIdentifierMap( allIds, identifiers -> String.valueOf( identifiers.getId() ), finalIds );
        populateIdentifierMap( allIds, ExpressionExperimentDao.Identifiers::getShortName, finalIds );
        populateIdentifierMap( allIds, ExpressionExperimentDao.Identifiers::getAccession, finalIds );
        if ( includeNames ) {
            populateIdentifierMap( allIds, ExpressionExperimentDao.Identifiers::getName, finalIds );
        }
        return finalIds;
    }

    private void populateIdentifierMap( Collection<ExpressionExperimentDao.Identifiers> identifiers,
            Function<ExpressionExperimentDao.Identifiers, String> extractor, Map<String, String> identifierMap ) {
        Map<String, String> eeIds = new TreeMap<>( String.CASE_INSENSITIVE_ORDER );
        Set<String> ambiguousIdentifiers = new HashSet<>();
        for ( ExpressionExperimentDao.Identifiers ids : identifiers ) {
            String id = extractor.apply( ids );
            if ( id == null ) {
                continue;
            }
            if ( identifierMap.containsKey( id ) ) {
                continue;
            }
            if ( eeIds.put( id, ids.getName() ) != null ) {
                ambiguousIdentifiers.add( id );
            }
        }
        ambiguousIdentifiers.forEach( eeIds::remove );
        identifierMap.putAll( eeIds );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment reload( ExpressionExperiment ee ) {
        return expressionExperimentDao.reload( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithPrimaryPublication( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            if ( ee.getPrimaryPublication() != null ) {
                Thaws.thawBibliographicReference( ee.getPrimaryPublication() );
            }
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithPrimaryPublicationAndOtherRelevantPublications( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            if ( ee.getPrimaryPublication() != null ) {
                Thaws.thawBibliographicReference( ee.getPrimaryPublication() );
            }
            ee.getOtherRelevantPublications().forEach( Thaws::thawBibliographicReference );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithMeanVarianceRelation( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            Hibernate.initialize( ee.getMeanVarianceRelation() );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadWithCharacteristics( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            Hibernate.initialize( ee.getCharacteristics() );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadAndThaw( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            expressionExperimentDao.thaw( ee );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadAndThawLite( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee != null ) {
            expressionExperimentDao.thawLite( ee );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment loadAndThawLiteWithRefreshCacheMode( Long id ) {
        ExpressionExperiment ee = expressionExperimentDao.load( id, CacheMode.REFRESH );
        if ( ee != null ) {
            expressionExperimentDao.evictCharacteristicsCache( ee );
            expressionExperimentDao.evictBioAssaysCache( ee );
            expressionExperimentDao.evictQuantitationTypesCache( ee );
            expressionExperimentDao.evictOtherPartsCache( ee );
            expressionExperimentDao.thawLite( ee );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier, defaultMissingMessage( id ) );
        expressionExperimentDao.thaw( ee );
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier, message );
        expressionExperimentDao.thawLite( ee );
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawLiteOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier, defaultMissingMessage( id ) );
        expressionExperimentDao.thawLite( ee );
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> ExpressionExperiment loadAndThawLiterOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        ExpressionExperiment ee = loadOrFail( id, exceptionSupplier, defaultMissingMessage( id ) );
        expressionExperimentDao.thawLiter( ee );
        return ee;
    }

    private <T extends Exception> ExpressionExperiment loadOrFail( Long id, Function<String, T> exceptionSupplier, String message ) throws T {
        ExpressionExperiment ee = expressionExperimentDao.load( id );
        if ( ee == null ) {
            throw exceptionSupplier.apply( message );
        }
        return ee;
    }

    private String defaultMissingMessage( Long id ) {
        return String.format( "No %s with ID %d.", ExpressionExperiment.class.getName(), id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByAccession( final DatabaseEntry accession ) {
        return expressionExperimentDao.findByAccession( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        return expressionExperimentDao.findByAccession( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findOneByAccession( String accession ) {
        return expressionExperimentDao.findOneByAccession( accession );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByBibliographicReference( final BibliographicReference bibRef ) {
        return expressionExperimentDao.findByBibliographicReference( bibRef );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByBioAssay( final BioAssay ba ) {
        return expressionExperimentDao.findByBioAssay( ba );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByBioAssay( BioAssay ba, boolean includeSubSets ) {
        return expressionExperimentDao.findByBioAssay( ba, includeSubSets );
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByBioAssay( BioAssay ba, boolean includeSubSets ) {
        return expressionExperimentDao.findIdByBioAssay( ba, includeSubSets );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByBioMaterial( final BioMaterial bm ) {
        return expressionExperimentDao.findByBioMaterial( bm );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByBioMaterial( BioMaterial bm, boolean includeSubSets ) {
        return expressionExperimentDao.findByBioMaterial( bm, includeSubSets );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> findIdsByBioMaterial( BioMaterial bm, boolean includeSubSets ) {
        return expressionExperimentDao.findIdsByBioMaterial( bm, includeSubSets );
    }

    @Override
    public Map<ExpressionExperiment, Collection<BioMaterial>> findByBioMaterials( Collection<BioMaterial> biomaterials ) {
        return expressionExperimentDao.findByBioMaterials( biomaterials );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByExpressedGene( final Gene gene, final double rank ) {
        return expressionExperimentDao.findByExpressedGene( gene, rank );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByDesign( ExperimentalDesign ed ) {
        return expressionExperimentDao.findByDesign( ed );
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByDesign( ExperimentalDesign design ) {
        return expressionExperimentDao.findIdByDesign( design );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByDesignId( Long designId ) {
        return expressionExperimentDao.findByDesignId( designId );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByFactor( final ExperimentalFactor factor ) {
        return expressionExperimentDao.findByFactor( factor );
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByFactor( ExperimentalFactor factor ) {
        return expressionExperimentDao.findIdByFactor( factor );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByFactors( Collection<ExperimentalFactor> factors ) {
        return expressionExperimentDao.findByFactors( factors );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByFactorValue( final FactorValue factorValue ) {
        return expressionExperimentDao.findByFactorValue( factorValue );
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByFactorValue( FactorValue factorValue ) {
        return expressionExperimentDao.findIdByFactorValue( factorValue );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByFactorValueId( final Long factorValueId ) {
        return expressionExperimentDao.findByFactorValueId( factorValueId );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByFactorValues( final Collection<FactorValue> factorValues ) {
        return expressionExperimentDao.findByFactorValues( factorValues );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByFactorValueIds( Collection<Long> factorValueIds ) {
        return expressionExperimentDao.findByFactorValueIds( factorValueIds );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByGene( final Gene gene ) {
        return expressionExperimentDao.findByGene( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByName( final String name ) {
        return expressionExperimentDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findOneByName( String name ) {
        return expressionExperimentDao.findOneByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByQuantitationType( QuantitationType type ) {
        return expressionExperimentDao.findByQuantitationType( type );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByShortName( final String shortName ) {
        return expressionExperimentDao.findByShortName( shortName );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByShortNameWithPrimaryPublication( String shortName ) {
        ExpressionExperiment ee = expressionExperimentDao.findByShortName( shortName );
        if ( ee != null && ee.getPrimaryPublication() != null ) {
            Thaws.thawBibliographicReference( ee.getPrimaryPublication() );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByShortNameAndThawLite( String shortName ) {
        ExpressionExperiment ee = expressionExperimentDao.findByShortName( shortName );
        if ( ee != null ) {
            expressionExperimentDao.thawLite( ee );
        }
        return ee;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findByTaxon( final Taxon taxon ) {
        return expressionExperimentDao.findByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpressionExperiment> findByUpdatedLimit( int limit ) {
        return expressionExperimentDao.findByUpdatedLimit( limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> findUpdatedAfter( Date date ) {
        return expressionExperimentDao.findUpdatedAfter( date );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment findByMeanVarianceRelation( MeanVarianceRelation mvr ) {
        return expressionExperimentDao.findByMeanVarianceRelation( mvr );
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByMeanVarianceRelation( MeanVarianceRelation mvr ) {
        return expressionExperimentDao.findIdByMeanVarianceRelation( mvr );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByShortName( String shortName ) {
        return expressionExperimentDao.existsByShortName( shortName );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadLackingFactors() {
        return expressionExperimentDao.loadLackingFactors();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperiment> loadLackingTags() {
        return expressionExperimentDao.loadLackingTags();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thaw( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        expressionExperimentDao.thaw( result );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thawLite( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        expressionExperimentDao.thawLite( result );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment thawLiter( final ExpressionExperiment expressionExperiment ) {
        ExpressionExperiment result = ensureInSession( expressionExperiment );
        expressionExperimentDao.thawLiter( result );
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Taxon getTaxon( final ExpressionExperiment ee ) {
        return expressionExperimentDao.getTaxon( ee );
    }

    /**
     * Re-implementation of {@code AbstractService.ensureInSession} so this service does
     * not need to extend the base service hierarchy.
     */
    private ExpressionExperiment ensureInSession( ExpressionExperiment entity ) {
        if ( entity == null ) {
            return null;
        }
        Long id = entity.getId();
        if ( id == null ) {
            return entity; // transient
        }
        return requireNonNull( expressionExperimentDao.load( id ),
                String.format( "No %s with ID %d.", ExpressionExperiment.class.getName(), id ) );
    }
}
