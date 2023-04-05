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
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseDao;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVector
 */
@SuppressWarnings("unused") // Possible external use
@Repository
public interface DesignElementDataVectorDao<T extends DesignElementDataVector> extends BaseDao<T> {

    void removeRawAndProcessed( Collection<DesignElementDataVector> vectors );

    Collection<DesignElementDataVector> findRawAndProcessed( BioAssayDimension dim );

    Collection<DesignElementDataVector> findRawAndProcessed( QuantitationType qt );

    /**
     * Thaw both raw and processed vectors.
     */
    void thawRawAndProcessed( Collection<? extends DesignElementDataVector> designElementDataVectors );

    /**
     * Creates a new instance of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector and adds from the
     * passed in <code>entities</code> collection
     *
     * @param entities the collection of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector instances to
     *                 create.
     * @return the created instances.
     */
    @Override
    Collection<T> create( Collection<T> entities );

    /**
     * @param designElementDataVector DE data vector
     * @return Creates an instance of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector and adds it to the
     * persistent store.
     */
    @Override
    T create( T designElementDataVector );

    /**
     * @param id id
     * @return Loads an instance of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector from the persistent store.
     */
    @Override
    T load( Long id );

    /**
     * Loads all entities of type {@link DesignElementDataVector}.
     *
     * @return the loaded entities.
     */
    @Override
    Collection<T> loadAll();

    void thaw( Collection<T> designElementDataVectors );

    /**
     * @param designElementDataVector Thaws associations of the given DesignElementDataVector
     */
    @SuppressWarnings("unused")
    // Possible external use
    void thaw( T designElementDataVector );

    Collection<T> find( BioAssayDimension bioAssayDimension );

    Collection<T> find( Collection<QuantitationType> quantitationTypes );

    Collection<T> find( QuantitationType quantitationType );

    Collection<T> find( ArrayDesign arrayDesign, QuantitationType quantitationType );

    Collection<T> find( Collection<CompositeSequence> designElements, QuantitationType quantitationType );

    /**
     * Find expression vectors by {@link ExpressionExperiment}.
     */
    Collection<T> findByExpressionExperiment( ExpressionExperiment ee, QuantitationType quantitationType );

    void removeDataForCompositeSequence( CompositeSequence compositeSequence );

    void removeDataForQuantitationType( QuantitationType quantitationType );
}
