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
package edu.columbia.gemma.expression.bioAssayData;

/**
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.bioAssayData.BioAssayDimensionService
 */
public class BioAssayDimensionServiceImpl extends
        edu.columbia.gemma.expression.bioAssayData.BioAssayDimensionServiceBase {

    /**
     * @see edu.columbia.gemma.expression.bioAssayData.BioAssayDimensionService#findOrCreate(edu.columbia.gemma.expression.bioAssayData.BioAssayDimension)
     */
    protected edu.columbia.gemma.expression.bioAssayData.BioAssayDimension handleFindOrCreate(
            edu.columbia.gemma.expression.bioAssayData.BioAssayDimension bioAssayDimension ) throws java.lang.Exception {
        return this.getBioAssayDimensionDao().findOrCreate( bioAssayDimension );
    }

}