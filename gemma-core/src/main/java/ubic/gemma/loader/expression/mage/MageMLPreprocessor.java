/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2010 University of British Columbia
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.expression.Preprocessor;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.Persister;
import ubic.gemma.util.ConfigUtils;

/**
 * Parse and persist the raw data files from MAGE-ML files. Files are also created that contain the data organized by
 * quantitation type. The input files are normally organized by bioassay. Note that this does not perform any
 * normalization or other types of preprocessing.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 */
public class MageMLPreprocessor implements Preprocessor {
    private String experimentName = null;
    private String localMatrixFilepath = null;

    /*
     * Note: currently this is not wired into the context and it is not used. We can put it back if we need it, but it's
     * semi-deprecated?
     */

    private Persister persisterHelper;

    Log log = LogFactory.getLog( MageMLPreprocessor.class.getName() );
    private int whichQuantitationType = -1;

    public MageMLPreprocessor() {
        this.localMatrixFilepath = ConfigUtils.getString( "local.rawData.matrix.basepath" );
    }

    /*
     * FIXME never called.
     */
    public MageMLPreprocessor( String experimentName ) {
        this();
        this.experimentName = experimentName.replaceAll( "\\W+", "_" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Preprocessor#preprocess(java.util.List,
     * ubic.gemma.loader.expression.mage.BioAssayDimensions)
     */
    public void preprocess( List<BioAssay> bioAssays, BioAssayDimensions dimensions ) throws IOException {
        RawDataParser rdp = new RawDataParser();
        rdp.setDimensions( dimensions );
        rdp.setBioAssays( bioAssays );
        rdp.setSeparator( ' ' );

        log.info( "Preprocessing the data ..." );

        int i = 0;
        if ( whichQuantitationType >= 0 ) {
            i = whichQuantitationType;
        }
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
                processResults( sourceFiles, rdp );
            }
            i++;
        }

        setSelector( rdp, -1 );

    }

    /**
     * This method is provided primarily for testing.
     * 
     * @param streams - InputStreams for the raw data files
     * @param expressionExperiment
     * @param bioAssays - bioAssays in the same order as the streams
     * @param dimensions
     * @throws IOException
     */
    public void preprocessStreams( List<InputStream> streams, ExpressionExperiment expressionExperiment,
            List<BioAssay> orderedBioAssays, BioAssayDimensions dimensions ) throws IOException {
        assert expressionExperiment != null;
        RawDataParser rdp = new RawDataParser();
        rdp.setExpressionExperiment( expressionExperiment );
        rdp.setDimensions( dimensions );
        rdp.setBioAssays( orderedBioAssays );
        rdp.setSeparator( ' ' );
        log.info( "Preprocessing the data ..." );

        rdp.parseStreams( streams );
        for ( InputStream is : streams ) {
            is.close();
        }
        // processResults( null );
        makeTabbedFiles( null, rdp );
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( Persister persisterHelper ) {
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
        try {
            lf.setLocalURL( outputFile.toURI().toURL() );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
        return lf;
    }

    /**
     * Make the DesignElementDataVectors persistent.
     */
    public void makePersistent( RawDataParser rdp ) {
        Collection<Object> matrices = rdp.getResults();
        // int i = 0;
        // for ( Object object : matrices ) {
        //
        // assert object instanceof RawDataMatrix;
        //
        // QuantitationType qType = ( ( RawDataMatrix ) object ).getQuantitationType();
        // if ( whichQuantitationType >= 0 ) {
        // QuantitationType qt = rdp.getQtData().getQuantitationTypes().get( whichQuantitationType );
        // if ( !qt.getName().equals( qType.getName() ) ) {
        // i++;
        // continue;
        // }
        // }
        //
        // // persistDesignElements( qType );
        //
        // // log.info( "Persisting matrix " + i + ", quantitation type " + qType.getName() );
        // // persisterHelper.persist( ( ( RawDataMatrix ) object ).getRows() );
        //
        // i++;
        // }

        if ( matrices == null || matrices.size() == 0
                || ( ( RawDataMatrix ) matrices.iterator().next() ).getRows() == null
                || ( ( RawDataMatrix ) matrices.iterator().next() ).getRows().size() == 0 ) {
            throw new IllegalStateException( "No matrices or no rows to persist!" );
        }

        DesignElementDataVector v = ( ( RawDataMatrix ) matrices.iterator().next() ).getRows().iterator().next();

        // This will bring in all the DesignElementDataVectors by composition.
        persisterHelper.persist( v.getExpressionExperiment() );

    }

    // /**
    // * @param qType
    // * @param object
    // */
    // @SuppressWarnings("unchecked")
    // private void persistDesignElements( QuantitationType qType ) {
    // log.info( "Persisting design elements " );
    // Collection<DesignElement> designElements = this.rdp.getQtData().getDesignElementsForQuantitationType( qType );
    //
    // Collection<DesignElement> persistentDesignElements = ( Collection<DesignElement> ) persisterHelper
    // .persist( designElements );
    //
    // Iterator<DesignElement> it = designElements.iterator();
    // for ( DesignElement persistentElement : persistentDesignElements ) {
    // DesignElement element = it.next();
    // assert persistentElement != null && element != null;
    // element.setId( persistentElement.getId() );
    // }
    // }

    /**
     * @param sourceFiles
     * @throws IOException
     */
    public void makeTabbedFiles( Collection<LocalFile> sourceFiles, RawDataParser rdp ) throws IOException {
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
     * After parsing all the files, Create text files, persist their information, and persist the data vectors.
     * 
     * @param sourceFiles
     * @throws IOException
     */
    private void processResults( Collection<LocalFile> sourceFiles, RawDataParser rdp ) throws IOException {
        makeTabbedFiles( sourceFiles, rdp );
        makePersistent( rdp );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.expression.mage.RawDataParser#setSelector(int)
     */
    public void setSelector( RawDataParser rdp, int selector ) {
        assert rdp != null;
        this.whichQuantitationType = selector;
        rdp.setSelector( selector );
    }
}
