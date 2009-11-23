/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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

import org.springframework.stereotype.Service;

/**
 * @author pavlidis
 * @version $Id$
 */
@Service
public class DesignElementDimensionServiceImpl extends
        ubic.gemma.model.expression.bioAssayData.DesignElementDimensionServiceBase {

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDimensionService#findOrCreate(ubic.gemma.model.expression.bioAssayData.DesignElementDimension)
     */
    @Override
    protected ubic.gemma.model.expression.bioAssayData.DesignElementDimension handleFindOrCreate(
            ubic.gemma.model.expression.bioAssayData.DesignElementDimension designElementDimension )
            throws java.lang.Exception {
        return this.getDesignElementDimensionDao().findOrCreate( designElementDimension );
    }

}