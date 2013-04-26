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
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * @author Paul
 * @version $Id$
 */
public interface DesignElementDataVectorService {

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public Collection<? extends DesignElementDataVector> create( Collection<? extends DesignElementDataVector> vectors );

    /**
     * Load all vectors meeting the criteria
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    public Collection<? extends DesignElementDataVector> find( ArrayDesign arrayDesign,
            QuantitationType quantitationType );

    /**
     * @param bioAssayDimension
     * @return any vectors that reference the given bioAssayDimensin
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    public Collection<? extends DesignElementDataVector> find( BioAssayDimension bioAssayDimension );

    /**
     * 
     */
    @Secured({ "GROUP_ADMIN" })
    public Collection<? extends DesignElementDataVector> find( Collection<QuantitationType> quantitationTypes );

    /**
     * Load all vectors meeting the criteria
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_DATAVECTOR_COLLECTION_READ" })
    public Collection<? extends DesignElementDataVector> find( QuantitationType quantitationType );

    /**
     * 
     */
    @Secured({ "GROUP_ADMIN" })
    public DesignElementDataVector load( java.lang.Long id );

    /**
     * 
     */
    @Secured({ "GROUP_ADMIN" })
    public void remove( Collection<? extends DesignElementDataVector> vectors );

    /**
     * 
     */
    @Secured({ "GROUP_ADMIN" })
    public void remove( RawExpressionDataVector designElementDataVector );

    /**
     * <p>
     * remove Design Element Data Vectors and Probe2ProbeCoexpression entries for a specified CompositeSequence.
     * </p>
     */
    @Secured({ "GROUP_ADMIN" })
    public void removeDataForCompositeSequence( CompositeSequence compositeSequence );

    /**
     * Removes the DesignElementDataVectors and Probe2ProbeCoexpressions for a quantitation type, given a
     * QuantitationType (which always comes from a specific ExpressionExperiment)
     */
    @Secured({ "GROUP_ADMIN" })
    public void removeDataForQuantitationType( QuantitationType quantitationType );

    /**
     * @return
     */
    public void thaw( Collection<? extends DesignElementDataVector> designElementDataVectors );

    /**
     * <p>
     * updates an already existing dedv
     * </p>
     */
    @Secured({ "GROUP_USER" })
    public void update( DesignElementDataVector dedv );

    /**
     * <p>
     * updates a collection of designElementDataVectors
     * </p>
     */
    @Secured({ "GROUP_USER" })
    public void update( java.util.Collection<? extends DesignElementDataVector> dedvs );

}
