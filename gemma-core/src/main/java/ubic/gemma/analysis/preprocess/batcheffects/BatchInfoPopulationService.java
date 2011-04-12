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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.loader.expression.geo.fetcher.RawDataFetcher;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.association.GOEvidenceCode;
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
     * dates, this is only used for the creation of ExperimentalFactors. Making this value too small causes the data to
     * be broken into many batches. I experimented with a value of 2, but this seemed too low. Anything greater than 24
     * doesn't make much sense.
     */
    protected static final int MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH = 8;

    /**
     * Delete unpacked raw data files when done? The zipped/tarred archived will be left alone anyway.
     */
    private static final boolean CLEAN_UP = true;

    private static Log log = LogFactory.getLog( BatchInfoPopulationService.class );

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

    @Autowired
    DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    /**
     * Attempt to obtain batch information from the data provider and populate it into the given experiment. The method
     * used may vary. For GEO, the default method is to download the raw data files, and look in them for a date. This
     * is not implemented for every possible type of raw data file.
     * 
     * @param ee
     * @return true if information was successfully obtained
     */
    public boolean fillBatchInformation( ExpressionExperiment ee ) {
        return this.fillBatchInformation( ee, false );
    }

    /**
     * Attempt to obtain batch information from the data provider and populate it into the given experiment. The method
     * used may vary. For GEO, the default method is to download the raw data files, and look in them for a date. This
     * is not implemented for every possible type of raw data file.
     * 
     * @param ee
     * @param force
     * @return true if information was successfully obtained
     */
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
     * @param batchNum
     * @param df
     * @param d
     * @return
     */
    private String formatBatchName( int batchNum, DateFormat df, Date d ) {
        String batchDateString;
        batchDateString = ExperimentalDesignUtils.BATCH_FACTOR_NAME_PREFIX
                + StringUtils.leftPad( Integer.toString( batchNum ), 2, "0" ) + "_"
                + df.format( DateUtils.truncate( d, Calendar.HOUR ) );
        return batchDateString;
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

        ExperimentalFactor factor = convertToFactor( ee, dates );

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
     * @return
     */
    private VocabCharacteristic getBatchFactorCategory() {
        VocabCharacteristic c = VocabCharacteristic.Factory.newInstance();
        c.setCategory( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME );
        c.setValue( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME );
        c.setValueUri( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_URI );
        c.setCategoryUri( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_URI );
        c.setEvidenceCode( GOEvidenceCode.IIA );
        return c;
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

        /*
         * FIXME this code is basically copied from ExperimentalFactorController and should be moved down into a
         * Model-level service.
         */
        log.info( "Removing existing batch factor: " + toRemove );

        /*
         * First, check to see if there are any diff results that use this factor.
         */
        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalysisService
                .findByFactor( toRemove );
        for ( DifferentialExpressionAnalysis a : analyses ) {
            differentialExpressionAnalysisService.delete( a );
        }

        ee = expressionExperimentService.thawLite( ee );

        for ( BioAssay ba : ee.getBioAssays() ) {
            for ( BioMaterial bm : ba.getSamplesUsed() ) {

                Collection<FactorValue> factorValuesToRemoveFromBioMaterial = new HashSet<FactorValue>();
                for ( FactorValue factorValue : bm.getFactorValues() ) {
                    if ( toRemove.equals( factorValue.getExperimentalFactor() ) ) {
                        factorValuesToRemoveFromBioMaterial.add( factorValue );
                    }
                }
                // if there are factors to remove
                if ( factorValuesToRemoveFromBioMaterial.size() > 0 ) {
                    bm.getFactorValues().removeAll( factorValuesToRemoveFromBioMaterial );
                    bioMaterialService.update( bm );
                }
            }
        }

        ed.getExperimentalFactors().remove( toRemove );
        // delete the experimental factor this cascades to values.
        experimentalFactorService.delete( toRemove );
        experimentalDesignService.update( ed );
    }

    /**
     * Apply some heuristics to condense the dates down to batches. For example, we might assume dates very close
     * together (for example, in the same day or within MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH, and we avoid singleton
     * batches) are to be treated as the same batch (see implementation for details).
     * 
     * @param allDates
     * @return
     */
    protected Map<String, Collection<Date>> convertDatesToBatches( Collection<Date> allDates ) {
        List<Date> lDates = new ArrayList<Date>( allDates );
        Collections.sort( lDates );
        Map<String, Collection<Date>> result = new LinkedHashMap<String, Collection<Date>>();

        int batchNum = 1;
        DateFormat df = DateFormat.getDateInstance( DateFormat.SHORT );

        String batchDateString = "";

        // apply sanity limit on number of batches. Completely arbitrary but guards against possible errors in getting
        // the dates in the first place.
        if ( lDates.size() > 99 ) {
            throw new IllegalStateException( "There are too many batches: " + lDates.size() );
        }

        boolean mergedAnySingletons = false;

        /*
         * Iterate over dates
         */
        Date lastDate = null;
        Date nextDate = null;
        for ( int i = 0; i < lDates.size(); i++ ) {
            Date currentDate = lDates.get( i );

            if ( i < lDates.size() - 1 ) {
                nextDate = lDates.get( i + 1 );
            } else {
                nextDate = null;
            }

            if ( lastDate == null ) {
                // Start our first batch.
                batchDateString = formatBatchName( batchNum, df, currentDate );
                result.put( batchDateString, new HashSet<Date>() );
                result.get( batchDateString ).add( currentDate );
                lastDate = currentDate;
                continue;
            }

            /*
             * Decide whether we have entered a new batch.
             * 
             * Rules:
             * 
             * - Processing on the same day is always considered the same batch.
             * 
             * - Gaps of less then MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH hours are considered the same batch even if
             * the day is different. Allows for "overnight running" of batches.
             * 
             * And then two rules that keep us from having batches with just one sample. Such batches buy us nothing at
             * all.
             * 
             * - A "singleton" batch at the end of the series is always combined with the last batch.
             * 
             * - A "singleton" batch in the middle is combined with either the next or previous batch, whichever is
             * nearer in time.
             */
            if ( gapIsLarge( lastDate, currentDate ) && result.get( batchDateString ).size() > 1 ) {

                if ( nextDate == null ) {
                    /*
                     * We're at the last sample, and it's a singleton. We fall through and allow adding it to the end of
                     * the last batch.
                     */
                    log.warn( "Singleton at the end of the series, combining with the last batch: gap is "
                            + String.format( "%.2f", ( currentDate.getTime() - lastDate.getTime() )
                                    / ( double ) ( 1000 * 60 * 60 * 24 ) ) + " hours." );
                    mergedAnySingletons = true;
                } else if ( gapIsLarge( currentDate, nextDate ) ) {
                    /*
                     * Then we have a singleton that will be stranded when we go to the next date. Do we combine it
                     * forwards or backwards? We choose the smaller gap.
                     */
                    long backwards = currentDate.getTime() - lastDate.getTime();
                    long forwards = nextDate.getTime() - currentDate.getTime();

                    if ( forwards < backwards ) {
                        // Start a new batch.
                        log.warn( "Singleton resolved by waiting for the next batch: gap is "
                                + String.format( "%.2f", ( nextDate.getTime() - currentDate.getTime() )
                                        / ( double ) ( 1000 * 60 * 60 * 24 ) ) + " hours." );
                        batchNum++;
                        batchDateString = formatBatchName( batchNum, df, currentDate );
                        result.put( batchDateString, new HashSet<Date>() );
                        mergedAnySingletons = true;
                    } else {
                        log.warn( "Singleton resolved by adding to the last batch: gap is "
                                + String.format( "%.2f", ( currentDate.getTime() - lastDate.getTime() )
                                        / ( double ) ( 1000 * 60 * 60 * 24 ) ) + " hours." );
                        // don't start a new batch, fall through.
                    }

                } else {
                    batchNum++;
                    batchDateString = formatBatchName( batchNum, df, currentDate );
                    result.put( batchDateString, new HashSet<Date>() );
                }

            }
            // else we fall through and add the current date to the current batch.

            // express the constraint that we don't allow batches of size 1, even if we would have normally left it in
            // its own batch.
            if ( result.get( batchDateString ).size() == 1 && gapIsLarge( lastDate, currentDate ) ) {
                mergedAnySingletons = true;
                log.warn( "Stranded singleton automatically being merged into a larger batch" );
            }

            result.get( batchDateString ).add( currentDate );
            lastDate = currentDate;
        }

        if ( mergedAnySingletons && result.size() == 1 ) {
            // The implication is that if we didn't have the singleton merging, we would have more than one batch.
            log.warn( "Singleton merging resulted in all batches being combined" );
        }

        return result;

    }

    /**
     * @param earlierDate
     * @param date
     * @return false if 'date' is considered to be in the same batch as 'earlierDate', true if we should treat it as a
     *         separate batch.
     */
    private boolean gapIsLarge( Date earlierDate, Date date ) {
        return !DateUtils.isSameDay( date, earlierDate )
                && DateUtils.addHours( earlierDate, MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH ).before( date );
    }

    /**
     * @param ee
     * @param dates
     * @return
     */
    protected ExperimentalFactor convertToFactor( ExpressionExperiment ee, Map<BioMaterial, Date> dates ) {

        /*
         * Go through the dates and convert to factor values.
         */
        Collection<Date> allDates = new HashSet<Date>();
        allDates.addAll( dates.values() );

        Map<String, Collection<Date>> datesToBatch = convertDatesToBatches( allDates );

        Map<Date, FactorValue> d2fv = new HashMap<Date, FactorValue>();
        ExperimentalFactor ef = null;
        if ( datesToBatch.size() < 2 ) {
            log.info( "There is only one batch" );
            // we still put the processing dates in, below.
        } else {
            ef = makeFactorForBatch( ee );
            for ( String batchId : datesToBatch.keySet() ) {
                FactorValue fv = FactorValue.Factory.newInstance();
                fv.setIsBaseline( false ); /* we could set true for the first batch, but nobody cares. */
                fv.setValue( batchId );
                Collection<Characteristic> chars = new HashSet<Characteristic>();
                VocabCharacteristic c = VocabCharacteristic.Factory.newInstance();
                c.setCategory( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME );
                c.setValue( batchId );
                c.setCategoryUri( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_URI );
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
         * Associate dates with bioassays and any new factors with the biomaterials. Note we can have missing values.
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
     * @param ee
     * @return
     */
    protected ExperimentalFactor makeFactorForBatch( ExpressionExperiment ee ) {
        ExperimentalDesign ed = ee.getExperimentalDesign();
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setType( FactorType.CATEGORICAL );
        ef.setCategory( getBatchFactorCategory() );
        ef.setExperimentalDesign( ed );
        ef.setName( ExperimentalDesignUtils.BATCH_FACTOR_NAME );
        ef.setDescription( "Scan date or similar proxy for 'sample processing batch'"
                + " extracted from the raw data files." );

        ef = persistFactor( ee, ef );
        return ef;
    }

    /**
     * @param ee
     * @param factor
     * @return
     */
    protected ExperimentalFactor persistFactor( ExpressionExperiment ee, ExperimentalFactor factor ) {
        ExperimentalDesign ed = experimentalDesignService.load( ee.getExperimentalDesign().getId() );

        if ( ed == null ) {
            throw new IllegalStateException( "No experimental design for " + ee );
        }

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
