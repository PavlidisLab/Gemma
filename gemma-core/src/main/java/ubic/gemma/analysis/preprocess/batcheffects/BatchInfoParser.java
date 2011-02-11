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

import org.apache.commons.lang.StringUtils;

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

    /**
     * @param ee
     * @param files
     * @return
     */
    public Map<BioMaterial, Date> getBatchInfo( ExpressionExperiment ee, Collection<LocalFile> files ) {

        Map<String, BioAssay> assayAccessions = getAccessionToBioAssayMap( ee );

        Map<BioAssay, File> bioAssays2Files = matchBioAssaysToRawDataFilse( files, assayAccessions );

        /*
         * Check if we should go on
         */
        if ( bioAssays2Files.size() != assayAccessions.size() ) {
            throw new IllegalStateException( "Did not get raw file for each bioassay: got " + bioAssays2Files.size()
                    + ", expected " + assayAccessions.size() );
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

            // temporary until we support others
            if ( !arrayDesignUsed.getDesignProvider().getName().equalsIgnoreCase( "affymetrix" ) ) {
                throw new UnsupportedOperationException( "Can't get batch information for non-affymetrix array type:"
                        + arrayDesignUsed );
            }

            accession.getExternalDatabase(); // check for GEO

            if ( StringUtils.isBlank( accession.getAccession() ) ) {
                throw new IllegalStateException(
                        "Must have accession for each bioassay to get batch information from source" );
            }

            assayAccessions.put( accession.getAccession(), ba );
        }
        return assayAccessions;
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

            try {
                InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() );

                // parse
                if ( ba.getArrayDesignUsed().getDesignProvider().getName().equalsIgnoreCase( "affymetrix" ) ) {
                    AffyScanDateExtractor ex = new AffyScanDateExtractor();
                    Date d = ex.extract( is );

                    for ( BioMaterial bm : ba.getSamplesUsed() ) {
                        result.put( bm, d );
                    }

                }

                is.close();
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
        Map<BioAssay, File> bioAssays2Files = new HashMap<BioAssay, File>();
        for ( LocalFile file : files ) {
            File f = file.asFile();

            String n = f.getName();

            /*
             * FIXME support other file types!
             */
            if ( !n.toLowerCase().contains( ".cel." ) ) {
                continue;
            }

            /*
             * keep just the GSMNNNNNN part. FIXME: only works with GEO
             */
            String acc = n.replaceAll( "\\..+", "" );

            BioAssay ba = assayAccessions.get( acc );
            if ( ba == null ) {
                /*
                 * Warn? Throw exception?
                 */
                continue;
            }
            bioAssays2Files.put( ba, f );
        }
        return bioAssays2Files;
    }
}
