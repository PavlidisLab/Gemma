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
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
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

    private static final String FASTQ_HEADER_EXTRATION_FAILURE_INDICATOR = "FAILURE";

    @Autowired
    private BioMaterialService bioMaterialService = null;

    @Autowired
    private ExperimentalDesignService experimentalDesignService;

    @Autowired
    private ExpressionExperimentService experimentService;

    @Override
    @Transactional
    public ExperimentalFactor createRnaSeqBatchFactor( ExpressionExperiment ee, Map<BioMaterial, String> headers ) {
        /*
         * Go through the headers and convert to factor values.
         */
        Map<String, Collection<String>> batchIdToHeaders = this
                .convertHeadersToBatches( headers.values() );

        Map<String, FactorValue> headerToFv = new HashMap<>();
        ExperimentalFactor ef = createExperimentalFactor( ee, batchIdToHeaders, headerToFv );
        bioMaterialService.associateBatchFactor( headers, headerToFv );

        return ef;
    }

    @Override
    @Transactional
    public ExperimentalFactor createBatchFactor( ExpressionExperiment ee, Map<BioMaterial, Date> dates ) {

        /*
         * Go through the dates and convert to factor values.
         */
        Map<String, Collection<Date>> datesToBatch = this.convertDatesToBatches( new HashSet<>( dates.values() ) );

        Map<Date, FactorValue> d2fv = new HashMap<>();
        ExperimentalFactor ef = createExperimentalFactor( ee, datesToBatch, d2fv );
        bioMaterialService.associateBatchFactor( dates, d2fv );

        return ef;
    }

    /**
     * Apply some heuristics to condense the dates down to batches. For example, we might assume dates very close
     * together (for example, in the same day or within MAX_GAP_BETWEEN_SAMPLES_TO_BE_SAME_BATCH, and we avoid singleton
     * batches) are to be treated as the same batch (see implementation for details).
     *
     * @param  allDates all dates
     * @return          map of batch identifiers to dates
     */
    Map<String, Collection<Date>> convertDatesToBatches( Collection<Date> allDates ) {
        List<Date> lDates = new ArrayList<>( allDates );
        Collections.sort( lDates );
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
        for ( int i = 0; i < lDates.size(); i++ ) {
            Date currentDate = lDates.get( i );

            if ( i < lDates.size() - 1 ) {
                nextDate = lDates.get( i + 1 );
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
     * Apply some heuristics to condense the fastq headers to batches. This operates only on strings to make testing
     * easier.
     *
     * @param  headers collection of fastq headers for all samples.
     * @return         map of batch names to the headers (sample-specific) for that batch.
     */
    Map<String, Collection<String>> convertHeadersToBatches( Collection<String> headers ) {
        Map<String, Collection<String>> result = new LinkedHashMap<>();

        Map<FastqHeaderData, Collection<String>> batchInfos = new HashMap<>();

        // parse headers; keep track of the platforms, which we use as a fallback if the headers are not clean
        Set<String> platforms = new HashSet<>();
        for ( String header : headers ) {
            FastqHeaderData batchInfoForSample = parseFASTQHeaderForBatch( header );

            String device = batchInfoForSample.getDevice();
            if ( device.startsWith( "GPL" ) ) {
                platforms.add( device );
            }

            if ( !batchInfos.containsKey( batchInfoForSample ) ) {
                batchInfos.put( batchInfoForSample, new HashSet<String>() );
            }
            batchInfos.get( batchInfoForSample ).add( header );
        }

        // key step ...
        batch( batchInfos, headers.size() );

        // switch to using string keys for batch identifiers (and check for bad headers)
        boolean anyBadHeaders = false;
        for ( FastqHeaderData fhd : batchInfos.keySet() ) {

            if ( !fhd.hadUsableHeader ) {
                anyBadHeaders = true;
            }
            String batchIdentifier = fhd.toString();
            if ( !result.containsKey( batchIdentifier ) ) {
                result.put( batchIdentifier, new HashSet<String>() );
            }
            Collection<String> headersInBatch = batchInfos.get( fhd );
            result.get( batchIdentifier ).addAll( headersInBatch );
        }

        //  If *any* of them are no good (and platform was constant), we fail noisily
        if ( platforms.size() == 1 && anyBadHeaders ) {
            throw new RuntimeException( "Batch could not be determined: at least one unusable header and platform is constant" );
        }

        // otherwise, having just one batch means as far we can tell there was only one batch.
        log.info( result.size() + " batches detected" );

        return result;

    }

    /*
     * 
     * RNA-seq: See how many batches we have for each level of granularity; pick the best number. This is pretty crude,
     * and involves recreating the map multiple times
     */
    private void batch( Map<FastqHeaderData, Collection<String>> batchInfos, int numSamples ) {

        int numBatches = batchInfos.size();

        /*
         * There's two problems. First, it could be there are no batches at all. Second, there could be too many
         * batches.
         */
        if ( numBatches == 1 ) {
            // no batches - this will get sorted out later, proceed
            return;
        } else if ( numBatches == numSamples || ( double ) numBatches / ( double ) numSamples < MINIMUM_SAMPLES_PER_RNASEQ_BATCH ) {
            // too few samples per batch. Try to reduce resolution and recount.
            Map<FastqHeaderData, Collection<String>> updatedBatchInfos = dropResolution( batchInfos );

            if ( updatedBatchInfos.size() == batchInfos.size() ) {
                return;
            }

            batchInfos = updatedBatchInfos;

            batch( batchInfos, numSamples ); // recurse
        } else {
            // reasonable number of samples per batch -- proceed. 
            return;
        }

    }

    /*
     * RNAseq: Update the batch info with a lower resolution. This is only effective if we have a usable header for all
     * samples.
     */
    private Map<FastqHeaderData, Collection<String>> dropResolution( Map<FastqHeaderData, Collection<String>> batchInfos ) {

        Map<FastqHeaderData, Collection<String>> result = new HashMap<>();
        for ( FastqHeaderData fhd : batchInfos.keySet() ) {

            if ( !fhd.hadUseableHeader() ) {
                // cannot drop resolution.
                result.put( fhd, batchInfos.get( fhd ) );
                continue;
            }

            FastqHeaderData updated = fhd.dropResolution();

            if ( updated.equals( fhd ) ) return batchInfos;

            if ( !result.containsKey( updated ) ) {
                result.put( updated, new HashSet<String>() );
            }
            result.get( updated ).addAll( batchInfos.get( fhd ) );
        }

        return result;

    }

    /**
     * We expect something like: @SRR5938435.1.1 D8ZGT8Q1:199:C5GKYACXX:5:1101:1224:1885 length=100
     * Only interested middle section (D8ZGT8Q1:199:C5GKYACXX:5 of the example);
     * 
     * We augment the original header with the GPL id, which is only used if the machine etc. cannot be read from the
     * rest of the header
     * 
     * Format 1: <platform id>;;;<machine_id>:<run number>:<flowcell ID>:<lane>:<tile>:<x-pos>:<y-pos> <read>:<is
     * filtered>:<control
     * number>:<index sequence>; we can use the first four fields
     * 
     * Format 2: <platform id>;;;<machine_id>:<lane>:<tile>:<x_coord>:<y_coord>; we can use machine and lane.
     * 
     * @param  header FASTQ header (can be multi-headers for cases where there is more than on FASTQ file)
     * @return        representation of the batch info, which is going to be a portion of the header string
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

            // first actual header
            String[] fields = field.split( "\\s" );
            String[] arr = fields[1].split( ":" );

            FastqHeaderData fqd = null;
            if ( field.equals( FASTQ_HEADER_EXTRATION_FAILURE_INDICATOR ) ) {
                // no actual headers available, only platform
                fqd = new FastqHeaderData( platform );
            } else if ( fields.length != 3 ) {
                // again, no usable headers, only platform
                fqd = new FastqHeaderData( platform );
            } else if ( arr.length == 7 ) {
                fqd = new FastqHeaderData( arr[0], arr[1], arr[2], arr[3] );
            } else if ( arr.length == 5 ) {
                // device and lane are the only usable fields
                fqd = new FastqHeaderData( arr[0], arr[1] );
            } else if ( !fields[1].contains( ":" ) ) {
                // not a valid header, we're expecting at least five fields delimited by :
                fqd = new FastqHeaderData( platform );
            } else {
                // something else but also not usable.
                fqd = new FastqHeaderData( platform );
            }

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

        @Override
        public String toString() {
            String s = null;

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
            return s;
        }

        /**
         * @return
         */
        public FastqHeaderData dropResolution() {
            if ( this.lane != null ) {
                return new FastqHeaderData( this.device, this.run, this.flowCell, null );
            } else if ( this.flowCell != null ) {
                return new FastqHeaderData( this.device, this.run, null, null );
            } else if ( this.run != null ) {
                return new FastqHeaderData( this.device, null, null, null );
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
         * @param device e.g. GPLXXXX
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

        private BatchInfoPopulationHelperServiceImpl getOuterType() {
            return BatchInfoPopulationHelperServiceImpl.this;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
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
            return true;
        }

    }

    /*
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
                 */
                String batchIdentifier = descriptorsToBatch.keySet().iterator().next();
                if ( batchIdentifier.startsWith( "GPL" ) ) {
                    throw new RuntimeException(
                            "No reliable batch information was available: no usable FASTQ headers and only one GPL ID associated with the experiment." );
                }

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
                Collection<Characteristic> chars = new HashSet<>();
                Characteristic c = Characteristic.Factory.newInstance();
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
                "Scan date or similar proxy for 'sample processing batch'" + " extracted from the raw data files." );

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
     * @return             false if 'date' is considered to be in the same batch as 'earlierDate', true if we should
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
