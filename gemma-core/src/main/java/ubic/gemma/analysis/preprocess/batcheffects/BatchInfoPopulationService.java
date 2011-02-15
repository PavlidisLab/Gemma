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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.loader.expression.geo.fetcher.RawDataFetcher;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedBatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedBatchInformationMissingEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalDesignService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;

/**
 * Retrieve batch information from the data source, if possible, and populate it into experiments.
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class BatchInfoPopulationService {

    /**
     * How many hours do we allow to pass between samples, before we consider them to be a separate batch (if they are
     * not run on the same day). This 'slack' is necessary to allow for the possibility that all the hybridizations were
     * run together, but the scanning took a while to complete. Of course we are still always recording the actual
     * dates, this is only used for the creation of ExperimentalFactors.
     */
    private static final int MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH = 2;

    /**
     * Delete unpacked raw data files when done? The zipped/tarred archived will be left alone anyway.
     */
    private static final boolean CLEAN_UP = true;

    private static Log log = LogFactory.getLog( BatchInfoPopulationService.class );

    @Autowired
    ExperimentalDesignService experimentalDesignService;

    @Autowired
    private ExperimentalFactorService experimentalFactorService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    @Autowired
    private FactorValueService factorValueService = null;

    @Autowired
    private BioMaterialService bioMaterialService = null;

    @Autowired
    BioAssayService bioAssayService;

    @Autowired
    AuditTrailService auditTrailService;

    @Autowired
    AuditEventService auditEventService;

    /**
     * Attempt to obtain batch information from the data provider and populate it into the given experiment. The method
     * used may vary. For GEO, the default method is to download the raw data files, and look in them for a date. This
     * is not implemented for every possible type of raw data file.
     * 
     * @param ee
     * @return a persistent factor representing the batch factor.
     */
    public ExperimentalFactor fillBatchInformation( ExpressionExperiment ee ) {
        ExpressionExperiment tee = expressionExperimentService.thawLite( ee );

        boolean needed = needToRun( tee );

        if ( !needed ) {
            /*
             * What to do? Return it?
             */
            log.info( "Study already has batch information, or it is known to be unavailable." );
            return null;
        }

        ExperimentalFactor factor;
        try {
            Collection<LocalFile> files = fetchRawDataFiles( tee );

            if ( files == null || files.isEmpty() ) {
                this.auditTrailService.addUpdateEvent( tee, FailedBatchInformationMissingEvent.class,
                        "No files were found", "" );
                return null;
            }

            factor = getBatchDataFromRawFiles( tee, files );

            if ( CLEAN_UP ) {
                for ( LocalFile localFile : files ) {
                    localFile.asFile().delete();
                }
            }

            this.auditTrailService.addUpdateEvent( tee, BatchInformationFetchingEvent.class, "", "" );
        } catch ( Exception e ) {
            this.auditTrailService.addUpdateEvent( tee, FailedBatchInformationFetchingEvent.class, e.getMessage(),
                    ExceptionUtils.getFullStackTrace( e ) );
            return null;
        }

        return factor;
    }

    /**
     * @param ee
     * @return true if it needs processing
     */
    private boolean needToRun( ExpressionExperiment ee ) {

        AuditEvent e = auditEventService.getLastEvent( ee, BatchInformationFetchingEvent.class );
        if ( e == null ) return true;

        if ( FailedBatchInformationMissingEvent.class.isAssignableFrom( e.getClass() ) ) return false; // don't bother
        // running again

        if ( FailedBatchInformationFetchingEvent.class.isAssignableFrom( e.getClass() ) ) return true; // worth trying
        // again perhaps

        return false; // already did it.

    }

    /**
     * @param ee
     * @param dates
     * @return
     */
    private ExperimentalFactor convertToFactor( ExpressionExperiment ee, Map<BioMaterial, Date> dates ) {
        ExperimentalFactor ef = makeFactorForBatch( ee );

        /*
         * Go through the dates and convert to factor values.
         */
        Collection<Date> allDates = new HashSet<Date>();
        allDates.addAll( dates.values() );

        Map<String, Collection<Date>> datesToBatch = convertDatesToBatches( allDates );

        Map<Date, FactorValue> d2fv = new HashMap<Date, FactorValue>();

        if ( datesToBatch.size() < 2 ) {
            log.info( "There is only one batch" );
            // we still put the processing dates in, below.
        } else {
            for ( String batchId : datesToBatch.keySet() ) {
                FactorValue fv = FactorValue.Factory.newInstance();
                fv.setIsBaseline( false );
                fv.setValue( batchId );
                Collection<Characteristic> chars = new HashSet<Characteristic>();
                VocabCharacteristic c = VocabCharacteristic.Factory.newInstance();
                c.setCategory( "ComplexAction" ); // FIXME
                c.setValue( batchId );
                c.setCategoryUri( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#ComplexAction" );
                c.setEvidenceCode( GOEvidenceCode.IIA );

                chars.add( c );
                fv.setCharacteristics( chars );
                fv.setExperimentalFactor( ef );

                /*
                 * persist
                 */
                fv.setCharacteristics( chars );
                factorValueService.create( fv );

                ef.getFactorValues().add( fv );

                experimentalFactorService.update( ef );

                for ( Date d : datesToBatch.get( batchId ) ) {
                    d2fv.put( d, fv );
                }
            }
        }

        /*
         * Associate dates with bioassays and any new factors with the biomaterials
         */
        for ( BioMaterial bm : dates.keySet() ) {
            bioMaterialService.thaw( bm );

            if ( !d2fv.isEmpty() ) bm.getFactorValues().add( d2fv.get( dates.get( bm ) ) );

            for ( BioAssay ba : bm.getBioAssaysUsedIn() ) {
                if ( ba.getProcessingDate() != null ) {
                    if ( !ba.getProcessingDate().equals( dates.get( bm ) ) ) {
                        ba.setProcessingDate( dates.get( bm ) );
                        bioAssayService.update( ba );
                    }
                } else {
                    ba.setProcessingDate( dates.get( bm ) );
                    bioAssayService.update( ba );
                }

            }
            bioMaterialService.update( bm );
        }

        return ef;
    }

    /**
     * Apply some heuristics to condense the dates down to batches. For example, we might assume dates very close
     * together (same day or within MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH) are to be treated as the same batch (see
     * implementation for details).
     * 
     * @param allDates
     * @return
     */
    protected Map<String, Collection<Date>> convertDatesToBatches( Collection<Date> allDates ) {
        List<Date> lDates = new ArrayList<Date>( allDates );
        Collections.sort( lDates );
        Map<String, Collection<Date>> result = new HashMap<String, Collection<Date>>();

        int batchNum = 1;
        DateFormat df = DateFormat.getDateInstance( DateFormat.SHORT );

        String batchDateString = "";

        Date lastDate = null;
        for ( Date d : lDates ) {

            if ( lastDate == null ) {
                batchDateString = "Batch_" + batchNum + "_" + df.format( DateUtils.truncate( d, Calendar.HOUR ) );
                result.put( batchDateString, new HashSet<Date>() );

                lastDate = d;

            } else {

                /*
                 * Decide whether we have entered a new batch.
                 * 
                 * Rules:
                 * 
                 * - Processing on the same day is always considered the same batch.
                 * 
                 * - Gaps of less then MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH hours are considered the same batch even
                 * if the day is different. Allows for "overnight running" of batches.
                 */
                if ( !DateUtils.isSameDay( d, lastDate )
                        && DateUtils.addHours( lastDate, MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH ).before( d ) ) {
                    batchNum++;
                    batchDateString = "Batch_" + batchNum + "_" + df.format( DateUtils.truncate( d, Calendar.HOUR ) );
                    result.put( batchDateString, new HashSet<Date>() );
                }
            }
            result.get( batchDateString ).add( d );
            lastDate = d;
        }

        return result;

    }

    private ExperimentalFactor makeFactorForBatch( ExpressionExperiment ee ) {
        ExperimentalDesign ed = ee.getExperimentalDesign();
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setType( FactorType.CATEGORICAL );
        ef.setCategory( getBatchFactorCategory() );
        ef.setExperimentalDesign( ed );
        ef.setName( "batch" );
        ef
                .setDescription( "Scan date or similar proxy for 'sample processing batch' extracted from the raw data files." );

        ef = persistFactor( ee, ef );
        return ef;
    }

    private VocabCharacteristic getBatchFactorCategory() {
        VocabCharacteristic c = VocabCharacteristic.Factory.newInstance();
        c.setCategory( "ComplexAction" );
        c.setValue( "ComplexAction" );
        c.setValueUri( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#ComplexAction" );
        c.setCategoryUri( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#ComplexAction" );
        c.setEvidenceCode( GOEvidenceCode.IIA );
        return c;
    }

    /**
     * Currently only supports GEO
     * 
     * @param ee
     * @return
     */
    private Collection<LocalFile> fetchRawDataFiles( ExpressionExperiment ee ) {
        RawDataFetcher fetcher = new RawDataFetcher();
        return fetcher.fetch( ee.getAccession().getAccession() );
    }

    /**
     * @param ee
     * @param files Local copies of raw data files obtained from the data provider (e.g. GEO)
     * @return
     */
    private ExperimentalFactor getBatchDataFromRawFiles( ExpressionExperiment ee, Collection<LocalFile> files ) {
        BatchInfoParser batchInfoParser = new BatchInfoParser();
        Map<BioMaterial, Date> dates = batchInfoParser.getBatchInfo( ee, files );
        ExperimentalFactor factor = convertToFactor( ee, dates );
        return factor;
    }

    /**
     * @param ee
     * @param factor
     * @return
     */
    private ExperimentalFactor persistFactor( ExpressionExperiment ee, ExperimentalFactor factor ) {
        ExperimentalDesign ed = experimentalDesignService.load( ee.getId() );

        /*
         * Note: this call should not be needed because of cascade behaviour.
         */
        // experimentalFactorService.create( ef );

        if ( ed.getExperimentalFactors() == null ) ed.setExperimentalFactors( new HashSet<ExperimentalFactor>() );
        ed.getExperimentalFactors().add( factor );

        experimentalDesignService.update( ed );

        return factor;

    }

}
