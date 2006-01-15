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
package edu.columbia.gemma.loader.expression.mage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.quantitationtype.QuantitationType;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.designElement.DesignElement;
import edu.columbia.gemma.loader.expression.Preprocessor;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;
import edu.columbia.gemma.util.ConfigUtils;

/**
 * Parse and persist the raw data files from MAGE-ML files. Files are also created that contain the data organized by
 * quantitation type. The input files are normally organized by bioassay. Note that this does not perform any
 * normalization or other types of preprocessing.
 * <p>
 * Copyright (c) 2004 - 2006 University of British Columbia
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="mageMLPreprocessor" singleton="false"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 */
public class MageMLPreprocessor implements Preprocessor {
    private final String experimentName;
    private String localMatrixFilepath;

    private PersisterHelper persisterHelper;

    Log log = LogFactory.getLog( MageMLPreprocessor.class.getName() );
    RawDataParser rdp = new RawDataParser();
    private int whichQuantitationType = -1;

    public MageMLPreprocessor( String experimentName ) {
        this.experimentName = experimentName.replaceAll( "\\W+", "_" );
        this.localMatrixFilepath = ConfigUtils.getProperty( "local.rawData.matrix.basepath" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Preprocessor#preprocess(java.util.List,
     *      edu.columbia.gemma.loader.expression.mage.BioAssayDimensions)
     */
    @SuppressWarnings("unchecked")
    public void preprocess( List<BioAssay> bioAssays, BioAssayDimensions dimensions ) throws IOException {

        rdp.setDimensions( dimensions );
        rdp.setBioAssays( bioAssays );
        rdp.setSeparator( ' ' );

        log.info( "Preprocessing the data ..." );

        int i = 0;
        if ( whichQuantitationType >= 0 ) i = whichQuantitationType;
        while ( true ) {
            if ( whichQuantitationType < 0 || !( whichQuantitationType >= 0 && i != whichQuantitationType ) ) {
                try {
                    rdp.parse();
                } catch ( NoMoreQuantitationTypesException e ) {
                    log.info( "No more quantitation types!" );
                    break;
                }
                Collection<LocalFile> sourceFiles = new HashSet<LocalFile>();
                for ( BioAssay assay : bioAssays ) {
                    sourceFiles.add( assay.getRawDataFile() );
                }
                processResults( sourceFiles );
            }
            i++;
        }

        setSelector( -1 );

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
        rdp.setDimensions( dimensions );
        rdp.setBioAssays( bioAssays );
        rdp.setSeparator( ' ' );
        log.info( "Preprocessing the data ..." );

        rdp.parseStreams( streams );
        for ( InputStream is : streams ) {
            is.close();
        }
        processResults( null );
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param sourceFiles
     * @param outputFile
     * @param matrix
     * @return
     */
    private LocalFile makeLocalFile( Collection<LocalFile> sourceFiles, File outputFile, Long size ) {
        // create the local file information.
        LocalFile lf = LocalFile.Factory.newInstance();
        lf.setVersion( new SimpleDateFormat().format( new Date() ) );
        lf.setSourceFiles( sourceFiles );
        lf.setSize( size.longValue() );
        lf.setLocalURI( "file://" + outputFile.getAbsolutePath().replaceAll( "\\\\", "/" ) );
        return lf;
    }

    /**
     * `
     */
    private void makePersistent() {
        assert persisterHelper != null;
        Collection<Object> matrices = rdp.getResults();
        int i = 0;
        for ( Object object : matrices ) {

            assert object instanceof RawDataMatrix;

            QuantitationType qType = ( ( RawDataMatrix ) object ).getQuantitationType();
            if ( whichQuantitationType >= 0 ) {
                QuantitationType qt = rdp.getQtData().getQuantitationTypes().get( whichQuantitationType );
                if ( !qt.getName().equals( qType.getName() ) ) {
                    i++;
                    continue;
                }
            }

            persistDesignElements( qType );

            log.info( "Persisting matrix " + i + ", quantitation type " + qType.getName() );
            persisterHelper.persist( ( ( RawDataMatrix ) object ).getRows() );

            i++;
        }
    }

    /**
     * @param i
     * @param object
     */
    @SuppressWarnings("unchecked")
    private void persistDesignElements( QuantitationType qType ) {
        log.info( "Persisting design elements " );
        Collection<DesignElement> designElements = this.rdp.getQtData().getDesignElementsForQuantitationType( qType );
        Collection<DesignElement> persistentDesignElements = ( Collection<DesignElement> ) persisterHelper
                .persist( designElements );
        log.info( "Copying IDs..." );
        Iterator<DesignElement> it = designElements.iterator();
        for ( DesignElement persistentElement : persistentDesignElements ) {
            DesignElement element = it.next();
            assert persistentElement != null && element != null;
            element.setId( persistentElement.getId() );
        }
    }

    /**
     * @param sourceFiles
     * @throws IOException
     */
    private void makeTabbedFiles( Collection<LocalFile> sourceFiles ) throws IOException {
        Collection<Object> matrices = rdp.getResults();
        Collection<LocalFile> localFiles = new HashSet<LocalFile>();

        int i = 0;
        for ( Object object : matrices ) {
            QuantitationType qt = rdp.getQtData().getQuantitationTypes().get( i );

            if ( whichQuantitationType >= 0 ) {
                QuantitationType typeToDo = rdp.getQtData().getQuantitationTypes().get( whichQuantitationType );
                if ( !qt.getName().equals( typeToDo.getName() ) ) {
                    i++;
                    continue;
                }
            }

            assert object instanceof RawDataMatrix;

            File outputDir = new File( localMatrixFilepath + File.separator + this.experimentName );

            log.info( "Seeking or creating output directory..." );
            if ( !outputDir.exists() && !outputDir.mkdirs() ) {
                log.warn( "Could not create output directory " + outputDir );
                outputDir = new File( System.getProperty( "java.io.tmpdir" ) + File.separator + experimentName );

                log.warn( "Will use local temporary directory: " + outputDir.getAbsolutePath() );
                if ( !outputDir.exists() && !outputDir.mkdirs() ) {
                    throw new IOException( "Could not create temporary directory" );
                }
            }

            File outputFile = new File( outputDir + File.separator + experimentName + "_" + qt.getName() + ".txt" );

            log.info( experimentName + ": Storing data for quantitation type " + qt.getName() + " in file: "
                    + outputFile );

            FileWriter fo = new FileWriter( outputFile );
            ( ( RawDataMatrix ) object ).print( fo );
            fo.close();

            LocalFile lf = makeLocalFile( sourceFiles, outputFile, ( ( RawDataMatrix ) object ).byteSize() );
            localFiles.add( lf );

            i++;
        }
    }

    /**
     * After parsing: Create text files, persist their information, and persist the data vectors.
     * 
     * @param sourceFiles
     * @throws IOException
     */
    private void processResults( Collection<LocalFile> sourceFiles ) throws IOException {
        makeTabbedFiles( sourceFiles );
        makePersistent();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.expression.mage.RawDataParser#setSelector(int)
     */
    public void setSelector( int selector ) {
        assert rdp != null;
        this.whichQuantitationType = selector;
        this.rdp.setSelector( selector );
    }
}
