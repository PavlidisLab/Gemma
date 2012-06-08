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
package ubic.gemma.loader.expression;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.TimeUtil;
import ubic.gemma.util.concurrent.GenericStreamConsumer;

/**
 * @author paul
 * @version $Id$
 */
public class AffyPowerToolsProbesetSummarize {

    private static final long AFFY_UPDATE_INTERVAL_MS = 1000 * 30;

    private static Log log = LogFactory.getLog( AffyPowerToolsProbesetSummarize.class );

    /*
     * These are supplied by Affymetrix. Current as of May 2012
     */
    private static final String hg = "hg18";
    private static final String mm = "mm9";
    private static final String rn = "rn4";

    private static final String METHOD = "rma";

    /*
     * Current as of May 2012
     */
    private static final String h = "HuEx-1_0-st-v2.r2";
    private static final String m = "MoEx-1_0-st-v1.r2";
    private static final String r = "RaEx-1_0-st-v1.r2";

    /**
     * @param ee
     * @param targetPlatform target platform
     * @param files list of CEL files (any other files included will be ignored)
     * @return
     */
    public Collection<RawExpressionDataVector> processExonArrayData( ExpressionExperiment ee,
            ArrayDesign targetPlatform, Collection<LocalFile> files ) {

        Collection<BioAssay> bioAssays = ee.getBioAssays();

        if ( bioAssays.isEmpty() ) {
            throw new IllegalArgumentException( "Experiment had no assays" );
        }

        if ( targetPlatform.getCompositeSequences().isEmpty() ) {
            throw new IllegalArgumentException( "Target design had no elements" );
        }

        List<String> celfiles = getCelFiles( files );
        log.info( celfiles.size() + " cel files" );

        String outputPath = getOutputFilePath( ee, "apt-output" );

        String cmd = getCommand( targetPlatform, celfiles, outputPath );

        log.info( "Running: " + cmd );

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
                    // okay, still
                    // waiting.
                }
                Thread.sleep( AFFY_UPDATE_INTERVAL_MS );

