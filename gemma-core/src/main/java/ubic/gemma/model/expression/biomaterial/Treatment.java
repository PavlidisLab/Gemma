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

package ubic.gemma.model.expression.biomaterial;

import ubic.gemma.model.common.AbstractDescribable;

import java.io.Serializable;

/**
 * @author Paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class Treatment extends AbstractDescribable implements Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 265514192370169605L;
    private Integer orderApplied = 1;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public Treatment() {
    }

    /**
     * @return The order in which this treatment was applied to the biomaterial, relative to the other treatments.
     */
    public Integer getOrderApplied() {
        return this.orderApplied;
    }

    public void setOrderApplied( Integer orderApplied ) {
        this.orderApplied = orderApplied;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof Treatment ) )
            return false;
        Treatment that = ( Treatment ) object;
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return false;
        }
    }

    public static final class Factory {

        public static ubic.gemma.model.expression.biomaterial.Treatment newInstance() {
            return new ubic.gemma.model.expression.biomaterial.Treatment();
        }

    }

}