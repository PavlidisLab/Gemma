/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.datastructure.matrix.MatrixWriter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.ConfigUtils;

/**
 * Supports the creation and location of 'flat file' versions of data in the system, for download by users. Files are
 * cached on the filesystem and reused if possible, rather than recreating them every time.
 * <p>
 * FIXME there is a possibility of having stale data.
 * 
 * @spring.bean id="expressionDataFileService"
 * @spring.property name = "designElementDataVectorService" ref="designElementDataVectorService"
 * @author paul
 * @version $Id$
 */
public class ExpressionDataFileService {

    public static final String DATA_FILE_SUFFIX = ".data.txt.gz";

    public static final String JSON_FILE_SUFFIX = ".data.json.gz";

    public static final String DATA_DIR = ConfigUtils.getString( "gemma.appdata.home" ) + File.separatorChar
            + "dataFiles" + File.separatorChar;

    private static Log log = LogFactory.getLog( ArrayDesignAnnotationService.class.getName() );;

    private DesignElementDataVectorService designElementDataVectorService;

    /**
     * @param type
     * @return
     * @throws IOException
     */
    public File getOutputFile( QuantitationType type ) throws IOException {
        String filename = type.getId() + "_" + type.getName().replaceAll( "\\s+", "_" ) + DATA_FILE_SUFFIX;
        String fullFilePath = DATA_DIR + filename;

        File f = new File( fullFilePath );

        if ( f.exists() ) {
            log.warn( "Will overwrite existing file " + f );
            f.delete();
        }

        File parentDir = f.getParentFile();
        if ( !parentDir.exists() ) parentDir.mkdirs();
        return f;
    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    /**
     * Locate or create a new data file for the given quantitation type. The output will include gene information if it
     * can be located from its own file.
     * 
     * @param type
     * @param forceWrite To not return the existing file, but create it anew.
     * @return location of the resulting file.
     */
    @SuppressWarnings("unchecked")
    public File writeOrLocateDataFile( QuantitationType type, boolean forceWrite ) {
        Collection<DesignElementDataVector> vectors = designElementDataVectorService.find( type );
        Collection<ArrayDesign> arrayDesigns = getArrayDesigns( vectors );
        Map<Long, Collection<Gene>> geneAnnotations = this.getGeneAnnotations( arrayDesigns );

        if ( vectors.size() == 0 ) {
            log.warn( "No vectors for " + type );
            return null;
        }

        try {
            File f = getOutputFile( type );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new data file: " + f );
            writeVectors( f, type.getRepresentation(), vectors, geneAnnotations );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param type
     * @param forceWrite
     * @return
     */
    @SuppressWarnings("unchecked")
    public File writeOrLocateJSONDataFile( QuantitationType type, boolean forceWrite ) {
        Collection<DesignElementDataVector> vectors = designElementDataVectorService.find( type );

        if ( vectors.size() == 0 ) {
            log.warn( "No vectors for " + type );
            return null;
        }

        try {
            File f = getJSONOutputFile( type );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new JSON data file: " + f );
            writeJson( f, type.getRepresentation(), vectors );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private Collection<ArrayDesign> getArrayDesigns( Collection<DesignElementDataVector> vectors ) {
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        for ( DesignElementDataVector v : vectors ) {
            ads.add( v.getDesignElement().getArrayDesign() );
        }
        return ads;
    }

    private Map<Long, Collection<Gene>> getGeneAnnotations( Collection<ArrayDesign> ads ) {
        Map<Long, Collection<Gene>> annots = new HashMap<Long, Collection<Gene>>();
        for ( ArrayDesign arrayDesign : ads ) {
            annots.putAll( ArrayDesignAnnotationService.readAnnotationFile( arrayDesign ) );
        }
        return annots;
    }

    /**
     * @param type
     * @return
     * @throws IOException
     */
    private File getJSONOutputFile( QuantitationType type ) throws IOException {
        String filename = type.getName().replaceAll( "\\s+", "_" ) + JSON_FILE_SUFFIX;
        String fullFilePath = DATA_DIR + filename;

        File f = new File( fullFilePath );

        if ( f.exists() ) {
            log.warn( "Will overwrite existing file " + f );
            f.delete();
        }

        File parentDir = f.getParentFile();
        if ( !parentDir.exists() ) parentDir.mkdirs();
        f.createNewFile();
        return f;
    }

    /**
     * @param file
     * @param representation
     * @param vectors
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private void writeJson( File file, PrimitiveType representation, Collection<DesignElementDataVector> vectors )
            throws IOException {
        designElementDataVectorService.thaw( vectors );
        ExpressionDataMatrix expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( representation, vectors );
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );
        MatrixWriter matrixWriter = new MatrixWriter();
        matrixWriter.writeJSON( writer, expressionDataMatrix, true );
    }

    // /**
    // * @param matrix
    // * @param columns
    // */
    // @SuppressWarnings("unchecked")
    // private Map<CompositeSequence, Collection<Gene>> getGeneAnnotations( ExpressionDataMatrix matrix ) {
    // int columns = matrix.columns();
    // Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
    // for ( int i = 0; i < columns; i++ ) {
    // Collection<BioAssay> bas = matrix.getBioAssaysForColumn( i );
    // for ( BioAssay ba : bas ) {
    // ads.add( ba.getArrayDesignUsed() );
    // }
    // }
    //
    // return getGeneAnnotations( ads );
    // }

    /**
     * @param response
     * @param writer
     * @param batch
     * @param geneAnnotations map of composite sequence ids to genes.
     * @param firstBatch
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private void writeVectors( File file, PrimitiveType representation, Collection<DesignElementDataVector> vectors,
            Map<Long, Collection<Gene>> geneAnnotations ) throws IOException {
        designElementDataVectorService.thaw( vectors );
        ExpressionDataMatrix expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( representation, vectors );
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );
        MatrixWriter matrixWriter = new MatrixWriter();
        matrixWriter.write( writer, expressionDataMatrix, geneAnnotations, true );
        writer.flush();
        writer.close();
    }

}
