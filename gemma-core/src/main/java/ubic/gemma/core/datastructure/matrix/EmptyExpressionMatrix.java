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

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Collection;
import java.util.List;

/**
 * Used to make a 'dummy matrix' that has the column information populated. This is useful for processing where we want
 * the sample information organized,but not the data. Data access operations are not supported.
 *
 * @author paul
 */
public class EmptyExpressionMatrix extends BaseExpressionDataMatrix<Object> {

    private static final long serialVersionUID = 1L;
    private final int numCols;

    public EmptyExpressionMatrix( BioAssayDimension ba ) {
        super();
        super.init();
        CompositeSequence dummy = CompositeSequence.Factory.newInstance();
        this.bioAssayDimensions.put( dummy, ba );
        this.numCols = this.setUpColumnElements();
    }

    public EmptyExpressionMatrix( Collection<BioAssayDimension> dims ) {
        super();
        super.init();
        long i = -1;
        for ( BioAssayDimension ba : dims ) {
            CompositeSequence dummy = CompositeSequence.Factory.newInstance();
            dummy.setId( i-- );
            this.bioAssayDimensions.put( dummy, ba );
        }

        this.numCols = this.setUpColumnElements();
    }

    @Override
    public int columns() {
        return numCols;
    }

    @Override
    public Object get( CompositeSequence designElement, BioAssay bioAssay ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get( int row, int column ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[][] get( List<CompositeSequence> designElements, List<BioAssay> bioAssays ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getColumn( BioAssay bioAssay ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getColumn( int column ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[][] getColumns( List<BioAssay> bioAssays ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[][] getRawMatrix() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getRow( CompositeSequence designElement ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] getRow( int index ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMissingValues() {
        return false;
    }

    @Override
    public int rows() {
        return 0;
    }

    @Override
    public void set( int row, int column, Object value ) {
        throw new UnsupportedOperationException();

    }

    @Override
    protected void vectorsToMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {
        throw new UnsupportedOperationException();
    }

}
