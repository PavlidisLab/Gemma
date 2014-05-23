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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

/**
 * @author kelsey
 * @version $Id$
 */
public interface ExpressionExperimentSubSetService {

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public ExpressionExperimentSubSet create( ExpressionExperimentSubSet expressionExperimentSubSet );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperimentSubSet find( ExpressionExperimentSubSet entity );

    @Secured({ "GROUP_USER" })
    public ExpressionExperimentSubSet findOrCreate( ExpressionExperimentSubSet entity );

    /**
     * @param entity
     * @param factor
     * @return the factor values of the given factor that are relevant to the subset.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<FactorValue> getFactorValuesUsed( ExpressionExperimentSubSet entity, ExperimentalFactor factor );

    /**
     * @param subSetId
     * @param experimentalFactor
     * @return
     */
    public Collection<FactorValueValueObject> getFactorValuesUsed( Long subSetId, Long experimentalFactor );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperimentSubSet load( Long id );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperimentSubSet> loadAll();

    /**
     * Deletes an experiment subset and all of its associated DifferentialExpressionAnalysis objects. This method is
     * similar to
     * ubic.gemma.expression.experiment.service.ExpressionExperimentServiceImpl.handleDelete(ExpressionExperiment) but
     * it doesn't include removal of sample coexpression matrices, PCA, probe2probe coexpression links, or adjusting of
     * experiment set members.
     * 
     * @param entity the subset to delete
     * @see ubic.gemma.expression.experiment.service.ExpressionExperimentServiceImpl.handleDelete(ExpressionExperiment)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void delete( ExpressionExperimentSubSet entity );

}
