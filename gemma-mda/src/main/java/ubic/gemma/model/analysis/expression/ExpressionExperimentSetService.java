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

/**
 * @author paul
 * @version $Id$
 */
public interface ExpressionExperimentSetService {

    /**
     * 
     */
    public ubic.gemma.model.analysis.expression.ExpressionExperimentSet create(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet );

    /**
     * 
     */
    public void delete( ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet );

    /**
     * 
     */
    public void update( ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet );

    /**
     * 
     */
    public java.util.Collection<ExpressionExperimentSet> loadAll();

    /**
     * @return ExpressionExperimentSets that have more than 1 experiment in them.
     */
    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets();

    /**
     * <p>
     * Load all ExpressionExperimentSets that belong to the given user.
     * </p>
     */
    public java.util.Collection<ExpressionExperimentSet> loadUserSets(
            ubic.gemma.model.common.auditAndSecurity.User user );

    /**
     * 
     */
    public ubic.gemma.model.analysis.expression.ExpressionExperimentSet load( java.lang.Long id );

    /**
     * <p>
     * Get analyses that use this set. Note that if this collection is not empty, modification of the
     * expressionexperimentset should be disallowed.
     * </p>
     */
    public java.util.Collection<ExpressionAnalysis> getAnalyses(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet );

    /**
     * 
     */
    public java.util.Collection<ExpressionExperimentSet> findByName( java.lang.String name );

}
