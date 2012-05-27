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
package ubic.gemma.datastructure.matrix;

import java.util.Collection;
import java.util.List;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Used to make a 'dummy matrix' that has the column information populated. This is useful for processing where we want
 * the sample information organized,but not the data. Data access operations are not supported.
 * 
 * @author paul
 * @version $Id$
 */
public class EmptyExpressionMatrix extends BaseExpressionDataMatrix<Object> {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private int numCols;

    @Override
    protected void vectorsToMatrix( Collection<? extends DesignElementDataVector> vectors ) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param ba
     */
    public EmptyExpressionMatrix( BioAssayDimension ba ) {
        super();
        super.init();
        CompositeSequence dummy = CompositeSequence.Factory.newInstance();
        this.bioAssayDimensions.put( dummy, ba );
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
    public Object[] getColumn( Integer column ) {
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
    public Object[] getRow( Integer index ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[][] getRows( List<CompositeSequence> designElements ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int rows() {
        return 0;
    }

    @Override
    public void set( int row, int column, Object value ) {
        throw new UnsupportedOperationException();

    }

}
