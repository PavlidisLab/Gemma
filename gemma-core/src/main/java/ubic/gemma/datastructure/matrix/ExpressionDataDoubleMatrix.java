/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.dataStructure.matrix.SparseRaggedDoubleMatrix2DNamed;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A data structure that holds a reference to the data for a given expression experiment. The data can be queried by row
 * or column, returning data for a specific DesignElement or data for a specific BioAssay. The data itself is backed by
 * a SparseRaggedDoubleMatrix2DNamed, which allows for each row to contain a different number of values.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataDoubleMatrix implements ExpressionDataMatrix {
    private Log log = LogFactory.getLog( ExpressionDataDoubleMatrix.class );

    private DoubleMatrixNamed matrix;

    private Map<DesignElement, Integer> rowMap;
    private Map<BioAssay, Integer> columnMap;

    private ByteArrayConverter byteArrayConverter;

    public ExpressionDataDoubleMatrix( ExpressionExperiment expressionExperiment, QuantitationType quantitationType ) {

    }

    /**
     * @param expressionExperiment
     * @param designElements
     * @param quantitationType
     */
    public ExpressionDataDoubleMatrix( ExpressionExperiment expressionExperiment,
            Collection<DesignElement> designElements, QuantitationType quantitationType ) {

        matrix = new SparseRaggedDoubleMatrix2DNamed();

        rowMap = new HashMap<DesignElement, Integer>();

        columnMap = new HashMap<BioAssay, Integer>();

        byteArrayConverter = new ByteArrayConverter();

        int i = 0;
        for ( DesignElement designElement : designElements ) {

            DesignElementDataVector vectorOfInterest = null;
            Collection<DesignElementDataVector> vectors = designElement.getDesignElementDataVectors();
            for ( DesignElementDataVector vector : vectors ) {
                QuantitationType vectorQuantitationType = vector.getQuantitationType();
                if ( vectorQuantitationType.getType().equals( quantitationType.getType() ) ) {
                    vectorOfInterest = vector;
                    break;
                }
            }
            if ( vectorOfInterest == null ) {
                log.warn( "Vector not found for given quantitation type.  Skipping ..." );
                continue;
            }

            byte[] byteData = vectorOfInterest.getData();
            double[] rawData = byteArrayConverter.byteArrayToDoubles( byteData );
            for ( int j = 0; j < rawData.length; j++ ) {
                matrix.set( i, j, rawData[j] );
            }
            rowMap.put( designElement, i );
            matrix.addRowName( designElement.getName(), i );
        }
    }

    public ExpressionDataDoubleMatrix( Collection<DesignElementDataVector> dataVectors ) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(ubic.gemma.model.expression.designElement.DesignElement,
     *      ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Double get( DesignElement designElement, BioAssay bioAssay ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#get(java.util.List, java.util.List)
     */
    public Double[][] get( List designElements, List bioAssays ) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumn(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    public Double[] getColumn( BioAssay bioAssay ) {
        if ( !columnMap.containsKey( bioAssay ) ) {
            return null;
        }
        double[] rawResult = this.matrix.getColumn( columnMap.get( bioAssay ) );
        assert rawResult != null;
        Double[] result = new Double[rawResult.length];
        for ( int i = 0; i < rawResult.length; i++ ) {
            result[i] = rawResult[i];
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumns(java.util.List)
     */
    public Double[][] getColumns( List bioAssays ) {
        // if ( bioAssays == null ) {
        // return null;
        // }
        //
        // List<BioAssay> assays = bioAssays;
        //
        // // Double[][] result = new Double[][assays.size()];
        // for ( BioAssay assay : assays ) {
        // Double[] columnResult = getColumn( assay );
        // }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getMatrix()
     */
    public Double[][] getMatrix() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRow(ubic.gemma.model.expression.designElement.DesignElement)
     */
    public Double[] getRow( DesignElement designElement ) {
        if ( !rowMap.containsKey( designElement ) ) {
            return null;
        }

        double[] rawResult = this.matrix.getRow( rowMap.get( designElement ) );
        assert rawResult != null;
        Double[] result = new Double[rawResult.length];
        for ( int i = 0; i < rawResult.length; i++ ) {
            result[i] = rawResult[i];
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRows(java.util.List)
     */
    public Double[][] getRows( List designElements ) {
        if ( designElements == null ) {
            return null;
        }

        List<DesignElement> elements = designElements;
        Double[][] result = new Double[elements.size()][];
        int i = 0;
        for ( DesignElement element : elements ) {
            Double[] rowResult = getRow( element );
            result[i] = rowResult;
            i++;
        }
        return result;
    }

    /**
     * Convert {@link DesignElementDataVector}s into a {@link DoubleMatrixNamed}.
     * 
     * @param vectors
     * @return
     */
    private DoubleMatrixNamed vectorsToDoubleMatrix( Collection<DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            return null;
        }

        ByteArrayConverter bac = new ByteArrayConverter();

        List<BioAssay> bioAssays = ( List<BioAssay> ) vectors.iterator().next().getBioAssayDimension().getBioAssays();

        assert bioAssays.size() > 0 : "Empty BioAssayDimension for the vectors";

        DoubleMatrixNamed matrix = DoubleMatrix2DNamedFactory.fastrow( vectors.size(), bioAssays.size() );

        // Use BioMaterial names to represent the column in the matrix (as it can span multiple BioAssays)
        for ( BioAssay assay : bioAssays ) {
            StringBuilder buf = new StringBuilder();
            List<BioMaterial> bms = new ArrayList<BioMaterial>( assay.getSamplesUsed() );
            // Collections.sort( bms ); // FIXME this should use a sort.
            for ( BioMaterial bm : bms ) {
                buf.append( bm.getName() );
            }
            matrix.addColumnName( buf.toString() );
        }

        int rowNum = 0;
        for ( DesignElementDataVector vector : vectors ) {
            String name = vector.getDesignElement().getName();
            matrix.addRowName( name );
            byte[] bytes = vector.getData();
            double[] vals = bac.byteArrayToDoubles( bytes );
            assert vals.length == bioAssays.size() : "Number of values in vector (" + vals.length
                    + ") don't match number of Bioassays (" + bioAssays.size() + ")";
            for ( int i = 0; i < vals.length; i++ ) {
                matrix.setQuick( rowNum, i, vals[i] );
            }
            rowNum++;
        }
        return matrix;
    }

}
