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
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;

/**
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVector
 */
@Repository
public interface DesignElementDataVectorDao<T extends DesignElementDataVector> extends BaseDao<T> {

    Collection<T> find( BioAssayDimension bioAssayDimension );

    Collection<T> find( Collection<QuantitationType> quantitationTypes );

    Collection<T> find( QuantitationType quantitationType );

    Collection<T> findByExpressionExperiment( ExpressionExperiment ee );

    /**
     * Thaw the given vector.
     */
    void thaw( T designElementDataVector );

    /**
     * Thaw a collection of vectors.
     */
    void thaw( Collection<T> designElementDataVectors );
}
