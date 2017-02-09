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

import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;

/**
 * This is only a separate entity to avoid having the blob stored in the ANALYSIS table.
 */
public abstract class SampleCoexpressionMatrix implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 4340621792397336563L;

    /**
     * Constructs new instances of {@link SampleCoexpressionMatrix}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link SampleCoexpressionMatrix}.
         */
        public static SampleCoexpressionMatrix newInstance() {
            return new SampleCoexpressionMatrixImpl();
        }

        /**
         * Constructs a new instance of {@link SampleCoexpressionMatrix}, taking all possible properties (except the
         * identifier(s))as arguments.
         */
        public static SampleCoexpressionMatrix newInstance( byte[] coexpressionMatrix,
                BioAssayDimension bioAssayDimension ) {
            final SampleCoexpressionMatrix entity = new SampleCoexpressionMatrixImpl();
            entity.setCoexpressionMatrix( coexpressionMatrix );
            entity.setBioAssayDimension( bioAssayDimension );
            return entity;
        }
    }

    private BioAssayDimension bioAssayDimension;

    private byte[] coexpressionMatrix;

    private Long id;

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
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public BioAssayDimension getBioAssayDimension() {
        return this.bioAssayDimension;
    }

    /**
     * 
     */
    public byte[] getCoexpressionMatrix() {
        return this.coexpressionMatrix;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setBioAssayDimension( BioAssayDimension bioAssayDimension ) {
        this.bioAssayDimension = bioAssayDimension;
    }

    public void setCoexpressionMatrix( byte[] coexpressionMatrix ) {
        this.coexpressionMatrix = coexpressionMatrix;
    }

    public void setId( Long id ) {
        this.id = id;
    }

}