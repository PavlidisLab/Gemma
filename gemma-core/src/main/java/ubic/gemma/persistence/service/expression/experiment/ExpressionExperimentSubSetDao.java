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

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;

/**
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet
 */
public interface ExpressionExperimentSubSetDao extends BaseDao<ExpressionExperimentSubSet> {

    @Override
    ExpressionExperimentSubSet find( ExpressionExperimentSubSet entity );

    /**
     * @return matching or new entity. Matching would mean the same bioassays.
     */
    @Override
    ExpressionExperimentSubSet findOrCreate( ExpressionExperimentSubSet entity );

    Collection<FactorValue> getFactorValuesUsed( ExpressionExperimentSubSet entity, ExperimentalFactor factor );

    Collection<FactorValueValueObject> getFactorValuesUsed( Long subSetId, Long experimentalFactor );

}
