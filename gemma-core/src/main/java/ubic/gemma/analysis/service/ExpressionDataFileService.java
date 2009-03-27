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
import java.io.FileNotFoundException;
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
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.datastructure.matrix.ExperimentalDesignWriter;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.datastructure.matrix.MatrixWriter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.ConfigUtils;

/**
 * Supports the creation and location of 'flat file' versions of data in the system, for download by users. Files are
 * cached on the filesystem and reused if possible, rather than recreating them every time.
 * <p>
 * FIXME there is a possibility of having stale data, if the data have been updated since the last file was generated.
 * This can be tested for.
 * 
 * @spring.bean id="expressionDataFileService"
 * @spring.property name = "designElementDataVectorService" ref="designElementDataVectorService"
 * @spring.property name="expressionDataMatrixService" ref="expressionDataMatrixService"
 * @spring.property name = "arrayDesignService" ref="arrayDesignService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @author paul
 * @version $Id$
 */
public class ExpressionDataFileService {

    public static final String DATA_FILE_SUFFIX = ".data.txt.gz";

    public static final String JSON_FILE_SUFFIX = ".data.json.gz";

    public static final String DATA_DIR = ConfigUtils.getString( "gemma.appdata.home" ) + File.separatorChar
            + "dataFiles" + File.separatorChar;

    private static Log log = LogFactory.getLog( ArrayDesignAnnotationService.class.getName() );

    ArrayDesignService arrayDesignService;

    private DesignElementDataVectorService designElementDataVectorService;

    private ExpressionDataMatrixService expressionDataMatrixService;

    private ExpressionExperimentService expressionExperimentService;

    /**
     * @param ee
     * @param filtered if the data matrix is filtered
     * @return
     */
    public File getOutputFile( ExpressionExperiment ee, boolean filtered ) {
        String filteredAdd = "";
        if ( !filtered ) {
            filteredAdd = ".unfilt";
        }
        String filename = ee.getId() + "_" + ee.getShortName().replaceAll( "\\s+", "_" ) + "_expmat" + filteredAdd
                + DATA_FILE_SUFFIX;
        return getOutputFile( filename );
    }

    /**
     * @param type
     * @return
     */
    public File getOutputFile( QuantitationType type ) {
        String filename = type.getId() + "_" + type.getName().replaceAll( "\\s+", "_" ) + DATA_FILE_SUFFIX;
        return getOutputFile( filename );
    }

