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

import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.expression.bioAssay.BioAssay;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stores the order of BioAssays referred to in DataVectors.
 * Note: Not a SecuredChild - maybe should be?
 */
public class BioAssayDimension extends AbstractIdentifiable {

    private List<BioAssay> bioAssays = new ArrayList<>();

    /**
     * Indicate if this BioAssayDimension resulting from merging other BioAssayDimensions.
     * TODO: switch to a regular boolean once all the entities have been migrated to the new schema.
     */
    @Nullable
    private Boolean merged;

    public List<BioAssay> getBioAssays() {
        return this.bioAssays;
    }

    public void setBioAssays( List<BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    @Nullable
    public Boolean getMerged() {
        return merged;
    }

    public void setMerged( @Nullable Boolean merged ) {
        this.merged = merged;
    }

    @Override
    public int hashCode() {
        return bioAssays.hashCode();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof BioAssayDimension ) )
            return false;
        BioAssayDimension that = ( BioAssayDimension ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        }
        return Objects.equals( getBioAssays(), that.getBioAssays() );
    }

    public static final class Factory {

        public static BioAssayDimension newInstance() {
            return new BioAssayDimension();
        }

        public static BioAssayDimension newInstance( List<BioAssay> bioAssays ) {
            final BioAssayDimension entity = newInstance();
            entity.setBioAssays( bioAssays );
            return entity;
        }
    }
}