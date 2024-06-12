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

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;

/**
 * @author paul
 */
public interface ExpressionExperimentSetValueObjectHelper {

    /**
     * Tries to load an existing experiment set with the param's id, if no experiment can be loaded, create a new one
     * with id = null. Sets all fields of the new entity with values from the valueObject param.
     *
     * @param setVO if null, returns null
     * @return ee set
     */
    ExpressionExperimentSet convertToEntity( ExpressionExperimentSetValueObject setVO );

}