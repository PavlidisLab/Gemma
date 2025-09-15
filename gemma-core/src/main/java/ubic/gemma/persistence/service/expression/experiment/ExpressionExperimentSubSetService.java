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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.SecurableBaseService;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author kelsey
 */
public interface ExpressionExperimentSubSetService extends SecurableBaseService<ExpressionExperimentSubSet> {

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<ExpressionExperimentSubSet> findByBioAssayIn( Collection<BioAssay> bioAssays );

    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ_QUIET" })
    ExpressionExperimentSubSet loadWithBioAssays( Long id );

    /**
     * Deletes an experiment subset and all of its associated DifferentialExpressionAnalysis objects. This method is
     * similar to
     * ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentServiceImpl.handleDelete(ExpressionExperiment) but
     * it doesn't include removal of sample coexpression matrices, PCA, probe2probe coexpression links, or adjusting of
     * experiment set members.
     *
     * @param entity the subset to remove
     */
    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( ExpressionExperimentSubSet entity );

    /**
     * @param entity entity
     * @param factor factor
     * @return the factor values of the given factor that are relevant to the subset.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    Collection<FactorValue> getFactorValuesUsed( ExpressionExperimentSubSet entity, ExperimentalFactor factor );

    Collection<FactorValueValueObject> getFactorValuesUsed( Long subSetId, Long experimentalFactor );
}
