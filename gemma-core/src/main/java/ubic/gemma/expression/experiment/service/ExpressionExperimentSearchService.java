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
package ubic.gemma.expression.experiment.service;

import java.util.Collection;
import java.util.List;

import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.search.SearchResultDisplayObject;

/**
 * TODO Document Me
 * 
 * @author tvrossum
 * @version $Id$
 */
public interface ExpressionExperimentSearchService {

    /**
     * @param query
     * @return Collection of expression experiment entity objects
     */
    public abstract Collection<ExpressionExperimentValueObject> searchExpressionExperiments( String query );

    /**
     * does not include session bound sets
     * 
     * @param query
     * @param taxonId if the search should not be limited by taxon, pass in null
     * @return Collection of SearchResultDisplayObjects
     */
    public abstract List<SearchResultDisplayObject> searchExperimentsAndExperimentGroups( String query, Long taxonId );
    
    public abstract List<SearchResultDisplayObject> getAllTaxonExperimentGroup( Long taxonId );

}