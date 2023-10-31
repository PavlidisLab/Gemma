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
package ubic.gemma.core.analysis.preprocess.batcheffects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.auditAndSecurity.eventType.SingletonBatchInvalidEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.UninformativeFASTQHeadersForBatchingEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.text.DateFormat;
import java.util.*;

/**
 * @author paul
 */
@Service
public class BatchInfoPopulationHelperServiceImpl implements BatchInfoPopulationHelperService {

    /**
     * For RNA-seq, the minimum number of samples per batch, if possible
     */
    private static final double MINIMUM_SAMPLES_PER_RNASEQ_BATCH = 2.0;

    /**
     * For microarrays (that come with scan timestamps): How many hours do we allow to pass between samples, before we
     * consider them to be a separate batch (if they are not run on the same day). This 'slack' is necessary to allow
     * for the possibility that all the hybridizations were run together, but the scanning took a while to complete. Of
     * course we are still always recording the actual dates, this is only used for the creation of ExperimentalFactors.
     * Making this value too small causes the data to be broken into many batches. I experimented with a value of 2, but
     * this seemed too low. Anything greater than 24 doesn't make much sense.
     */
    private static final int MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH = 8;

    private static final Log log = LogFactory.getLog( BatchInfoPopulationHelperServiceImpl.class );

    private static final String FASTQ_HEADER_EXTRACTION_FAILURE_INDICATOR = "FAILURE";

    @Autowired
    private BioMaterialService bioMaterialService = null;

    @Autowired
    private ExperimentalDesignService experimentalDesignService;

    @Autowired
    private ExpressionExperimentService experimentService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Override
    @Transactional
    public ExperimentalFactor createRnaSeqBatchFactor( ExpressionExperiment ee, Map<BioMaterial, String> headers ) {
        /*
         * Go through the headers and convert to factor values.
         */
        Map<String, Collection<String>> batchIdToHeaders;
        try {
            batchIdToHeaders = this
                    .convertHeadersToBatches( ee, headers.values() );
        } catch ( FASTQHeadersPresentButNotUsableException e ) {
            this.auditTrailService.addUpdateEvent( ee, UninformativeFASTQHeadersForBatchingEvent.class, "Batches unable to be determined",
                    "RNA-seq experiment, FASTQ headers and platform not informative for batches" );
            return null;
        } catch ( SingletonBatchesException e ) {
            this.auditTrailService.addUpdateEvent( ee, SingletonBatchInvalidEvent.class, "At least one singleton batch",
                    "RNA-seq experiment, FASTQ headers indicate at least one batch of just one sample" );
            return null;
        }

        // other situations
        if ( batchIdToHeaders.isEmpty() ) {
            // we were unable to find batches.
            return null; // should be handled by the exception above
        } else if ( batchIdToHeaders.size() == 1 ) {
            // this is a hack to signal the caller that we have only one batch.
            return ExperimentalFactor.Factory.newInstance();
        } else {

            Map<String, FactorValue> headerToFv = new HashMap<>();
            ExperimentalFactor ef = createExperimentalFactor( ee, batchIdToHeaders, headerToFv );
            bioMaterialService.associateBatchFactor( headers, headerToFv );
            return ef;
        }
    }

    @Override
    @Transactional
    public ExperimentalFactor createBatchFactor( ExpressionExperiment ee, Map<BioMaterial, Date> dates ) {
        TreeMap<BioMaterial, Date> sortedB2Dates = new TreeMap<>( Comparator.comparing( dates::get ) );
        sortedB2Dates.putAll( dates );

        List<Boolean> isUsable = new ArrayList<>();
        for ( BioMaterial bm : sortedB2Dates.keySet() ) {
            isUsable.add( !bm.getBioAssaysUsedIn().iterator().next().getIsOutlier() );
        }

        List<Date> sortedDates = new ArrayList<>( sortedB2Dates.values() );

        /*
         * Go through the dates and convert to factor values.
         */
        Map<String, Collection<Date>> datesToBatch = this.convertDatesToBatches( sortedDates, isUsable );


        Map<Date, FactorValue> d2fv = new HashMap<>();
        ExperimentalFactor ef = createExperimentalFactor( ee, datesToBatch, d2fv );
        bioMaterialService.associateBatchFactor( dates, d2fv );

        return ef;
    }

