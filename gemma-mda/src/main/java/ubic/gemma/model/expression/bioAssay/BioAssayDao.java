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

import java.util.Collection;

import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.expression.bioAssay.BioAssay
 */
public interface BioAssayDao extends BaseDao<BioAssay> {
    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * /**
     */
    public ubic.gemma.model.expression.bioAssay.BioAssay find( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay );

    /**
     * 
     */
    public java.util.Collection<BioAssayDimension> findBioAssayDimensions(
            ubic.gemma.model.expression.bioAssay.BioAssay bioAssay );

    /**
     * @param accession
     * @return
     */
    public Collection<BioAssay> findByAccession( String accession );

    /**
     * 
     */
    public ubic.gemma.model.expression.bioAssay.BioAssay findOrCreate(
            ubic.gemma.model.expression.bioAssay.BioAssay bioAssay );

    /**
     * @param bioAssays
     * @return
     */
    public Collection<BioAssay> thaw( Collection<BioAssay> bioAssays );

    /**
     * 
     */
    public void thaw( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay );

    /**
     * @param ids
     * @return
     */
    public Collection<BioAssay> loadValueObjects( Collection<Long> ids );

}
