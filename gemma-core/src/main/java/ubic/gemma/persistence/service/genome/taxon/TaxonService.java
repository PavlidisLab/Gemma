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
package ubic.gemma.persistence.service.genome.taxon;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;

import java.util.Collection;

/**
 * @author kelsey
 */
public interface TaxonService {

    Taxon find( Taxon taxon );

    Taxon findByAbbreviation( String abbreviation );

    Taxon findByCommonName( String commonName );

    Taxon findByScientificName( String scientificName );

    Collection<Taxon> findChildTaxaByParent( Taxon parentTaxon );

    @Secured({ "GROUP_USER" })
    Taxon findOrCreate( Taxon taxon );

    Taxon load( Long id );

    TaxonValueObject loadValueObject( Long id );

    Collection<Taxon> loadAll();

    @Secured({ "GROUP_USER" })
    void remove( Taxon taxon );

    @Secured({ "GROUP_USER" })
    void update( Taxon taxon );

    void thaw( Taxon taxon );

    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    Collection<Taxon> loadAllTaxaWithGenes();

    Collection<TaxonValueObject> loadAllValueObjects();

    /**
     * @return Taxon that are species. (only returns usable taxa)
     */
    Collection<TaxonValueObject> getTaxaSpecies();

    Collection<TaxonValueObject> getTaxaWithEvidence();

    /**
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    Collection<TaxonValueObject> getTaxaWithGenes();

    /**
     * @return collection of taxa that have expression experiments available.
     */
    Collection<TaxonValueObject> getTaxaWithDatasets();

    /**
     * @return List of taxa with array designs in gemma
     */
    Collection<TaxonValueObject> getTaxaWithArrays();

    /**
     * @return Taxon that are on NeuroCarta evidence
     */
    java.util.Collection<Taxon> loadTaxonWithEvidence();

}
