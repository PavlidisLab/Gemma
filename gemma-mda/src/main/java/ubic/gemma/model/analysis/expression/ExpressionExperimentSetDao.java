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
package ubic.gemma.model.analysis.expression;

import java.util.Collection;

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSet
 */
public interface ExpressionExperimentSetDao extends BaseDao<ExpressionExperimentSet> {

    /**
     * Locate expressionExperimentSets that contain the given bioAssaySet.
     */
    public Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet );

    /**
     * 
     */
    public java.util.Collection<ExpressionExperimentSet> findByName( java.lang.String name );

    /**
     * <p>
     * Get analyses that use this set. Note that if this collection is not empty, modification of the
     * expressionexperimentset should be disallowed.
     * </p>
     */
    public java.util.Collection<ExpressionAnalysis> getAnalyses(
            ExpressionExperimentSet expressionExperimentSet );

    /**
     * @param id
     * @return
     * @see ubic.gemma.expression.experiment.ExpressionExperimentSetService.getExperimentsInSet
     */
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id );

    /**
     * @return ExpressionExperimentSets that have more than 1 experiment in them.
     */
    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets();

    /**
     * @return ExpressionExperimentSets that have more than 1 experiment in them.
     */
    public Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon();

    /**
     * @param expressionExperimentSet
     */
    public void thaw( ExpressionExperimentSet expressionExperimentSet );

    int getExperimentCount( Long id );

}
