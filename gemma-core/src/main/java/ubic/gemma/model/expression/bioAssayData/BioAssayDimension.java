/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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

import ubic.gemma.model.common.Describable;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import java.util.List;

/**
 * Stores the order of BioAssays referred to in DataVectors.
 * Note: Not a SecuredChild - maybe should be?
 */
public abstract class BioAssayDimension extends Describable {

    private static final long serialVersionUID = 34226197127833406L;

    private List<BioAssay> bioAssays = new java.util.ArrayList<>();

    public List<BioAssay> getBioAssays() {
        return this.bioAssays;
    }

    public void setBioAssays( List<BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    public static final class Factory {

        public static BioAssayDimension newInstance() {
            return new BioAssayDimensionImpl();
        }

        public static BioAssayDimension newInstance( String name, String description, List<BioAssay> bioAssays ) {
            final BioAssayDimension entity = new BioAssayDimensionImpl();
            entity.setName( name );
            entity.setDescription( description );
            entity.setBioAssays( bioAssays );
            return entity;
        }

    }

}