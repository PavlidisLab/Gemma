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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.*;

/**
 * @author keshav
 */
@Service
public class TaxonServiceImpl extends AbstractFilteringVoEnabledService<Taxon, TaxonValueObject> implements TaxonService {

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
    private ExpressionExperimentService expressionExperimentService;
    private ArrayDesignService arrayDesignService;

    @Autowired
    public TaxonServiceImpl( TaxonDao taxonDao ) {
        super( taxonDao );
        this.taxonDao = taxonDao;
    }

    @Autowired
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    @Autowired
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
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

    @Override
    @Transactional(readOnly = true)
    public Taxon findByNcbiId( final Integer ncbiId ) {
        return this.taxonDao.findByNcbiId( ncbiId );
    }

    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Taxon> loadAllTaxaWithGenes() {
        SortedSet<Taxon> taxaWithGenes = new TreeSet<>( TaxonServiceImpl.TAXON_COMPARATOR );
        for ( Taxon taxon : this.loadAll() ) {
            if ( taxon.getIsGenesUsable() ) {
                taxaWithGenes.add( taxon );
            }
        }
        return taxaWithGenes;
    }

    /**
     * @return Taxon that are on NeuroCarta evidence
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<TaxonValueObject> getTaxaWithEvidence() {

        SortedSet<TaxonValueObject> taxaSpecies = new TreeSet<>( TaxonServiceImpl.TAXON_VO_COMPARATOR );
        for ( Taxon taxon : this.taxonDao.findTaxonUsedInEvidence() ) {
            taxaSpecies.add( TaxonValueObject.fromEntity( taxon ) );
        }
        return taxaSpecies;
    }

    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<TaxonValueObject> getTaxaWithGenes() {
        SortedSet<TaxonValueObject> taxaWithGenes = new TreeSet<>( TaxonServiceImpl.TAXON_VO_COMPARATOR );
        for ( Taxon taxon : this.loadAll() ) {
            if ( taxon.getIsGenesUsable() ) {
                taxaWithGenes.add( TaxonValueObject.fromEntity( taxon ) );
            }
        }
        return taxaWithGenes;
    }

    /**
     * @return collection of taxa that have expression experiments available.
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<TaxonValueObject> getTaxaWithDatasets() {
        Set<TaxonValueObject> taxaWithDatasets = new TreeSet<>( TaxonServiceImpl.TAXON_VO_COMPARATOR );

        Map<Taxon, Long> perTaxonCount = expressionExperimentService.getPerTaxonCount();

        for ( Taxon taxon : this.loadAll() ) {
            if ( perTaxonCount.containsKey( taxon ) && perTaxonCount.get( taxon ) > 0 ) {
                taxaWithDatasets.add( TaxonValueObject.fromEntity( taxon ) );
            }
        }
        return taxaWithDatasets;
    }

    /**
     * @return List of taxa with array designs in gemma
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<TaxonValueObject> getTaxaWithArrays() {
        Set<TaxonValueObject> taxaWithArrays = new TreeSet<>( TaxonServiceImpl.TAXON_VO_COMPARATOR );

        for ( Taxon taxon : arrayDesignService.getPerTaxonCount().keySet() ) {
            taxaWithArrays.add( TaxonValueObject.fromEntity( taxon ) );
        }
        AbstractService.log.debug( "GenePicker::getTaxaWithArrays returned " + taxaWithArrays.size() + " results" );
        return taxaWithArrays;
    }
}