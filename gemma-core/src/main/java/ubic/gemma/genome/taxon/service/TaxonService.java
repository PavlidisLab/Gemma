/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.TaxonValueObject;
import ubic.gemma.model.genome.Taxon;

/**
 * @author kelsey
 * @version $Id$
 */
public interface TaxonService {

    public Taxon find( Taxon taxon );

    public Taxon findByAbbreviation( String abbreviation );

    public Taxon findByCommonName( String commonName );

    public Taxon findByScientificName( String scientificName );

    public Collection<Taxon> findChildTaxaByParent( Taxon parentTaxon );

    @Secured( { "GROUP_USER" })
    public Taxon findOrCreate( Taxon taxon );

    public Taxon load( Long id );

    public TaxonValueObject loadValueObject( Long id );

    public Collection<Taxon> loadAll();


    @Secured( { "GROUP_USER" })
    public void remove( Taxon taxon );

    @Secured( { "GROUP_USER" })
    public void update( Taxon taxon );

    public void thaw( Taxon taxon );

    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    public Collection<Taxon> loadAllTaxaWithGenes();
    
    public Collection<TaxonValueObject> loadAllValueObjects();
    /**
     * @return Taxon that are species. (only returns usable taxa)
     */
    public Collection<TaxonValueObject> getTaxaSpecies();
    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    public Collection<TaxonValueObject> getTaxaWithGenes();
    /**
     * @return collection of taxa that have expression experiments available.
     */
    public Collection<TaxonValueObject> getTaxaWithDatasets(); 

    /**
     * @return List of taxa with array designs in gemma
     */
    public Collection<TaxonValueObject> getTaxaWithArrays();

}
