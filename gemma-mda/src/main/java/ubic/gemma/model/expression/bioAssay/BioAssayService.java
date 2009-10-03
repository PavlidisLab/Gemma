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
package ubic.gemma.model.expression.bioAssay;

import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;

/**
 * 
 */
public interface BioAssayService extends ubic.gemma.model.common.AuditableService {

    /**
     * <p>
     * Associates a bioMaterial with a specified bioAssay.
     * </p>
     */
    public void addBioMaterialAssociation( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay,
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial );

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * <p>
     * Locate all BioAssayDimensions in which the selected BioAssay occurs
     * </p>
     */
    public java.util.Collection<BioAssayDimension> findBioAssayDimensions(
            ubic.gemma.model.expression.bioAssay.BioAssay bioAssay );

    /**
     * 
     */
    public ubic.gemma.model.expression.bioAssay.BioAssay findOrCreate(
            ubic.gemma.model.expression.bioAssay.BioAssay bioAssay );

    public BioAssay create( BioAssay bioAssay );

    /**
     * 
     */
    public ubic.gemma.model.expression.bioAssay.BioAssay load( java.lang.Long id );

    /**
     * 
     */
    public java.util.Collection<BioAssay> loadAll();

    /**
     * 
     */
    public void remove( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay );

    /**
     * <p>
     * Removes the association between a specific bioMaterial and a bioAssay.
     * </p>
     */
    public void removeBioMaterialAssociation( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay,
            ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial );

    /**
     * 
     */
    public void thaw( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay );

    /**
     * 
     */
    public void update( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay );

}
