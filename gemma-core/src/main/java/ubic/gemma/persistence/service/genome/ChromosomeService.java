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

import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.BaseReadOnlyService;
import ubic.gemma.persistence.service.BaseService;

import java.util.Collection;

/**
 * @author kelsey
 */
public interface ChromosomeService extends BaseReadOnlyService<Chromosome> {

    /**
     * Locate by name e.g. 'X'.
     *
     * @param taxon taxon to look at
     * @param name  the name to look for, e.g. 'X'
     * @return all matching chromosomes
     */
    Collection<Chromosome> find( String name, Taxon taxon );

}
