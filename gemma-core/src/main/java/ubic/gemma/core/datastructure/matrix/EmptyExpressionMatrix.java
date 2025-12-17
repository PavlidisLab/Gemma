/*
 * The Gemma project
 *
 * Copyright (c) 2007-2009 University of British Columbia
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
package ubic.gemma.core.datastructure.matrix;

import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Used to make a 'dummy matrix' that has the column information populated. This is useful for processing where we want
 * the sample information organized,but not the data. Data access operations are not supported.
 *
 * @author paul
 */
public class EmptyExpressionMatrix extends AbstractMultiAssayExpressionDataMatrix<Object> {

    private static final Object[][] EMPTY_MATRIX = new Object[0][0];
    private static final Object[] EMPTY_COLUMN = new Object[0];

    public EmptyExpressionMatrix( @Nullable ExpressionExperiment ee, BioAssayDimension ba ) {
        super( ee, Collections.singleton( ba ) );
    }

    public EmptyExpressionMatrix( @Nullable ExpressionExperiment ee, Collection<BioAssayDimension> dims ) {
        super( ee, dims );
    }

    @Override
    public Object get( int row, int column ) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public Object[] getColumn( int column ) {
        if ( column >= 0 && column < columns() ) {
            return EMPTY_COLUMN;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public Object[][] getRawMatrix() {
        return EMPTY_MATRIX;
    }

    @Override
    public BulkExpressionDataMatrix<Object> sliceColumns( List<BioMaterial> bioMaterials ) {
        Set<BioAssayDimension> newDims = getBioAssayDimensions().stream()
                .map( dim -> {
                    BioAssayDimension newDim = new BioAssayDimension();
                    newDim.setBioAssays( dim.getBioAssays().stream()
                            .filter( ba -> bioMaterials.contains( ba.getSampleUsed() ) )
                            .collect( Collectors.toList() ) );
                    return newDim;
                } ).collect( Collectors.toSet() );
        return new EmptyExpressionMatrix( getExpressionExperiment(), newDims );
    }

    @Override
    public EmptyExpressionMatrix sliceColumns( List<BioMaterial> bioMaterials, BioAssayDimension dimension ) {
        return new EmptyExpressionMatrix( getExpressionExperiment(), dimension );
    }

    @Override
    public Object[] getRow( int index ) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public EmptyExpressionMatrix sliceRows( List<CompositeSequence> designElements ) {
        if ( designElements.isEmpty() ) {
            return this;
        }
        throw new IllegalArgumentException( "None of the requested design elements are present in the matrix." );
    }

    @Override
    public boolean hasMissingValues() {
        return false;
    }

    @Override
    protected String format( int row, int column ) {
        throw new IndexOutOfBoundsException();
    }
}
