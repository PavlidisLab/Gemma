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
package ubic.gemma.model.analysis.expression.coexpression;

import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;

import java.util.Arrays;
import java.util.Objects;

/**
 * Holds the data of the sample coexpression matrix
 */
public class SampleCoexpressionMatrix extends AbstractIdentifiable {

    private BioAssayDimension bioAssayDimension;
    private byte[] coexpressionMatrix;

    public BioAssayDimension getBioAssayDimension() {
        return this.bioAssayDimension;
    }

    public void setBioAssayDimension( BioAssayDimension bioAssayDimension ) {
        this.bioAssayDimension = bioAssayDimension;
    }

    public byte[] getCoexpressionMatrix() {
        return this.coexpressionMatrix;
    }

    public void setCoexpressionMatrix( byte[] coexpressionMatrix ) {
        this.coexpressionMatrix = coexpressionMatrix;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        return Objects.hash( bioAssayDimension );
    }

    /**
     * Returns <code>true</code> if the argument is an SampleCoexpressionMatrix instance and all identifiers for this
     * entity equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof SampleCoexpressionMatrix ) ) {
            return false;
        }
        final SampleCoexpressionMatrix that = ( SampleCoexpressionMatrix ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return Objects.equals( getBioAssayDimension(), that.getBioAssayDimension() )
                    && Arrays.equals( getCoexpressionMatrix(), that.getCoexpressionMatrix() );
        }
    }

    public static class Factory {

        public static SampleCoexpressionMatrix newInstance( BioAssayDimension bioAssayDimension, byte[] coexpressionMatrix ) {
            SampleCoexpressionMatrix matrix = new SampleCoexpressionMatrix();
            matrix.setBioAssayDimension( bioAssayDimension );
            matrix.setCoexpressionMatrix( coexpressionMatrix );
            return matrix;
        }
    }
}