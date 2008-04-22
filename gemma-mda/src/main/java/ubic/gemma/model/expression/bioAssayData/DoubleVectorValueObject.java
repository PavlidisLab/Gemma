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
import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * Simple wrapper for a double[] that is derived from a DesignElementDataVector.
 * 
 * @author paul
 * @version $Id$
 */
public class DoubleVectorValueObject extends DataVectorValueObject {

    boolean masked = false;
    double[] data = null;

    public DoubleVectorValueObject( DesignElementDataVector dedv ) {
        super( dedv );
        QuantitationType qt = dedv.getQuantitationType();
        if ( !qt.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
            throw new IllegalArgumentException( "Can only store double vectors, got " + qt + " "
                    + qt.getRepresentation() );
        }
        if ( qt.getIsMaskedPreferred() ) {
            this.masked = true;
        }
        this.data = byteArrayConverter.byteArrayToDoubles( dedv.getData() );
    }

    public double[] getData() {
        return data;
    }

    public boolean isMasked() {
        return masked;
    }

    public void setMasked( boolean masked ) {
        this.masked = masked;
    }

}
