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

import java.util.Collection;
import java.util.List;

import ubic.gemma.model.common.Describable;
import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * Stores the order of BioAssays referred to in DataVectors. Represents a set of microarrays. It can be associated with
 * one or more BioMaterialDimensions to represent the RNA samples run on the arrays.
 */
public abstract class BioAssayDimension extends Describable {

    /**
     * Constructs new instances of {@link BioAssayDimension}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link BioAssayDimension}.
         */
        public static BioAssayDimension newInstance() {
            return new BioAssayDimensionImpl();
        }

        /**
         * Constructs a new instance of {@link BioAssayDimension}, taking all possible properties (except the
         * identifier(s))as arguments.
         */
        public static BioAssayDimension newInstance( String name, String description, List<BioAssay> bioAssays ) {
            final BioAssayDimension entity = new BioAssayDimensionImpl();
            entity.setName( name );
            entity.setDescription( description );
            entity.setBioAssays( bioAssays );
            return entity;
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private List<BioAssay> bioAssays = new java.util.ArrayList<BioAssay>();

    /**
     * 
     */
    public List<BioAssay> getBioAssays() {
        return this.bioAssays;
    }

    public void setBioAssays( List<BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }

}