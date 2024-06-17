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

import java.util.Collection;

/**
 * @author paul
 */
public interface ExpressionExperimentSetValueObjectHelper {

    /**
     * Create an experiment set from a VO.
     * <p>
     * The set is made public if {@link ExpressionExperimentSetValueObject#getIsPublic()} is true, otherwise it is made
     * private.
     */
    ExpressionExperimentSet create( ExpressionExperimentSetValueObject eesvo );

    /**
     * Update corresponding entity based on value object
     */
    void update( ExpressionExperimentSetValueObject eesvo );

    /**
     * Updates the database record for the param experiment set value object (permission permitting) with the value
     * object's name and description.
     *
     * @param loadEEIds whether the returned value object should have the {@link ExpressionExperimentSetValueObject#getExpressionExperimentIds()}
     *                  collection populated. This might be useful information, but loading the IDs takes slightly longer,
     *                  so for larger amount of EE sets this might want to be avoided.
     */
    ExpressionExperimentSetValueObject updateNameAndDescription( ExpressionExperimentSetValueObject eeSetVO, boolean loadEEIds );

    /**
     * Updates the database record for the param experiment set value object (permission permitting) with the members
     * specified of the set, not the name or description etc.
     */
    void updateMembers( Long groupId, Collection<Long> eeIds );

    /**
     * Delete the experiment set corresponding to the given VO.
     */
    void delete( ExpressionExperimentSetValueObject eesvo );

    ExpressionExperimentSet convertToEntity( ExpressionExperimentSetValueObject eesvo );
}