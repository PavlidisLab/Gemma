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
package ubic.gemma.analysis.linkAnalysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ubic.basecode.bio.geneset.GeneAnnotations;
import ubic.basecode.dataStructure.matrix.DoubleMatrix2DNamedFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.datafilter.AffymetrixProbeNameFilter;
import ubic.basecode.datafilter.Filter;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * This class is to set the analysis parameters for linkAnalysis.
 * 
 * @author xiangwan
 */
public class LinkAnalysisDataLoader extends ExpressionDataLoader {

    private DoubleMatrixNamed<String, String> dataMatrix = null;

    public LinkAnalysisDataLoader( ExpressionExperiment paraExpressionExperiment, String goFile ) {
        super( paraExpressionExperiment, goFile );
        this.dataMatrix = this.vectorsToDoubleMatrix( this.designElementDataVectors );
        this.filter();
    }

    private void filter() {
        Filter<String, String> x = new AffymetrixProbeNameFilter<String, String>();
        DoubleMatrixNamed<String, String> r = ( DoubleMatrixNamed<String, String> ) x.filter( this.dataMatrix );
        this.dataMatrix = r;
        System.err.println( this.dataMatrix );
        this.uniqueItems = this.dataMatrix.rows();
    }

    public void writeDataIntoFile( String paraFileName ) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter( new FileWriter( this.analysisResultsPath + paraFileName ) );
        } catch ( IOException e ) {
            log.error( "File for output expression data " + this.analysisResultsPath + paraFileName
                    + "could not be opened" );
        }
        try {
            int cols = this.dataMatrix.columns();
            for ( int i = 0; i < cols; i++ ) {
                writer.write( "\t" + this.getDataMatrix().getColName( i ) );
            }
            writer.write( "\n" );
            int rows = this.dataMatrix.rows();
            for ( int i = 0; i < rows; i++ ) {
                writer.write( this.dataMatrix.getRowName( i ).toString() );
                double rowData[] = this.dataMatrix.getRow( i );
                for ( int j = 0; j < rowData.length; j++ )
                    writer.write( "\t" + rowData[j] );
                writer.write( "\n" );
            }
            writer.close();
        } catch ( IOException e ) {
            log.error( "Error in write data into file" );
        }
    }

    private DoubleMatrixNamed<String, String> vectorsToDoubleMatrix( Collection<DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            return null;
        }

        ByteArrayConverter bac = new ByteArrayConverter();

        List<BioAssay> bioAssays = ( List<BioAssay> ) vectors.iterator().next().getBioAssayDimension().getBioAssays();

        assert bioAssays.size() > 0 : "Empty BioAssayDimension for the vectors";

        DoubleMatrixNamed<String, String> matrix = DoubleMatrix2DNamedFactory
                .fastrow( vectors.size(), bioAssays.size() );

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

    public DoubleMatrixNamed getDataMatrix() {
        return this.dataMatrix;
    }

    public GeneAnnotations getGeneAnnotations() {
        return this.geneAnnotations;
    };
}