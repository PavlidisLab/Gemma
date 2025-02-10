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

import lombok.Data;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import java.util.Arrays;

/**
 * This is used to represent missing value data.
 *
 * @author paul
 */
@Data
public class BooleanVectorValueObject extends DataVectorValueObject {

    private static final long serialVersionUID = 1L;

    private boolean[] data;

    public BooleanVectorValueObject( BulkExpressionDataVector dedv, ExpressionExperimentValueObject eevo, QuantitationTypeValueObject qtvo, BioAssayDimensionValueObject badvo ) {
        super( dedv, eevo, qtvo, badvo, null );
        if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.BOOLEAN ) ) {
            throw new IllegalArgumentException( "Can only store boolean vectors, got " + dedv.getQuantitationType() );
        }
        this.data = dedv.getDataAsBooleans();
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( !( obj instanceof BooleanVectorValueObject ) ) {
            return false;
        }
        BooleanVectorValueObject other = ( BooleanVectorValueObject ) obj;
        return super.equals( obj )
                && Arrays.equals( data, other.data );
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
