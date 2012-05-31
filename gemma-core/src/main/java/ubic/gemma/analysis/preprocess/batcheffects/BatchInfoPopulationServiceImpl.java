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
package ubic.gemma.analysis.preprocess.batcheffects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.fetcher.RawDataFetcher;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedBatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedBatchInformationMissingEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Retrieve batch information from the data source, if possible, and populate it into experiments.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class BatchInfoPopulationServiceImpl implements BatchInfoPopulationService {

    /**
     * Delete unpacked raw data files when done? The zipped/tarred archived will be left alone anyway.
     */
    private static final boolean CLEAN_UP = true;

    private static Log log = LogFactory.getLog( BatchInfoPopulationServiceImpl.class );

    /**
     * @param ef
     * @return true if the factor seems to be a 'batch' factor.
     */
    public static boolean isBatchFactor( ExperimentalFactor ef ) {
        Characteristic c = ef.getCategory();

        boolean isBatchFactor = false;

        boolean looksLikeBatch = ef.getName().equals( ExperimentalDesignUtils.BATCH_FACTOR_NAME );

        if ( c != null && c instanceof VocabCharacteristic ) {
            VocabCharacteristic v = ( VocabCharacteristic ) c;
            if ( v.getCategory().equals( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME ) ) {
                isBatchFactor = true;

            }
        } else if ( looksLikeBatch ) {
            isBatchFactor = true;
        }

        return isBatchFactor;
    }

    @Autowired
    private BatchInfoPopulationHelperService batchInfoPopulationHelperService = null;

    @Autowired
    private ExperimentalFactorService experimentalFactorService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private AuditEventService auditEventService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationService#fillBatchInformation(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment)
     */
    @Override
    public boolean fillBatchInformation( ExpressionExperiment ee ) {
        return this.fillBatchInformation( ee, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationService#fillBatchInformation(ubic.gemma.model.
     * expression.experiment.ExpressionExperiment, boolean)
     */
    @Override
    public boolean fillBatchInformation( ExpressionExperiment ee, boolean force ) {
        ExpressionExperiment tee = expressionExperimentService.thawLite( ee );

        boolean needed = force || needToRun( tee );

        if ( !needed ) {
            log.info( "Study already has batch information, or it is known to be unavailable; use 'force' to override" );
            return false;
        }

        Collection<LocalFile> files = null;
        try {
            files = fetchRawDataFiles( tee );

            if ( files == null || files.isEmpty() ) {
                this.auditTrailService.addUpdateEvent( tee, FailedBatchInformationMissingEvent.class,
                        "No files were found", "" );
                return false;
            }

            boolean success = getBatchDataFromRawFiles( tee, files ); // does audit as well.

            return success;

        } catch ( Exception e ) {

            log.info( e, e );

            this.auditTrailService.addUpdateEvent( tee, FailedBatchInformationFetchingEvent.class, e.getMessage(),
                    ExceptionUtils.getFullStackTrace( e ) );

            return false;
        } finally {
            if ( CLEAN_UP && files != null ) {
                for ( LocalFile localFile : files ) {
                    localFile.asFile().delete();
                }
            }
        }
    }

    /**
     * Currently only supports GEO
     * 
     * @param ee
     * @return
     */
    private Collection<LocalFile> fetchRawDataFiles( ExpressionExperiment ee ) {
        RawDataFetcher fetcher = new RawDataFetcher();
        DatabaseEntry accession = ee.getAccession();
        if ( accession == null ) {
            log.warn( "No accession for " + ee.getShortName() );
            return new HashSet<LocalFile>();
        }
        return fetcher.fetch( accession.getAccession() );
    }

    /**
     * @param ee
     * @param files Local copies of raw data files obtained from the data provider (e.g. GEO), adds audit event.
     * @return
     */
    private boolean getBatchDataFromRawFiles( ExpressionExperiment ee, Collection<LocalFile> files ) {
        BatchInfoParser batchInfoParser = new BatchInfoParser();
        Map<BioMaterial, Date> dates = batchInfoParser.getBatchInfo( ee, files );

        removeExistingBatchFactor( ee );

        ExperimentalFactor factor = batchInfoPopulationHelperService.convertToFactor( ee, dates );

        if ( !dates.isEmpty() ) {
            int numberOfBatches = factor == null || factor.getFactorValues().size() == 0 ? 0 : factor.getFactorValues()
                    .size();

            List<Date> allDates = new ArrayList<Date>();
            allDates.addAll( dates.values() );
            Collections.sort( allDates );
            String datesString = StringUtils.join( allDates, "\n" );

            log.info( "Got batch information for: " + ee.getShortName() + ", with " + numberOfBatches + " batches." );
            this.auditTrailService.addUpdateEvent( ee, BatchInformationFetchingEvent.class, batchInfoParser
                    .getScanDateExtractor().getClass().getSimpleName()
                    + "; " + numberOfBatches + " batches.", "Dates of sample runs: " + datesString );
            return true;
        }

        return false;
    }

    /**
     * @param ee
     * @return true if it needs processing
     */
    private boolean needToRun( ExpressionExperiment ee ) {

        AuditEvent e = auditEventService.getLastEvent( ee, BatchInformationFetchingEvent.class );
        if ( e == null ) return true;

        if ( FailedBatchInformationFetchingEvent.class.isAssignableFrom( e.getClass() ) ) return true; // worth trying
        // again perhaps

        // on occasions the files appear or were missed the first time ...? GSE20842
        if ( FailedBatchInformationMissingEvent.class.isAssignableFrom( e.getClass() ) ) {
            RawDataFetcher fetcher = new RawDataFetcher();
            return fetcher.checkForFile( ee.getAccession().getAccession() );
        }

        return false; // already did it.

    }

    /**
     * Remove an existing batch factor, if it exists. This is really only relevant in a 'force' situation.
     * 
     * @param ee
     */
    private void removeExistingBatchFactor( ExpressionExperiment ee ) {
        ExperimentalDesign ed = ee.getExperimentalDesign();

        ExperimentalFactor toRemove = null;

        for ( ExperimentalFactor ef : ed.getExperimentalFactors() ) {

            if ( isBatchFactor( ef ) ) {
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

        log.info( "Removing existing batch factor: " + toRemove );
        experimentalFactorService.delete( toRemove );

    }

}
