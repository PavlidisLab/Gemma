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
package ubic.gemma.genome.taxon.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.TaxonValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.genome.Taxon;

/**
 * @author keshav
 * @version $Id$
 */
@Service
public class TaxonServiceImpl implements TaxonService {

    private static Log log = LogFactory.getLog( TaxonServiceImpl.class );
    
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
  
    @Autowired
    private ArrayDesignService arrayDesignService;
    
    @Autowired
    private ubic.gemma.model.genome.TaxonDao taxonDao;

    private static Comparator<TaxonValueObject> TAXON_COMPARATOR = new Comparator<TaxonValueObject>() {
        public int compare( TaxonValueObject o1, TaxonValueObject o2 ) {
            return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
        }
    };
    
    /**
     * @see TaxonService#createDatabaseEntity(Taxon)
     */
    protected Taxon handleCreate( Taxon taxon ) throws java.lang.Exception {
        return this.getTaxonDao().create( taxon );
    }

    /**
     * @see TaxonService#find(Taxon)
     */
    protected Taxon handleFind( Taxon taxon ) throws java.lang.Exception {
        return this.getTaxonDao().find( taxon );
    }

    protected Taxon handleFindByAbbreviation( String abbreviation ) throws Exception {
        return this.getTaxonDao().findByAbbreviation( abbreviation );
    }

    protected Taxon handleFindByCommonName( String commonName ) throws Exception {
        return this.getTaxonDao().findByCommonName( commonName );
    }

    protected Taxon handleFindByScientificName( String scientificName ) throws Exception {
        return this.getTaxonDao().findByScientificName( scientificName );
    }

    /**
     * @see TaxonService#findChildTaxaByParent(Taxon)
     */
    protected Collection<Taxon> handleFindChildTaxaByParent( Taxon taxon ) throws java.lang.Exception {
        return this.getTaxonDao().findChildTaxaByParent( taxon );
    }

    protected Taxon handleFindOrCreate( Taxon taxon ) throws Exception {
        return this.getTaxonDao().findOrCreate( taxon );
    }

    protected Taxon handleLoad( Long id ) throws Exception {
        return this.getTaxonDao().load( id );
    }

    protected Collection<Taxon> handleLoadAll() throws Exception {
        return ( Collection<Taxon> ) this.getTaxonDao().loadAll();
    }

    /**
     * @see TaxonService#remove(Taxon)
     */
    protected void handleRemove( Taxon taxon ) throws java.lang.Exception {
        this.getTaxonDao().remove( taxon );
    }

    /**
     * @see TaxonService#update(Taxon)
     */
    protected void handleUpdate( Taxon taxon ) throws java.lang.Exception {
        this.getTaxonDao().update( taxon );
    }
   
    protected void handleThaw( Taxon taxon ) throws Exception {
        this.getTaxonDao().thaw( taxon );        
    }
    
    public TaxonValueObject loadValueObject( Long id ){
        return TaxonValueObject.fromEntity( load( id ) );
    }
    
    public Collection<TaxonValueObject> loadAllValueObjects( ){
        Collection<TaxonValueObject> result = new ArrayList<TaxonValueObject>();
        for(Taxon tax : loadAll()){
            result.add( TaxonValueObject.fromEntity( tax ));
        }
        
        return result;
    }
    /**
     * @return Taxon that are species. (only returns usable taxa)
     */
    public Collection<TaxonValueObject> getTaxaSpecies() {
        SortedSet<TaxonValueObject> taxaSpecies = new TreeSet<TaxonValueObject>( TAXON_COMPARATOR );
        for ( Taxon taxon : loadAll() ) {
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
    public Collection<TaxonValueObject> getTaxaWithGenes() {
        SortedSet<TaxonValueObject> taxaWithGenes = new TreeSet<TaxonValueObject>( TAXON_COMPARATOR );
        for ( Taxon taxon : loadAll() ) {
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
    public Collection<TaxonValueObject> getTaxaWithDatasets() {
        Set<TaxonValueObject> taxaWithDatasets = new TreeSet<TaxonValueObject>( TAXON_COMPARATOR );

        Map<Taxon, Long> perTaxonCount = expressionExperimentService.getPerTaxonCount();

        for ( Taxon taxon : loadAll() ) {
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
    public Collection<TaxonValueObject> getTaxaWithArrays() {
        Set<TaxonValueObject> taxaWithArrays = new TreeSet<TaxonValueObject>( TAXON_COMPARATOR );

        for ( Taxon taxon : arrayDesignService.getPerTaxonCount().keySet() ) {
            //taxonService.thaw( taxon );
            taxaWithArrays.add( TaxonValueObject.fromEntity( taxon ) );
        }

        log.debug( "GenePicker::getTaxaWithArrays returned " + taxaWithArrays.size() + " results" );
        return taxaWithArrays;
    }
    

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#find(ubic.gemma.model.genome.Taxon)
     */
    public ubic.gemma.model.genome.Taxon find( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFind( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.find(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#findByAbbreviation(java.lang.String)
     */
    public ubic.gemma.model.genome.Taxon findByAbbreviation( final java.lang.String abbreviation ) {
        try {
            return this.handleFindByAbbreviation( abbreviation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.findByAbbreviation(java.lang.String abbreviation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#findByCommonName(java.lang.String)
     */
    public ubic.gemma.model.genome.Taxon findByCommonName( final java.lang.String commonName ) {
        try {
            return this.handleFindByCommonName( commonName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.findByCommonName(java.lang.String commonName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#findByScientificName(java.lang.String)
     */
    public ubic.gemma.model.genome.Taxon findByScientificName( final java.lang.String scientificName ) {
        try {
            return this.handleFindByScientificName( scientificName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.findByScientificName(java.lang.String scientificName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#findChildTaxaByParent(ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection<ubic.gemma.model.genome.Taxon> findChildTaxaByParent( Taxon parentTaxa ) {
        try {
            return this.handleFindChildTaxaByParent( parentTaxa );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.findByScientificName(java.lang.String scientificName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#findOrCreate(ubic.gemma.model.genome.Taxon)
     */
    public ubic.gemma.model.genome.Taxon findOrCreate( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindOrCreate( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.findOrCreate(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#load(java.lang.Long)
     */
    public ubic.gemma.model.genome.Taxon load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.load(java.lang.Long id)' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#loadAll()
     */
    public java.util.Collection<Taxon> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.loadAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#remove(ubic.gemma.model.genome.Taxon)
     */
    public void remove( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            this.handleRemove( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.remove(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>taxon</code>'s DAO.
     */
    public void setTaxonDao( ubic.gemma.model.genome.TaxonDao taxonDao ) {
        this.taxonDao = taxonDao;
    }

    /**
     * @see ubic.gemma.genome.taxon.service.TaxonService#update(ubic.gemma.model.genome.Taxon)
     */
    public void update( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            this.handleUpdate( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.update(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }
    
    /**
     * thaws taxon
     */
    public void thaw( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            this.handleThaw( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.genome.taxon.service.TaxonServiceException(
                    "Error performing 'ubic.gemma.model.genome.TaxonService.thaw(ubic.gemma.model.genome.Taxon taxon)' -->' --> "
                            + th, th );
        }
    }    
    

    /**
     * Gets the reference to <code>taxon</code>'s DAO.
     */
    protected ubic.gemma.model.genome.TaxonDao getTaxonDao() {
        return this.taxonDao;
    }

}