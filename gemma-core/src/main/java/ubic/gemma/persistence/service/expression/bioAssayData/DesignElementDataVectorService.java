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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Collection;

/**
 * @author Paul
 */
public interface DesignElementDataVectorService {

    java.lang.Integer countAll();

    @Secured({ "GROUP_USER" })
    Collection<? extends DesignElementDataVector> create( Collection<? extends DesignElementDataVector> vectors );

    /**
     * Load all vectors meeting the criteria
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    Collection<? extends DesignElementDataVector> find( ArrayDesign arrayDesign, QuantitationType quantitationType );

    /**
     * @return any vectors that reference the given bioAssayDimensin
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    Collection<? extends DesignElementDataVector> find( BioAssayDimension bioAssayDimension );

    @Secured({ "GROUP_ADMIN" })
    Collection<? extends DesignElementDataVector> find( Collection<QuantitationType> quantitationTypes );

    /**
     * Load all vectors meeting the criteria
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    Collection<? extends DesignElementDataVector> find( QuantitationType quantitationType );

    @Secured({ "GROUP_ADMIN" })
    DesignElementDataVector load( java.lang.Long id );

    @Secured({ "GROUP_ADMIN" })
    void remove( Collection<? extends DesignElementDataVector> vectors );

    @Secured({ "GROUP_ADMIN" })
    void remove( RawExpressionDataVector designElementDataVector );

    @Secured({ "GROUP_ADMIN" })
    void removeDataForCompositeSequence( CompositeSequence compositeSequence );

    @Secured({ "GROUP_ADMIN" })
    void removeDataForQuantitationType( QuantitationType quantitationType );

    void thaw( Collection<? extends DesignElementDataVector> designElementDataVectors );

    /**
     * <p>
     * updates an already existing dedv
     * </p>
     */
    @Secured({ "GROUP_USER" })
    void update( DesignElementDataVector dedv );

    /**
     * <p>
     * updates a collection of designElementDataVectors
     * </p>
     */
    @Secured({ "GROUP_USER" })
    void update( java.util.Collection<? extends DesignElementDataVector> dedvs );

}
