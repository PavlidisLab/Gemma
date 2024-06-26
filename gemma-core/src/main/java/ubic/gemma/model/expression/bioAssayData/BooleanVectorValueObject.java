/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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

import ubic.gemma.model.common.quantitationtype.PrimitiveType;

/**
 * This is used to represent missing value data.
 *
 * @author paul
 *
 */
public class BooleanVectorValueObject extends DataVectorValueObject {
   
    private static final long serialVersionUID = 1L;

    private final boolean[] data;

    public BooleanVectorValueObject( DesignElementDataVector dedv, BioAssayDimensionValueObject badvo ) {
        super( dedv, badvo );
        if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.BOOLEAN ) ) {
            throw new IllegalArgumentException( "Can only store boolean vectors, got " + dedv.getQuantitationType() );
        }

        this.data = byteArrayConverter.byteArrayToBooleans( dedv.getData() );
    }

    public boolean[] getData() {
        return data;
    }
}
