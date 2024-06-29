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
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResultDisplayObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import ubic.gemma.core.lang.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * @author tvrossum
 */
public interface ExpressionExperimentSearchService {

    /**
     * @param query the query
     * @return Collection of expression experiment entity objects
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<ExpressionExperimentValueObject> searchExpressionExperiments( String query ) throws SearchException;

    /**
     * @param value the term values
     * @return Collection of expression experiment VOs for EEs that are associated with all the given terms.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    Collection<ExpressionExperimentValueObject> searchExpressionExperiments( List<String> value ) throws SearchException;

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Collection<Long> searchExpressionExperiments( String query, Long taxonId ) throws SearchException;

    /**
     * does not include session bound sets
     *
     * @param query   the query
     * @param taxonId if the search should not be limited by taxon, pass in null
     * @return Collection of SearchResultDisplayObjects
     */
    List<SearchResultDisplayObject> searchExperimentsAndExperimentGroups( String query, @Nullable Long taxonId ) throws SearchException;

    List<SearchResultDisplayObject> getAllTaxonExperimentGroup( Long taxonId );
}