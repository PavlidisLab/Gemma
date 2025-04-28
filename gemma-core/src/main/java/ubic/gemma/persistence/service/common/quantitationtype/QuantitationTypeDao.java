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
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

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

    @Nullable
    QuantitationType loadByIdAndVectorType( Long id, ExpressionExperiment ee, Class<? extends DataVector> dataVectorType );

    List<QuantitationType> loadByDescription( String description );

    /**
     * Find a quantitation type by experiment, name and data vector type.
     * @throws org.hibernate.NonUniqueResultException if more than one QT with the name and vector type exists
     */
    @Nullable
    QuantitationType findByNameAndVectorType( ExpressionExperiment ee, String name, Class<? extends DataVector> dataVectorType );

    List<QuantitationTypeValueObject> loadValueObjectsWithExpressionExperiment( Collection<QuantitationType> qts, ExpressionExperiment ee );
}
