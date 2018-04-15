/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.loader.expression;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.util.ConfigUtils;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.util.TimeUtil;
import ubic.gemma.core.util.concurrent.GenericStreamConsumer;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.Settings;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

/**
 * @author paul
 */
public class AffyPowerToolsProbesetSummarize {

    private static final String AFFY_CDFS_PROPERTIES_FILE_NAME = "ubic/gemma/core/loader/affy.cdfs.properties";
    private static final String AFFY_CHIPNAME_PROPERTIES_FILE_NAME = "ubic/gemma/core/loader/affy.celmappings.properties";
    private static final String AFFY_MPS_PROPERTIES_FILE_NAME = "ubic/gemma/core/loader/affy.mps.properties";

    private static final String AFFY_POWER_TOOLS_CDF_PATH = "affy.power.tools.cdf.path";

    private static final long AFFY_UPDATE_INTERVAL_S = 30;
    private static final Log log = LogFactory.getLog( AffyPowerToolsProbesetSummarize.class );

    // rma-sketch uses much less memory, supposedly makes little difference in final results.
    private static final String METHOD = "rma";

    /**
     * @param bmap
     * @param sampleName
     * @return BioAssay, or null if not found.
     */
    public static BioAssay matchBioAssayToCelFileName( Map<String, BioAssay> bmap, String sampleName ) {
        /*
         * Look for patterns like GSM476194_SK_09-BALBcJ_622.CEL
         */
        BioAssay assay = null;
        if ( sampleName.matches( "^GSM[0-9]+_.+" ) ) {
            String geoAcc = sampleName.split( "_" )[0];

            if ( bmap.containsKey( geoAcc ) ) {
                assay = bmap.get( geoAcc );
            } else {
                AffyPowerToolsProbesetSummarize.log.debug( "No bioassay found " + geoAcc );
            }
        } else {

            /*
             * Sometimes column names are like Aud_19L.CEL - no GSM number. Sometimes this works, but it's last ditch.
             */
            assay = bmap.get( sampleName );
        }
        return assay;
    }

