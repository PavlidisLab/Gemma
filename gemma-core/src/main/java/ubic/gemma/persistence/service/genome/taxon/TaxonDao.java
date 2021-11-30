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

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;

import java.util.Collection;
import java.util.List;

/**
 * @see ubic.gemma.model.genome.Taxon
 */
public interface TaxonDao extends FilteringVoEnabledDao<Taxon, TaxonValueObject> {

    String OBJECT_ALIAS = "taxon";

    Taxon findByCommonName( String commonName );

    /**
     * Searches for a taxon by its scientific name, case insensitive.
     *
     * @param scientificName the scientific name to be matched
     * @return a Taxon whose scientific name matches the given string.
     */
    Taxon findByScientificName( String scientificName );

    Collection<Taxon> findTaxonUsedInEvidence();

    void thaw( Taxon taxon );

    Taxon findByNcbiId( Integer ncbiId );
}
