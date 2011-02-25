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
package ubic.gemma.loader.expression.mage;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import cern.colt.list.ByteArrayList;

/**
 * Class to store and print data matrices.
 * 
 * @author pavlidis
 * @version $Id$
 */
class RawDataMatrix {

    private static Log log = LogFactory.getLog( RawDataMatrix.class.getName() );

    private long byteSize = 0;
    List<BioAssay> assays;
    ByteArrayConverter converter = new ByteArrayConverter();
    QuantitationType quantitationType;
    Map<String, DesignElementDataVector> rows = new HashMap<String, DesignElementDataVector>();
    Map<String, ByteArrayList> rowTemp = new HashMap<String, ByteArrayList>();
    BioAssayDimension bioAssayDimension = BioAssayDimension.Factory.newInstance();

    /**
     * @param type
     */
    public RawDataMatrix( List<BioAssay> bioAssays, QuantitationType type ) {
        this.assays = bioAssays;
        this.quantitationType = type;

        // The following is a little silly and also ad hoc. Still need to fill in the BioMaterialDimension as well.
        log.info( "Setting up BioAssayDimension" );
        StringBuilder buf = new StringBuilder();
        for ( BioAssay assay : bioAssays ) {
            bioAssayDimension.getBioAssays().add( assay );
            String name = assay.getName();
            buf.append( name + "," );
        }
        bioAssayDimension.setName( buf.length() > 100 ? buf.toString().substring( 0, 100 ) : buf.toString() + "..." );
        bioAssayDimension.setDescription( buf.toString() );
    }

    /**
     * Add an object to a row corresponding to a given DesignElement.
     * 
     * @param de
     * @param data
     */
    public void addDataToRow( CompositeSequence de, QuantitationType qt, ExpressionExperiment exp, Object data ) {
        if ( de == null || data == null ) throw new IllegalArgumentException( "Null" );

        addRow( de, qt, exp );

        if ( !rowTemp.containsKey( de.getName() ) ) {
            rowTemp.put( de.getName(), new ByteArrayList() );
        }

        ByteArrayList existingBytes = rowTemp.get( de.getName() );

        byte[] newBytes = converter.toBytes( data );
        byteSize += newBytes.length;
        for ( int i = 0; i < newBytes.length; i++ ) {
            existingBytes.add( newBytes[i] );
        }
    }

    /**
     * @param de
     * @param qt
     */
    public void addRow( CompositeSequence de, QuantitationType qt, ExpressionExperiment expressionExperiment ) {
        if ( rows.containsKey( de.getName() ) ) return;
        RawExpressionDataVector tobeAdded = RawExpressionDataVector.Factory.newInstance();
        tobeAdded.setExpressionExperiment( expressionExperiment );
        assert this.bioAssayDimension != null;
        tobeAdded.setBioAssayDimension( this.bioAssayDimension );
        tobeAdded.setDesignElement( de );
        tobeAdded.setQuantitationType( qt );
        expressionExperiment.getRawExpressionDataVectors().add( tobeAdded );
        addRow( tobeAdded );
    }

    /**
     * Add a row to the matrix.
     * 
     * @param de
     * @param qt
     * @param data
     */
    public void addRow( CompositeSequence de, QuantitationType qt, ExpressionExperiment expressionExperiment,
            List<Object> data ) {
        DesignElementDataVector tobeAdded = RawExpressionDataVector.Factory.newInstance();
        tobeAdded.setDesignElement( de );
        tobeAdded.setQuantitationType( qt );
        tobeAdded.setExpressionExperiment( expressionExperiment );
        assert this.bioAssayDimension != null;
        tobeAdded.setBioAssayDimension( this.bioAssayDimension );
        byte[] bytes = converter.toBytes( data.toArray() );
        byteSize += bytes.length;
        tobeAdded.setData( bytes );
        addRow( tobeAdded );
    }

    /**
     * Add a row to the matrix.
     * 
     * @param vector
     */
    public void addRow( DesignElementDataVector vector ) {
        rows.put( vector.getDesignElement().getName(), vector );
    }

    /**
     * @return Size of the matrix in bytes.
     */
    public Long byteSize() {
        return new Long( byteSize );
    }

    /**
     * @return Returns the assays.
     */
    public List<BioAssay> getAssays() {
        return this.assays;
    }

    /**
     * @return Returns the quantitationType.
     */
    public QuantitationType getQuantitationType() {
        return this.quantitationType;
    }

    /**
     * @param de
     * @return
     */
    public DesignElementDataVector getRow( CompositeSequence de ) {
        String name = de.getName();
        ByteArrayList bytes = this.rowTemp.get( name );
        this.rows.get( name ).setData( bytes.elements() );
        this.rowTemp.remove( name );
        DesignElementDataVector result = this.rows.get( name );
        assert result.getBioAssayDimension() != null;
        assert result.getQuantitationType() != null;
        assert result.getDesignElement() != null;
        assert result.getExpressionExperiment() != null;
        return result;
    }

    /**
     * @return Returns the rows.
     */
    public Collection<DesignElementDataVector> getRows() {

        // If we need to clear out the temporary data.
        if ( this.rowTemp.keySet().size() != 0 ) {
            for ( String rowName : rows.keySet() ) {
                ByteArrayList bytes = this.rowTemp.get( rowName );
                this.rows.get( rowName ).setData( bytes.elements() );
                this.rowTemp.remove( rowName );
            }
        }
        return this.rows.values();
    }

    /**
     * @param o
     * @throws IOException
     * @throws UnsupportedOperationException if Class is a type that can't be handled by this.
     */
    public void print( Writer o ) throws IOException {

        o.write( "Probe" );
        for ( BioAssay bioAssay : assays ) {
            o.write( "\t" + bioAssay.getName() );
        }
        o.write( "\n" );

        this.getRows(); // make sure the vectors are initialized.
        for ( DesignElementDataVector vector : rows.values() ) {
            PrimitiveType pt = vector.getQuantitationType().getRepresentation();
            o.write( vector.getDesignElement().getName() + "\t" );
            String line = null;
            byte[] data = vector.getData();

            if ( data == null ) {
                throw new NullPointerException( "No bytes in vector!" );
            }

            if ( pt.equals( PrimitiveType.BOOLEAN ) ) {
                line = converter.byteArrayToTabbedString( data, Boolean.class );
            } else if ( pt.equals( PrimitiveType.DOUBLE ) ) {
                line = converter.byteArrayToTabbedString( data, Double.class );
            } else if ( pt.equals( PrimitiveType.INT ) ) {
                line = converter.byteArrayToTabbedString( data, Integer.class );
            } else if ( pt.equals( PrimitiveType.STRING ) ) {
                line = converter.byteArrayToTabbedString( data, String.class );
            } else {
                throw new UnsupportedOperationException( "Can't deal with " + pt );
            }
            o.write( line + "\n" );
        }
    }

}
