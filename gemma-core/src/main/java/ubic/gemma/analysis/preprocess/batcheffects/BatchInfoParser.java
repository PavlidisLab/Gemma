/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.analysis.preprocess.batcheffects;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Parse information on batch from raw data files. This will typically correspond to "scan date" for an array.
 * 
 * @author paul
 * @version $Id$
 */
public class BatchInfoParser {

    private static Log log = LogFactory.getLog( BatchInfoParser.class );

    /**
     * @param ee
     * @param files
     * @return
     */
    public Map<BioMaterial, Date> getBatchInfo( ExpressionExperiment ee, Collection<LocalFile> files ) {

        Map<String, BioAssay> assayAccessions = getAccessionToBioAssayMap( ee );

        if ( assayAccessions.isEmpty() ) {
            throw new UnsupportedOperationException(
                    "Couldn't get any scan date information, could not determine provider or it is not supported for "
                            + ee.getShortName() );
        }

        Map<BioAssay, File> bioAssays2Files = matchBioAssaysToRawDataFilse( files, assayAccessions );

        /*
         * Check if we should go on
         */
        if ( bioAssays2Files.size() < assayAccessions.size() ) {

            if ( bioAssays2Files.size() > 0 ) {
                /*
                 * Missing a few for some reason.
                 */
                for ( BioAssay ba : bioAssays2Files.keySet() ) {
                    if ( !assayAccessions.containsKey( ba.getAccession().getAccession() ) ) {
                        log.warn( "Missing raw data file for " + ba + " on " + ee.getShortName() );
                    }
                }
            }

            throw new IllegalStateException( "Did not get enough raw files :got " + bioAssays2Files.size()
                    + ", expected " + assayAccessions.size() + " while processing " + ee.getShortName() );
        }

        Map<BioMaterial, Date> result = getBatchInformationFromFiles( bioAssays2Files );

        return result;
    }

    /**
     * Bookkeeping
     * 
     * @param ee
     * @return
     */
    private Map<String, BioAssay> getAccessionToBioAssayMap( ExpressionExperiment ee ) {
        Map<String, BioAssay> assayAccessions = new HashMap<String, BioAssay>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            DatabaseEntry accession = ba.getAccession();
            ArrayDesign arrayDesignUsed = ba.getArrayDesignUsed();

            if ( !isSupported( arrayDesignUsed ) ) {
                log.warn( "Can't get batch information for:" + arrayDesignUsed );
                continue;
            }

            // accession.getExternalDatabase(); // check for GEO

            if ( StringUtils.isBlank( accession.getAccession() ) ) {
                throw new IllegalStateException(
                        "Must have accession for each bioassay to get batch information from source for "
                                + ee.getShortName() );
            }

            assayAccessions.put( accession.getAccession(), ba );
        }
        return assayAccessions;
    }

    private boolean isSupported( ArrayDesign arrayDesignUsed ) {

        if ( arrayDesignUsed.getName().toLowerCase().contains( "affymetrix" ) ) return true;

        if ( arrayDesignUsed.getName().toLowerCase().contains( "agilent" ) ) return true;

        if ( arrayDesignUsed.getDesignProvider() == null
                || StringUtils.isBlank( arrayDesignUsed.getDesignProvider().getName() ) ) return false;

        if ( arrayDesignUsed.getDesignProvider().getName().equalsIgnoreCase( "affymetrix" ) ) return true;

        if ( arrayDesignUsed.getDesignProvider().getName().equalsIgnoreCase( "agilent" ) ) return true;

        return false;

    }

    /**
     * Now we can parse the file to get the batch information
     * 
     * @param bioAssays2Files
     * @return
     */
    private Map<BioMaterial, Date> getBatchInformationFromFiles( Map<BioAssay, File> bioAssays2Files ) {

        Map<BioMaterial, Date> result = new HashMap<BioMaterial, Date>();
        for ( BioAssay ba : bioAssays2Files.keySet() ) {
            File f = bioAssays2Files.get( ba );

            ArrayDesign arrayDesignUsed = ba.getArrayDesignUsed();

            try {

                ScanDateExtractor ex = null;

                String providerName = arrayDesignUsed.getDesignProvider().getName();
                if ( providerName.equalsIgnoreCase( "affymetrix" )
                        || arrayDesignUsed.getName().toLowerCase().contains( "affymetrix" ) ) {
                    ex = new AffyScanDateExtractor();
                } else if ( providerName.equalsIgnoreCase( "agilent" )
                        || arrayDesignUsed.getName().toLowerCase().contains( "agilent" ) ) {
                    ex = new AgilentScanDateExtractor();
                } else {
                    throw new UnsupportedRawdataFileFormatException( arrayDesignUsed
                            + " not matched to a supported platform type for scan date extraction for " + ba );
                }

                InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() );
                Date d = ex.extract( is );
                for ( BioMaterial bm : ba.getSamplesUsed() ) {
                    result.put( bm, d );
                }
            } catch ( FileNotFoundException e ) {
                throw new RuntimeException( e );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
        return result;
    }

    /**
     * From the file names, match to the bioassays. GEO names things consistently (??) so this should work but not
     * ideal.
     * 
     * @param files
     * @param assayAccessions
     * @return
     */
    private Map<BioAssay, File> matchBioAssaysToRawDataFilse( Collection<LocalFile> files,
            Map<String, BioAssay> assayAccessions ) {

        Pattern regex = Pattern.compile( "(GSM[0-9]+).+" );

        Map<BioAssay, File> bioAssays2Files = new HashMap<BioAssay, File>();
        for ( LocalFile file : files ) {
            File f = file.asFile();

            String n = f.getName();

            /*
             * We only support the newer style of storing these.
             */
            if ( !n.startsWith( "GSM" ) ) {
                continue;
            }

            if ( n.toUpperCase().contains( "CHP" ) || n.contains( "DAT" ) || n.contains( "EXP" ) ) {
                continue;
            }

            /*
             * keep just the GSMNNNNNN part. FIXME: only works with GEO
             */
            Matcher matcher = regex.matcher( n );
            if ( !matcher.matches() ) {
                continue;
            }
            String acc = matcher.group( 1 );

            assert acc.matches( "GSM[0-9]+" );

            BioAssay ba = assayAccessions.get( acc );
            if ( ba == null ) {
                /*
                 * Warn? Throw exception?
                 */
                continue;
            }
            if ( bioAssays2Files.containsKey( ba ) ) {
                log.warn( "Multiple files matching " + ba + ": " + bioAssays2Files.get( ba ) + "; " + f );
            }
            bioAssays2Files.put( ba, f );
        }
        return bioAssays2Files;
    }
}