                synchronized ( outputPath ) {
                    File outputFile = new File( outputPath + File.separator + "apt-probeset-summarize.log" );
                    Long size = outputFile.length();

                    String minutes = TimeUtil.getMinutesElapsed( overallWatch );
                    log.info( String.format( "apt-probeset-summarize logging output so far: %.2f", size / 1024.0 )
                            + " kb (" + minutes + " minutes elapsed)" );
                }
            }

            overallWatch.stop();
            String minutes = TimeUtil.getMinutesElapsed( overallWatch );
            log.info( "apt-probeset-summarize took a total of " + minutes + " minutes" );

            DoubleMatrix<String, String> matrix = parse( new FileInputStream( outputPath + File.separator + METHOD
                    + ".summary.txt" ) );

            if ( matrix.rows() == 0 ) {
                throw new IllegalStateException( "Matrix from APT had no rows" );
            }
            if ( matrix.columns() == 0 ) {
                throw new IllegalStateException( "Matrix from APT had no columns" );
            }
            if ( matrix.columns() != bioAssays.size() ) {
                throw new IllegalStateException( "Matrix from APT had the wrong number of colummns" );
            }

            BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
            bad.setName( "For " + ee.getShortName() );
            bad.setDescription( "Generated from output of apt-probeset-summarize" );

            /*
             * Add them ...
             */

            Map<String, BioAssay> bmap = new HashMap<String, BioAssay>();
            for ( BioAssay bioAssay : bioAssays ) {

                if ( bmap.containsKey( bioAssay.getAccession().getAccession() )
                        || bmap.containsKey( bioAssay.getName() ) ) {
                    throw new IllegalStateException( "Duplicate" );
                }
                bmap.put( bioAssay.getAccession().getAccession(), bioAssay );
                bmap.put( bioAssay.getName(), bioAssay );
            }

            for ( int i = 0; i < matrix.columns(); i++ ) {
                String columnName = matrix.getColName( i );

                String sampleName = columnName.replaceAll( ".(CEL|cel)$", "" );

                /*
                 * Column names are like Aud_19L.CEL
                 */
                BioAssay assay = bmap.get( sampleName );

                if ( assay == null ) {
                    throw new IllegalStateException( "No bioassay could be matched to CEL file identified by "
                            + sampleName );
                }

                log.info( "Matching CEL sample " + sampleName + " to bioassay " + assay + " ["
                        + assay.getAccession().getAccession() + "]" );

                assay.setArrayDesignUsed( targetPlatform ); // OK?
                bad.getBioAssays().add( assay );
            }
            return convertDesignElementDataVectors( ee, bad, targetPlatform, makeExonArrayQuantiationType(), matrix );

        } catch ( InterruptedException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    private void checkFileReadable( String pgf ) {
        if ( !new File( pgf ).canRead() ) {
            throw new IllegalArgumentException( pgf + " could not be read" );
        }
    }

    /**
     * Stolen from SimpleExpressionDataLoaderService
     * 
     * @param expressionExperiment
     * @param bioAssayDimension
     * @param arrayDesign target design
     * @param quantitationType
     * @param matrix
     * @return Collection<DesignElementDataVector>
     */
    private Collection<RawExpressionDataVector> convertDesignElementDataVectors(
            ExpressionExperiment expressionExperiment, BioAssayDimension bioAssayDimension, ArrayDesign arrayDesign,
            QuantitationType quantitationType, DoubleMatrix<String, String> matrix ) {
        ByteArrayConverter bArrayConverter = new ByteArrayConverter();

        Collection<RawExpressionDataVector> vectors = new HashSet<RawExpressionDataVector>();

        Map<String, CompositeSequence> csMap = new HashMap<String, CompositeSequence>();
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
            vector.setQuantitationType( quantitationType );
            vector.setExpressionExperiment( expressionExperiment );
            vector.setBioAssayDimension( bioAssayDimension );
            vectors.add( vector );

        }
        log.info( "Setup " + vectors.size() + " data vectors for " + matrix.rows() + " results from APT" );
        return vectors;
    }

    /**
     * @param files
     * @return
     */
    private List<String> getCelFiles( Collection<LocalFile> files ) {

        Set<String> celfiles = new HashSet<String>();
        for ( LocalFile f : files ) {
            try {
                File fi = new File( f.getLocalURL().toURI() );
                if ( fi.canRead()
                        && ( fi.getName().toUpperCase().endsWith( ".CEL" ) || fi.getName().toUpperCase()
                                .endsWith( ".CEL.GZ" ) ) ) {

                    if ( FileTools.isGZipped( fi.getName() ) ) {
                        log.info( "Found CEL file " + fi + ", unzipping" );
                        try {
                            String unGzipFile = FileTools.unGzipFile( fi.getAbsolutePath() );
                            celfiles.add( unGzipFile );
                        } catch ( IOException e ) {
                            throw new RuntimeException( e );
                        }
                    } else {
                        log.info( "Found CEL file " + fi );
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
        return new ArrayList<String>( celfiles );
    }

    /**
     * Like
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
     * @param ad
     * @param celfiles
     * @param outputPath directory
     * @return
     */
    private String getCommand( ArrayDesign ad, List<String> celfiles, String outputPath ) {
        /*
         * Get the pgf, clf, mps file for this platform. qc probesets: optional.
         */
        String toolPath = ConfigUtils.getString( "affy.power.tools.exec" );
        String refPath = ConfigUtils.getString( "affy.power.tools.ref.path" );

        checkFileReadable( toolPath );

        if ( !new File( refPath ).isDirectory() ) {
            throw new IllegalStateException( refPath + " is not a valid directory" );
        }

        Taxon primaryTaxon = ad.getPrimaryTaxon();

        String base = h;
        String genome = hg;
        if ( primaryTaxon.getCommonName().equals( "human" ) ) {
            base = h;
            genome = hg;
        } else if ( primaryTaxon.getCommonName().equals( "mouse" ) ) {
            base = m;
            genome = mm;
        } else if ( primaryTaxon.getCommonName().equals( "rat" ) ) {
            base = r;
            genome = rn;
        } else {
            throw new IllegalArgumentException( "Cannot use " + primaryTaxon );
        }

        String pgf = refPath + File.separator + base + ".pgf";
        String clf = refPath + File.separator + base + ".clf";
        String mps = refPath + File.separator + base + ".dt1." + genome + ".core.mps";
        String qcc = refPath + File.separator + base + ".qcc";

        checkFileReadable( pgf );
        checkFileReadable( clf );
        checkFileReadable( mps );
        checkFileReadable( qcc );

        String cmd = toolPath + " -a " + METHOD + " -p " + pgf + " -c " + clf + " -m " + mps + " -o " + outputPath
                + " --qc-probesets " + qcc + " " + StringUtils.join( celfiles, " " );
        return cmd;
    }

    /**
     * @param ee
     * @param base
     * @return
     */
    private String getOutputFilePath( ExpressionExperiment ee, String base ) {
        File tmpdir = new File( ConfigUtils.getDownloadPath() );
        return tmpdir + File.separator + ee.getId() + "_" + base;
    }

    /**
     * @return
     */
    private QuantitationType makeExonArrayQuantiationType() {
        QuantitationType result = QuantitationType.Factory.newInstance();

        result.setGeneralType( GeneralType.QUANTITATIVE );
        result.setRepresentation( PrimitiveType.DOUBLE ); // no choice here
        result.setIsPreferred( Boolean.TRUE );
        result.setIsNormalized( Boolean.TRUE );
        result.setIsBackgroundSubtracted( Boolean.TRUE );
        result.setIsBackground( false );
        result.setName( METHOD + " value" );
        result.setDescription( "Computed in Gemma by apt-probeset-summarize" );
        result.setType( StandardQuantitationType.AMOUNT );
        result.setIsMaskedPreferred( false ); // this is raw data.
        result.setScale( ScaleType.LOG2 );
        result.setIsRatio( false );

        return result;
    }

    /**
     * @param data
     * @return
     * @throws IOException
     */
    private DoubleMatrix<String, String> parse( InputStream data ) throws IOException {
        DoubleMatrixReader reader = new DoubleMatrixReader();
        return reader.read( data );
    }

}
