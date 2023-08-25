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
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.util.Specification;

import java.util.Collection;
import java.util.List;

/**
 * @see QuantitationType
 */
public interface QuantitationTypeDao extends FilteringVoEnabledDao<QuantitationType, QuantitationTypeValueObject> {


    List<QuantitationType> loadByDescription( String description );

    /**
     * Locate a QT associated with the given ee matching the specification of the passed quantitationType, or null if
     * there isn't one.
     *
     * @return found QT
     */
    QuantitationType find( ExpressionExperiment ee, Specification<QuantitationType> quantitationType );

    QuantitationType findByIdAndDataVectorType( ExpressionExperiment ee, Long id, Class<? extends DesignElementDataVector> dataVectorClass );

    /**
     * Retrieve the quantitation type matching.
     */
    QuantitationType findByNameAndDataVectorType( ExpressionExperiment ee, String name, Class<? extends DesignElementDataVector> dataVectorClass );

    List<QuantitationTypeValueObject> loadValueObjectsWithExpressionExperiment( Collection<QuantitationType> qts, ExpressionExperiment ee );
}
