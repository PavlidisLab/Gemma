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
 * an "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.preprocess.batcheffects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.expression.geo.fetcher.RawDataFetcher;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationMissingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedBatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SingleBatchDeterminationEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignUtils;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Retrieve batch information from the data source, if possible, and populate it into experiments.
 *
 * @author paul
 */
@Service
public class BatchInfoPopulationServiceImpl implements BatchInfoPopulationService {

    /**
     * String to use to delimit FASTQ headers when there is more than one per sample.
     */
    static final String MULTIFASTQHEADER_DELIMITER = ";;;";
    /**
     * Delete unpacked raw microarray data files when done? The zipped/tarred archived will be left alone anyway.
     */
    private static final boolean CLEAN_UP = true;
    /**
     * we have files named like GSE1234.fastq-headers-table.txt; specified in our RNA-seq pipelineF
     */
    private static final String FASTQHEADERSFILE_SUFFIX = ".fastq-headers-table.txt";
    /**
     *
     */
    private static final String GEMMA_FASTQ_HEADERS_DIR_CONFIG = "gemma.fastq.headers.dir";
    private static final Log log = LogFactory.getLog( BatchInfoPopulationServiceImpl.class );

    /**
     * @param  ef ef
     * @return true if the factor seems to be a 'batch' factor.
     */
    public static boolean isBatchFactor( ExperimentalFactor ef ) {
        Characteristic c = ef.getCategory();

        if ( c == null )
            return false;

        return ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME.equals( c.getCategory() )
                || ExperimentalDesignUtils.BATCH_FACTOR_NAME.equals( ef.getName() );
    }

    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private BatchInfoPopulationHelperService batchInfoPopulationHelperService = null;
    @Autowired
    private BioAssayService bioAssayService;
    @Autowired
    private ExperimentalFactorService experimentalFactorService = null;
    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;
    @Autowired
    private ExpressionExperimentBatchInformationService expressionExperimentBatchInformationService;

    @Override
    @Transactional
    public void fillBatchInformation( ExpressionExperiment ee, boolean force ) throws BatchInfoPopulationException {

        ee = expressionExperimentService.thawLite( ee );

        boolean isRNASeq = expressionExperimentService.isRNASeq( ee );
        boolean needed = force || this.needToRun( ee, isRNASeq );

        if ( !needed ) {
            BatchInfoPopulationServiceImpl.log
                    .info( "Study already has batch information, or it is known to be unavailable; use 'force' to override" );
            return;
        }

        Collection<LocalFile> files = null;
        try {
            if ( isRNASeq ) {
                this.createBatchFactorFromFASTQHeaders( ee );
            } else {
                // microarray case
                files = this.fetchRawDataFiles( ee );
                if ( files == null || files.isEmpty() ) {
                    throw new BatchInfoMissingException( ee, "No file were found." );
                }
                this.getBatchDataFromRawFiles( ee, files );
            }
        } catch ( BatchInfoMissingException e ) {
            this.auditTrailService.addUpdateEvent( ee, BatchInformationMissingEvent.class, e.getMessage(), e );
            throw e;
        } catch ( Exception e ) {
            this.auditTrailService.addUpdateEvent( ee, FailedBatchInformationFetchingEvent.class, e.getMessage(), e );
            if ( e instanceof BatchInfoPopulationException ) {
                throw e;
            }
            throw new BatchInfoPopulationException( ee, e );
        } finally {
            if ( BatchInfoPopulationServiceImpl.CLEAN_UP && files != null ) {
                for ( LocalFile localFile : files ) {
                    EntityUtils.deleteFile( localFile.asFile() );
                }
            }
        }
    }

    /**
     * Exposed for testing
     *
     * @param  accession             GEO accession
     * @return map of GEO id to headers, including the platform ID
     */
    Map<String, String> readFastqHeaders( String accession ) throws IOException {
        Map<String, String> result = new HashMap<>();
        File headerFile = new File( Settings.getString( GEMMA_FASTQ_HEADERS_DIR_CONFIG ) + File.separator
                + accession + FASTQHEADERSFILE_SUFFIX );
        try ( BufferedReader br = new BufferedReader( new FileReader( headerFile ) ) ) {
            String line;
            while ( ( line = br.readLine() ) != null ) {

                String[] fields = StringUtils.split( line, "\t" );

                if ( fields.length < 5 ) {
                    continue;
                }

                String geoID = fields[0];
                String geoPlatformID = fields[2]; // we may use this if the headers are not usable.
                String headers = fields[4]; // this may be FAILURE (possibly more than once)

                result.put( geoID, geoPlatformID + MULTIFASTQHEADER_DELIMITER + headers );
            }

        }

        return result;
    }

