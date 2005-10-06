/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.mage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.dataStructure.matrix.NamedMatrix;
import baseCode.io.ByteArrayConverter;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.bioAssayData.DesignElementDataVector;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.loader.expression.Preprocessor;
import edu.columbia.gemma.util.ConfigUtils;

/**
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="mageMLPreprocessor" singleton="false"
 */
public class MageMLPreprocessor implements Preprocessor {
    Log log = LogFactory.getLog( MageMLPreprocessor.class.getName() );
    RawDataParser rdp = null;

    private String localMatrixFilepath;

    private final String experimentName;

    public MageMLPreprocessor( String experimentName ) {
        this.experimentName = experimentName.replaceAll( "\\W+", "_" );
        this.localMatrixFilepath = ConfigUtils.getProperty( "local.rawData.matrix.basepath" );
    }

    /**
     * This method is provided primarily for testing.
     * 
     * @param streams
     * @param bioAssays
     * @param dimensions
     * @throws IOException
     */
    public void preprocessStreams( List<InputStream> streams, List<BioAssay> bioAssays, BioAssayDimensions dimensions )
            throws IOException {
        rdp = new RawDataParser( bioAssays, dimensions );
        rdp.setSeparator( ' ' );
        log.info( "Preprocessing the data ..." );

        rdp.parseStreams( streams );
        for ( InputStream is : streams ) {
            is.close();
        }
        processResults( null );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Preprocessor#preprocess(java.util.List,
     *      edu.columbia.gemma.loader.expression.mage.BioAssayDimensions)
     */
    @SuppressWarnings("unchecked")
    public void preprocess( List<BioAssay> bioAssays, BioAssayDimensions dimensions ) throws IOException {

        rdp = new RawDataParser( bioAssays, dimensions );
        rdp.setSeparator( ' ' );

        log.info( "Preprocessing the data ..." );

        rdp.parse();

        Collection<LocalFile> sourceFiles = new HashSet<LocalFile>();
        for ( BioAssay assay : bioAssays ) {
            sourceFiles.add( assay.getRawDataFile() );
        }

        processResults( sourceFiles );

    }

    /**
     * After parsing: Create text files, persist their information, and persist the data vectors.
     * 
     * @param sourceFiles
     * @throws IOException
     */
    private void processResults( Collection<LocalFile> sourceFiles ) throws IOException {

        Collection<Object> matrices = rdp.getResults();
        Collection<DesignElementDataVector> vectors = new HashSet<DesignElementDataVector>();
        Collection<LocalFile> localFiles = new HashSet<LocalFile>();
        ByteArrayConverter converter = new ByteArrayConverter();
        int i = 0;
        for ( Object object : matrices ) {
            assert object instanceof NamedMatrix;

            QuantitationType qt = rdp.getQtData().getQuantitationTypes().get( i );

            File outputDir = new File( localMatrixFilepath + File.separator + this.experimentName );

            if ( !outputDir.exists() ) {
                boolean success = outputDir.mkdirs();
                if ( !success ) throw new IOException( "Could not create " + outputDir );
            }

            File outputFile = new File( localMatrixFilepath + File.separator + this.experimentName + File.separator
                    + experimentName + File.separator + experimentName + "_" + qt.getName() );

            log.info( experimentName + ": Storing data for quantitation type " + qt.getName() + " in file: "
                    + outputFile );

            FileWriter fo = new FileWriter( outputFile );
            String matrix = ( ( NamedMatrix ) object ).toString();
            fo.write( matrix );
            fo.close();

            LocalFile lf = makeLocalFile( sourceFiles, outputFile, matrix );
            localFiles.add( lf );

            makeDataVectors( vectors, converter, object, qt );
            i++;
        }
    }

    /**
     * @param vectors
     * @param converter
     * @param object
     * @param qt
     */
    private void makeDataVectors( Collection<DesignElementDataVector> vectors, ByteArrayConverter converter,
            Object object, QuantitationType qt ) {
        for ( int i = 0; i < ( ( NamedMatrix ) object ).rows(); i++ ) {

            DesignElement de = rdp.getQtData().getDesignElementsForQuantitationType( qt ).get( i );

            Object[] row = ( ( NamedMatrix ) object ).getRowObj( i );

            if ( row == null ) {
                throw new NullPointerException( "Got null row " + i + " for quantitation type " + qt.getName() );
            }

            byte[] bytes = converter.toBytes( row );

            DesignElementDataVector v = DesignElementDataVector.Factory.newInstance();
            v.setData( bytes );
            v.setDesignElement( de );
            v.setQuantitationType( qt );
            vectors.add( v );
        }
    }

    /**
     * @param sourceFiles
     * @param outputFile
     * @param matrix
     * @return
     */
    private LocalFile makeLocalFile( Collection<LocalFile> sourceFiles, File outputFile, String matrix ) {
        // create the local file information.
        LocalFile lf = LocalFile.Factory.newInstance();
        lf.setVersion( new SimpleDateFormat().format( new Date() ) );
        lf.setSourceFiles( sourceFiles );
        lf.setSize( matrix.getBytes().length );
        lf.setLocalURI( "file://" + outputFile.getAbsolutePath().replaceAll( "\\\\", "/" ) );
        return lf;
    }
}
