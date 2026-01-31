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

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @see QuantitationType
 */
public interface QuantitationTypeDao extends FilteringVoEnabledDao<QuantitationType, QuantitationTypeValueObject> {

    /**
     * Obtain all the data vector types that are associated with quantitation types.
     */
    Collection<Class<? extends DataVector>> getVectorTypes();

    @Nullable
    QuantitationType loadById( Long id, ExpressionExperiment ee );

    /**
     * Load a QT from an experiment by ID and vector type.
     * <p>
     * This is used to make sure that the QT is used by a specific vector type.
     */
    @Nullable
    QuantitationType loadByIdAndVectorType( Long id, ExpressionExperiment ee, Class<? extends DataVector> dataVectorType );

    /**
     * Retrieve all the QTs associated with the given experiment.
     * <p>
     * This method will scan through all the vector types and also the denormalized QTs in {@link ExpressionExperiment#getQuantitationTypes()}.
     */
    Map<Class<? extends DataVector>, Set<QuantitationType>> findByExpressionExperiment( ExpressionExperiment ee );

    Collection<QuantitationType> findByExpressionExperiment( ExpressionExperiment ee, Class<? extends DataVector> dataVectorType );

    /**
     * Retrieve all the QTs associated with the given experiment and dimension.
     * <p>
     * This method will only consider vectors of type {@link ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector}.
     */
    Collection<QuantitationType> findByExpressionExperimentAndDimension( ExpressionExperiment expressionExperiment, BioAssayDimension dimension );

    /**
     * Retrieve all the QTs associated with the given experiment and dimension.
     */
    Collection<QuantitationType> findByExpressionExperimentAndDimension( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, Collection<Class<? extends BulkExpressionDataVector>> vectorTypes );

    /**
     * Find a QT matching the given template as per {@link QuantitationType#equals(Object)}.
     */
    @Nullable
    @Override
    QuantitationType find( QuantitationType entity );

    /**
     * Find a QT matching the given template for the given data vector type as per {@link #find(QuantitationType)}.
     */
    @Nullable
    QuantitationType find( QuantitationType entity, Class<? extends DataVector> dataVectorType );

    /**
     * Find a QT matching the given template as per {@link #find(QuantitationType)} in a given experiment for and for a
     * given data vector type.
     * @param ee               experiment to restrict the search to
     * @param quantitationType QT template to find
     * @param dataVectorTypes  types of data vectors to look into, if non-null
     * @return found QT or null if there isn't one
     */
    @Nullable
    QuantitationType find( ExpressionExperiment ee, QuantitationType quantitationType, @Nullable Set<Class<? extends DataVector>> dataVectorTypes );

    /**
     * Find a quantitation type by experiment, name and data vector type.
     * @throws org.hibernate.NonUniqueResultException if more than one QT with the name and vector type exists
     */
    @Nullable
    QuantitationType findByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends DataVector> dataVectorType );

    <T extends DataVector> Collection<QuantitationType> findAllByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends T> vectorType );

    /**
     * Find a QT as per {@link #find(QuantitationType, Class)} and if none is found, create a new one with {@link #create(QuantitationType, Class)}.
     */
    QuantitationType findOrCreate( QuantitationType quantitationType, Class<? extends DataVector> dataVectorType );

    QuantitationType create( QuantitationType quantitationType, Class<? extends DataVector> dataVectorType );

    /**
     * Load {@link QuantitationTypeValueObject} in the context of an associated expression experiment.
     * <p>
     * The resulting VO has a few more fields filled which would be otherwise hidden from JSON serialization.
     * @see QuantitationTypeValueObject#QuantitationTypeValueObject(QuantitationType, ExpressionExperiment, Class)
     */
    List<QuantitationTypeValueObject> loadValueObjectsWithExpressionExperiment( Collection<QuantitationType> qts, ExpressionExperiment ee );

    /**
     * @return a data vector type, or null if the QT is not associated to any vectors
     */
    @Nullable
    Class<? extends DataVector> getDataVectorType( QuantitationType qt );

    /**
     * Obtain the vector types that are mapped and subclass of the given vector type.
     */
    <T extends DataVector> Collection<Class<? extends T>> getMappedDataVectorTypes( Class<T> vectorType );
}
