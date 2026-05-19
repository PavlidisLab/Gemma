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
package ubic.gemma.persistence.service.genome.taxon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Implementation of {@link TaxonReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link TaxonService} interface -- this class is unsecured
 * at the AOP boundary on purpose, so intra-{@code gemma-core} callers can bypass
 * duplicate ACL checks once authenticated. (The read methods on the facade carry no
 * {@code @Secured} annotation today.)
 *
 * @see TaxonService
 */
@Service("taxonReadService")
public class TaxonReadServiceImpl implements TaxonReadService {

    private static final Logger log = LoggerFactory.getLogger( TaxonReadServiceImpl.class );

    private static final Comparator<TaxonValueObject> TAXON_VO_COMPARATOR = new Comparator<TaxonValueObject>() {
        @Override
        public int compare( TaxonValueObject o1, TaxonValueObject o2 ) {
            return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
        }
    };
    private static final Comparator<Taxon> TAXON_COMPARATOR = new Comparator<Taxon>() {
        @Override
        public int compare( Taxon o1, Taxon o2 ) {
            return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
        }
    };

    private final TaxonDao taxonDao;
    private final ExpressionExperimentService expressionExperimentService;
    private final ArrayDesignService arrayDesignService;

    @Autowired
    public TaxonReadServiceImpl( TaxonDao taxonDao,
            ExpressionExperimentService expressionExperimentService,
            ArrayDesignService arrayDesignService ) {
        this.taxonDao = taxonDao;
        this.expressionExperimentService = expressionExperimentService;
        this.arrayDesignService = arrayDesignService;
    }

    @Override
    @Transactional(readOnly = true)
    public Taxon findByCommonName( final String commonName ) {
        return this.taxonDao.findByCommonName( commonName );
    }

    @Override
    @Transactional(readOnly = true)
    public Taxon findByScientificName( final String scientificName ) {
        return this.taxonDao.findByScientificName( scientificName );
    }

    @Override
    @Transactional(readOnly = true)
    public Taxon findByNcbiId( final Integer ncbiId ) {
        return this.taxonDao.findByNcbiId( ncbiId );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Taxon> loadAllTaxaWithGenes() {
        SortedSet<Taxon> taxaWithGenes = new TreeSet<>( TAXON_COMPARATOR );
        for ( Taxon taxon : this.taxonDao.loadAll() ) {
            if ( taxon.getIsGenesUsable() ) {
                taxaWithGenes.add( taxon );
            }
        }
        return taxaWithGenes;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<TaxonValueObject> getTaxaWithGenes() {
        SortedSet<TaxonValueObject> taxaWithGenes = new TreeSet<>( TAXON_VO_COMPARATOR );
        for ( Taxon taxon : this.taxonDao.loadAll() ) {
            if ( taxon.getIsGenesUsable() ) {
                taxaWithGenes.add( TaxonValueObject.fromEntity( taxon ) );
            }
        }
        return taxaWithGenes;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<TaxonValueObject> getTaxaWithDatasets() {
        Set<TaxonValueObject> taxaWithDatasets = new TreeSet<>( TAXON_VO_COMPARATOR );

        Map<Taxon, Long> perTaxonCount = expressionExperimentService.getPerTaxonCount();

        for ( Taxon taxon : this.taxonDao.loadAll() ) {
            if ( perTaxonCount.containsKey( taxon ) && perTaxonCount.get( taxon ) > 0 ) {
                taxaWithDatasets.add( TaxonValueObject.fromEntity( taxon ) );
            }
        }
        return taxaWithDatasets;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<TaxonValueObject> getTaxaWithArrays() {
        Set<TaxonValueObject> taxaWithArrays = new TreeSet<>( TAXON_VO_COMPARATOR );

        for ( Taxon taxon : arrayDesignService.getPerTaxonCount().keySet() ) {
            taxaWithArrays.add( TaxonValueObject.fromEntity( taxon ) );
        }
        log.debug( "GenePicker::getTaxaWithArrays returned " + taxaWithArrays.size() + " results" );
        return taxaWithArrays;
    }
}
