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
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.FilteringVoEnabledService;

import java.util.Collection;
import java.util.List;

/**
 * @author kelsey
 */
public interface QuantitationTypeService extends FilteringVoEnabledService<QuantitationType, QuantitationTypeValueObject> {

    /**
     * Locate a QT associated with the given ee matching the specification of the passed quantitationType, or null if
     * there isn't one.
     * 
     * @param  ee
     * @param  quantitationType
     * @return                  found QT
     */
    @Secured({ "GROUP_USER" })
    QuantitationType find( ExpressionExperiment ee, QuantitationType quantitationType );

    /**
     * Find a quantitation type by ID and vector type.
     * <p>
     * While the QT can be retrieved uniquely by ID, the purpose of this method is to ensure that it also belongs to a
     * given expression experiment and data vector type.
     */
    QuantitationType findByIdAndDataVectorType( ExpressionExperiment ee, Long id, Class<? extends DesignElementDataVector> dataVectorType );

    /**
     * Locate a QT by name.
     * @param ee
     * @param name
     * @return
     */
    QuantitationType findByNameAndDataVectorType( ExpressionExperiment ee, String name, Class<? extends DesignElementDataVector> dataVectorType );

    @Override
    @Secured({ "GROUP_USER" })
    QuantitationType findOrCreate( QuantitationType quantitationType );

    @Override
    @Secured({ "GROUP_USER" })
    void create( QuantitationType quantitationType );

    @Override
    @Secured({ "GROUP_USER" })
    void remove( Collection<QuantitationType> entities );

    @Override
    @Secured({ "GROUP_USER" })
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

    @Secured({ "GROUP_USER" })
    List<QuantitationType> loadByDescription( String description );

    @Secured({ "GROUP_USER" })
    List<QuantitationTypeValueObject> loadValueObjectsWithExpressionExperiment( Collection<QuantitationType> qts, ExpressionExperiment expressionExperiment );
}