    /**
     * <p>Apply some heuristics to condense the dates down to batches. For example, we might assume dates very close
     * together (for example, in the same day or within MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH, and we avoid singleton
     * batches) are to be treated as the same batch (see implementation for details).
     *</p><p>
     * If a sample is an outlier, it is effectively ignored when deciding if a batch is a singleton.
     * That is, a batch of size 2 in which one is usuable is still considered a singleton.</p>
     *
     * @param allDates all dates, in ascending order (important!)
     * @param isUsable whether the corresponding date is usable (i.e. not an outlier sample). If null this is ignored.
     * @return map of batch identifiers to dates
     */
    Map<String, Collection<Date>> convertDatesToBatches( List<Date> allDates, List<Boolean> isUsable ) {
        Map<String, Collection<Date>> result = new LinkedHashMap<>();

        int batchNum = 1;
        DateFormat df = DateFormat.getDateInstance( DateFormat.SHORT );
        String batchDateString = "";

        boolean mergedAnySingletons = false;

        /*
         * Iterate over dates
         */
        Date lastDate = null;
        Date nextDate;
        for ( int i = 0; i < allDates.size(); i++ ) {
            Date currentDate = allDates.get( i );

            Boolean usable = true;
            if ( isUsable != null ) {
                usable = isUsable.get( i );
            }

            if ( i < allDates.size() - 1 ) {
                nextDate = allDates.get( i + 1 );
            } else {
                nextDate = null;
            }

            if ( lastDate == null ) {
                // Start our first batch.
                batchDateString = this.formatBatchName( batchNum, df, currentDate );
                result.put( batchDateString, new HashSet<Date>() );
                result.get( batchDateString ).add( currentDate );
                lastDate = currentDate;
                continue;
            }


            // unusable samples always get added to the current batch.
            if ( !usable ) {
                result.get( batchDateString ).add( currentDate );
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
            if ( this.gapIsLarge( lastDate, currentDate ) && result.get( batchDateString ).size() > 1 ) {

                if ( nextDate == null ) {
                    /*
                     * We're at the last sample, and it's a singleton. We fall through and allow adding it to the end of
                     * the last batch.
                     */
                    BatchInfoPopulationHelperServiceImpl.log
                            .warn( "Singleton at the end of the series, combining with the last batch: gap is " + String
                                    .format( "%.2f",
                                            ( currentDate.getTime() - lastDate.getTime() ) / ( double ) ( 1000 * 60 * 60
                                                    * 24 ) )
                                    + " hours." );
                    mergedAnySingletons = true;
                } else if ( this.gapIsLarge( currentDate, nextDate ) ) {
                    /*
                     * Then we have a singleton that will be stranded when we go to the next date. Do we combine it
                     * forwards or backwards? We choose the smaller gap.
                     */
                    long backwards = currentDate.getTime() - lastDate.getTime();
                    long forwards = nextDate.getTime() - currentDate.getTime();

                    if ( forwards < backwards ) {
                        // Start a new batch.
                        BatchInfoPopulationHelperServiceImpl.log
                                .warn( "Singleton resolved by waiting for the next batch: gap is " + String
                                        .format( "%.2f",
                                                ( nextDate.getTime() - currentDate.getTime() ) / ( double ) ( 1000 * 60
                                                        * 60 * 24 ) )
                                        + " hours." );
                        batchNum++;
                        batchDateString = this.formatBatchName( batchNum, df, currentDate );
                        result.put( batchDateString, new HashSet<Date>() );
                        mergedAnySingletons = true;
                    } else {
                        BatchInfoPopulationHelperServiceImpl.log
                                .warn( "Singleton resolved by adding to the last batch: gap is " + String
                                        .format( "%.2f",
                                                ( currentDate.getTime() - lastDate.getTime() ) / ( double ) ( 1000 * 60
                                                        * 60 * 24 ) )
                                        + " hours." );
                        // don't start a new batch, fall through.
                    }

                } else {
                    batchNum++;
                    batchDateString = this.formatBatchName( batchNum, df, currentDate );
                    result.put( batchDateString, new HashSet<Date>() );
                }

            }
            // else we fall through and add the current date to the current batch.

            // express the constraint that we don't allow batches of size 1, even if we would have normally left it in
            // its own batch.
            // This detection of a final singleton can fail if it contains one usable and one unusable sample, but it's just a logging statement.
            if ( result.get( batchDateString ).size() == 1 && this.gapIsLarge( lastDate, currentDate ) ) {
                mergedAnySingletons = true;
                BatchInfoPopulationHelperServiceImpl.log
                        .warn( "Stranded singleton automatically being merged into a larger batch" );
            }

            result.get( batchDateString ).add( currentDate );
            lastDate = currentDate;
        }

        if ( mergedAnySingletons && result.size() == 1 ) {
            // The implication is that if we didn't have the singleton merging, we would have more than one batch.
            BatchInfoPopulationHelperServiceImpl.log.warn( "Singleton merging resulted in all batches being combined" );
        }

        return result;

    }

    /**
     * RNA-seq; Apply some heuristics to condense the fastq headers to batches. This operates only on strings to make
     * testing
     * easier.
     *
     * @param ee
     * @param headers collection of fastq headers for all samples.
     * @return map of batch names to the headers (sample-specific) for that
     * batch. It will be empty if batches
     * couldn't be determined.
     */
    private Map<String, Collection<String>> convertHeadersToBatches( ExpressionExperiment ee, Collection<String> headers )
            throws FASTQHeadersPresentButNotUsableException, SingletonBatchesException {
        Map<String, Collection<String>> result = new LinkedHashMap<>();

        Map<FastqHeaderData, Collection<String>> goodHeaderSampleInfos = new HashMap<>();
        Map<FastqHeaderData, Collection<String>> badHeaderSampleInfos = new HashMap<>();

        // we keep track of the platforms, which we use as a fallback if the headers are not clean
        // we also track the "unusable" headers
        Set<String> platforms = new HashSet<>();
        Set<String> platformsForGoodHeaderSamples = new HashSet<>();
        Set<String> platformsForBadHeaderSamples = new HashSet<>();
        //   Set<String> badHeaderSampleHeaders = new HashSet<>();
        for ( String header : headers ) {
            FastqHeaderData batchInfoForSample = parseFASTQHeaderForBatch( header );

            platforms.add( batchInfoForSample.getGplId() );

            // This splitting up and batching separately shouldn't be required
            if ( batchInfoForSample.hadUsableHeader ) {
                if ( !goodHeaderSampleInfos.containsKey( batchInfoForSample ) ) {
                    goodHeaderSampleInfos.put( batchInfoForSample, new HashSet<String>() );
                }
                goodHeaderSampleInfos.get( batchInfoForSample ).add( header );
                platformsForGoodHeaderSamples.add( batchInfoForSample.getGplId() );

            } else {

                if ( !badHeaderSampleInfos.containsKey( batchInfoForSample ) ) {
                    badHeaderSampleInfos.put( batchInfoForSample, new HashSet<String>() );
                }
                badHeaderSampleInfos.get( batchInfoForSample ).add( header );
                platformsForBadHeaderSamples.add( batchInfoForSample.getGplId() );
                //  badHeaderSampleHeaders.add( header ); // to track if they are all the same or not. Not used

            }
        }

        /*
         * Case 1: all headers are usable (has device, lane etc. info). No problem
         * Case 2: no headers are usable, only one platform, and all headers are the same - no batches will be found.
         * Either flag as "no batch info" or just a single batch?
         * Case 3: no headers are usable, but there are either more than one header style and/or more than one platform.
         * We batch based on those.
         * Case 4: various mixtures
         * Case 4a: mix of case 1 and 2. We would put all the "unusable" ones in one batch, and batch the others.
         * Case 4b: mix of case 1 and 3. Again, samples with usable headers will be batched, ones without will be
         * batches as best we can
         *
         * Complication: Batches with one sample. It is going to be hard to determine what batch they belong to. This is
         * annoying if the other samples are batchable.
         * Complication: Case 3 for when there is more than one platform, and one or both platforms have a mix of usable
         * and unusable headers. Probably we would just ignore this.
         */
        Map<FastqHeaderData, Collection<String>> batchInfos = new HashMap<>();
        if ( !goodHeaderSampleInfos.isEmpty() ) {
            goodHeaderSampleInfos = batch( goodHeaderSampleInfos, headers.size() );
            batchInfos.putAll( goodHeaderSampleInfos );
        }

        if ( !badHeaderSampleInfos.isEmpty() ) {
            //   batchSamplesWithUnusableHeaders( badHeaderSampleInfos, platformsForBadHeaderSamples, badHeaderSampleHeaders, headers.size() );
            // we don't actually have to batch them separately - it will happen all by itself
            batchInfos.putAll( badHeaderSampleInfos );
        }

        // switch to using string keys for batch identifiers, this forms the final set of batches
        for ( FastqHeaderData fhd : batchInfos.keySet() ) {
            String batchIdentifier = fhd.toString();
            if ( !result.containsKey( batchIdentifier ) ) {
                result.put( batchIdentifier, new HashSet<String>() );
            }
            Collection<String> headersInBatch = batchInfos.get( fhd );
            result.get( batchIdentifier ).addAll( headersInBatch );
        }

        // DEBUG CODE
        //        log.info( "--------------------------" );
        //        for ( String b : result.keySet() ) {
        //            log.info( "Batch: " + b );
        //            for ( String batchmember : result.get( b ) ) {
        //                log.info( "   " + batchmember );
        //            }
        //        }

        /*
         * Finalize
         */

        // if we have only one batch, that's probably okay if there is just one platform and/or the headers were okay. 
        // However, if all the headers were "bad", that's a different situation
        if ( result.size() == 1 ) {
            if ( goodHeaderSampleInfos.isEmpty() ) {
                throw new FASTQHeadersPresentButNotUsableException( ee, "Samples didn't have any useable information for batching" );
                // perhaps we should just return an empty result to signal this instead of raising an exception
                //  result.clear();
            }

        } else {
            //check for singleton batches
            boolean singleton = false;
            for ( String batchid : result.keySet() ) {
                if ( result.get( batchid ).size() == 1 ) {
                    singleton = true;
                }
            }
            if ( singleton ) {
                throw new SingletonBatchesException( ee, "Could not resolve singleton batches" );
            }
        }

        log.info( result.size() + " batches detected" );

        return result;

    }

    /**
     * For tests only.
     */
    Map<String, Collection<String>> convertHeadersToBatches( Collection<String> headers ) {
        return convertHeadersToBatches( ExpressionExperiment.Factory.newInstance(), headers );
    }

    /**
     *
     *
     * RNA-seq, for the case of when we have "usable" headers with device, lane etc.: See how many batches we have for
     * each level of granularity; pick the best number. This is pretty crude,
     * and involves recreating the map multiple times
     *
     *
     * @param  batchInfos only of samples that have "good" headers
     * @param  numSamples how many samples
     * @return Map of batches (represented by the appropriate FastqHeaderData) to samples that are in the
     *                    batch.
     */
    private Map<FastqHeaderData, Collection<String>> batch( Map<FastqHeaderData, Collection<String>> batchInfos, int numSamples ) {

        int numBatches = batchInfos.size();

        /*
         * There's two problems. First, it could be there are no batches at all. Second, there could be too many
         * batches.
         */
        if ( numBatches == 1 ) {
            // no batches - this will get sorted out later, proceed
            return batchInfos;
        } else if ( numBatches == numSamples || ( double ) numBatches / numSamples < MINIMUM_SAMPLES_PER_RNASEQ_BATCH ) {

            for ( FastqHeaderData hd : batchInfos.keySet() ) {
                assert batchInfos.containsKey( hd );

                int batchSize = batchInfos.get( hd ).size();
                if ( batchSize < MINIMUM_SAMPLES_PER_RNASEQ_BATCH ) {

                    // too few samples for at least one batch. Try to reduce resolution and recount.
                    Map<FastqHeaderData, Collection<String>> updatedBatchInfos = dropResolution( batchInfos );

                    if ( updatedBatchInfos.size() == batchInfos.size() ) {
                        // we've reached the bottom

                        return updatedBatchInfos;
                    }

                    batch( updatedBatchInfos, numSamples ); // recurse
                }
            }

        }
        // reasonable number of samples per batch -- proceed. 
        return batchInfos;

    }

    /*
     * RNAseq: Update the batch info with a lower resolution. This is only effective if we have a usable header for all
     * samples.
     */
    private Map<FastqHeaderData, Collection<String>> dropResolution( Map<FastqHeaderData, Collection<String>> batchInfos ) {

        Map<FastqHeaderData, Collection<String>> result = new HashMap<>();
        for ( FastqHeaderData fhd : batchInfos.keySet() ) {

            //            if ( !fhd.hadUseableHeader() ) {
            //                // cannot drop resolution.
            //                result.put( fhd, batchInfos.get( fhd ) );
            //                continue;
            //            }

            FastqHeaderData updated = fhd.dropResolution();

            if ( updated.equals( fhd ) ) { // we can reduce resolution no more
                return batchInfos;
            }

            log.info( "Adding: " + updated );
            if ( !result.containsKey( updated ) ) {
                result.put( updated, new HashSet<String>() );
            }
            result.get( updated ).addAll( batchInfos.get( fhd ) );
        }

        return result;

    }

    /**
     * We expect something like: @SRR5938435.1.1 D8ZGT8Q1:199:C5GKYACXX:5:1101:1224:1885 length=100 but can have extra
     * fields like
     * <p>
     * {@code @SRR12623632.1.1 NB551168:228:HF7FFBGX7:1:11101:12626:1034_RX:Z:CGCTNTNN_QX:Z:36,36,36,36,2,36,2,2 length=75}
     * <p>
     * Only interested middle section (D8ZGT8Q1:199:C5GKYACXX:5 of the example);
     * <p>
     * Underscores can be used instead of ":", see https://www.ncbi.nlm.nih.gov/sra/docs/submitformats/ and
     * https://help.basespace.illumina.com/articles/descriptive/fastq-files/
     * <p>
     * We augment the original header with the GPL id, which is only used if the machine etc. cannot be read from the
     * rest of the header
     * <p>
     * Format 1: {@code <platform id>;;;<machine_id>:<run number>:<flowcell ID>:<lane>:<tile>:<x-pos>:<y-pos> <read>:<is filtered>:<control number>:<index sequence>;} we can use the first four fields
     * <p>
     * Format 2: {@code <platform id>;;;<machine_id>:<lane>:<tile>:<x_coord>:<y_coord>;} we can use machine and lane.
     *
     * @param  header FASTQ header (can be multi-headers for cases where there is more than on FASTQ file)
     * @return representation of the batch info, which is going to be a portion of the header string
     */
    FastqHeaderData parseFASTQHeaderForBatch( String header ) {

        if ( !header.contains( BatchInfoPopulationServiceImpl.MULTIFASTQHEADER_DELIMITER ) ) {
            throw new UnsupportedOperationException( "Header does not appear to be in the expected format: " + header );
        }

        String[] headers = header.split( BatchInfoPopulationServiceImpl.MULTIFASTQHEADER_DELIMITER );
        FastqHeaderData currentBatch = null;

        String platform = headers[0]; // e.g. GPL134

        for ( String field : headers ) {
            if ( StringUtils.isBlank( field ) ) continue;
            if ( field.equals( platform ) ) continue; // skip the first field.

            FastqHeaderData fqd = null;

            if ( field.equals( FASTQ_HEADER_EXTRACTION_FAILURE_INDICATOR ) ) {
                // no actual headers available, only platform
                fqd = new FastqHeaderData( platform );
                fqd.setUnusableHeader( field );
            } else {

                // first actual header
                String[] fields = field.split( "\\s" );

                String[] arr = new String[1];
                if ( fields[1].contains( ":" ) ) {
                    arr = fields[1].split( ":" );
                } else if ( fields[1].contains( "_" ) ) {
                    // fallback in case it uses underscores. Cannot use by default because
                    // in the ":" version the strings can contain "_".
                    arr = fields[1].split( "_" );

                    // this may still be invalid, as in VAB_KCl_hr0_total_RNA_b1_t11_48_981
                    // ensure the second and third fields are numbers. Might not be enough to ensure validity
                    if ( !( arr.length > 2 && arr[1].matches( "[0-9]+" ) && arr[2].matches( "[0-9]+" ) ) ) {
                        fqd = new FastqHeaderData( platform );
                        fqd.setUnusableHeader( field );
                        arr = new String[1]; // unusabe
                    }

                } else {
                    // not a valid header, we're expecting at least five fields delimited by : or "_"
                    fqd = new FastqHeaderData( platform );
                    fqd.setUnusableHeader( field );
                }

                /*
                 * Even when the header is not usable, keep it as a possible indicator of batch (along with platform)
                 * See https://github.com/PavlidisLab/Gemma/issues/97 for some discussion
                 */

                if ( fields.length != 3 ) {
                    // again, no usable headers, only platform
                    fqd = new FastqHeaderData( platform );
                    fqd.setUnusableHeader( field );
                } else if ( arr.length == 3 ) { // example: 1_40_501
                    // This is not usable as far as we know. seen for one ABI
                    fqd = new FastqHeaderData( platform );
                    fqd.setUnusableHeader( field );
                } else if ( arr.length >= 7 ) { // this is the normal format, though it should be 7 exactly we see variants
                    fqd = new FastqHeaderData( arr[0], arr[1], arr[2], arr[3] );
                } else if ( arr.length == 5 ) { // this is another normal format (legacy)
                    // device and lane are the only usable fields
                    fqd = new FastqHeaderData( arr[0], arr[1] );
                    //                } else if (arr.length == 4) { //  rare and not usable e.g. 3:1:231:803 - first value is not lane, nor is second likely
                    //                    fqd = new FastqHeaderData(null, arr[1]); 
                } else if ( arr.length == 6 ) { // HW-ST997_0144_6_1101_1138_2179 - this is not an official format? but we work with it
                    // there are a number of variants of this, with or without underscores. See https://github.com/PavlidisLab/Gemma/issues/171
                    fqd = new FastqHeaderData( arr[0], arr[1], arr[2] );
                } else {
                    // something else but also not usable.
                    fqd = new FastqHeaderData( platform );
                    fqd.setUnusableHeader( field );
                }
            }
            fqd.setGplId( platform ); // always keep track of the GPL ID in case we have a mix of usable and unusable headers

            if ( currentBatch == null ) {
                currentBatch = fqd;
            } else {
                if ( currentBatch.equals( fqd ) ) {
                    continue;
                }

                // from a different run
                currentBatch.add( fqd );

            }

        }
        return currentBatch;
    }

    class FastqHeaderData {

        private String unusableHeader = null;

        @Override
        public String toString() {
            String s = null;

            if ( this.gplId != null ) {
                s = "GPL=" + this.gplId;
            }

            if ( this.device != null ) {
                s = "Device=" + device;
            }
            if ( this.run != null ) {
                s = s + ":Run=" + run;
            }
            if ( this.flowCell != null ) {
                s = s + ":Flowcell=" + flowCell;
            }
            if ( this.lane != null ) {
                s = s + ":Lane=" + lane;
            }

            // this will probably not work as each unusable header will probably be unique to the sample
            //            if ( this.unusableHeader != null ) {
            //                s = s + ":UnusableHeader=" + unusableHeader;
            //            }
            return s;
        }

        /**
         * @param field the unusable header
         */
        public void setUnusableHeader( String field ) {
            this.unusableHeader = field;
            this.hadUsableHeader = false; // just to be sure.
        }

        /**
         *
         * @return the unusable header, or null if the header was usable
         */
        public String getUnusableHeader() {
            return unusableHeader;
        }

        /**
         */
        private FastqHeaderData dropResolution() {
            // note that 'device' is the GPL if the header wasn't usable
            if ( this.lane != null ) {
                return new FastqHeaderData( this.device, this.run, this.flowCell, null );
            } else if ( this.flowCell != null ) {
                return new FastqHeaderData( this.device, this.run, null, null );
            } else if ( this.run != null ) {
                return new FastqHeaderData( this.device, null, null, null );
            } else if ( this.unusableHeader != null ) {
                // fallback
                FastqHeaderData f = new FastqHeaderData( this.device, this.unusableHeader, null, null );
                f.hadUsableHeader = false; // we don't count having a platform as a usable header
            }
            return this; // might want to return null if we need to signal a stopping condition.
        }

        private String device = null;

        protected String getDevice() {
            return device;
        }

        // assumption here: the header we are adding has the same fields as the original.
        public void add( FastqHeaderData fqd ) {

            if ( this.equals( fqd ) ) return;

            if ( this.device != null && !this.device.equals( fqd.getDevice() ) ) {
                this.device = this.device + "/" + fqd.getDevice();
            }

            if ( this.run != null && !this.run.equals( fqd.getRun() ) ) {
                this.run = this.run + "/" + fqd.getRun();
            }

            if ( this.flowCell != null && !this.flowCell.equals( fqd.getFlowCell() ) ) {
                this.flowCell = this.flowCell + "/" + fqd.getFlowCell();
            }

            if ( this.lane != null && !this.lane.equals( fqd.getLane() ) ) {
                this.lane = this.lane + "/" + fqd.getLane();
            }

        }

        protected String getLane() {
            return lane;
        }

        protected String getFlowCell() {
            return flowCell;
        }

        protected String getRun() {
            return run;
        }

        private String lane = null;
        private String flowCell = null;
        private String run = null;
        private String gplId = null;

        protected String getGplId() {
            return gplId;
        }

        protected void setGplId( String gplId ) {
            this.gplId = gplId;
        }

        /**
         * This means the headers had no information on device, run or lane, so they're useless.
         */
        private boolean hadUsableHeader = false;

        protected boolean hadUseableHeader() {
            return hadUsableHeader;
        }

        FastqHeaderData( String device, String lane ) {
            this.device = device;
            this.lane = lane;
            this.hadUsableHeader = true;
        }

        /**
         * Only for use when we don't have a useable device:run etc.
         *
         * @param platform e.g. GPLXXXX
         */
        FastqHeaderData( String platform ) {
            this.device = platform;
            this.hadUsableHeader = false;
        }

        FastqHeaderData( String device, String run, String flowCell, String lane ) {
            this( device, lane );
            this.flowCell = flowCell;
            this.run = run;
            this.hadUsableHeader = true;
        }

        /**
         * e.g. HW-ST997_0144_6_1101_1138_2179 - first three fields.
         *
         */
        public FastqHeaderData( String device, String flowcell, String lane ) {
            this( device, lane );
            this.flowCell = flowcell;
            this.hadUsableHeader = true;
        }

        private BatchInfoPopulationHelperServiceImpl getOuterType() {
            return BatchInfoPopulationHelperServiceImpl.this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            //    result = prime * result + ( ( this.gplId == null ) ? 0 : gplId.hashCode() );
            // result = prime * result + ( ( this.unusableHeader == null ) ? 0 : unusableHeader.hashCode() );
            result = prime * result + ( ( device == null ) ? 0 : device.hashCode() );
            result = prime * result + ( ( flowCell == null ) ? 0 : flowCell.hashCode() );
            result = prime * result + ( ( lane == null ) ? 0 : lane.hashCode() );
            result = prime * result + ( ( run == null ) ? 0 : run.hashCode() );
            return result;
        }

        @Override
        public boolean equals( Object obj ) {
            if ( this == obj ) {
                return true;
            }
            if ( obj == null ) {
                return false;
            }
            if ( getClass() != obj.getClass() ) {
                return false;
            }
            FastqHeaderData other = ( FastqHeaderData ) obj;
            if ( !getOuterType().equals( other.getOuterType() ) ) {
                return false;
            }

            if ( device == null ) {
                if ( other.device != null ) {
                    return false;
                }
            } else if ( !device.equals( other.device ) ) {
                return false;
            }
            if ( flowCell == null ) {
                if ( other.flowCell != null ) {
                    return false;
                }
            } else if ( !flowCell.equals( other.flowCell ) ) {
                return false;
            }
            if ( lane == null ) {
                if ( other.lane != null ) {
                    return false;
                }
            } else if ( !lane.equals( other.lane ) ) {
                return false;
            }
            if ( run == null ) {
                if ( other.run != null ) {
                    return false;
                }
            } else if ( !run.equals( other.run ) ) {
                return false;
            }

            // redundant with device
            //            if ( gplId == null ) {
            //                if ( other.gplId != null ) {
            //                    return false;
            //                }
            //            } else if ( !gplId.equals( other.gplId ) ) {
            //                return false;
            //            }

            //            if ( unusableHeader == null ) {
            //                if ( other.unusableHeader != null ) {
            //                    return false;
            //                }
            //            } else if ( !unusableHeader.equals( other.unusableHeader ) ) {
            //                return false;
            //            }

            return true;
        }

    }

    /*
     * This also handles case where we decide not to create a batch factor - i.e. there is only one batch.
     *
     * For RNA-seq, descriptorsToBatch is a map of batchids to headers
     * for microarrays, it a map of batchids to dates
     * d2fv is populated by this call to be a map of headers or dates to factor values
     */
    private <T> ExperimentalFactor createExperimentalFactor( ExpressionExperiment ee,
            Map<String, Collection<T>> descriptorsToBatch, Map<T, FactorValue> d2fv ) {
        ExperimentalFactor ef = null;
        if ( descriptorsToBatch == null || descriptorsToBatch.size() < 2 ) {
            if ( descriptorsToBatch != null ) {

                /*
                 * Corner case. It's possible we are not sure there are actually batches or not
                 * because of the lack of information. The example would be when we don't have usable FASTQ headers, and
                 * all the GPL ids are the same.
                 *
                 */

                // I don't think this case will happen with the code revisions and we don't want to throw an exception.
                //   String batchIdentifier = descriptorsToBatch.keySet().iterator().next();
                //                if ( batchIdentifier.startsWith( "GPL" ) ) {
                //                    throw new RuntimeException(
                //                            "No reliable batch information was available: no informative "
                //                                    + "FASTQ headers and only one GPL ID associated with the experiment." );
                //                }

                // Otherwise, we trust that either the FASTQ headers or dates are a reasonable representation.
                BatchInfoPopulationHelperServiceImpl.log.info( "There is only one 'batch', no factor will be created" );

            }

        } else {
            log.info( "Persisting information on " + descriptorsToBatch.size() + " batches" );

            ef = this.makeFactorForBatch( ee );

            for ( String batchId : descriptorsToBatch.keySet() ) {
                FactorValue fv = FactorValue.Factory.newInstance();
                fv.setIsBaseline( false ); /* we could set true for the first batch, but nobody cares. */
                fv.setValue( batchId );
                Set<Statement> chars = new HashSet<>();
                Statement c = Statement.Factory.newInstance();
                c.setCategory( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME );
                c.setSubject( batchId );
                c.setCategoryUri( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_URI );
                c.setEvidenceCode( GOEvidenceCode.IIA );

                chars.add( c );
                fv.setCharacteristics( chars );
                fv.setExperimentalFactor( ef );

                /*
                 * persist
                 */
                fv.setCharacteristics( chars );
                experimentService.addFactorValue( ee, fv );

                for ( T d : descriptorsToBatch.get( batchId ) ) {
                    d2fv.put( d, fv );
                }
            }
        }
        return ef;
    }

    private ExperimentalFactor makeFactorForBatch( ExpressionExperiment ee ) {
        ExperimentalDesign ed = ee.getExperimentalDesign();
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setType( FactorType.CATEGORICAL );
        ef.setCategory( this.getBatchFactorCategory() );
        ef.setExperimentalDesign( ed );
        ef.setName( ExperimentalDesignUtils.BATCH_FACTOR_NAME );
        ef.setDescription(
                "Scan date or similar proxy for 'batch'" + " extracted from the raw data files." );

        ef = this.persistFactor( ee, ef );
        return ef;
    }

    private ExperimentalFactor persistFactor( ExpressionExperiment ee, ExperimentalFactor factor ) {
        ExperimentalDesign ed = experimentalDesignService.load( ee.getExperimentalDesign().getId() );

        if ( ed == null ) {
            throw new IllegalStateException( "No experimental design for " + ee );
        }

        return this.experimentService.addFactor( ee, factor );

    }

    private String formatBatchName( int batchNum, DateFormat df, Date d ) {
        String batchDateString;
        batchDateString = ExperimentalDesignUtils.BATCH_FACTOR_NAME_PREFIX + StringUtils
                .leftPad( Integer.toString( batchNum ), 2, "0" ) + "_"
                + df
                .format( DateUtils.truncate( d, Calendar.HOUR ) );
        return batchDateString;
    }

    /**
     * @param  earlierDate earlier date
     * @param  date        data
     * @return false if 'date' is considered to be in the same batch as 'earlierDate', true if we should
     *                     treat it as a
     *                     separate batch.
     */
    private boolean gapIsLarge( Date earlierDate, Date date ) {
        return !DateUtils.isSameDay( date, earlierDate ) && DateUtils
                .addHours( earlierDate, BatchInfoPopulationHelperServiceImpl.MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH )
                .before( date );
    }

    private Characteristic getBatchFactorCategory() {
        Characteristic c = Characteristic.Factory.newInstance();
        c.setCategory( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME );
        c.setValue( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_NAME );
        c.setValueUri( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_URI );
        c.setCategoryUri( ExperimentalDesignUtils.BATCH_FACTOR_CATEGORY_URI );
        c.setEvidenceCode( GOEvidenceCode.IIA );
        return c;
    }

}