    /**
     * Currently only supports GEO
     *
     * @param  ee ee
     * @return local file
     */
    private Collection<LocalFile> fetchRawDataFiles( ExpressionExperiment ee ) {
        RawDataFetcher fetcher = new RawDataFetcher();
        DatabaseEntry accession = ee.getAccession();
        if ( accession == null ) {
            BatchInfoPopulationServiceImpl.log.warn( "No accession for " + ee.getShortName() );
            return new HashSet<>();
        }
        return fetcher.fetch( accession.getAccession() );
    }

    /**
     * Look for batch information and create a Factor for batch if there is more than one batch.
     */
    private void createBatchFactorFromFASTQHeaders( ExpressionExperiment ee ) {
        // Read and store header data.
        // map of sample ID to raw headers
        Map<String, String> rawHeaders;
        try {
            rawHeaders = readFastqHeaders( ee );
        } catch ( IOException e ) {
            throw new BatchInfoMissingException( ee, "Failed to locate FASTQ header information", e );
        }

        if ( rawHeaders == null || rawHeaders.isEmpty() ) {
            throw new BatchInfoMissingException( ee, "FASTQ header file was empty." );
        }

        Map<BioMaterial, String> headers = assignRawHeadersToSamples( ee, rawHeaders );

        // Create batch factor.
        this.removeExistingBatchFactor( ee );

        ExperimentalFactor bf = batchInfoPopulationHelperService.createRnaSeqBatchFactor( ee, headers );

        if ( bf != null ) {
            if ( bf.getId() == null ) { // hack to signal a single batch
                this.auditTrailService.addUpdateEvent( ee, SingleBatchDeterminationEvent.class, "Single batch experiment",
                        "RNA-seq experiment (most likely a single lane)" );
            } else {
                this.auditTrailService.addUpdateEvent( ee, BatchInformationFetchingEvent.class, bf.getFactorValues().size()
                        + " batches." );
            }
        }

    }

    /**
     * For microarray case
     *
     * @param  files Local copies of raw data files obtained from the data provider (e.g. GEO), adds audit event.
     * @param  ee    ee
     */
    private void getBatchDataFromRawFiles( ExpressionExperiment ee, Collection<LocalFile> files ) throws BatchInfoPopulationException {
        BatchInfoParser batchInfoParser = new BatchInfoParser();
        ee = expressionExperimentService.thaw( ee );

        if ( ee.getAccession() == null ) {
            // in fact, currently it has to be from GEO.
            throw new IllegalArgumentException(
                    "The experiment does not seem to be from an external source that would have batch information available." );
        }
        Map<BioMaterial, Date> dates = batchInfoParser.getBatchInfo( ee, files );

        this.removeExistingBatchFactor( ee );

        ExperimentalFactor factor = batchInfoPopulationHelperService.createBatchFactor( ee, dates );

        // we don't make a batch factor if there is just one batch.
        int numberOfBatches = factor == null || factor.getFactorValues().isEmpty() ? 1 : factor.getFactorValues().size();

        List<Date> allDates = new ArrayList<>( dates.values() );
        Collections.sort( allDates );
        String datesString = StringUtils.join( allDates, MULTIFASTQHEADER_DELIMITER );

        BatchInfoPopulationServiceImpl.log
                .info( "Got batch information for: " + ee.getShortName() + ", with " + numberOfBatches
                        + " batches." );

        if ( numberOfBatches == 1 ) {
            this.auditTrailService.addUpdateEvent( ee, SingleBatchDeterminationEvent.class, "Single batch experiment",
                    "Dates of sample runs: " + datesString );
        } else {
            this.auditTrailService.addUpdateEvent( ee, BatchInformationFetchingEvent.class,
                    batchInfoParser.getScanDateExtractor().getClass().getSimpleName() + "; " + numberOfBatches
                            + " batches.",
                    "Dates of sample runs: " + datesString );
        }
    }

