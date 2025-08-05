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
package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @see ubic.gemma.model.expression.experiment.FactorValue
 */
public interface FactorValueDao extends FilteringVoEnabledDao<FactorValue, FactorValueValueObject> {

    /**
     * Locate based on string value of the value.
     *
     * @param valuePrefix value prefix
     * @param maxResults
     * @return collection of factor values
     */
    @Deprecated
    Collection<FactorValue> findByValue( String valuePrefix, int maxResults );

    @Deprecated
    FactorValue loadWithOldStyleCharacteristics( Long id, boolean readOnly );

    /**
     * Load all the factor values IDs with their number of old-style characteristics.
     * @param excludedIds list of excluded IDs
     */
    @Deprecated
    Map<Long, Integer> loadIdsWithNumberOfOldStyleCharacteristics( Set<Long> excludedIds );

    /**
     * Update a FactorValue without involving ACL advice.
     * @deprecated do not use this, it is only a workaround to make FV migration faster
     */
    @Deprecated
    void updateIgnoreAcl( FactorValue fv );
}
