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
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseService;
import ubic.gemma.persistence.service.FilteringVoEnabledService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
     * @see QuantitationTypeDao#reload(Object)
     */
    QuantitationType reload( QuantitationType quantitationType );

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

    /**
     * Find all the QT associated to the given experiment, name and vector type.
     * <p>
     * This is useful if more than one QT with the same name have been created.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    <T extends DataVector> Collection<QuantitationType> findAllByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends T> vectorType );

    /**
     * Find all the QT associated to the given experiment.
     * <p>
     * QTs are grouped by which vector type they are associated with. If no vector is found for a given QT, {@code null}
     * will be used as their type in the returned mapping.
     * @see QuantitationTypeDao#findByExpressionExperiment(ExpressionExperiment)
     */
    Map<Class<? extends DataVector>, Set<QuantitationType>> findByExpressionExperiment( ExpressionExperiment ee );

    /**
     * Find all the QT associated to the given experiment and data vector type.
     */
    <T extends DataVector> Collection<QuantitationType> findByExpressionExperiment( ExpressionExperiment ee, Class<? extends T> dataVectorType );

    <T extends DataVector> Collection<QuantitationType> findByExpressionExperiment( ExpressionExperiment ee, Collection<Class<? extends T>> vectorTypes );

    /**
     * @see QuantitationTypeDao#findByExpressionExperimentAndDimension(ExpressionExperiment, BioAssayDimension)
     */
    Collection<QuantitationType> findByExpressionExperimentAndDimension( ExpressionExperiment expressionExperiment, BioAssayDimension dimension );

    Collection<QuantitationType> findByExpressionExperimentAndDimension( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, Collection<Class<? extends BulkExpressionDataVector>> dataVectorTypes );

    @Override
    @Secured({ "GROUP_USER" })
    QuantitationType findOrCreate( QuantitationType quantitationType );

    /**
     * @see QuantitationTypeDao#findOrCreate(QuantitationType, Class)
     */
    @Secured("GROUP_USER")
    QuantitationType findOrCreate( QuantitationType quantitationType, Class<? extends DataVector> dataVectorType );

    @Override
    @Secured({ "GROUP_USER" })
    QuantitationType create( QuantitationType quantitationType );

    /**
     * Create a new QuantitationType for the given vector type.
     * <p>
     * For now, this is just doing the same thing as {@link #create(QuantitationType)}, but with extra validations.
     */
    @Secured({ "GROUP_USER" })
    QuantitationType create( QuantitationType quantitationType, Class<? extends DataVector> dataVectorType );

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

    @Nullable
    Class<? extends DataVector> getDataVectorType( QuantitationType qt );

    Map<QuantitationType, Class<? extends DataVector>> getDataVectorTypes( Collection<QuantitationType> qts );

    /**
     * Infer all the mapped vector types that are subclasses of the given vector type.
     */
    <T extends DataVector> Collection<Class<? extends T>> getMappedDataVectorType( Class<T> vectorType );
}
