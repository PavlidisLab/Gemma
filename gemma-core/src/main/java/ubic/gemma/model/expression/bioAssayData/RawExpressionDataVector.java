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

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Data for one design element, across one or more bioassays, for a single quantitation type. For example, the
 * "expression profile" for a probe (gene) across a set of samples
 */
@Getter
@Setter
public class RawExpressionDataVector extends BulkExpressionDataVector {

    /**
     * Number of cells that were used to compute each value in this vector.
     *
     * @see ExpressionExperiment#getNumberOfCells()
     * @see BioAssay#getNumberOfCells()
     */
    @Nullable
    private RawExpressionDataVectorNumberOfCells numberOfCellsObject;

    @Nullable
    RawExpressionDataVectorNumberOfCells getNumberOfCellsObject() {
        return numberOfCellsObject;
    }

    void setNumberOfCellsObject( @Nullable RawExpressionDataVectorNumberOfCells numberOfCellsObject ) {
        this.numberOfCellsObject = numberOfCellsObject;
    }

    @Nullable
    public int[] getNumberOfCells() {
        return numberOfCellsObject != null ? numberOfCellsObject.getNumberOfCells() : null;
    }

    public void setNumberOfCells( @Nullable int[] numberOfCells ) {
        if ( numberOfCells != null ) {
            if ( numberOfCellsObject != null ) {
                this.numberOfCellsObject.setNumberOfCells( numberOfCells );
            } else {
                this.numberOfCellsObject = RawExpressionDataVectorNumberOfCells.Factory.newInstance( this, numberOfCells );
            }
        } else {
            this.numberOfCellsObject = null;
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof RawExpressionDataVector ) )
            return false;
        RawExpressionDataVector other = ( RawExpressionDataVector ) object;
        if ( getId() != null && other.getId() != null )
            return getId().equals( other.getId() );
        return Objects.equals( getExpressionExperiment(), other.getExpressionExperiment() )
                && Objects.equals( getQuantitationType(), other.getQuantitationType() )
                && Objects.equals( getDesignElement(), other.getDesignElement() );
    }

    public static final class Factory {

        public static RawExpressionDataVector newInstance() {
            return new RawExpressionDataVector();
        }
    }
}