    /**
     * @param filename
     * @return
     */
    public File getOutputFile( String filename ) {
        String fullFilePath = DATA_DIR + filename;
        File f = new File( fullFilePath );

        if ( f.exists() ) {
            return f;
        }

        File parentDir = f.getParentFile();
        if ( !parentDir.exists() ) parentDir.mkdirs();
        return f;
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    public void setExpressionDataMatrixService( ExpressionDataMatrixService expressionDataMatrixService ) {
        this.expressionDataMatrixService = expressionDataMatrixService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * Locate or create a data file containing the 'preferred and masked' expression data matrix, with filtering for low
     * expression applied (currently supports default settings only).
     * 
     * @param ee
     * @param forceWrite
     * @return
     */
    public File writeOrLocateDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) {

        try {
            File f = getOutputFile( ee, filtered );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }
            log.info( "Creating new expression data file: " + f );
            ExpressionDataDoubleMatrix matrix = getDataMatrix( ee, filtered, f );

            Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( ee );
            Map<Long, Collection<Gene>> geneAnnotations = this.getGeneAnnotations( arrayDesigns );
            writeMatrix( f, geneAnnotations, matrix );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
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

        try {
            File f = getOutputFile( type );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new quantitation type expression data file: " + f );

            Collection<? extends DesignElementDataVector> vectors = designElementDataVectorService.find( type );
            Collection<ArrayDesign> arrayDesigns = getArrayDesigns( vectors );
            Map<Long, Collection<Gene>> geneAnnotations = this.getGeneAnnotations( arrayDesigns );

            if ( vectors.size() == 0 ) {
                log.warn( "No vectors for " + type );
                return null;
            }

            writeVectors( f, type.getRepresentation(), vectors, geneAnnotations );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Locate or create an experimental design file for a given experiment.
     * 
     * @param ee
     * @param forceWrite
     * @return
     */
    public File writeOrLocateDesignFile( ExpressionExperiment ee, boolean forceWrite ) {

        expressionExperimentService.thawLite( ee );

        String filename = ee.getId() + "_" + ee.getShortName().replaceAll( "\\s+", "_" ) + "_expdesign"
                + DATA_FILE_SUFFIX;
        try {
            File f = getOutputFile( filename );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new experimental design file: " + f );
            writeDesignMatrix( f, ee );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param ee
     * @param forceWrite
     * @param filtered if the data should be filtered.
     * @see ExpressionDataMatrixService.getFilteredMatrix
     * @return
     */
    public File writeOrLocateJSONDataFile( ExpressionExperiment ee, boolean forceWrite, boolean filtered ) {

        try {
            File f = getOutputFile( ee, filtered );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new JSON expression data file: " + f );
            ExpressionDataDoubleMatrix matrix = getDataMatrix( ee, filtered, f );

            Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( ee );
            Map<Long, Collection<Gene>> geneAnnotations = this.getGeneAnnotations( arrayDesigns );

            writeJson( f, geneAnnotations, matrix );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param type
     * @param forceWrite
     */
    @SuppressWarnings("unchecked")
    public File writeOrLocateJSONDataFile( QuantitationType type, boolean forceWrite ) {

        try {
            File f = getJSONOutputFile( type );
            if ( !forceWrite && f.canRead() ) {
                log.info( f + " exists, not regenerating" );
                return f;
            }

            log.info( "Creating new quantitation type  JSON data file: " + f );

            Collection<? extends DesignElementDataVector> vectors = designElementDataVectorService.find( type );

            if ( vectors.size() == 0 ) {
                log.warn( "No vectors for " + type );
                return null;
            }

            writeJson( f, type.getRepresentation(), vectors );
            return f;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param vectors
     * @return
     */
    private Collection<ArrayDesign> getArrayDesigns( Collection<? extends DesignElementDataVector> vectors ) {
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        for ( DesignElementDataVector v : vectors ) {
            ads.add( v.getDesignElement().getArrayDesign() );
        }
        return ads;
    }

    /**
     * @param ee
     * @param filtered
     * @param f
     * @return
     */
    private ExpressionDataDoubleMatrix getDataMatrix( ExpressionExperiment ee, boolean filtered, File f ) {

        FilterConfig filterConfig = new FilterConfig();
        filterConfig.setIgnoreMinimumSampleThreshold( true );
        filterConfig.setIgnoreMinimumRowsThreshold( true );
        expressionExperimentService.thawLite( ee );
        ExpressionDataDoubleMatrix matrix;
        if ( filtered ) {
            matrix = expressionDataMatrixService.getFilteredMatrix( ee, filterConfig );
        } else {
            matrix = expressionDataMatrixService.getProcessedExpressionDataMatrix( ee );
        }
        return matrix;
    }

    /**
     * @param ads
     * @return
     */
    private Map<Long, Collection<Gene>> getGeneAnnotations( Collection<ArrayDesign> ads ) {
        Map<Long, Collection<Gene>> annots = new HashMap<Long, Collection<Gene>>();
        for ( ArrayDesign arrayDesign : ads ) {
            arrayDesignService.thawLite( arrayDesign );
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
     * Writes out the experimental design for the given experiment. The bioassays (col 0) matches match the header row
     * of the data matrix printed out by the {@link MatrixWriter}.
     * 
     * @param file
     * @param expressionExperiment
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void writeDesignMatrix( File file, ExpressionExperiment expressionExperiment ) throws IOException,
            FileNotFoundException {
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );
        ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter();
        edWriter.write( writer, expressionExperiment, true );
        writer.flush();
        writer.close();
    }

    /**
     * @param file
     * @param geneAnnotations
     * @param expressionDataMatrix
     * @throws IOException
     * @throws FileNotFoundException
     */
    @SuppressWarnings("unchecked")
    private void writeJson( File file, Map<Long, Collection<Gene>> geneAnnotations,
            ExpressionDataMatrix expressionDataMatrix ) throws IOException, FileNotFoundException {
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );
        MatrixWriter matrixWriter = new MatrixWriter();
        matrixWriter.writeJSON( writer, expressionDataMatrix, true );
        writer.flush();
        writer.close();
    }

    /**
     * @param file
     * @param representation
     * @param vectors
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private void writeJson( File file, PrimitiveType representation,
            Collection<? extends DesignElementDataVector> vectors ) throws IOException {
        designElementDataVectorService.thaw( vectors );
        ExpressionDataMatrix expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( representation, vectors );
        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );
        MatrixWriter matrixWriter = new MatrixWriter();
        matrixWriter.writeJSON( writer, expressionDataMatrix, true );
    }

    /**
     * @param file
     * @param geneAnnotations
     * @param expressionDataMatrix
     * @throws IOException
     * @throws FileNotFoundException
     */
    @SuppressWarnings("unchecked")
    private void writeMatrix( File file, Map<Long, Collection<Gene>> geneAnnotations,
            ExpressionDataMatrix expressionDataMatrix ) throws IOException, FileNotFoundException {

        Writer writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( file ) ) );
        MatrixWriter matrixWriter = new MatrixWriter();
        matrixWriter.write( writer, expressionDataMatrix, geneAnnotations, true );
        writer.flush();
        writer.close();
    }

    /**
     * @param file
     * @param representation
     * @param vectors
     * @param geneAnnotations
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private void writeVectors( File file, PrimitiveType representation,
            Collection<? extends DesignElementDataVector> vectors, Map<Long, Collection<Gene>> geneAnnotations )
            throws IOException {
        designElementDataVectorService.thaw( vectors );

        ExpressionDataMatrix expressionDataMatrix = ExpressionDataMatrixBuilder.getMatrix( representation, vectors );

        writeMatrix( file, geneAnnotations, expressionDataMatrix );
    }

}
