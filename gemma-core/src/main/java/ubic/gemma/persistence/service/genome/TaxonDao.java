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
package ubic.gemma.persistence.service.genome;

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;

/**
 * @see ubic.gemma.model.genome.Taxon
 */
public interface TaxonDao extends BaseDao<Taxon> {

    Taxon find( Taxon taxon );

    /**
     * <p>
     * A finder method to find a taxon based on an abbreviation.
     * </p>
     */
    Taxon findByAbbreviation( String abbreviation );

    Taxon findByCommonName( String commonName );

    /**
     * Searches for a taxon by its scientific name, case insensitive.
     * @param scientificName the scientific name to be matched
     * @return a Taxon whose scientific name matches the given string.
     */
    Taxon findByScientificName( String scientificName );

    /**
     * Find the child<code>taxa</code> for this parent.
     */
    Collection<Taxon> findChildTaxaByParent( Taxon parentTaxon );

    Taxon findOrCreate( Taxon taxon );

    /**
     * Thaw the taxon
     */
    void thaw( Taxon taxon );

    Collection<Taxon> findTaxonUsedInEvidence();

}
