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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.genome.taxon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.*;

/**
 * @author keshav
 */
@Service
public class TaxonServiceImpl implements TaxonService {

    private static final Log log = LogFactory.getLog( TaxonServiceImpl.class );

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

    private final ExpressionExperimentService expressionExperimentService;
    private final ArrayDesignService arrayDesignService;
    private final TaxonDao taxonDao;

    /* ********************************
     * Constructors
     * ********************************/

    @Autowired
    public TaxonServiceImpl( ExpressionExperimentService expressionExperimentService,
            ArrayDesignService arrayDesignService, TaxonDao taxonDao ) {
        this.expressionExperimentService = expressionExperimentService;
        this.arrayDesignService = arrayDesignService;
        this.taxonDao = taxonDao;
    }

    /* ********************************
     * Public methods
     * ********************************/

    /**
     * @see TaxonService#find(Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Taxon find( final Taxon taxon ) {
        return this.taxonDao.find( taxon );
    }

    /**
     * @see TaxonService#findByAbbreviation(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Taxon findByAbbreviation( final String abbreviation ) {
        return this.taxonDao.findByAbbreviation( abbreviation );
    }

    /**
     * @see TaxonService#findByCommonName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Taxon findByCommonName( final String commonName ) {
        return this.taxonDao.findByCommonName( commonName );
    }

    /**
     * @see TaxonService#findByScientificName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Taxon findByScientificName( final String scientificName ) {
        return this.taxonDao.findByScientificName( scientificName );
    }

    /**
     * @see TaxonService#findChildTaxaByParent(Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Taxon> findChildTaxaByParent( Taxon parentTaxa ) {
        return this.taxonDao.findChildTaxaByParent( parentTaxa );
    }

    /**
     * @see TaxonService#findOrCreate(Taxon)
     */
    @Override
    @Transactional
    public Taxon findOrCreate( final Taxon taxon ) {
        return this.taxonDao.findOrCreate( taxon );
    }

    /**
     * @return Taxon that are species. (only returns usable taxa)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<TaxonValueObject> getTaxaSpecies() {
        SortedSet<TaxonValueObject> taxaSpecies = new TreeSet<>( TAXON_VO_COMPARATOR );
        for ( Taxon taxon : loadAll() ) {
            if ( taxon.getIsSpecies() ) {
                taxaSpecies.add( TaxonValueObject.fromEntity( taxon ) );
            }
        }
        return taxaSpecies;
    }

    /**
     * @return List of taxa with array designs in gemma
     */
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

    /**
     * @return collection of taxa that have expression experiments available.
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<TaxonValueObject> getTaxaWithDatasets() {
        Set<TaxonValueObject> taxaWithDatasets = new TreeSet<>( TAXON_VO_COMPARATOR );

        Map<Taxon, Long> perTaxonCount = expressionExperimentService.getPerTaxonCount();

        for ( Taxon taxon : loadAll() ) {
            if ( perTaxonCount.containsKey( taxon ) && perTaxonCount.get( taxon ) > 0 ) {
                taxaWithDatasets.add( TaxonValueObject.fromEntity( taxon ) );
            }
        }
        return taxaWithDatasets;
    }

    /**
     * @return Taxon that are on NeuroCarta evidence
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<TaxonValueObject> getTaxaWithEvidence() {

        SortedSet<TaxonValueObject> taxaSpecies = new TreeSet<>( TAXON_VO_COMPARATOR );
        for ( Taxon taxon : loadTaxonWithEvidence() ) {
            if ( taxon.getIsSpecies() ) {
                taxaSpecies.add( TaxonValueObject.fromEntity( taxon ) );
            }
        }
        return taxaSpecies;
    }

    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<TaxonValueObject> getTaxaWithGenes() {
        SortedSet<TaxonValueObject> taxaWithGenes = new TreeSet<>( TAXON_VO_COMPARATOR );
        for ( Taxon taxon : loadAll() ) {
            if ( taxon.getIsGenesUsable() ) {
                taxaWithGenes.add( TaxonValueObject.fromEntity( taxon ) );
            }
        }
        return taxaWithGenes;
    }

    /**
     * @see TaxonService#load(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public Taxon load( final Long id ) {
        return this.taxonDao.load( id );
    }

    /**
     * @see TaxonService#loadAll()
     */
    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Collection<Taxon> loadAll() {
        return ( Collection<Taxon> ) this.taxonDao.loadAll();
    }

    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Taxon> loadAllTaxaWithGenes() {
        SortedSet<Taxon> taxaWithGenes = new TreeSet<>( TAXON_COMPARATOR );
        for ( Taxon taxon : loadAll() ) {
            if ( taxon.getIsGenesUsable() ) {
                taxaWithGenes.add( taxon );
            }
        }
        return taxaWithGenes;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<TaxonValueObject> loadAllValueObjects() {
        Collection<TaxonValueObject> result = new ArrayList<>();
        for ( Taxon tax : loadAll() ) {
            result.add( TaxonValueObject.fromEntity( tax ) );
        }
        return result;
    }

    /**
     * @see TaxonService#loadTaxonWithEvidence()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Taxon> loadTaxonWithEvidence() {
        return this.taxonDao.findTaxonUsedInEvidence();
    }

    /**
     * @see TaxonService#loadValueObject(Long)
     */
    @Override
    @Transactional(readOnly = true)
    public TaxonValueObject loadValueObject( Long id ) {
        return TaxonValueObject.fromEntity( load( id ) );
    }

    /**
     * @see TaxonService#remove(Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public void remove( final Taxon taxon ) {
        this.taxonDao.remove( taxon );
    }

    /**
     * @see TaxonService#thaw(Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public void thaw( final Taxon taxon ) {
        this.taxonDao.thaw( taxon );
    }

    /**
     * @see TaxonService#update(Taxon)
     */
    @Override
    @Transactional
    public void update( final Taxon taxon ) {
        this.taxonDao.update( taxon );
    }

}