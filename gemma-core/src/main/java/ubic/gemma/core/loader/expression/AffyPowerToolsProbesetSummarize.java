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

    private static final String AFFY_CDFS_PROPERTIES_FILE_NAME = "ubic/gemma/affy.cdfs.properties";
    private static final String AFFY_MPS_PROPERTIES_FILE_NAME = "ubic/gemma/affy.mps.properties";

    private static final String AFFY_POWER_TOOLS_CDF_PATH = "affy.power.tools.cdf.path";

    private static final long AFFY_UPDATE_INTERVAL_MS = 1000 * 30;
    private static final Log log = LogFactory.getLog( AffyPowerToolsProbesetSummarize.class );

    private static final String METHOD = "rma";

    public static QuantitationType makeAffyQuantitationType() {
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
        result.setScale( ScaleType.LOG2 );
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
     * For either 3' or Exon arrays.
     *
     * @param ee ee
     * @param aptOutputFileToRead file
     * @param targetPlatform deal with data from this platform (call multiple times if there is more than one platform)
     * @return raw data vectors
     * @throws IOException io problem
     * @throws FileNotFoundException file not found
     */
    public Collection<RawExpressionDataVector> processData( ExpressionExperiment ee, String aptOutputFileToRead,
            ArrayDesign targetPlatform ) throws IOException {

        AffyPowerToolsProbesetSummarize.log.info( "Parsing " + aptOutputFileToRead );

        try (InputStream is = new FileInputStream( aptOutputFileToRead )) {
            DoubleMatrix<String, String> matrix = this.parse( is );

            if ( matrix.rows() == 0 ) {
                throw new IllegalStateException( "Matrix from APT had no rows" );
            }
            if ( matrix.columns() == 0 ) {
                throw new IllegalStateException( "Matrix from APT had no columns" );
            }

            Collection<BioAssay> allBioAssays = ee.getBioAssays();

            Collection<BioAssay> bioAssaysToUse = new HashSet<>();
            for ( BioAssay bioAssay : allBioAssays ) {
                if ( bioAssay.getArrayDesignUsed().equals( targetPlatform ) ) {
                    bioAssaysToUse.add( bioAssay );
                }
            }

            if ( allBioAssays.size() > bioAssaysToUse.size() ) {
                AffyPowerToolsProbesetSummarize.log
                        .info( "Using " + bioAssaysToUse.size() + "/" + allBioAssays.size() + " bioassays (those on "
                                + targetPlatform.getShortName() + ")" );
            }

            if ( matrix.columns() < bioAssaysToUse.size() ) {
                // having > is okay, there can be extra.
                throw new IllegalStateException(
                        "Matrix from APT had the wrong number of colummns: expected " + bioAssaysToUse.size() + ", got "
                                + matrix.columns() );
            }

            AffyPowerToolsProbesetSummarize.log
                    .info( "Read " + matrix.rows() + " x " + matrix.columns() + ", matching with " + bioAssaysToUse
                            .size() + " samples on " + targetPlatform );

            BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
            bad.setName( "For " + ee.getShortName() + " on " + targetPlatform );
            bad.setDescription( "Generated from output of apt-probeset-summarize" );

            /*
             * Add them ...
             */

            Map<String, BioAssay> bmap = new HashMap<>();
            for ( BioAssay bioAssay : bioAssaysToUse ) {
                assert bioAssay.getArrayDesignUsed().equals( targetPlatform );
                if ( bmap.containsKey( bioAssay.getAccession().getAccession() ) || bmap
                        .containsKey( bioAssay.getName() ) ) {
                    throw new IllegalStateException( "Duplicate" );
                }
                bmap.put( bioAssay.getAccession().getAccession(), bioAssay );
                bmap.put( bioAssay.getName(), bioAssay );
            }

            if ( AffyPowerToolsProbesetSummarize.log.isDebugEnabled() )
                AffyPowerToolsProbesetSummarize.log
                        .debug( "Will match result data file columns to bioassays referred to by any of the following strings:\n"
                                + StringUtils.join( bmap.keySet(), "\n" ) );

            int found = 0;
            List<String> columnsToKeep = new ArrayList<>();
            for ( int i = 0; i < matrix.columns(); i++ ) {
                String columnName = matrix.getColName( i );

                String sampleName = columnName.replaceAll( ".(CEL|cel)$", "" );

                /*
                 * Look for patterns like GSM476194_SK_09-BALBcJ_622.CEL
                 */
                BioAssay assay = null;
                if ( sampleName.matches( "^GSM[0-9]+_.+" ) ) {
                    String geoAcc = sampleName.split( "_" )[0];

                    AffyPowerToolsProbesetSummarize.log.info( "Found column for " + geoAcc );
                    if ( bmap.containsKey( geoAcc ) ) {
                        assay = bmap.get( geoAcc );
                    } else {
                        AffyPowerToolsProbesetSummarize.log.warn( "No bioassay for " + geoAcc );
                    }
                } else {

                    /*
                     * Sometimes column names are like Aud_19L.CEL or
                     */
                    assay = bmap.get( sampleName );
                }

                if ( assay == null ) {
                    /*
                     * This is okay, if we have extras
                     */
                    if ( matrix.columns() == bioAssaysToUse.size() ) {
                        throw new IllegalStateException(
                                "No bioassay could be matched to CEL file identified by " + sampleName );
                    }
                    AffyPowerToolsProbesetSummarize.log.warn( "No bioassay for " + sampleName );
                    continue;
                }

                AffyPowerToolsProbesetSummarize.log
                        .info( "Matching CEL sample " + sampleName + " to bioassay " + assay + " [" + assay
                                .getAccession().getAccession() + "]" );

                columnsToKeep.add( columnName );
                assert assay.getArrayDesignUsed().equals( targetPlatform );
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

    /**
     * @param ee ee
     * @param targetPlatform target platform; call multiple times if there is more than one platform (though that should
     *        not happen for exon arrays)
     * @param files list of CEL files (any other files included will be ignored)
     * @return raw data vectors
     */
    public Collection<RawExpressionDataVector> processExonOrGeneArrayData( ExpressionExperiment ee,
            ArrayDesign targetPlatform, Collection<LocalFile> files ) {

        Collection<BioAssay> bioAssays = ee.getBioAssays();

        if ( bioAssays.isEmpty() ) {
            throw new IllegalArgumentException( "Experiment had no assays" );
        }

        if ( targetPlatform.getCompositeSequences().isEmpty() ) {
            throw new IllegalArgumentException( "Target design had no elements" );
        }

        return this.tryRun( ee, targetPlatform, files, null, false );
    }

    /**
     * Call once for each platform used by the experiment.
     *
     * @param ee ee
     * @param targetPlatform to match the CDF file
     * @param files files
     * @return raw data vectors
     */
    public Collection<RawExpressionDataVector> processThreeprimeArrayData( ExpressionExperiment ee,
            ArrayDesign targetPlatform, Collection<LocalFile> files ) {

        Collection<BioAssay> bioAssays = ee.getBioAssays();

        if ( bioAssays.isEmpty() ) {
            throw new IllegalArgumentException( "Experiment had no assays" );
        }

        if ( targetPlatform.getCompositeSequences().isEmpty() ) {
            throw new IllegalArgumentException( "Target design had no elements" );
        }

        /*
         * we may have multiple platforms; we need to get only the bioassays of interest.
         */
        Collection<String> accessionsOfInterest = new HashSet<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            if ( ba.getArrayDesignUsed().equals( targetPlatform ) ) {
                accessionsOfInterest.add( ba.getAccession().getAccession() );
            }
        }

        return this.tryRun( ee, targetPlatform, files, accessionsOfInterest, true );
    }

    private void checkFileReadable( String f ) {
        if ( !new File( f ).canRead() ) {
            throw new IllegalArgumentException( f + " could not be read" );
        }
    }

    /**
     * Stolen from SimpleExpressionDataLoaderService
     *
     * @param expressionExperiment ee
     * @param bioAssayDimension BA dim
     * @param arrayDesign target design
     * @param matrix matrix
     * @return raw data vectors
     */
    private Collection<RawExpressionDataVector> convertDesignElementDataVectors(
            ExpressionExperiment expressionExperiment, BioAssayDimension bioAssayDimension, ArrayDesign arrayDesign,
            DoubleMatrix<String, String> matrix ) {
        ByteArrayConverter bArrayConverter = new ByteArrayConverter();

        Collection<RawExpressionDataVector> vectors = new HashSet<>();

        Map<String, CompositeSequence> csMap = new HashMap<>();
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            csMap.put( cs.getName(), cs );
        }

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
        AffyPowerToolsProbesetSummarize.log
                .info( "Setup " + vectors.size() + " data vectors for " + matrix.rows() + " results from APT" );
        return vectors;
    }

    private File findCdf( ArrayDesign ad ) {
        String affyCdfs = Settings.getString( AFFY_POWER_TOOLS_CDF_PATH );

        Map<String, String> cdfNames = this.loadCdfNames();

        String shortName = ad.getShortName();
        String cdfName = cdfNames.get( shortName );

        if ( cdfName == null ) {
            throw new IllegalArgumentException(
                    "No CDF could be located for " + ad + ", please provide correct affy.power.tools.cdf.path "
                            + "and a valid affy.cdfs.properties file in your classpath, "
                            + "or specify via the -cdf option" );
        }

        return new File( affyCdfs + File.separatorChar + cdfName );
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
     * @return string
     */
    private String getCommand( ArrayDesign ad, List<String> celfiles, String outputPath ) {
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
    private String getThreePrimeSummarizationCommand( ArrayDesign targetPlatform, String cdfFileName,
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

    /**
     * Map of GPLXXXX -> {mps, pgc, qcc, clf} -> file name
     * 
     * @return
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

    private DoubleMatrix<String, String> parse( InputStream data ) throws IOException {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        return reader.read( data );
    }

    private Collection<RawExpressionDataVector> tryRun( ExpressionExperiment ee, ArrayDesign targetPlatform,
            Collection<LocalFile> files, Collection<String> accessionsOfInterest, boolean threePrime ) {

        List<String> celFiles = this.getCelFiles( files, accessionsOfInterest );
        AffyPowerToolsProbesetSummarize.log.info( "Located " + celFiles.size() + " cel files" );
        String outputPath = this.getOutputFilePath( ee );
        String cmd;

        if ( threePrime /* cdf based */ ) {
            String cdfFileName = this.findCdf( targetPlatform ).getAbsolutePath();
            cmd = this.getThreePrimeSummarizationCommand( targetPlatform, cdfFileName, celFiles, outputPath );
        } else {
            /* mps based */
            cmd = this.getCommand( targetPlatform, celFiles, outputPath );
        }

        AffyPowerToolsProbesetSummarize.log.info( "Running: " + cmd );

        int exitVal = Integer.MIN_VALUE;

        StopWatch overallWatch = new StopWatch();
        overallWatch.start();

        try {
            final Process run = Runtime.getRuntime().exec( cmd );
            GenericStreamConsumer gscErr = new GenericStreamConsumer( run.getErrorStream() );
            GenericStreamConsumer gscIn = new GenericStreamConsumer( run.getInputStream() );
            gscErr.start();
            gscIn.start();

            while ( exitVal == Integer.MIN_VALUE ) {
                try {
                    exitVal = run.exitValue();
                } catch ( IllegalThreadStateException e ) {
                    // okay, still waiting.
                }
                Thread.sleep( AffyPowerToolsProbesetSummarize.AFFY_UPDATE_INTERVAL_MS );

                File outputFile = new File( outputPath + File.separator + "apt-probeset-summarize.log" );
                Long size = outputFile.length();

                String minutes = TimeUtil.getMinutesElapsed( overallWatch );
                AffyPowerToolsProbesetSummarize.log
                        .info( String.format( "apt-probeset-summarize logging output so far: %.2f", size / 1024.0 )
                                + " kb (" + minutes + " minutes elapsed)" );
            }

            overallWatch.stop();
            String minutes = TimeUtil.getMinutesElapsed( overallWatch );
            AffyPowerToolsProbesetSummarize.log
                    .info( "apt-probeset-summarize took a total of " + minutes + " minutes" );

            return this.processData( ee,
                    outputPath + File.separator + AffyPowerToolsProbesetSummarize.METHOD + ".summary.txt",
                    targetPlatform );

        } catch ( InterruptedException | IOException e ) {
            throw new RuntimeException( e );
        }
    }

}
