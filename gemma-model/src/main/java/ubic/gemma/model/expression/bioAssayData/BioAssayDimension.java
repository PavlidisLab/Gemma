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

/**
 * Stores the order of BioAssays referred to in DataVectors. Represents a set of microarrays. It can be associated with
 * one or more BioMaterialDimensions to represent the RNA samples run on the arrays.
 */
public abstract class BioAssayDimension extends ubic.gemma.model.common.Describable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.expression.bioAssayData.BioAssayDimension}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.bioAssayData.BioAssayDimension}.
         */
        public static ubic.gemma.model.expression.bioAssayData.BioAssayDimension newInstance() {
            return new ubic.gemma.model.expression.bioAssayData.BioAssayDimensionImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.bioAssayData.BioAssayDimension}, taking all
         * possible properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.expression.bioAssayData.BioAssayDimension newInstance( String name,
                String description, Collection<ubic.gemma.model.expression.bioAssay.BioAssay> bioAssays ) {
            final ubic.gemma.model.expression.bioAssayData.BioAssayDimension entity = new ubic.gemma.model.expression.bioAssayData.BioAssayDimensionImpl();
            entity.setName( name );
            entity.setDescription( description );
            entity.setBioAssays( bioAssays );
            return entity;
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -3509905780277133617L;
    private Collection<ubic.gemma.model.expression.bioAssay.BioAssay> bioAssays = new java.util.ArrayList<ubic.gemma.model.expression.bioAssay.BioAssay>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public BioAssayDimension() {
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.expression.bioAssay.BioAssay> getBioAssays() {
        return this.bioAssays;
    }

    public void setBioAssays( Collection<ubic.gemma.model.expression.bioAssay.BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }

}