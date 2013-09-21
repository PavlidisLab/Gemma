/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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

/**
 * @see ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector
 */
public class RawExpressionDataVectorImpl extends ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -7410374297463625206L;

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof DesignElementDataVector ) ) {
            return false;
        }
        final DesignElementDataVector that = ( DesignElementDataVector ) object;
        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {
            if ( this.getDesignElement() == null || that.getDesignElement() == null ) {
                return false;
            }

            if ( this.getQuantitationType() == null || that.getQuantitationType() == null ) {
                return false;
            }

            if ( this.getBioAssayDimension() == null || that.getBioAssayDimension() == null ) {
                return false;
            }

            return this.getDesignElement().getName().equals( that.getDesignElement().getName() )
                    && this.getQuantitationType().getName().equals( that.getQuantitationType().getName() )
                    && this.getBioAssayDimension().getName().equals( that.getBioAssayDimension().getName() );
        }
        return true;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( this.getId() == null ? computeHashCode() : this.getId().hashCode() );

        return hashCode;
    }

    @Override
    /*
     * @see ubic.gemma.model.common.Describable#toString()
     */
    public String toString() {
        return this.getClass().getSimpleName()
                + ( this.getId() == null ? "" : " Id=" + this.getId() )
                + ( this.getDesignElement() == null ? "" : " DE=" + this.getDesignElement().getName() )
                + ( this.getQuantitationType() == null ? "" : " QT=" + this.getQuantitationType().getName() )
                + ( this.getExpressionExperiment() == null ? "" : " EE=" + this.getExpressionExperiment().getName()
                        + ", " + this.getData().length + " bytes" );

    }

    private int computeHashCode() {
        int hashCode = 0;

        // if it matters, just using the designelement name hashcode would probably be nearly good enough.
        if ( this.getDesignElement() != null ) {
            hashCode += this.getDesignElement().getName().hashCode();
        }

        if ( this.getQuantitationType() != null ) {
            hashCode += this.getQuantitationType().getName().hashCode();
        }

        if ( this.getBioAssayDimension() != null ) {
            hashCode += this.getBioAssayDimension().getName().hashCode();
        }

        // least important as it is unlikely we would have multiple expression experiments in the same collection.
        // if ( this.getExpressionExperiment() != null ) {
        // hashCode += this.getExpressionExperiment().getName().hashCode();
        // }

        return hashCode;
    }

}