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

import org.hibernate.Criteria;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @see QuantitationType
 */
public interface QuantitationTypeDao extends FilteringVoEnabledDao<QuantitationType, QuantitationTypeValueObject> {

    /**
     * Load a QT from an experiment by ID and vector type.
     * <p>
     * This is used to make sure that the QT is used by a specific vector type.
     */
    @Nullable
    QuantitationType loadByIdAndVectorType( Long id, ExpressionExperiment ee, Class<? extends DataVector> dataVectorType );

    /**
     * Find a QT matching the given template as per {@link BusinessKey#addRestrictions(Criteria, QuantitationType)}.
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

    /**
     * Load {@link QuantitationTypeValueObject} in the context of an associated expression experiment.
     * <p>
     * The resulting VO has a few more fields filled which would be otherwise hidden from JSON serialization.
     * @see QuantitationTypeValueObject#QuantitationTypeValueObject(QuantitationType, ExpressionExperiment, Class)
     */
    List<QuantitationTypeValueObject> loadValueObjectsWithExpressionExperiment( Collection<QuantitationType> qts, ExpressionExperiment ee );
}
