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
package ubic.gemma.persistence.service.common.quantitationtype;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author kelsey
 */
public interface QuantitationTypeService extends BaseService<QuantitationType>, FilteringVoEnabledService<QuantitationType, QuantitationTypeValueObject> {

    /**
     * Find a quantitation type by ID and vector type.
     * <p>
     * While the QT can be retrieved uniquely by ID, the purpose of this method is to ensure that it also belongs to a
     * given expression experiment and data vector type.
     */
    @Nullable
    QuantitationType loadByIdAndVectorType( Long id, ExpressionExperiment ee, Class<? extends DataVector> dataVectorType );

    /**
     * Locate a QT associated with the given ee matching the specification of the passed quantitationType, or null if
     * there isn't one.
     *
     * @return found QT
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    QuantitationType find( ExpressionExperiment ee, QuantitationType quantitationType, Class<? extends DataVector> dataVectorTypes );

    /**
     * @see QuantitationTypeDao#findByNameAndVectorType(ExpressionExperiment, String, Class)
     * @throws NonUniqueQuantitationTypeByNameException if more than one QT matches the given name and vector type
     */
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    QuantitationType findByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends DataVector> dataVectorType ) throws NonUniqueQuantitationTypeByNameException;

    @Override
    @Secured({ "GROUP_USER" })
    QuantitationType findOrCreate( QuantitationType quantitationType );

    @Override
    @Secured({ "GROUP_USER" })
    QuantitationType create( QuantitationType quantitationType );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( Collection<QuantitationType> entities );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Long id );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( QuantitationType quantitationType );

    @Override
    @Secured({ "GROUP_USER" })
    void update( Collection<QuantitationType> entities );

    @Override
    @Secured({ "GROUP_USER" })
    void update( QuantitationType quantitationType );

    List<QuantitationTypeValueObject> loadValueObjectsWithExpressionExperiment( Collection<QuantitationType> qts, ExpressionExperiment expressionExperiment );
}
