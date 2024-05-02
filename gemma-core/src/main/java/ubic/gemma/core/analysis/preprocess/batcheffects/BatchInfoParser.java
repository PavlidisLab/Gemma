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
package ubic.gemma.core.analysis.preprocess.batcheffects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse information on batch from raw data files. This will typically correspond to "scan date" for an array.
 *
 * @author paul
 */
public class BatchInfoParser {

    private static final Log log = LogFactory.getLog( BatchInfoParser.class );

    private ScanDateExtractor scanDateExtractor = null;

    /**
     * @param ee experiment
     * @return bioassay accession (GSM...) to bioassay map
     */
    public static Map<String, BioAssay> getAccessionToBioAssayMap( ExpressionExperiment ee ) {
        Map<String, BioAssay> assayAccessions = new HashMap<>();
        for ( BioAssay ba : ee.getBioAssays() ) {
            DatabaseEntry accession = ba.getAccession();
            if ( StringUtils.isBlank( accession.getAccession() ) ) {
                throw new IllegalStateException(
                        "Must have accession for each bioassay to get batch information from source for " + ee
                                .getShortName() );
            }

            assayAccessions.put( accession.getAccession(), ba );
        }
        return assayAccessions;
    }

    public Map<BioMaterial, Date> getBatchInfo( ExpressionExperiment ee, Collection<LocalFile> files ) {

        Map<String, BioAssay> assayAccessions = BatchInfoParser.getAccessionToBioAssayMap( ee );

        if ( assayAccessions.isEmpty() ) {
            throw new UnsupportedOperationException(
                    "Couldn't get any scan date information, could not determine provider or it is not supported for "
                            + ee.getShortName() );
        }

        Map<BioAssay, File> bioAssays2Files = this.matchBioAssaysToRawDataFiles( files, assayAccessions );

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
                        BatchInfoParser.log.warn( "Missing raw data file for " + ba + " on " + ee.getShortName() );
                    }
                }
            }
            throw new BatchInfoPopulationException(ee,
                    "Did not get enough raw files :got " + bioAssays2Files.size() + ", expected " + assayAccessions
                            .size() + " while processing " + ee.getShortName() );
        }

        return this.getBatchInformationFromFiles( ee, bioAssays2Files );
    }

    /**
     * @return the extractor used. The class of the value can tell you what type of file was detected, or if it was
     * generic.
     */
    public ScanDateExtractor getScanDateExtractor() {
        return scanDateExtractor;
    }

    /**
     * Now we can parse the file to get the batch information.
     * We allow ourselves to add dates to _some_ of the bioassays. It turns out to be common for there to be a single
     * corrupted date in CEL files, for example. However, downstream code has to be careful, and the batch factor could
     * be a problem too.
     *
     * @param ee
     * @param bioAssays2Files BA 2 files
     * @return map of biomaterials to dates. Biomaterials which did not have associated dates are not included in the
     * map.
     */
    private Map<BioMaterial, Date> getBatchInformationFromFiles( ExpressionExperiment ee, Map<BioAssay, File> bioAssays2Files ) {

        Map<BioMaterial, Date> result = new HashMap<>();
        Collection<File> missingDate = new HashSet<>();
        for ( BioAssay ba : bioAssays2Files.keySet() ) {
            File f = bioAssays2Files.get( ba );

            ArrayDesign arrayDesignUsed = ba.getArrayDesignUsed();

            try ( InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() ) ) {

                this.locateExtractor( ee, arrayDesignUsed, ba, f );

                Date d = scanDateExtractor.extract( is );

                // sanity check. Strictly speaking, due to time zone differences it is theoretically possible for this
                // to be okay, but let's assume we're not getting data the same day it was generated!
                if ( d != null && d.after( new Date() ) ) {
                    throw new RuntimeException( "Date was in the future for: " + ba + " from " + f.getName() );
                }

                BioMaterial bm = ba.getSampleUsed();
                result.put( bm, d );

            } catch ( IOException | ParseException e ) {
                BatchInfoParser.log.warn( "Failure while parsing: " + f + ": " + e.getMessage() );
                missingDate.add( f );
            }

        }

        if ( missingDate.size() == bioAssays2Files.size() ) {
            throw new IllegalStateException( "Dates were not found for any of the files." );
        }

        if ( missingDate.size() > 0 ) {
            BatchInfoParser.log.warn( "Dates were not obtained for " + missingDate + " files: " );
            for ( File f : missingDate ) {
                BatchInfoParser.log.info( "Missing date for: " + f.getName() );
            }
        }

        return result;
    }

    private void locateExtractor( ExpressionExperiment ee, ArrayDesign arrayDesignUsed, BioAssay ba, File f ) throws UnsupportedRawdataFileFormatException {
        String providerName =
                arrayDesignUsed.getDesignProvider() == null ? "" : arrayDesignUsed.getDesignProvider().getName();
        if ( providerName.equalsIgnoreCase( "affymetrix" ) || arrayDesignUsed.getName().toLowerCase()
                .contains( "affymetrix" ) ) {
            scanDateExtractor = new AffyScanDateExtractor();
        } else if ( providerName.equalsIgnoreCase( "agilent" ) || arrayDesignUsed.getName().toLowerCase()
                .contains( "agilent" ) ) {
            scanDateExtractor = new AgilentScanDateExtractor();
        } else if ( providerName.equalsIgnoreCase( "illumina" ) || arrayDesignUsed.getName().toLowerCase()
                .contains( "illumina" ) || arrayDesignUsed.getName().toLowerCase().contains( "sentrix" ) ) {

            /*
             * Not all illumina arrays are beadarrays - e.g. GPL6799.
             */
            if ( f.getName().contains( ".gpr" ) ) {
                /*
                 * We'll give it a try.
                 */
                BatchInfoParser.log.info( "Looks like an Illumina spotted array with GPR formatted scan file: " + f );
                scanDateExtractor = new GenericScanFileDateExtractor();
            } else {
                /*
                 * We can attempt to use the slide number as the key, if the data is in the beadarray file format.s
                 */

                throw new UnsupportedRawdataFileFormatException( ee,
                        arrayDesignUsed + " not matched to a supported platform type for scan date extraction for " + ba
                                + "(Illumina files do not contain dates)" );
            }
        } else {
            BatchInfoParser.log.warn( "Unknown provider/format, attempting a generic extractor for " + f );
            scanDateExtractor = new GenericScanFileDateExtractor();
        }
    }

    /**
     * From the file names, match to the bioassays. GEO names things consistently (??) so this should work but not
     * ideal.
     *
     * @param files           files
     * @param assayAccessions accessions
     * @return map
     */
    @SuppressWarnings("StatementWithEmptyBody") // Better readability
    private Map<BioAssay, File> matchBioAssaysToRawDataFiles( Collection<LocalFile> files,
            Map<String, BioAssay> assayAccessions ) {

        Pattern regex = Pattern.compile( "(GSM[0-9]+).+" );

        Map<BioAssay, File> bioAssays2Files = new HashMap<>();
        for ( LocalFile file : files ) {
            File f = file.asFile();

            String n = f.getName();

            /*
             * We only support the newer style of storing these.
             */
            if ( !n.startsWith( "GSM" ) ) {
                continue;
            }

            if ( n.toUpperCase().contains( ".CHP" ) || n.toUpperCase().contains( ".DAT" ) || n.toUpperCase()
                    .contains( ".EXP" ) || n.toUpperCase().contains( ".RPT" ) || n.toUpperCase().contains( ".TIF" ) ) {
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
                /*
                 * Don't clobber a valid file. For affymetrix, CEL is what we want. Other cases harder to predict, but
                 * .txt files can be either good or bad. (We could do this check earlier)
                 */
                if ( bioAssays2Files.get( ba ).getName().toUpperCase().contains( ".CEL" ) ) {
                    BatchInfoParser.log.debug( "Retaining CEL file, ignoring " + f.getName() );
                    continue;
                } else if ( f.getName().toUpperCase().contains( ".CEL" ) ) {
                    // we displace the old file with this CEL file, but there is no need to warn.
                } else {
                    BatchInfoParser.log.warn( "Multiple files matching " + ba + ": " + bioAssays2Files.get( ba )
                            + "; using new file: " + f );
                }
            }
            bioAssays2Files.put( ba, f );
        }
        return bioAssays2Files;
    }
}