    /**
     * Map of strings found in CEL files to GPL ids.
     * 
     * @return
     */
    protected static Map<String, String> loadChipNames() {
        try {
            PropertiesConfiguration pc = ConfigUtils.loadClasspathConfig( AFFY_CHIPNAME_PROPERTIES_FILE_NAME );
            Map<String, String> result = new HashMap<>();

            for ( Iterator<String> it = pc.getKeys(); it.hasNext(); ) {
                String k = it.next();
                result.put( k, pc.getString( k ) );
            }
            return result;

        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

    protected static QuantitationType makeAffyQuantitationType() {
        QuantitationType result = QuantitationType.Factory.newInstance();

        result.setGeneralType( GeneralType.QUANTITATIVE );
        result.setRepresentation( PrimitiveType.DOUBLE ); // no choice here
        result.setIsPreferred( Boolean.TRUE );
        result.setIsNormalized( Boolean.TRUE );
        result.setIsBackgroundSubtracted( Boolean.TRUE );
        result.setIsBackground( false );
        result.setName( AffyPowerToolsProbesetSummarize.METHOD + " value" );
        result.setDescription( "Computed in Gemma by apt-probeset-summarize" );
        result.setType( StandardQuantitationType.AMOUNT );
        result.setIsMaskedPreferred( false ); // this is raw data.
        result.setScale( ScaleType.LOG2 ); // always.
        result.setIsRatio( false );
        result.setIsRecomputedFromRawData( true );

        return result;
    }

    private QuantitationType quantitationType;

    public AffyPowerToolsProbesetSummarize() {
        this.quantitationType = AffyPowerToolsProbesetSummarize.makeAffyQuantitationType();
    }

    /**
     * This constructor is used for multiplatform situations where the same QT must be used for each platform.
     *
     * @param qt qt
     */
    public AffyPowerToolsProbesetSummarize( QuantitationType qt ) {
        this.quantitationType = qt;
    }

    /**
     * @return Map of GPLXXXX to {mps, pgc, qcc, clf} to file name
     */
    protected Map<String, Map<String, String>> loadMpsNames() {
        try {
            PropertiesConfiguration pc = ConfigUtils.loadClasspathConfig( AFFY_MPS_PROPERTIES_FILE_NAME );
            Map<String, Map<String, String>> result = new HashMap<>();

            for ( Iterator<String> it = pc.getKeys(); it.hasNext(); ) {
                String k = it.next();
                String[] k2 = k.split( "\\." );
                String platform = k2[0];
                String type = k2[1];

                if ( !result.containsKey( platform ) ) {
                    result.put( platform, new HashMap<String, String>() );
                }
                result.get( platform ).put( type, pc.getString( k ) );

            }
            return result;

        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * For when we are reprocessing and needed to figure out what the original platform was from the CEL files.
     * 
     * @param ee ee
     * @param targetPlatform target platform (thawed); call multiple times if there is more than one platform (though
     *        that should
     *        not happen for exon arrays)
     * @param originalPlatform
     * @param BioAssays to use
     * @param files list of CEL files (any other files included will be ignored)
     * @return raw data vectors
     */
    protected Collection<RawExpressionDataVector> processData( ExpressionExperiment ee,
            ArrayDesign targetPlatform, ArrayDesign originalPlatform, Collection<BioAssay> bioAssays, Collection<LocalFile> files ) {

        if ( bioAssays.isEmpty() ) {
            throw new IllegalArgumentException( "No assays" );
        }

        if ( targetPlatform.getCompositeSequences().isEmpty() ) {
            throw new IllegalArgumentException( "Target design had no elements" );
        }

        return this.tryRun( ee, targetPlatform, originalPlatform, files, bioAssays );
    }

    /**
     * For either 3' or Exon arrays.
     *
     * @param ee ee
     * @param aptOutputFileToRead file
     * @param targetPlatform (thawed) deal with data from this platform (call multiple times if there is more than one
     *        platform)
     * @param originalPlatform can be the same as the targetPlatform. But we specify this in case there is more than one
     *        original platform, so we're not trying to match up bioassays that are not relevant.
     * @param bioAssays that we're dealing with (could be a subset of a multi-platform experiment)
     * @return raw data vectors
     * @throws IOException io problem
     * @throws FileNotFoundException file not found
     */
    protected Collection<RawExpressionDataVector> processData( ExpressionExperiment ee, String aptOutputFileToRead,
            ArrayDesign targetPlatform, ArrayDesign originalPlatform, Collection<BioAssay> bioAssaysToUse ) throws IOException {

        checkFileReadable( aptOutputFileToRead );

        AffyPowerToolsProbesetSummarize.log.info( "Parsing " + aptOutputFileToRead );

        try (InputStream is = new FileInputStream( aptOutputFileToRead )) {
            DoubleMatrix<String, String> matrix = this.parse( is );

            if ( matrix.rows() == 0 ) {
                throw new IllegalStateException( "Matrix from APT had no rows" );
            }
            if ( matrix.columns() == 0 ) {
                throw new IllegalStateException( "Matrix from APT had no columns" );
            }

            if ( bioAssaysToUse.isEmpty() ) {
                throw new IllegalStateException( "No bioassays were on the platform: " + originalPlatform );
            }

            // this might just be confusing.
            //            if ( ee.getBioAssays().size() > bioAssaysToUse.size() ) {
            //                AffyPowerToolsProbesetSummarize.log
            //                        .info( "Using " + bioAssaysToUse.size() + "/" + ee.getBioAssays().size() + " bioassays (those on "
            //                                + originalPlatform.getShortName() + ")" );
            //            }

            if ( matrix.columns() < bioAssaysToUse.size() ) {
                // having > is okay, there can be extra.
                throw new IllegalStateException(
                        "Matrix from APT had the wrong number of colummns: expected " + bioAssaysToUse.size() + ", got "
                                + matrix.columns() );
            }

            AffyPowerToolsProbesetSummarize.log
                    .info( "Read " + matrix.rows() + " x " + matrix.columns() + ", matching with " + bioAssaysToUse
                            .size() + " samples on " + originalPlatform );

            BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
            bad.setName( "For " + ee.getShortName() + " on " + targetPlatform );
            bad.setDescription( "Generated from output of apt-probeset-summarize" );

            /*
             * Add them ... note we haven't switched platforms yet (and might not do that)
             */

            // this is to help us match results to bioassays
            Map<String, BioAssay> bmap = new HashMap<>();
            for ( BioAssay bioAssay : bioAssaysToUse ) {
                if ( bmap.containsKey( bioAssay.getAccession().getAccession() ) || bmap
                        .containsKey( bioAssay.getName() ) ) {
                    // defensive.
                    throw new IllegalStateException( "Name or accesion was duplicated for : " + bioAssay );
                }
                bmap.put( bioAssay.getAccession().getAccession(), bioAssay );
                bmap.put( bioAssay.getName(), bioAssay );
            }

            log.info( "Will attempt to match " + bmap.size() + " bioAssays in data set to apt output" );

            log.debug( "Will match result data file columns to bioassays referred to by any of the following strings:\n"
                    + StringUtils.join( bmap.keySet(), "\n" ) );

            int found = 0;
            List<String> columnsToKeep = new ArrayList<>();
            for ( int i = 0; i < matrix.columns(); i++ ) {
                String columnName = matrix.getColName( i );

                String sampleName = columnName.replaceAll( ".(CEL|cel)$", "" );

                BioAssay assay = matchBioAssayToCelFileName( bmap, sampleName );

                if ( assay == null ) {
                    /*
                     * This is okay, if we have extras
                     */
                    if ( matrix.columns() == bioAssaysToUse.size() ) {
                        throw new IllegalStateException(
                                "No bioassay could be matched to CEL file identified by " + sampleName );
                    }
                    AffyPowerToolsProbesetSummarize.log.warn( "No bioassay found yet for " + sampleName );
                    continue;
                }

                AffyPowerToolsProbesetSummarize.log
                        .info( "Matching CEL sample " + sampleName + " to bioassay " + assay + " [" + assay
                                .getAccession().getAccession() + "]" );

                columnsToKeep.add( columnName );
                bad.getBioAssays().add( assay );
                found++;
            }

            if ( found != bioAssaysToUse.size() ) {
                throw new IllegalStateException(
                        "Failed to find a data column for every bioassay on the given platform " + targetPlatform );
            }

            if ( columnsToKeep.size() < matrix.columns() ) {
                matrix = matrix.subsetColumns( columnsToKeep );
            }

            if ( quantitationType == null ) {
                quantitationType = AffyPowerToolsProbesetSummarize.makeAffyQuantitationType();
            }
            return this.convertDesignElementDataVectors( ee, bad, targetPlatform, matrix );
        }
    }

    private void checkFileReadable( String f ) {
        if ( !new File( f ).canRead() ) {
            throw new IllegalArgumentException( f + " could not be read" );
        }
    }

    /**
     * Stolen from SimpleExpressionDataLoaderService.
     *
     * @param expressionExperiment ee
     * @param bioAssayDimension BA dim
     * @param targetPlatform target design (thawed)
     * @param matrix matrix read from apt output.
     * @return raw data vectors
     */
    private Collection<RawExpressionDataVector> convertDesignElementDataVectors(
            ExpressionExperiment expressionExperiment, BioAssayDimension bioAssayDimension, ArrayDesign targetPlatform,
            DoubleMatrix<String, String> matrix ) {
        ByteArrayConverter bArrayConverter = new ByteArrayConverter();

        Collection<RawExpressionDataVector> vectors = new HashSet<>();

        Map<String, CompositeSequence> csMap = new HashMap<>();
        for ( CompositeSequence cs : targetPlatform.getCompositeSequences() ) {
            csMap.put( cs.getName(), cs );
        }

        log.info( "Target platform has " + csMap.size() + " elements, apt data matrix has " + matrix.rows() );

        for ( int i = 0; i < matrix.rows(); i++ ) {
            byte[] bdata = bArrayConverter.doubleArrayToBytes( matrix.getRow( i ) );

            RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
            vector.setData( bdata );

            CompositeSequence cs = csMap.get( matrix.getRowName( i ) );
            if ( cs == null ) {
                continue;
            }
            vector.setDesignElement( cs );
            vector.setQuantitationType( this.quantitationType );
            vector.setExpressionExperiment( expressionExperiment );
            vector.setBioAssayDimension( bioAssayDimension );
            vectors.add( vector );

        }
        log.info( "Setup " + vectors.size() + " data vectors for " + matrix.rows() + " results from APT" );
        return vectors;
    }

    /**
     * 
     * @param ad
     * @return file or null if not found
     */
    private File findCdf( ArrayDesign ad ) {
        String affyCdfs = Settings.getString( AFFY_POWER_TOOLS_CDF_PATH );

        Map<String, String> cdfNames = this.loadCdfNames();

        String shortName = ad.getShortName();
        String cdfName = cdfNames.get( shortName );

        if ( cdfName == null ) {
            return null;
        }

        return new File( affyCdfs + File.separatorChar + cdfName );
    }

    /**
     * For 3' arrays. Run RMA with quantile normalization.
     * 
     * <pre>
     * apt-probeset-summarize -a rma  -d HG-U133A_2.cdf -o GSE123.genelevel.data
     * /bigscratch/GSE123/*.CEL
     * </pre>
     *
     * @param targetPlatform ad
     * @param cdfFileName e g. HG-U133A_2.cdf
     * @param celfiles celfiles
     * @param outputPath path
     * @return string
     */
    private String getCDFCommand( ArrayDesign targetPlatform, String cdfFileName,
            List<String> celfiles, String outputPath ) {
        String toolPath = Settings.getString( "affy.power.tools.exec" );

        /*
         * locate the CDF file
         */
        String cdfPath = Settings.getString( "affy.power.tools.cdf.path" );
        String cdfName;
        if ( cdfFileName != null ) {
            cdfName = cdfFileName;
        } else {
            String shortName = targetPlatform.getShortName();
            // probably won't work ...
            cdfName = shortName + ".cdf";
        }
        String cdfFullPath;
        if ( !cdfName.contains( cdfPath ) ) {
            cdfFullPath = cdfPath + File.separator + cdfName; // might be .cdf or .cdf.gz
        } else {
            cdfFullPath = cdfName;
        }
        this.checkFileReadable( cdfFullPath );

        /*
         * HG_U95C.CDF.gz, Mouse430_2.cdf.gz etc.
         */

        return toolPath + " -a " + AffyPowerToolsProbesetSummarize.METHOD + " -d " + cdfFullPath + " -o " + outputPath
                + " " + StringUtils.join( celfiles, " " );
    }

    /**
     * @param files files
     * @param accessionsOfInterest Used for multiplatform studies; if null, ignored
     * @return strings
     */
    private List<String> getCelFiles( Collection<LocalFile> files, Collection<String> accessionsOfInterest ) {

        Set<String> celfiles = new HashSet<>();
        for ( LocalFile f : files ) {
            try {
                File fi = new File( f.getLocalURL().toURI() );

                // If both unpacked and packed files are there, it looks at both of them. No major problem - the dups are resolved - just a little ugly.
                if ( fi.canRead() && ( fi.getName().toUpperCase().endsWith( ".CEL" ) || fi.getName().toUpperCase()
                        .endsWith( ".CEL.GZ" ) ) ) {

                    if ( accessionsOfInterest != null ) {
                        String acc = fi.getName().replaceAll( "(GSM[0-9]+).+", "$1" );
                        if ( !accessionsOfInterest.contains( acc ) ) {
                            continue;
                        }
                    }

                    if ( FileTools.isGZipped( fi.getName() ) ) {
                        AffyPowerToolsProbesetSummarize.log.info( "Found CEL file " + fi + ", unzipping" );
                        try {
                            String unGzipFile = FileTools.unGzipFile( fi.getAbsolutePath() );
                            celfiles.add( unGzipFile );
                        } catch ( IOException e ) {
                            throw new RuntimeException( e );
                        }
                    } else {
                        AffyPowerToolsProbesetSummarize.log.info( "Found CEL file " + fi );
                        celfiles.add( fi.getAbsolutePath() );
                    }
                }
            } catch ( URISyntaxException e ) {
                throw new RuntimeException( e );
            }
        }

        if ( celfiles.isEmpty() ) {
            throw new IllegalArgumentException( "No valid CEL files were found" );
        }
        return new ArrayList<>( celfiles );
    }

    /**
     * For exon arrays and others that don't have CDFs. Like
     * 
     * <pre>
     * apt-probeset-summarize -a rma -p HuEx-1_0-st-v2.r2.pgf -c HuEx-1_0-st-v2.r2.clf -m
     * HuEx-1_0-st-v2.r2.dt1.hg18.core.mps -qc-probesets HuEx-1_0-st-v2.r2.qcc -o GSE13344.genelevel.data
     * /bigscratch/GSE13344/*.CEL
     * </pre>
     * 
     * http://media.affymetrix.com/support/developer/powertools/changelog/apt-probeset-summarize.html
     * http://bib.oxfordjournals.org/content/early/2011/04/15/bib.bbq086.full
     *
     * @param ad ad
     * @param celfiles celfiles
     * @param outputPath directory
     * @return string or null if not found.s
     */
    private String getMPSCommand( ArrayDesign ad, List<String> celfiles, String outputPath ) {
        /*
         * Get the pgf, clf, mps file for this platform. qc probesets: optional.
         */
        String toolPath = Settings.getString( "affy.power.tools.exec" );
        String refPath = Settings.getString( "affy.power.tools.ref.path" );

        this.checkFileReadable( toolPath );

        if ( !new File( refPath ).isDirectory() ) {
            throw new IllegalStateException( refPath + " is not a valid directory" );
        }

        Map<String, Map<String, String>> mpsnames = loadMpsNames();

        String p = ad.getShortName();

        if ( mpsnames == null || !mpsnames.containsKey( p ) ) {
            return null;
        }

        String pgf = refPath + File.separator + mpsnames.get( p ).get( "pgf" );
        String clf = refPath + File.separator + mpsnames.get( p ).get( "clf" );
        String mps = refPath + File.separator + mpsnames.get( p ).get( "mps" );
        String qcc = refPath + File.separator + mpsnames.get( p ).get( "qcc" );

        this.checkFileReadable( pgf );
        this.checkFileReadable( clf );
        this.checkFileReadable( mps );
        this.checkFileReadable( qcc );

        return toolPath + " -a " + AffyPowerToolsProbesetSummarize.METHOD + " -p " + pgf + " -c " + clf + " -m " + mps
                + " -o " + outputPath + " --qc-probesets " + qcc + " " + StringUtils.join( celfiles, " " );
    }

    private String getOutputFilePath( ExpressionExperiment ee ) {
        File tmpDir = new File( Settings.getDownloadPath() );
        return tmpDir + File.separator + ee.getId() + "_" + RandomStringUtils.randomAlphanumeric( 4 ) + "_"
                + "apt-output";
    }

    private Map<String, String> loadCdfNames() {
        try {
            PropertiesConfiguration pc = ConfigUtils.loadClasspathConfig( AFFY_CDFS_PROPERTIES_FILE_NAME );
            Map<String, String> result = new HashMap<>();

            for ( Iterator<String> it = pc.getKeys(); it.hasNext(); ) {
                String k = it.next();
                result.put( k, pc.getString( k ) );
            }
            return result;

        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

    private DoubleMatrix<String, String> parse( InputStream data ) throws IOException {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        return reader.read( data );
    }

    /**
     * @param ee
     * @param targetPlatform - platform whose configuratino we're using to run apt-probeset-summarize
     * @param originalPlatform - only really necessary if we are switching platforms AND there are multiple platforms
     *        for the data set
     * @param files
     * @param bioAssays that we're dealing with (would be a subset of a dataset if multiplatform)
     * @return
     */
    private Collection<RawExpressionDataVector> tryRun( ExpressionExperiment ee, ArrayDesign targetPlatform,
            ArrayDesign originalPlatform, Collection<LocalFile> files, Collection<BioAssay> bioAssays ) {

        Collection<String> accessionsOfInterest = new HashSet<>();
        for ( BioAssay ba : bioAssays ) {
            accessionsOfInterest.add( ba.getAccession().getAccession() );
        }

        List<String> celFiles = this.getCelFiles( files, accessionsOfInterest );
        AffyPowerToolsProbesetSummarize.log.info( "Located " + celFiles.size() + " cel files" );
        String outputPath = this.getOutputFilePath( ee );
        String cmd = null;

        // look for a CDF first.
        File cdf = this.findCdf( targetPlatform );
        if ( cdf != null ) {
            String cdfFileName = cdf.getAbsolutePath();
            cmd = this.getCDFCommand( targetPlatform, cdfFileName, celFiles, outputPath );
        } else {
            /* presumably mps based */
            cmd = this.getMPSCommand( targetPlatform, celFiles, outputPath );
        }

        if ( cmd == null ) {
            throw new IllegalArgumentException( "There is no MPS configuration for " + targetPlatform.getShortName() + ", check "
                    + AFFY_MPS_PROPERTIES_FILE_NAME + " and " + AFFY_CDFS_PROPERTIES_FILE_NAME );
        }

        log.info( "Original platform: " + originalPlatform + "; Target platform (apt-probeset-summarize will be called with): " + targetPlatform );

        AffyPowerToolsProbesetSummarize.log.info( "Running: " + cmd );

        int exitVal = Integer.MIN_VALUE;

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        try {
            final Process run = Runtime.getRuntime().exec( cmd );
            GenericStreamConsumer gscErr = new GenericStreamConsumer( run.getErrorStream(), true );
            GenericStreamConsumer gscIn = new GenericStreamConsumer( run.getInputStream() );
            gscErr.start();
            gscIn.start();

            int i = 0;
            while ( exitVal == Integer.MIN_VALUE ) {
                try {
                    exitVal = run.exitValue();
                } catch ( IllegalThreadStateException e ) {
                    // okay, still waiting.
                }
                Thread.sleep( 1000 );

                if ( ++i % AffyPowerToolsProbesetSummarize.AFFY_UPDATE_INTERVAL_S == 0 ) {
                    File outputFile = new File( outputPath + File.separator + "apt-probeset-summarize.log" );
                    Long size = outputFile.length();

                    String minutes = TimeUtil.getMinutesElapsed( overallWatch );
                    AffyPowerToolsProbesetSummarize.log
                            .info( String.format( "apt-probeset-summarize logging output so far: %.2f", size / 1024.0 )
                                    + " kb (" + minutes + " minutes elapsed)" );
                }

            }

            if ( exitVal > 0 ) {
                log.warn( "apt-probeset-summarize exit value was non-zero: " + exitVal );
            }

            overallWatch.stop();
            String minutes = TimeUtil.getMinutesElapsed( overallWatch );
            AffyPowerToolsProbesetSummarize.log
                    .info( "apt-probeset-summarize took a total of " + minutes + " minutes" );

            return this.processData( ee,
                    outputPath + File.separator + AffyPowerToolsProbesetSummarize.METHOD + ".summary.txt",
                    targetPlatform, originalPlatform, bioAssays );

        } catch ( InterruptedException | IOException e ) {
            throw new RuntimeException( e );
        }
    }

}
