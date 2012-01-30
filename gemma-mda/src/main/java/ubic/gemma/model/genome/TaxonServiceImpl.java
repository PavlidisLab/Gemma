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
package ubic.gemma.model.genome;

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

import ubic.gemma.model.TaxonValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * @author keshav
 * @version $Id$
 */
@Service
public class TaxonServiceImpl extends TaxonServiceBase {

    private static Log log = LogFactory.getLog( TaxonServiceImpl.class );
    
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
  
    @Autowired
    private ArrayDesignService arrayDesignService;
    
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
    @Override
    protected Taxon handleFind( Taxon taxon ) throws java.lang.Exception {
        return this.getTaxonDao().find( taxon );
    }

    @Override
    protected Taxon handleFindByAbbreviation( String abbreviation ) throws Exception {
        return this.getTaxonDao().findByAbbreviation( abbreviation );
    }

    @Override
    protected Taxon handleFindByCommonName( String commonName ) throws Exception {
        return this.getTaxonDao().findByCommonName( commonName );
    }

    @Override
    protected Taxon handleFindByScientificName( String scientificName ) throws Exception {
        return this.getTaxonDao().findByScientificName( scientificName );
    }

    /**
     * @see TaxonService#findChildTaxaByParent(Taxon)
     */
    @Override
    protected Collection<Taxon> handleFindChildTaxaByParent( Taxon taxon ) throws java.lang.Exception {
        return this.getTaxonDao().findChildTaxaByParent( taxon );
    }

    @Override
    protected Taxon handleFindOrCreate( Taxon taxon ) throws Exception {
        return this.getTaxonDao().findOrCreate( taxon );
    }

    @Override
    protected Taxon handleLoad( Long id ) throws Exception {
        return this.getTaxonDao().load( id );
    }

    @Override
    protected Collection<Taxon> handleLoadAll() throws Exception {
        return ( Collection<Taxon> ) this.getTaxonDao().loadAll();
    }

    /**
     * @see TaxonService#remove(Taxon)
     */
    @Override
    protected void handleRemove( Taxon taxon ) throws java.lang.Exception {
        this.getTaxonDao().remove( taxon );
    }

    /**
     * @see TaxonService#update(Taxon)
     */
    @Override
    protected void handleUpdate( Taxon taxon ) throws java.lang.Exception {
        this.getTaxonDao().update( taxon );
    }

   
    @Override
    protected void handleThaw( Taxon taxon ) throws Exception {
        this.getTaxonDao().thaw( taxon );        
    }
    
    @Override
    public TaxonValueObject loadValueObject( Long id ){
        return TaxonValueObject.fromEntity( load( id ) );
    }
    
    @Override
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
    @Override
    public Collection<TaxonValueObject> getTaxaSpecies() {
        SortedSet<TaxonValueObject> taxaSpecies = new TreeSet<TaxonValueObject>( TAXON_COMPARATOR );
        for ( TaxonValueObject taxon : loadAllValueObjects() ) {
            if ( taxon.getIsSpecies() ) {
                taxaSpecies.add( taxon );
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
        for ( TaxonValueObject taxon : loadAllValueObjects() ) {
            if ( taxon.getIsGenesUsable() ) {
                taxaWithGenes.add( taxon );
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

}