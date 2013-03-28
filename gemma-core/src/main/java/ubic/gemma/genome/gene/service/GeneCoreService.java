/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.genome.gene.service;

import java.util.Collection;

import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * core service for Gene
 * 
 * @author nicolas
 * @version $Id$
 */
public interface GeneCoreService {

    /**
     * Returns a detailVO for a geneId
     * 
     * @param geneId The gene id
     * @return GeneDetailsValueObject a representation of that gene
     */
    public GeneValueObject loadGeneDetails( long geneId );

    /**
     * Search for genes (by name or symbol)
     * 
     * @param query
     * @param taxonId, can be null to not constrain by taxon
     * @return Collection of Gene entity objects
     */
    public Collection<GeneValueObject> searchGenes( String query, Long taxonId );

}
