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
package ubic.gemma.persistence.service.expression.bioAssay;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @see BioAssay
 */
public interface BioAssayDao extends FilteringVoEnabledDao<BioAssay, BioAssayValueObject> {

    String OBJECT_ALIAS = "ba";

    @Nullable
    BioAssay findByShortName( String shortName );

    Collection<BioAssayDimension> findBioAssayDimensions( BioAssay bioAssay );

    Collection<BioAssay> findByAccession( String accession );

    Collection<BioAssaySet> getBioAssaySets( BioAssay bioAssay );

    /**
     * @see BioAssayValueObject#BioAssayValueObject(BioAssay, Map, BioAssay, boolean, boolean)
     */
    List<BioAssayValueObject> loadValueObjects( Collection<BioAssay> entities,
            @Nullable Map<ArrayDesign, ArrayDesignValueObject> ad2vo,
            @Nullable Map<BioAssay, BioAssay> assay2sourceAssayMap,
            boolean basic, boolean allFactorValues );
}