    /**
     * Assign raw FASTQ headers to individual samples.
     *
     * @param  ee          experiment
     * @return map of Biomaterial to header
     */
    private Map<BioMaterial, String> assignRawHeadersToSamples( ExpressionExperiment ee, Map<String, String> rawHeaders ) {
        Map<BioMaterial, String> headers = new HashMap<>();

        if ( rawHeaders == null || rawHeaders.isEmpty() ) return null;

        for ( BioAssay ba : ee.getBioAssays() ) {
            String gsm = ba.getAccession().getAccession();

            if ( !rawHeaders.containsKey( gsm ) ) {
                throw new IllegalStateException( "There was no header information for " + ba );
            }

            String h = rawHeaders.get( gsm );

            if ( StringUtils.isBlank( h ) ) {
                throw new IllegalStateException( "There was no header information for " + ba );
            }

            headers.put( ba.getSampleUsed(), h );

            ba.setFastqHeaders( h );

            /*
             * TODO we could use this as an opportunity to update the "original platform" if it is not populated
             */
            if ( ba.getOriginalPlatform() == null ) {

            }

            // Note: for microarray processing dates, we persist in the Biomaterialservice.associateBatchFactor.
            // The difference for RNAseq is that we want to store the entire header, which includes parts that are not needed for the batch information.
            bioAssayService.update( ba );

        }

        return headers;
    }

    /**
     */
    private File locateFASTQheadersForBatchInfo( String accession ) {
        String fhd = Settings.getString( GEMMA_FASTQ_HEADERS_DIR_CONFIG );

        if ( StringUtils.isBlank( fhd ) ) {
            throw new IllegalStateException( "You must configure the path to extracted headers directory (" + GEMMA_FASTQ_HEADERS_DIR_CONFIG + ")" );
        }

        return new File( fhd + File.separator
                + accession + FASTQHEADERSFILE_SUFFIX );
    }

    /**
     * @param  ee     ee
     * @param  rnaSeq if the data set is RNAseq
     * @return true if it needs processing and data is available
     */
    private boolean needToRun( ExpressionExperiment ee, boolean rnaSeq ) {

        if ( rnaSeq ) {
            return !expressionExperimentBatchInformationService.checkHasBatchInfo( ee );
        }

        if ( ee.getAccession() == null || StringUtils.isBlank( ee.getAccession().getAccession() ) ) {
            BatchInfoPopulationServiceImpl.log.info( ee
                    + " lacks an external accession to use for fetching, will not attempt to fetch raw data files." );
            return false;
        }

        AuditEvent e = auditEventService.getLastEvent( ee, BatchInformationFetchingEvent.class );
        if ( e == null )
            return true;

        if ( e.getEventType() instanceof FailedBatchInformationFetchingEvent )
            return true; // worth trying
        // again perhaps

        // on occasions the files appear or were missed the first time ...? GSE20842
        if ( e.getEventType() instanceof BatchInformationMissingEvent ) {
            RawDataFetcher fetcher = new RawDataFetcher();
            return fetcher.checkForFile( ee.getAccession().getAccession() );
        }

        return false; // already did it.

    }

    /**
     * Expects to find a file with the following 5 tab-delimited fields (and no header):
     *
     * <ol>
     * <li>GEO Sample ID
     * <li>SRA sample ID
     * <li>GEO platform ID
     * <li>SRA URL
     * <li>Header(s) delimited by MULTIFASTQHEADER_DELIMITER
     * </ol>
     *
     * Example
     *
     * <pre>
     * GSM2083854 SRR3212828 GPL17021 https://www.ncbi.nlm.nih.gov/sra?term=SRX1620346 @SRR3212828.1.1 1 length=101
     * </pre>
     *
     * @param  ee experiment
     * @return map of GEO id to headers, including the platform ID
     */
    private Map<String, String> readFastqHeaders( ExpressionExperiment ee ) throws IOException {
        String accession = Objects.requireNonNull( ee.getAccession(), String.format( "%s does not have an accession", ee ) )
                .getAccession();
        File headerFile = locateFASTQheadersForBatchInfo( accession );

        if ( !headerFile.canRead() ) {
            throw new IOException( "No header file for " + ee );
        }

        return readFastqHeaders( accession );

    }

    /**
     * Remove an existing batch factor, if it exists. This is really only relevant in a 'force' situation.
     *
     * @param ee ee
     */
    private void removeExistingBatchFactor( ExpressionExperiment ee ) {
        ExperimentalDesign ed = ee.getExperimentalDesign();

        ExperimentalFactor toRemove = null;

        for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {

            if ( BatchInfoPopulationServiceImpl.isBatchFactor( ef ) ) {
                toRemove = ef;
                break;
                /*
                 * FIXME handle the case where we somehow have two or more.
                 */
            }
        }

        if ( toRemove == null ) {
            return;
        }

        BatchInfoPopulationServiceImpl.log.info( "Removing existing batch factor: " + toRemove );
        experimentalFactorService.remove( toRemove );
        ee.getExperimentalDesign().getExperimentalFactors().remove( toRemove );
        this.expressionExperimentService.update( ee );
    }

}
