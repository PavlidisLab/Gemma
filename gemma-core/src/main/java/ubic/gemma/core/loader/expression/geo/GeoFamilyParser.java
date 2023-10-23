/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.loader.expression.geo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.loader.expression.geo.model.*;
import ubic.gemma.core.loader.util.parser.Parser;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for parsing GSE and GDS files from NCBI GEO. See
 * <a href="http://www.ncbi.nlm.nih.gov/projects/geo/info/soft2.html">ncbi geo</a> for format information.
 *
 * @author keshav
 * @author pavlidis
 */
public class GeoFamilyParser implements Parser<Object> {
    private static final char FIELD_DELIM = '\t';

    private static final int MAX_WARNINGS = 100;

    private static final Log log = LogFactory.getLog( GeoFamilyParser.class.getName() );
    /**
     * For each platform, the map of column names to column numbers in the data.
     */
    private final Map<GeoPlatform, Map<String, Integer>> quantitationTypeKey = new HashMap<>();
    /**
     * This is used to put the data in the right place later. We know the actual column is where it is NOW, for this
     * sample, but in our data structure we put it where we EXPECT it to be (where it was the first time we saw it).
     * This is our attempt to fix problems with columns moving around from sample to sample.
     */
    private final Map<GeoPlatform, Map<Integer, Integer>> quantitationTypeTargetColumn = new HashMap<>();
    private final GeoParseResult results = new GeoParseResult();
    /*
     * Elements seen for the 'current sample'.
     */
    private final Collection<String> processedDesignElements = new HashSet<>();
    private final Collection<Integer> wantedQuantitationTypes = new HashSet<>();
    private boolean alreadyWarnedAboutClobbering = false;
    private boolean alreadyWarnedAboutInconsistentColumnOrder = false;
    private boolean alreadyWarnedAboutDuplicateColumnName = false;
    private Integer previousNumTokens = null;
    private String currentDatasetAccession;
    private String currentPlatformAccession;
    private String currentSampleAccession;
    private String currentSeriesAccession;
    private String currentSubsetAccession;
    private boolean haveReadPlatformHeader = false;
    private boolean haveReadSampleDataHeader = false;
    private boolean inDatabase = false;
    private boolean inDataset = false;
    private boolean inDatasetTable = false;
    private boolean inPlatform = false;
    private boolean inPlatformTable = false;
    private boolean inSample = false;
    private boolean inSampleTable = false;
    private boolean inSeries = false;
    private boolean inSeriesTable = false;
    private boolean inSubset = false;
    private int parsedLines;
    private int platformLines = 0;
    private int sampleDataLines = 0;
    private boolean processPlatformsOnly;
    private int numWarnings = 0;

    @Override
    public Collection<Object> getResults() {
        Collection<Object> r = new HashSet<>();
        r.add( this.results );
        return r;
    }

    @Override
    public void parse( File f ) throws IOException {
        try (InputStream a = new FileInputStream( f )) {
            this.parse( a );
        }
    }

    @Override
    public void parse( InputStream is ) throws IOException {
        if ( is == null ) {
            throw new IOException( "Inputstream was null" );
        }

        if ( is.available() == 0 ) {
            throw new IOException( "No bytes to read from the input stream." );
        }

        try (final BufferedReader dis = new BufferedReader( new InputStreamReader( is ) )) {

            GeoFamilyParser.log.debug( "Parsing...." );

            final ExecutorService executor = Executors.newSingleThreadExecutor();

            FutureTask<Exception> future = new FutureTask<>( new Callable<Exception>() {
                @Override
                public Exception call() {
                    try {
                        GeoFamilyParser.this.doParse( dis );
                        dis.close();
                        return null;
                    } catch ( Exception e ) {
                        GeoFamilyParser.log.error( e, e );
                        return e;
                    }
                }
            } );

            executor.execute( future );
            executor.shutdown();

            while ( !future.isDone() && !future.isCancelled() ) {
                try {
                    TimeUnit.SECONDS.sleep( 5L );
                } catch ( InterruptedException e ) {
                    // probably cancelled.
                    dis.close();
                    return;
                }
                GeoFamilyParser.log.info( parsedLines + " lines parsed." );
            }

            try {
                Exception e = future.get();
                if ( e != null ) {
                    GeoFamilyParser.log.error( e.getMessage() );
                    throw new RuntimeException( e.getCause() );
                }
            } catch ( ExecutionException e ) {
                throw new RuntimeException( "Parse failed", e.getCause() );
            } catch ( java.util.concurrent.CancellationException e ) {
                throw new RuntimeException( "Parse was cancelled", e.getCause() );
            } catch ( InterruptedException e ) {
                throw new RuntimeException( "Parse was interrupted", e.getCause() );
            }

            executor.shutdownNow();

            assert future.isDone();
            // assert executor.isTerminated();

            GeoFamilyParser.log.info( "Done parsing." );
        }
    }

    @Override
    public void parse( String fileName ) throws IOException {
        try (InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( fileName )) {
            this.parse( is );
        }
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void sampleTypeSet( String accession, String string ) {
        GeoSample sample = results.getSampleMap().get( accession );
        if ( string.equalsIgnoreCase( "cDNA" ) ) {
            sample.setType( "RNA" );
        } else if ( string.equalsIgnoreCase( "RNA" ) || string.equalsIgnoreCase( "transcriptomic" ) ) {
            sample.setType( "RNA" );
        } else if ( string.equalsIgnoreCase( "genomic" ) ) {
            sample.setType( "genomic" );
        } else if ( string.equalsIgnoreCase( "protein" ) ) {
            sample.setType( "protein" );
        } else if ( string.equalsIgnoreCase( "mixed" ) ) {
            sample.setType( "mixed" );
        } else if ( string.equalsIgnoreCase( "SAGE" ) ) {
            sample.setType( "SAGE" );
        } else if ( string.equalsIgnoreCase( "MPSS" ) || string.equalsIgnoreCase( "SRA" ) ) {
            sample.setType( "MPSS" );
        } else if ( string.equalsIgnoreCase( "SARST" ) ) {
            sample.setType( "protein" );
        } else if ( string.equalsIgnoreCase( "other" ) ) {
            sample.setType( "other" );
        } else {
            throw new IllegalArgumentException( "Unknown sample type " + string );
        }
    }

    public void setProcessPlatformsOnly( boolean b ) {
        this.processPlatformsOnly = b;
    }

    /**
     * Check to make sure data has been added for all the design elements, and all quantitation types. This is necessary
     * where the data for some design elements is omitted. This can happen if there is some variability between the
     * samples in terms of what design elements they have. Important: This has to be called IMMEDIATELY after the data
     * for the sample is read in, so the values get added in the right place.
     *
     * @param currentSample current sample
     */
    private void addMissingData( GeoSample currentSample ) {

        /*
         * Skip if we're not going to use the data.
         */
        if ( !currentSample.hasUsableData() ) {
            GeoFamilyParser.log.info( "Sample is not expected to have any data" );
            return;
        }

        if ( currentSample.isMightNotHaveDataInFile() ) {
            GeoFamilyParser.log.info( "Sample might not have any data: " + currentSample
                    + ", skipping missing data check fillin" );
            return;
        }

        if ( currentSample.getPlatforms().size() > 1 ) {
            GeoFamilyParser.log.warn( "Multi-platform sample: " + currentSample );
        }

        GeoPlatform samplePlatform = currentSample.getPlatforms().iterator().next();
        assert samplePlatform != null;
        Collection<String> designElementNames = samplePlatform.getColumnData( samplePlatform.getIdColumnName() );
        if ( designElementNames == null )
            throw new IllegalStateException( samplePlatform + " did not have recognizable id column" );

        if ( GeoFamilyParser.log.isDebugEnabled() )
            GeoFamilyParser.log
                    .debug( "Checking " + currentSample + " for missing design elements on " + samplePlatform );

        GeoValues values = results.getSeriesMap().get( currentSeriesAccession ).getValues();

        Collection<Integer> qTypeIndexes = values.getQuantitationTypes( samplePlatform );

        int countMissing = 0;
        String lastMissingValue = null;
        for ( String el : designElementNames ) {
            if ( !processedDesignElements.contains( el ) ) {
                countMissing++;
                lastMissingValue = el;
                for ( Integer i : qTypeIndexes ) {
                    values.addValue( currentSample, i, el, " " );
                }
                if ( GeoFamilyParser.log.isDebugEnabled() )
                    GeoFamilyParser.log
                            .debug( "Added data missing from sample=" + currentSample + " for probe=" + el + " on "
                                    + samplePlatform );
            }
        }
        if ( countMissing > 0 ) {
            GeoFamilyParser.log
                    .warn( "Added data missing for " + countMissing + " probes for sample=" + currentSample + "  on "
                            + samplePlatform + "; last probe with missing data was " + lastMissingValue );
        }

    }

    /**
     * Add a new sample to the results.
     *
     * @param sampleAccession sample accession
     */
    private void addNewSample( String sampleAccession ) {
        if ( GeoFamilyParser.log.isDebugEnabled() )
            GeoFamilyParser.log.debug( "Adding new sample " + sampleAccession );
        GeoSample newSample = new GeoSample();
        newSample.setGeoAccession( sampleAccession );
        results.getSampleMap().put( sampleAccession, newSample );
    }

    private void addSeriesSample( String value ) {
        if ( !results.getSampleMap().containsKey( value ) ) {
            GeoFamilyParser.log.debug( "New sample (for series): " + value );
            this.addNewSample( value );
        }
        GeoFamilyParser.log.debug( "Adding sample: " + value + " to series " + currentSeriesAccession );
        results.getSeriesMap().get( currentSeriesAccession ).addSample( results.getSampleMap().get( value ) );
    }

    private void checkDataCompleteness() {
        if ( currentSampleAccession != null ) {
            GeoSample currentSample = this.results.getSampleMap().get( currentSampleAccession );
            assert currentSample != null;
            if ( currentSample.getPlatforms().size() > 1 ) {
                GeoFamilyParser.log
                        .warn( "Can't check for data completeness when sample uses more than one platform." );
            } else {
                this.addMissingData( currentSample );
            }
            this.validate();
        }
    }

    private void checkForAndFixMissingColumnNames() {
        Map<String, GeoSample> sampleMap = this.results.getSampleMap();
        List<String> representativeColumnNames = null;
        GeoSample representativeSample = null;
        for ( GeoSample sam : sampleMap.values() ) {
            if ( !sam.getColumnNames().isEmpty() ) {
                representativeColumnNames = sam.getColumnNames();
                representativeSample = sam;
                break;
            }
        }

        //noinspection ConstantConditions // Better readability
        if ( representativeColumnNames == null || representativeSample == null ) {
            return;
        }

        for ( GeoSample sam : sampleMap.values() ) {
            if ( sam.getColumnNames().isEmpty() ) {
                int i = 0;
                for ( String colName : representativeColumnNames ) {
                    sam.addColumnName( colName );
                    sam.getColumnDescriptions().add( representativeSample.getColumnDescriptions().get( i ) );
                    i++;
                }

            }
        }
    }

    private <T> void contactSet( GeoContact contact, BiConsumer<GeoContact, T> property, T value ) {
        property.accept( contact, value );
    }

    private <T> void contactSet( GeoData data, BiConsumer<GeoContact, T> property, T value ) {
        if ( data.getContact() == null ) {
            throw new IllegalArgumentException( "Missing contact for data " + data.getGeoAccession() );
        }
        this.contactSet( data.getContact(), property, value );
    }

    private GeoDataset currentDataset() {
        return this.results.getDatasetMap().get( currentDatasetAccession );
    }

    private GeoPlatform currentPlatform() {
        return this.results.getPlatformMap().get( currentPlatformAccession );
    }

    private GeoSample currentSample() {
        return this.results.getSampleMap().get( currentSampleAccession );
    }

    private GeoSeries currentSeries() {
        return this.results.getSeriesMap().get( currentSeriesAccession );
    }

    private <T> void datasetSet( String accession, java.util.function.BiConsumer<GeoDataset, T> property, T value ) {
        GeoDataset dataset = results.getDatasetMap().get( accession );
        if ( dataset == null )
            throw new IllegalArgumentException( "Unknown dataset " + accession );
        property.accept( dataset, value );
    }

    private void doParse( BufferedReader dis ) {
        if ( dis == null ) {
            throw new RuntimeException( "Null reader" );
        }
        this.numWarnings = 0;
        haveReadPlatformHeader = false;
        haveReadSampleDataHeader = false;
        alreadyWarnedAboutClobbering = false;
        alreadyWarnedAboutInconsistentColumnOrder = false;
        alreadyWarnedAboutDuplicateColumnName = false;
        String line;
        parsedLines = 0;
        processedDesignElements.clear();

        StopWatch timer = new StopWatch();
        timer.start();
        try {

            while ( ( line = dis.readLine() ) != null ) {
                if ( StringUtils.isBlank( line ) ) {
                    continue;
                }

                this.parseLine( line );
                if ( ++parsedLines % 20000 == 0 && Thread.currentThread().isInterrupted() ) {
                    dis.close(); // clean up
                    throw new java.util.concurrent.CancellationException(
                            "Thread was terminated during parsing. " + this.getClass() );
                }
            }

            this.tidyUp();

        } catch ( Exception e ) {
            GeoFamilyParser.log.error( "Parsing failed (Cancelled?) :" + e.getMessage() );
            /*
             * This happens if there was a cancellation.
             */
            throw new RuntimeException( e );
        }

        timer.stop();
        if ( timer.getTime() > 10000 ) { // 10 s
            GeoFamilyParser.log.info( "Parsed total of " + parsedLines + " lines in " + String
                    .format( "%.2gs", timer.getTime() / 1000.0 ) );
        }
        GeoFamilyParser.log.debug( this.platformLines + " platform  lines" );
        int seriesDataLines = 0;
        GeoFamilyParser.log.debug( seriesDataLines + " series data lines" );
        int dataSetDataLines = 0;
        GeoFamilyParser.log.debug( dataSetDataLines + " data set data lines" );
        GeoFamilyParser.log.debug( this.sampleDataLines + " sample data lines" );
    }

    private int extractChannelNumber( String line ) {
        int chIndex = line.lastIndexOf( "_ch" );
        if ( chIndex < 0 )
            return 1; // that's okay, there is only one channel.
        String candidateInt = line.substring( chIndex + 3, chIndex + 4 );
        try {
            return Integer.parseInt( candidateInt );
        } catch ( NumberFormatException e ) {
            return 1;
        }
    }

    /**
     * Turns a line in the format #key = value into a column name and description. This is used to handle lines such as
     * (in a platform section of a GSE file):
     * 
     * <pre>
     * #SEQ_LEN = Sequence length
     * </pre>
     *
     * @param line line
     * @param dataToAddTo GeoData object, must not be null.
     */
    private void extractColumnIdentifier( String line, GeoData dataToAddTo ) {
        if ( dataToAddTo == null )
            throw new IllegalArgumentException( "Data cannot be null" );

        Map<String, String> res = this.extractKeyValue( line );

        if ( res == null )
            return;

        String columnName = res.keySet().iterator().next();
        dataToAddTo.addColumnName( columnName );
        dataToAddTo.getColumnDescriptions().add( res.get( columnName ) );
        if ( GeoFamilyParser.log.isDebugEnabled() )
            GeoFamilyParser.log.debug( "Adding " + columnName + " to column names for " + dataToAddTo );
    }

    /**
     * Extract a key and value pair from a line in the format #key = value.
     *
     * @param line line
     * @return Map containing the String key and String value. Return null if it is misformatted.
     */
    private Map<String, String> extractKeyValue( String line ) {
        if ( !line.startsWith( "#" ) )
            throw new IllegalArgumentException( "Wrong type of line" );
        Map<String, String> result = new HashMap<>();
        String fixed = line.substring( line.indexOf( '#' ) + 1 );

        String[] tokens = fixed.split( "=", 2 );
        if ( tokens.length != 2 ) {
            GeoFamilyParser.log.warn( "Invalid key-value line, expected an '=' somewhere, got: '" + line + "'" );
            return null;
        }
        String key = tokens[0];
        String value = tokens[1];
        key = StringUtils.strip( key );
        value = StringUtils.strip( value );
        result.put( key, value );
        return result;
    }

    /**
     * Extract a value from a line in the format xxxx=value.
     *
     * @param line line
     * @return String following the first occurrence of '=', or null if there is no '=' in the String.
     */
    private String extractValue( String line ) {
        int eqIndex = line.indexOf( '=' );
        if ( eqIndex < 0 ) {
            return null; // that's okay, there are lines that just indicate the end of sections.
        }

        return StringUtils.strip( line.substring( eqIndex + 1 ) );
    }

    /**
     * Parse a line to extract an integer <em>n</em> from the a variable description line like "!Series_variable_[n] =
     * age"
     *
     * @param line line
     * @return int
     * @throws IllegalArgumentException if the line doesn't fit the format.
     */
    private int extractVariableNumber( String line ) {
        Pattern p = Pattern.compile( "_(\\d+)$" );
        Matcher m = p.matcher( line );
        if ( m.matches() ) {
            try {
                return Integer.parseInt( line.substring( m.start( 1 ) ) );
            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException( "Wrong kind of string: " + line );
            }
        }
        throw new IllegalArgumentException( "Wrong kind of string: " + line );
    }

    /**
     * This is run once for each sample, to try to figure out where the data are for each quantitation type (because it
     * can vary from sample to sample). Each platform in the series gets its own reference.
     * Note that the first column is the "ID_REF"; the first 'real' quantitation type gets column number 0. This
     * initialization is run for each sample.
     */
    private void initializeQuantitationTypes() {
        wantedQuantitationTypes.clear();
        quantitationTypeTargetColumn.clear();
        GeoSeries geoSeries = results.getSeriesMap().get( currentSeriesAccession );
        if ( geoSeries == null ) {
            throw new IllegalStateException( "No series is being parsed" );
        }

        GeoValues values = geoSeries.getValues();
        Map<GeoPlatform, Integer> currentIndex = new HashMap<>();
        Collection<String> seenColumnNames = new HashSet<>();

        /*
         * In some data sets, the quantitation types are not in the same columns in different samples. ARRRGH!
         */
        Collection<GeoPlatform> platforms = this.currentSample().getPlatforms();
        if ( platforms.size() > 1 ) {
            GeoFamilyParser.log.warn( "Multiple platforms for " + this.currentSample() );
        }

        GeoPlatform platformForSample = platforms.iterator().next();
        GeoFamilyParser.log.debug( "Initializing quantitation types for " + this.currentSample() + ", Platform="
                + platformForSample );

        if ( this.currentSample().getColumnNames().isEmpty() ) {
            /*
             * We need to fill in dummy values.
             */
            geoSeries.getValues().addSample( this.currentSample() );
        }

        for ( String columnName : this.currentSample().getColumnNames() ) {
            boolean isWanted = values.isWantedQuantitationType( columnName );

            if ( !isWanted )
                GeoFamilyParser.log.debug( columnName + " will not be included in final data" );

            if ( !currentIndex.containsKey( platformForSample ) ) {
                currentIndex.put( platformForSample, 0 );
            }

            int actualColumnNumber = currentIndex.get( platformForSample ) - 1;

            /*
             * In some datasets (e.g. GSE432) the column names are not distinct. ARRRGH. We try to salvage the situation
             * by adding a suffix to the name.
             */
            if ( seenColumnNames.contains( columnName ) ) {

                if ( !alreadyWarnedAboutDuplicateColumnName ) {
                    GeoFamilyParser.log.warn( "\n---------- WARNING ------------\n" + columnName
                            + " appears more than once for sample " + this.currentSample()
                            + ", it will be mangled to make it unique.\nThis usually indicates a problem with the GEO file format! (future similar warnings for this data set suppressed)\n" );
                    alreadyWarnedAboutDuplicateColumnName = true;
                }
                /*
                 * This method of mangling the name means that the repeated name had better show up in the same column
                 * each time. If it doesn't, then things are REALLY confused.
                 */
                columnName = columnName + "___" + actualColumnNumber;
            }

            this.initMaps( platformForSample );

            /*
             * Stores the column index for the column name.
             */
            Map<String, Integer> qtMapForPlatform = quantitationTypeKey.get( platformForSample );

            /*
             * Once we've seen a column, we check to see if it is in the same place as before.
             */
            Integer desiredColumnNumber = actualColumnNumber;
            if ( qtMapForPlatform.containsKey( columnName ) ) {
                desiredColumnNumber = qtMapForPlatform.get( columnName );
                if ( desiredColumnNumber != actualColumnNumber ) {
                    if ( !alreadyWarnedAboutInconsistentColumnOrder ) {
                        GeoFamilyParser.log
                                .warn( "\n---------- POSSIBLE GEO FILE FORMAT PROBLEM WARNING! ------------\n"
                                        + columnName + " is not in previous column " + desiredColumnNumber
                                        + ":\nFor sample " + this.currentSample() + ", it is in column "
                                        + actualColumnNumber
                                        + ". This usually isn't a problem but it's worth checking to make sure data isn't misaligned"
                                        + " (future warnings for this data set suppressed)\n" );
                        alreadyWarnedAboutInconsistentColumnOrder = true;
                    }
                    /*
                     * This is used to put the data in the right place later. We know the actual column is where it is
                     * NOW, for this sample, but in our data structure we put it where we EXPECT it to be (where it was
                     * the first time we saw it). This is our attempt to fix problems with columns moving around.
                     */
                    quantitationTypeTargetColumn.get( platformForSample )
                            .put( actualColumnNumber, desiredColumnNumber );
                }
                values.addQuantitationType( platformForSample, columnName, desiredColumnNumber );
            } else {
                /*
                 * First time we see this column name (for the platform for the current sample). Normally we assume it
                 * just goes at the column index we're at. However, make sure that there isn't another column name in
                 * this sample that should be at the same index. We have to 'look ahead'. This isn't the usual case, but
                 * it isn't rare either. (Example: GSE3500)
                 */
                boolean clobbers = this
                        .willClobberOtherQuantitationType( columnName, actualColumnNumber, qtMapForPlatform );

                if ( clobbers ) {
                    // we need to put it at the end - at the highest index we know about.
                    Collection<Integer> allIndexes = qtMapForPlatform.values();
                    int max = -1;
                    for ( Integer v : allIndexes ) {
                        if ( v > max ) {
                            max = v;
                        }
                    }
                    desiredColumnNumber = max + 1;
                    quantitationTypeTargetColumn.get( platformForSample )
                            .put( actualColumnNumber, desiredColumnNumber );
                    if ( !alreadyWarnedAboutClobbering ) {
                        GeoFamilyParser.log
                                .warn( "\n---------- POSSIBLE GEO FILE FORMAT PROBLEM WARNING! ------------\n"
                                        + "Current column name " + columnName + " reassigned to index "
                                        + desiredColumnNumber
                                        + " to avoid clobbering. This usually isn't a problem but it's worth checking to make sure data isn't misaligned "
                                        + "(future similar warnings for this data set suppressed)\n" );
                        alreadyWarnedAboutClobbering = true;
                    }
                }
                GeoFamilyParser.log.debug( columnName + " ---> " + desiredColumnNumber );
                qtMapForPlatform.put( columnName, desiredColumnNumber );
                values.addQuantitationType( platformForSample, columnName, desiredColumnNumber );
            }

            /*
             * Some quantitation types are skipped to save space.
             */
            if ( !isWanted ) {
                if ( GeoFamilyParser.log.isDebugEnabled() )
                    GeoFamilyParser.log
                            .debug( "Data column " + columnName + " will be skipped for " + this.currentSample()
                                    + " - it is an 'unwanted' quantitation type (column number " + currentIndex
                                            .get( platformForSample )
                                    + ", " + desiredColumnNumber
                                    + "the quantitation type.)" );
            } else {
                wantedQuantitationTypes.add( desiredColumnNumber );
            }

            seenColumnNames.add( columnName );

            // update the current index, note that it is platform-specific.
            currentIndex.put( platformForSample, currentIndex.get( platformForSample ) + 1 );
        } // end iteration over column names.

    }

    private void initMaps( GeoPlatform platformForSample ) {
        if ( !quantitationTypeKey.containsKey( platformForSample ) ) {
            quantitationTypeKey.put( platformForSample, new HashMap<String, Integer>() );

        }
        if ( !quantitationTypeTargetColumn.containsKey( platformForSample ) ) {
            quantitationTypeTargetColumn.put( platformForSample, new HashMap<Integer, Integer>() );
        }
    }

    private boolean isWantedQuantitationType( int index ) {
        return wantedQuantitationTypes.contains( index );
    }

    private void lastUpdateDateSet( Object object, String value ) {

        if ( object instanceof GeoPlatform )
            ( ( GeoPlatform ) object ).setLastUpdateDate( value );

        else if ( object instanceof GeoSeries )
            ( ( GeoSeries ) object ).setLastUpdateDate( value );

        else if ( object instanceof GeoSample )
            ( ( GeoSample ) object ).setLastUpdateDate( value );
    }

    /**
     * Parse the column identifier strings from a GDS or GSE file.
     * In GSE files, in a 'platform' section, these become column descriptions for the platform descriptors.
     * For samples in GSE files, they become values for the data in the sample. For example
     * 
     * <pre>
     * #ID_REF = probe id
     * #VALUE = RMA value
     * </pre>
     * 
     * In GDS files, if we are in a 'dataset' section, these become "titles" for the samples if they aren't already
     * provided. Here is an example.
     * 
     * <pre>
     * #GSM549 = Value for GSM549: lexA vs. wt, before UV treatment, MG1655; src: 0' wt, before UV treatment, 25 ug total RNA, 2 ug pdN6&lt;-&gt;0' lexA, before UV 25 ug total RNA, 2 ug pdN6
     * #GSM542 = Value for GSM542: lexA 20' after NOuv vs. 0', MG1655; src: 0', before UV treatment, 25 ug total RNA, 2 ug pdN6&lt;-&gt;lexA 20 min after NOuv, 25 ug total RNA, 2 ug pdN6
     * #GSM543 = Value for GSM543: lexA 60' after NOuv vs. 0', MG1655; src: 0', before UV treatment, 25 ug total RNA, 2 ug pdN6&lt;-&gt;lexA 60 min after NOuv, 25 ug total RNA, 2 ug pdN6
     * </pre>
     *
     * @param line line
     */
    @SuppressWarnings("StatementWithEmptyBody") // Better readability
    private void parseColumnIdentifier( String line ) {
        if ( inPlatform ) {
            this.extractColumnIdentifier( line, this.currentPlatform() );
        } else if ( inSample ) {
            if ( !processPlatformsOnly ) {
                this.extractColumnIdentifier( line, this.currentSample() );
            }
        } else if ( inSeries ) {
            if ( !processPlatformsOnly )
                this.extractColumnIdentifier( line, this.currentSeries() );
        } else if ( inSubset ) {
            // nothing.
        } else if ( inDataset ) {
            if ( processPlatformsOnly )
                return;

            /*
             * Datasets give titles to samples that sometimes differ from the ones given in the GSE files. Sometimes
             * these are useful to keep around (for matching across data sets), so we store it in an "auxiliary" title.
             */

            this.extractColumnIdentifier( line, this.currentDataset() );
            Map<String, String> res = this.extractKeyValue( line );
            if ( res == null ) {
                throw new IllegalStateException( "Failed to extract key-value pair from the given line" );
            }
            String potentialSampleAccession = res.keySet().iterator().next();
            String potentialTitle = res.get( potentialSampleAccession );

            // First add the sample if we haven't seen it before.
            if ( potentialSampleAccession.startsWith( "GSM" ) && !results.getSampleMap()
                    .containsKey( potentialSampleAccession ) ) {
                this.addNewSample( potentialSampleAccession );
            }

            // Set the titleInDataset
            if ( potentialSampleAccession.startsWith( "GSM" ) && !StringUtils.isBlank( potentialTitle ) ) {
                potentialTitle = potentialTitle.substring( potentialTitle.indexOf( ':' ) + 2 ); // throw out the
                this.sampleSet( potentialSampleAccession, GeoSample::setTitleInDataset, potentialTitle );
            }

        } else {
            throw new IllegalStateException( "Wrong state to deal with '" + line + "'" );
        }
    }

    /**
     * Parse a line in a 'dataset' section of a GDS file. This is metadata about the experiment.
     *
     * @param line line
     * @param value value
     */
    @SuppressWarnings("StatementWithEmptyBody") // Better readability
    private void parseDatasetLine( String line, String value ) {
        if ( this.processPlatformsOnly )
            return;
        /*
         * *************************************************************************************************************
         * DATASET
         **************************************************************************************************************/
        if ( this.startsWithIgnoreCase( line, "!Dataset_title" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setTitle, value );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_description" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setDescription, value );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_maximum_probes" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setNumProbes, Integer.parseInt( value ) );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_order" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setOrder, value );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_organism" ) ) { // note, no longer used?
            this.datasetSet( currentDatasetAccession, GeoDataset::setOrganism, value );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_platform_organism" ) ) {
            // redundant, we get this from the series
        } else if ( this.startsWithIgnoreCase( line, "!dataset_platform_technology_type" ) ) {
            // we also get this from the platform directly.
        } else if ( this.startsWithIgnoreCase( line, "!dataset_platform" ) ) {
            if ( !results.getPlatformMap().containsKey( value ) ) {
                results.getPlatformMap().put( value, new GeoPlatform() );
                results.getPlatformMap().get( value ).setGeoAccession( value );
            }
            results.getDatasetMap().get( currentDatasetAccession ).setPlatform( results.getPlatformMap().get( value ) );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_probe_type" ) ) { // obsolete
            this.datasetSet( currentDatasetAccession, GeoDataset::setPlatformType, GeoDataset.convertStringToPlatformType( value ) );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_platform_technology_type" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setPlatformType, GeoDataset.convertStringToPlatformType( value ) );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_reference_series" ) ) {
            if ( !results.getSeriesMap().containsKey( value ) ) {
                GeoFamilyParser.log.debug( "Adding series " + value );
                results.getSeriesMap().put( value, new GeoSeries() );
                results.getSeriesMap().get( value ).setGeoAccession( value );
            }

            // FIXME this is really a bug: the same series comes up more than once, but empty in some case.
            GeoSeries series = results.getSeriesMap().get( value );
            if ( !results.getDatasetMap().get( currentDatasetAccession ).getSeries().contains( series ) ) {
                GeoFamilyParser.log.debug( currentDatasetAccession + " already has reference to series " + value );
            }

            if ( series.getSamples() != null && series.getSamples().size() > 0 ) {
                results.getDatasetMap().get( currentDatasetAccession ).addSeries( series );
            } else {
                GeoFamilyParser.log.warn( "Empty series " + series );
            }

        } else if ( this.startsWithIgnoreCase( line, "!dataset_total_samples" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setNumSamples, Integer.parseInt( value ) );
        } else if ( this
                .startsWithIgnoreCase( line, "!dataset_sample_count" ) ) { // is this the same as "total_samples"?
            this.datasetSet( currentDatasetAccession, GeoDataset::setNumSamples, Integer.parseInt( value ) );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_update_date" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setUpdateDate, value );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_value_type" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setValueType, GeoDataset.convertStringToValueType( value ) );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_completeness" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setCompleteness, value );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_experiment_type" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setExperimentType,
                    GeoDataset.convertStringToExperimentType( value ) ); // this is now "platform type"? in new GEO files?
        } else if ( this.startsWithIgnoreCase( line, "!dataset_type" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setDatasetType, value );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_feature_count" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setFeatureCount, value );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_sample_organism" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setOrganism, value ); // note, redundant with 'organism'.
        } else if ( this.startsWithIgnoreCase( line, "!dataset_sample_type" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setSampleType, GeoDataset.convertStringToSampleType( value ) );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_pubmed_id" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setPubmedId, value );
        } else if ( this.startsWithIgnoreCase( line, "!dataset_table_begin" ) ) {
            this.inDatasetTable = true;
            // haveReadDatasetDataHeader = false;
        } else if ( this.startsWithIgnoreCase( line, "!dataset_table_end" ) ) {
            this.inDatasetTable = false;
        } else if ( this.startsWithIgnoreCase( line, "!dataset_channel_count" ) ) {
            this.datasetSet( currentDatasetAccession, GeoDataset::setNumChannels, Integer.parseInt( value ) );
        } else {
            GeoFamilyParser.log.error( "Unknown flag in dataset: " + line );
        }
    }

    private void parseLine( String line ) {
        if ( StringUtils.isBlank( line ) )
            return;
        if ( line.startsWith( "^" ) ) {
            if ( this.startsWithIgnoreCase( line, "^DATABASE" ) ) {
                inDatabase = true;
                inSubset = false;
                inDataset = false;
                inSample = false;
                inPlatform = false;
                inSeries = false;
            } else if ( this.startsWithIgnoreCase( line, "^SAMPLE" ) ) {

                processedDesignElements.clear();
                inSample = true;
                inSubset = false;
                inDataset = false;
                inDatabase = false;
                inPlatform = false;
                inSeries = false;
                if ( this.processPlatformsOnly )
                    return;
                String value = this.extractValue( line );
                currentSampleAccession = value;
                GeoFamilyParser.log.debug( "Starting new sample " + value );
                if ( results.getSampleMap().containsKey( value ) )
                    return;
                this.addNewSample( value );
            } else if ( this.startsWithIgnoreCase( line, "^PLATFORM" ) ) {
                inPlatform = true;
                inSubset = false;
                inDataset = false;
                inDatabase = false;
                inSample = false;
                inSeries = false;
                String value = this.extractValue( line );
                currentPlatformAccession = value;
                if ( results.getPlatformMap().containsKey( value ) )
                    return;
                GeoPlatform platform = new GeoPlatform();
                platform.setGeoAccession( value );
                results.getPlatformMap().put( value, platform );
                GeoFamilyParser.log.info( "Starting platform " + platform );
            } else if ( this.startsWithIgnoreCase( line, "^SERIES" ) ) {
                inSeries = true;
                inSubset = false;
                inDataset = false;
                inPlatform = false;
                inSample = false;
                inDatabase = false;
                if ( this.processPlatformsOnly )
                    return;
                String value = this.extractValue( line );
                currentSeriesAccession = value;
                if ( results.getSeriesMap().containsKey( value ) )
                    return;
                GeoSeries series = new GeoSeries();
                series.setGeoAccession( value );
                results.getSeriesMap().put( value, series );
                GeoFamilyParser.log.debug( "In series " + series );
            } else if ( this.startsWithIgnoreCase( line, "^DATASET" ) ) {
                inDataset = true;
                inSubset = false;
                inSeries = false;
                inPlatform = false;
                inSample = false;
                inDatabase = false;
                if ( this.processPlatformsOnly )
                    return;
                String value = this.extractValue( line );
                currentDatasetAccession = value;
                if ( results.getDatasetMap().containsKey( value ) )
                    return;
                GeoDataset ds = new GeoDataset();
                ds.setGeoAccession( value );
                results.getDatasetMap().put( value, ds );
                GeoFamilyParser.log.debug( "In dataset " + ds );
            } else if ( this.startsWithIgnoreCase( line, "^SUBSET" ) ) {
                inSubset = true;
                inDataset = false;
                inSeries = false;
                inPlatform = false;
                inSample = false;
                inDatabase = false;
                if ( this.processPlatformsOnly )
                    return;
                String value = this.extractValue( line );
                currentSubsetAccession = value;
                if ( results.getSubsetMap().containsKey( value ) )
                    return;
                GeoSubset ss = new GeoSubset();
                ss.setGeoAccession( value );
                ss.setOwningDataset( results.getDatasetMap().get( this.currentDatasetAccession ) );
                results.getDatasetMap().get( this.currentDatasetAccession ).addSubset( ss );
                results.getSubsetMap().put( value, ss );
                GeoFamilyParser.log.debug( "In subset " + ss );
            } else {
                GeoFamilyParser.log.error( "Unknown flag in subset: " + line );
            }
        } else {
            this.parseRegularLine( line );
        }
    }

    /**
     * If a line does not have the same number of fields as the column headings, it is skipped.
     *
     * @param line line
     */
    private void parsePlatformLine( String line ) {

        if ( !haveReadPlatformHeader ) {
            haveReadPlatformHeader = true;
            return;
        }
        GeoPlatform currentPlatform = results.getPlatformMap().get( currentPlatformAccession );
        assert currentPlatform != null;

        /*
         * Skip platform information when it is not going to be usable, unless we are ONLY parsing a platform.
         */
        // Actually this isn't as important, since we filter out bad elements.
        // if ( !processPlatformsOnly && !currentPlatform.useDataFromGeo() ) {
        // return;
        // }

        String[] tokens = StringUtils.splitPreserveAllTokens( line, GeoFamilyParser.FIELD_DELIM );

        List<String> columnNames = currentPlatform.getColumnNames();
        int numColumns = columnNames.size();

        if ( numColumns != tokens.length && numWarnings < GeoFamilyParser.MAX_WARNINGS ) {
            GeoFamilyParser.log.warn( "Wrong number of tokens in line (" + tokens.length + ", expected " + numColumns
                    + "), line was '" + line + "'; Possible corrupt file or invalid format?" );
            numWarnings++;
            if ( numWarnings == GeoFamilyParser.MAX_WARNINGS ) {
                GeoFamilyParser.log.warn( "Further warnings suppressed" );
            }

            return;
        }

        for ( int i = 0; i < tokens.length; i++ ) {
            String token = tokens[i];
            String columnName = columnNames.get( i );
            currentPlatform.addToColumnData( columnName, token );
        }
        platformLines++;
    }

    /**
     * Parse a line in a 'platform' section of a GSE file. This deals with meta-data about the platform.
     *
     * @param line line
     * @param value value
     */
    @SuppressWarnings("StatementWithEmptyBody") // Better readability
    private void parsePlatformLine( String line, String value ) {
        /*
         * *************************************************************************************************************
         * PLATFORM
         **************************************************************************************************************/
        if ( this.startsWithIgnoreCase( line, "!Platform_title" ) ) {
            this.platformSet( currentPlatformAccession, GeoPlatform::setTitle, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_geo_accession" ) ) {
            currentPlatformAccession = value;
        } else if ( this.startsWithIgnoreCase( line, "!Platform_status" ) ) {
            this.platformSet( currentPlatformAccession, GeoPlatform::setStatus, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_manufacturer" ) ) {
            this.platformSet( currentPlatformAccession, GeoPlatform::setManufacturer, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_manufacture_protocol" ) ) {
            this.platformSet( currentPlatformAccession, GeoPlatform::setManufactureProtocol, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_submission_date" ) ) {
            this.platformSet( currentPlatformAccession, GeoPlatform::setSubmissionDate, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_technology" ) ) {
            this.platformSet( currentPlatformAccession, GeoPlatform::setTechnology, GeoDataset.convertStringToPlatformType( value ) );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_distribution" ) ) {
            this.platformSet( currentPlatformAccession, GeoPlatform::setDistribution, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_organism" ) ) {
            this.platformAddTo( currentPlatformAccession, GeoPlatform::addToOrganisms, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_description" ) ) {
            this.platformAddTo( currentPlatformAccession, GeoPlatform::addToDescription, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_name" ) ) {
            this.platformContactSet( currentPlatformAccession, GeoContact::setName, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_email" ) ) {
            this.platformContactSet( currentPlatformAccession, GeoContact::setEmail, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_institute" ) ) {
            this.platformContactSet( currentPlatformAccession, GeoContact::setInstitute, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_laboratory" ) ) {
            this.platformContactSet( currentPlatformAccession, GeoContact::setLaboratory, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_department" ) ) {
            this.platformContactSet( currentPlatformAccession, GeoContact::setDepartment, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_address" ) ) { // may not be used any more.
            this.platformContactSet( currentPlatformAccession, GeoContact::setAddress, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_city" ) ) {
            this.platformContactSet( currentPlatformAccession, GeoContact::setCity, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_zip/postal_code" ) ) {
            this.platformContactSet( currentPlatformAccession, GeoContact::setPostCode, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_state" ) ) {
            this.platformContactSet( currentPlatformAccession, GeoContact::setState, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_country" ) ) {
            this.platformContactSet( currentPlatformAccession, GeoContact::setCountry, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_phone" ) ) {
            this.platformContactSet( currentPlatformAccession, GeoContact::setPhone, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_web_link" ) ) {
            this.platformContactSet( currentPlatformAccession, GeoContact::setWebLink, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_support" ) ) {
            // use this (maybe)
        } else if ( this.startsWithIgnoreCase( line, "!Platform_coating" ) ) {
            // use this (maybe)
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contact_fax" ) ) {
            this.platformContactSet( currentPlatformAccession, GeoContact::setFax, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_web_link" ) ) {
            this.platformSet( currentPlatformAccession, GeoPlatform::setWebLink, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_sample_id" ) ) {
            this.platformSet( currentPlatformAccession, GeoPlatform::setId, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_table_begin" ) ) {
            inPlatformTable = true;
            haveReadPlatformHeader = false;
        } else if ( this.startsWithIgnoreCase( line, "!Platform_table_end" ) ) {
            inPlatformTable = false;
        } else if ( this.startsWithIgnoreCase( line, "!Platform_contributor" ) ) {
            // noop. This is the name of the person who submitted the platform.
        } else if ( this.startsWithIgnoreCase( line, "!Platform_series_id" ) ) {
            // no-op. This identifies which series were run on this platform. We don't care to get this
            // information this way.
        } else if ( this.startsWithIgnoreCase( line, "!Platform_data_row_count" ) ) {
            // nothing. However, if this is zero, we might be able to skip later steps.
        } else if ( this.startsWithIgnoreCase( line, "!Platform_catalog_number" ) ) {
            // do nothing
        } else if ( this.startsWithIgnoreCase( line, "!Platform_last_update_date" ) ) {
            platformSet( currentPlatformAccession, GeoPlatform::setLastUpdateDate, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_supplementary_file" ) ) {
            platformSet( currentPlatformAccession, GeoPlatform::setSupplementaryFile, value );
        } else if ( this.startsWithIgnoreCase( line, "!Platform_pubmed_id" ) ) {
            // do nothing. for now.
        } else if ( this.startsWithIgnoreCase( line, "!Platform_relation" ) ) {
            // no op for now. Links to other platforms this is derived from.
        } else if ( this.startsWithIgnoreCase( line, "!Platform_taxid" ) ) {
            // no op for now....
        } else {
            GeoFamilyParser.log.error( "Unknown flag in platform: " + line );
        }
    }

    /**
     * Parse lines in GSE and GDS files. Lines are classified into three types:
     * <ul>
     * <li>Starting with "!". These indicate meta data.
     * <li>Starting with "#". These indicate descriptions of columns in a data table.
     * <li>Starting with anything else, primarily (only?) data tables (expression data or platform probe annotations).
     * </ul>
     *
     * @param line line
     */
    @SuppressWarnings("StatementWithEmptyBody") // Better readability
    private void parseRegularLine( String line ) {
        if ( line.startsWith( "!" ) ) {
            String value = this.extractValue( line );
            if ( inSample ) {
                this.parseSampleLine( line, value );
            } else if ( inSeries ) {
                this.parseSeriesLine( line, value );
            } else if ( inDatabase ) {
                // we are going to ignore these lines.
            } else if ( inPlatform ) {
                this.parsePlatformLine( line, value );
            } else if ( inDataset ) {
                inDatasetTable = true;
                this.parseDatasetLine( line, value );
            } else if ( inSubset ) {
                this.parseSubsetLine( line, value );
            } else {
                throw new IllegalStateException( "Unknown flag: " + line );
            }
        } else if ( line.startsWith( "#" ) ) {
            this.parseColumnIdentifier( line );
        } else {
            if ( inPlatformTable ) {
                this.parsePlatformLine( line );
            } else if ( inSampleTable ) {
                this.parseSampleDataLine( line );
            } else if ( inSeriesTable ) {
                // we ignore this and use the sample data instead.
            } else if ( inDatasetTable ) {
                // we ignore this and use the sample data instead.
            } else if ( inSubset ) {
                // do nothing.
            } else {
                // do nothing.
            }
        }

    }

    /**
     * The data for one sample is all the values for each quantitation type.
     * Important implementation note: In the sample table sections of GSEXXX_family files, the first column is always
     * ID_REF, according to the kind folks at NCBI. If this changes, this code will BREAK.
     * Similarly, the column names between the different samples are not necessarily the same, but we trust that they
     * all refer to the same quantitation types in the same order, for a given platform. That is, the nth column for
     * this sample 'means' the same thing as the nth column for another sample in this series (on the same platform). If
     * that isn't true, this will be BROKEN. However, we do try to sort it out if we can.
     *
     * @param line line
     */
    private void parseSampleDataLine( String line ) {

        if ( StringUtils.isBlank( line ) )
            return;

        if ( !haveReadSampleDataHeader ) {
            haveReadSampleDataHeader = true;
            previousNumTokens = null;
            this.initializeQuantitationTypes();
            return;
        }

        GeoSample sample = results.getSampleMap().get( currentSampleAccession );

        /*
         * skip this step if it's not a supported platform type (RNA-seq, exon arrays: we put the data in later)
         */
        if ( !sample.hasUsableData() ) {
            return;
        }

        String[] tokens = StringUtils.splitPreserveAllTokens( line, GeoFamilyParser.FIELD_DELIM );

        assert tokens != null;

        /*
         * This can happen in some files that are mildly corrupted. -- we have to ignore it.
         */
        if ( tokens.length <= 1 && numWarnings < GeoFamilyParser.MAX_WARNINGS ) {
            GeoFamilyParser.log
                    .error( "Parse error, sample data line has too few elements (" + tokens.length + "), line was '"
                            + line + "'" );
            numWarnings++;
            if ( numWarnings == GeoFamilyParser.MAX_WARNINGS ) {
                GeoFamilyParser.log.warn( "Further warnings suppressed" );
            }
            return;
        }

        if ( previousNumTokens != null && tokens.length != previousNumTokens ) {
            GeoFamilyParser.log
                    .warn( "Last line had " + ( previousNumTokens - 1 ) + " quantitation types, this one has " + ( tokens.length - 1 ) );
        }

        previousNumTokens = tokens.length;

        if ( results.getSeriesMap().get( currentSeriesAccession ) == null ) {
            return; // this happens if we are parsing a GPL file.
        }

        GeoPlatform platformForSample = sample.getPlatforms().iterator().next(); // slow

        GeoValues values = results.getSeriesMap().get( currentSeriesAccession ).getValues();

        String designElement = tokens[0]; // ID_REF. For bug 1709, adding toLower() will fix this.
        Map<Integer, Integer> map = quantitationTypeTargetColumn.get( platformForSample );

        for ( int i = 1; i < tokens.length; i++ ) {
            String value = tokens[i];
            int qtIndex = i - 1;

            /*
             * This map tells us which column this quantitation type is SUPPOSED to go in.
             */

            if ( map.containsKey( qtIndex ) )
                qtIndex = map.get( qtIndex );
            if ( !this.isWantedQuantitationType( qtIndex ) ) {
                continue;
            }

            if ( GeoFamilyParser.log.isTraceEnabled() ) {
                GeoFamilyParser.log
                        .trace( "Adding: " + value + " to  quantitationType " + ( qtIndex ) + " for " + designElement );
            }
            values.addValue( sample, qtIndex, designElement, value );
            processedDesignElements.add( designElement );
        }

        sampleDataLines++;
    }

    /**
     * Parse a line from a sample section of a GSE file. These contain details about the samples and the 'raw' data for
     * the sample.
     *
     * @param line line
     * @param value value
     */
    @SuppressWarnings("StatementWithEmptyBody") // Better readability
    private void parseSampleLine( String line, String value ) {
        if ( this.processPlatformsOnly )
            return;

        /*
         * *************************************************************************************************************
         * SAMPLE
         **************************************************************************************************************/
        if ( this.startsWithIgnoreCase( line, "!sample_table_begin" ) ) {
            inSampleTable = true;
            haveReadSampleDataHeader = false;
        } else if ( this.startsWithIgnoreCase( line, "!sample_table_end" ) ) {
            this.checkDataCompleteness();
            inSampleTable = false;
        } else if ( this.startsWithIgnoreCase( line, "!Sample_title" ) ) {
            if ( this.inDataset ) {
                this.sampleSet( currentSampleAccession, GeoSample::setTitleInDataset, value );
            } else {
                this.sampleSet( currentSampleAccession, GeoSample::setTitle, value );
            }
        } else if ( this.startsWithIgnoreCase( line, "!Sample_geo_accession" ) ) {
            currentSampleAccession = value;
            if ( !results.getSampleMap().containsKey( currentSampleAccession ) ) {
                GeoFamilyParser.log.debug( "New sample " + currentSampleAccession );
                results.getSampleMap().put( currentSampleAccession, new GeoSample() );
            }
        } else if ( this.startsWithIgnoreCase( line, "!Sample_status" ) ) {
            this.sampleSet( currentSampleAccession, GeoSample::setStatus, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_submission_date" ) ) {
            this.sampleSet( currentSampleAccession, GeoSample::setSubmissionDate, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_channel_count" ) ) {
            int numExtraChannelsNeeded = Integer.parseInt( value ) - 1;
            for ( int i = 0; i < numExtraChannelsNeeded; i++ ) {
                results.getSampleMap().get( currentSampleAccession ).addChannel();
            }
            this.sampleSet( currentSampleAccession, GeoSample::setChannelCount, Integer.parseInt( value ) );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_source_name" ) ) {
            int channel = this.extractChannelNumber( line );
            this.sampleChannelSet( currentSampleAccession, GeoChannel::setSourceName, channel, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_organism" ) ) {
            int channel = this.extractChannelNumber( line );
            this.sampleChannelSet( currentSampleAccession, GeoChannel::setOrganism, channel, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_biomaterial_provider" ) ) {
            int channel = this.extractChannelNumber( line );
            this.sampleChannelSet( currentSampleAccession, GeoChannel::setBioMaterialProvider, channel, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_treatment_protocol" ) ) {
            int channel = this.extractChannelNumber( line );
            this.sampleChannelAddTo( currentSampleAccession, GeoChannel::addToTreatmentProtocol, channel, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_molecule" ) ) {
            int channel = this.extractChannelNumber( line );
            this.sampleChannelSet( currentSampleAccession, GeoChannel::setMolecule, channel, GeoChannel.convertStringToMolecule( value ) );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_growth_protocol" ) ) {
            int channel = this.extractChannelNumber( line );
            this.sampleChannelAddTo( currentSampleAccession, GeoChannel::addToGrowthProtocol, channel, value );
        } else if ( this.startsWithIgnoreCase( line, "!sample_extract_protocol" ) ) {
            int channel = this.extractChannelNumber( line );
            this.sampleChannelAddTo( currentSampleAccession, GeoChannel::addToExtractProtocol, channel, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_hyb_protocol" ) ) {
            this.sampleAddTo( currentSampleAccession, GeoSample::addToHybProtocol, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_scan_protocol" ) ) {
            this.sampleAddTo( currentSampleAccession, GeoSample::addToScanProtocol, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_data_processing" ) ) {
            this.sampleAddTo( currentSampleAccession, GeoSample::addToDataProcessing, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_description" ) ) {
            this.sampleAddTo( currentSampleAccession, GeoSample::addToDescription, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_label_protocol" ) ) {
            int channel = this.extractChannelNumber( line );
            this.sampleChannelSet( currentSampleAccession, GeoChannel::setLabelProtocol, channel, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_label" ) ) {
            int channel = this.extractChannelNumber( line );
            this.sampleChannelSet( currentSampleAccession, GeoChannel::setLabel, channel, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_characteristics" ) ) {
            int channel = this.extractChannelNumber( line );
            GeoSample sample = results.getSampleMap().get( currentSampleAccession );
            sample.getChannel( channel ).addCharacteristic( value ); // might be something like "rin: 7.2"
        } else if ( this.startsWithIgnoreCase( line, "!Sample_platform_id" ) ) {
            this.sampleSet( currentSampleAccession, GeoSample::setId, value );
            if ( results.getPlatformMap().containsKey( value ) ) {
                results.getSampleMap().get( currentSampleAccession )
                        .addPlatform( results.getPlatformMap().get( value ) );
            }
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_name" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setName, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_email" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setEmail, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_institute" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setInstitute, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_laboratory" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setLaboratory, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_department" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setDepartment, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_address" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setAddress, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_city" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setCity, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_state" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setState, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_country" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setCountry, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_zip/postal_code" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setPostCode, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_phone" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setPhone, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_web_link" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setWebLink, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_contact_fax" ) ) {
            this.sampleContactSet( currentSampleAccession, GeoContact::setFax, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_series_id" ) ) {
            if ( results.getSeriesMap().containsKey( value ) ) {
                results.getSeriesMap().get( value ).addSample( results.getSampleMap().get( currentSampleAccession ) );
            }
            this.seriesSet( currentSeriesAccession, GeoSeries::setSeriesId, value );
            results.getSampleMap().get( currentSampleAccession ).addSeriesAppearsIn( value );

        } else if ( this.startsWithIgnoreCase( line, "!Sample_supplementary_file" ) ) {
            sampleSet( currentSampleAccession, GeoSample::setSupplementaryFile, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_last_update_date" ) ) {
            this.sampleSet( currentSampleAccession, GeoSample::setLastUpdateDate, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_data_row_count" ) ) {
            if ( value.equals( "0" ) ) {
                /*
                 * Empty sample, we won't get any data and this messes things up later.
                 */
                GeoFamilyParser.log.warn( "No data for sample " + currentSampleAccession );
                this.initializeQuantitationTypes();
                this.checkDataCompleteness(); // because we don't get the table_end.
            }
        } else if ( this.startsWithIgnoreCase( line, "!Sample_type" ) ) {
            // e.g. SRA - this is not actually the type!
            this.sampleTypeSet( currentSampleAccession, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_comment" ) ) {
            // noop.
        } else if ( this.startsWithIgnoreCase( line, "!Sample_taxid_ch" ) ) {
            // noop.
        } else if ( this.startsWithIgnoreCase( line, "!Sample_relation" ) ) {
            // noop, for now. Example is "!Sample_relation = Reanalyzed by: GSE26971" in GSE12093
            // also SRA: http://www.ncbi.nlm.nih.gov/sra?term=SRX119472; or BioSample:
            // http://www.ncbi.nlm.nih.gov/biosample/SAMN00788643
        } else if ( this.startsWithIgnoreCase( line, "!Sample_instrument_model" ) ) {
            // e.g. Illumina HiSeq 2000
        } else if ( this.startsWithIgnoreCase( line, "!Sample_library_selection" ) ) {
            // e.g. 'cDNA', 'other'
        } else if ( this.startsWithIgnoreCase( line, "!Sample_library_source" ) ) {
            // see http://www.ncbi.nlm.nih.gov/geo/info/soft-seq.html
            // e.g. 'transcriptomic' - if not skip? GENOMIC, OTHER
            if ( value.equals( "genomic" ) ) {
                ///
            }
            this.sampleSetLibSource( currentSampleAccession, value );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_library_strategy" ) ) {
            this.sampleSetLibStrategy( currentSampleAccession, value );
            this.sampleSet( currentSampleAccession, GeoSample::setMightNotHaveDataInFile, true );
        } else if ( this.startsWithIgnoreCase( line, "!Sample_anchor" ) ) {
            // e.g. NlaIII for SAGE
        } else if ( this.startsWithIgnoreCase( line, "!Sample_tag_length" ) ) {
            // SAGE
        } else if ( this.startsWithIgnoreCase( line, "!Sample_tag_count" ) ) {
            // SAGE
        } else {
            GeoFamilyParser.log.error( "Unknown flag in sample: " + line );
        }
    }

    /**
     */
    private void sampleSetLibSource( String accession, String string ) {
        GeoSample sample = results.getSampleMap().get( accession );
        if ( string.equalsIgnoreCase( "transcriptomic" ) ) {
            sample.setLibSource( "transcriptomic" );
        } else if ( string.equalsIgnoreCase( "genomic" ) ) {
            sample.setLibSource( "genomic" );
        } else {
            throw new IllegalArgumentException( "Unknown library source: " + string );
        }

    }

    /**
     */
    private void sampleSetLibStrategy( String accession, String string ) {
        GeoSample sample = results.getSampleMap().get( accession );
        if ( string.equalsIgnoreCase( "RNA-Seq" ) ) {
            sample.setLibStrategy( "RNA-Seq" );
        } else if ( string.equalsIgnoreCase( "Bisulfite-Seq" ) ) {
            sample.setLibStrategy( "Bisulfite-Seq" );
        } else if ( string.equalsIgnoreCase( "DNase-Hypersensitivity" ) ) {
            sample.setLibStrategy( "DNase-Hypersensitivity" );
        } else if ( string.equalsIgnoreCase( "ATAC-seq" ) ) {
            sample.setLibStrategy( "ATAC-seq" );
        } else if ( string.equalsIgnoreCase( "ChIP-Seq" ) ) {
            sample.setLibStrategy( "ChIP-Seq" );
        } else if ( string.equalsIgnoreCase( "OTHER" ) ) {
            sample.setLibStrategy( "OTHER" );
        } else if ( string.equalsIgnoreCase( "MRE-Seq" ) ) {
            sample.setLibStrategy( "MRE-Seq" );
        } else if ( string.equalsIgnoreCase( "miRNA-Seq" ) ) {
            sample.setLibStrategy( "miRNA-Seq" );
        } else if ( string.equalsIgnoreCase( "RIP-Seq" ) ) {
            sample.setLibStrategy( "RIP-Seq" );
        } else if ( string.equalsIgnoreCase( "Hi-C" ) ) {
            sample.setLibStrategy( "Hi-C" );
        } else if ( string.equalsIgnoreCase( "ssRNA-seq" ) ) {
            sample.setLibStrategy( "ssRNA-seq" );
        } else if ( string.equalsIgnoreCase( "MBD-Seq" ) ) {
            sample.setLibStrategy( "MBD-Seq" );
        } else if ( string.equalsIgnoreCase( "FAIRE-seq" ) ) {
            sample.setLibStrategy( "FAIRE-seq" );
        } else if ( string.equalsIgnoreCase( "MeDIP-Seq" ) ) {
            sample.setLibStrategy( "MeDIP-Seq" );
        } else if ( string.equalsIgnoreCase( "MNase-Seq" ) ) {
            sample.setLibStrategy( "MNase-Seq" );
        } else if ( string.equalsIgnoreCase( "ChIA-PET" ) ) {
            sample.setLibSource( "ChIA-PET" );
        } else if ( string.equalsIgnoreCase( "ncRNA-Seq" ) ) {
            sample.setLibStrategy( "ncRNA-Seq" );
        } else {
            throw new IllegalArgumentException( "Unknown library source: " + string );
        }

    }

    /**
     * Parse a line from the "series" section of a GSE file. This contains annotations about the series.
     *
     * @param line line
     * @param value value
     */
    @SuppressWarnings("StatementWithEmptyBody") // Better readability
    private void parseSeriesLine( String line, String value ) {
        if ( this.processPlatformsOnly )
            return;
        /*
         * *************************************************************************************************************
         * SERIES
         **************************************************************************************************************/
        if ( this.startsWithIgnoreCase( line, "!Series_title" ) ) {
            this.seriesSet( currentSeriesAccession, GeoSeries::setTitle, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_geo_accession" ) ) {
            currentSeriesAccession = value;
        } else if ( this.startsWithIgnoreCase( line, "!Series_status" ) ) {
            this.seriesSet( currentSeriesAccession, GeoSeries::setStatus, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_submission_date" ) ) {
            this.seriesSet( currentSeriesAccession, GeoSeries::setSubmissionDate, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_pubmed_id" ) ) {
            this.seriesAddTo( currentSeriesAccession, GeoSeries::addToPubmedIds, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_overall_design" ) ) {
            this.seriesSet( currentSeriesAccession, GeoSeries::setOverallDesign, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_relation" ) ) {

            if ( value.toLowerCase().startsWith( "superseries" ) ) {
                GeoFamilyParser.log.info( " ** SuperSeries detected **" );
                this.seriesSet( currentSeriesAccession, GeoSeries::setIsSuperSeries, true );
            } else if ( value.toLowerCase().startsWith( "subseries" ) ) {
                GeoFamilyParser.log.info( " ** Subseries detected **" );
                this.seriesSet( currentSeriesAccession, GeoSeries::setIsSubSeries, true );
            }

        } else if ( this.startsWithIgnoreCase( line, "!Series_summary" ) ) {

            if ( value.toLowerCase().startsWith( "this superseries" ) ) {
                GeoFamilyParser.log.info( " ** SuperSeries detected **" );
                this.seriesSet( currentSeriesAccession, GeoSeries::setIsSuperSeries, true );
            } else if ( value.toLowerCase().startsWith( "gse" ) && results.getSeriesMap().get( currentSeriesAccession )
                    .isSuperSeries() ) {
                String[] fields = value.split( ":", 2 );
                if ( fields.length != 2 ) {
                    throw new IllegalStateException( "Expected a colon in " + value );
                }
                results.getSeriesMap().get( currentSeriesAccession ).addSubSeries( fields[0] );
            } else if ( value.toLowerCase().contains( "keyword" ) ) {
                String keyword = this.extractValue( value );
                this.seriesAddTo( currentSeriesAccession, GeoSeries::addToKeyWords, keyword );
            } else {
                this.seriesAddTo( currentSeriesAccession, GeoSeries::addToSummary, value );
            }
        } else if ( this.startsWithIgnoreCase( line, "!Series_type" ) ) {
            // currently there is no spec for what values Series_type can take
            /*
             * Series can have multiple types if it has mixtures of samples.
             */
            this.seriesAddTo( currentSeriesAccession, GeoSeries::addToSeriesTypes, GeoSeries.convertStringToSeriesType( value ) );

        } else if ( this.startsWithIgnoreCase( line, "!Series_contributor" ) ) {
            GeoContact contributer = new GeoContact();
            String[] nameFields = StringUtils.split( value, "," );
            contributer.setName( StringUtils.join( nameFields, " " ) );
            results.getSeriesMap().get( currentSeriesAccession ).addContributer( contributer );
        } else if ( this.startsWithIgnoreCase( line, "!Series_sample_id" ) ) {
            this.addSeriesSample( value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_name" ) ) {
            this.seriesContactSet( currentSeriesAccession, GeoContact::setName, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_email" ) ) {
            this.seriesContactSet( currentSeriesAccession, GeoContact::setEmail, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_institute" ) ) {
            this.seriesContactSet( currentSeriesAccession, GeoContact::setInstitute, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_laboratory" ) ) {
            this.seriesContactSet( currentSeriesAccession, GeoContact::setLaboratory, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_department" ) ) {
            this.seriesContactSet( currentSeriesAccession, GeoContact::setDepartment, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_address" ) ) { // may not be used any longer.
            this.seriesContactSet( currentSeriesAccession, GeoContact::setAddress, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_state" ) ) { // new
            this.seriesContactSet( currentSeriesAccession, GeoContact::setState, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_zip/postal_code" ) ) { // new
            this.seriesContactSet( currentSeriesAccession, GeoContact::setPostCode, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_country" ) ) { // new
            this.seriesContactSet( currentSeriesAccession, GeoContact::setCountry, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_city" ) ) {
            this.seriesContactSet( currentSeriesAccession, GeoContact::setCity, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_phone" ) ) {
            this.seriesContactSet( currentSeriesAccession, GeoContact::setPhone, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_fax" ) ) {
            this.seriesContactSet( currentSeriesAccession, GeoContact::setFax, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_contact_web_link" ) ) {
            this.seriesContactSet( currentSeriesAccession, GeoContact::setWebLink, value );
        } else if ( this.startsWithIgnoreCase( line, "!series_platform_id" ) ) {
            this.seriesSet( currentSeriesAccession, GeoSeries::setPlatformId, value );
        } else if ( this.startsWithIgnoreCase( line, "!series_table_begin" ) ) {
            inSeriesTable = true;
        } else if ( this.startsWithIgnoreCase( line, "!series_table_end" ) ) {
            inSeriesTable = false;
        } else if ( this.startsWithIgnoreCase( line, "!Series_variable_description_" ) ) {
            Integer variableId = this.extractVariableNumber( line );
            results.getSeriesMap().get( currentSeriesAccession ).getVariables().get( variableId )
                    .setDescription( value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_variable_sample_list_" ) ) {
            this.parseSeriesVariableSampleListLine( line, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_variable_repeats_" ) ) {
            Integer variableId = this.extractVariableNumber( line );
            results.getSeriesMap().get( currentSeriesAccession ).getReplicates().get( variableId )
                    .setRepeats( GeoReplication.convertStringToRepeatType( value ) );
        } else if ( this.startsWithIgnoreCase( line, "!Series_variable_repeats_sample_list" ) ) {
            this.parseSeriesVariableRepeatsSampleListLine( line, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_web_link" ) ) {
            // seriesSet( currentSeriesAccession, "platformId", value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_variable_" ) ) {
            Integer variableId = this.extractVariableNumber( line );
            GeoVariable v = new GeoVariable();
            v.setType( GeoVariable.convertStringToType( value ) );
            results.getSeriesMap().get( currentSeriesAccession ).addToVariables( variableId, v );
        } else if ( this.startsWithIgnoreCase( line, "!Series_supplementary_file" ) ) {
            seriesSet( currentSeriesAccession, GeoSeries::setSupplementaryFile, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_last_update_date" ) ) {
            seriesSet( currentSeriesAccession, GeoSeries::setLastUpdateDate, value );
        } else if ( this.startsWithIgnoreCase( line, "!Series_citation" ) ) {
            // no-op. This should be redundant with the pubmed info and is hard to parse anyway
        } else if ( this.startsWithIgnoreCase( line, "!Series_platform_taxid" ) ) {
            // no-op for now
        } else if ( this.startsWithIgnoreCase( line, "!Series_sample_taxid" ) ) {
            // no-op for now.
        } else {
            GeoFamilyParser.log.error( "Unknown flag in series: " + line );
        }
    }

    private void parseSeriesVariableRepeatsSampleListLine( String line, String value ) {
        Integer variableId = this.extractVariableNumber( line );
        GeoReplication var = this.currentSeries().getReplicates().get( variableId );
        String[] samples = StringUtils.split( value, ", " );
        for ( String string : samples ) {
            GeoSample sam = results.getSampleMap().get( string );
            var.addToRepeatsSampleList( sam );
            sam.addReplication( var );
        }
    }

    private void parseSeriesVariableSampleListLine( String line, String value ) {
        Integer variableId = this.extractVariableNumber( line );
        GeoVariable var = this.currentSeries().getVariables().get( variableId );
        String[] samples = StringUtils.split( value, "," );
        for ( String string : samples ) {
            GeoSample sam = results.getSampleMap().get( string );
            var.addToVariableSampleList( sam );
            sam.addVariable( var );
        }
    }

    /**
     * Parse a line from a "subset" section of a GDS file. This section contains information about experimental subsets
     * within a dataset. These usually correspond to different factor values such as "drug-treated" vs. "placebo".
     *
     * @param line line
     * @param value value
     */
    private void parseSubsetLine( String line, String value ) {
        /*
         * *************************************************************************************************************
         * SUBSET
         **************************************************************************************************************/
        if ( this.startsWithIgnoreCase( line, "!Dataset_title" ) ) {
            this.subsetSet( currentSubsetAccession, GeoSubset::setTitle, value );
        } else if ( this.startsWithIgnoreCase( line, "!subset_dataset_id" ) ) {
            this.subsetSet( currentSubsetAccession, GeoSubset::setDataSet, value );
        } else if ( this.startsWithIgnoreCase( line, "!subset_description" ) ) {
            this.subsetAddToDescription( currentSubsetAccession, value );
        } else if ( this.startsWithIgnoreCase( line, "!subset_sample_id" ) ) {
            // This should yield a list of samples we have already seen.
            String[] values = value.split( "," );
            for ( String sampleAccession : values ) {
                if ( !results.getSampleMap().containsKey( sampleAccession ) ) {
                    this.addNewSample( sampleAccession );
                }

                if ( GeoFamilyParser.log.isDebugEnabled() )
                    GeoFamilyParser.log
                            .debug( "Adding sample: " + sampleAccession + " to subset " + currentSubsetAccession );

                results.getSubsetMap().get( currentSubsetAccession )
                        .addSample( results.getSampleMap().get( sampleAccession ) );
            }

        } else if ( this.startsWithIgnoreCase( line, "!subset_type" ) ) {
            this.subsetSet( currentSubsetAccession, GeoSubset::setType, GeoVariable.convertStringToType( value ) );
        } else {
            GeoFamilyParser.log.error( "Unknown flag: " + line );
        }
    }

    private <T> void platformAddTo( String accession, BiConsumer<GeoPlatform, T> consumer, T value ) {
        GeoPlatform platform = results.getPlatformMap().get( accession );
        if ( platform == null )
            throw new IllegalArgumentException( "Unknown platform " + accession );
        consumer.accept( platform, value );
    }

    private <T> void platformContactSet( String accession, BiConsumer<GeoContact, T> property, T value ) {
        GeoPlatform platform = results.getPlatformMap().get( accession );
        if ( platform == null )
            throw new IllegalArgumentException( "Unknown platform " + accession );
        this.contactSet( platform, property, value );
    }

    private <T> void platformSet( String accession, BiConsumer<GeoPlatform, T> property, T value ) {
        GeoPlatform platform = results.getPlatformMap().get( accession );
        if ( platform == null )
            throw new IllegalArgumentException( "Unknown platform " + accession );
        property.accept( platform, value );
    }

    private <T> void sampleAddTo( String accession, BiConsumer<GeoSample, T> property, T value ) {
        GeoSample sample = results.getSampleMap().get( accession );
        if ( sample == null )
            throw new IllegalArgumentException( "Unknown sample " + accession );
        property.accept( sample, value );
    }

    private void sampleChannelAddTo( String sampleAccession, BiConsumer<GeoChannel, String> property, int channel, String value ) {
        GeoSample sample = results.getSampleMap().get( sampleAccession );
        if ( sample == null ) {
            throw new IllegalArgumentException( "Unknown sample " + sampleAccession );
        }
        GeoChannel entity = sample.getChannel( channel );
        if ( entity == null ) {
            throw new IllegalArgumentException( "Missing channel " + channel + " for sample " + sampleAccession );
        }
        GeoChannel target = sample.getChannel( channel );
        property.accept( target, value );
    }

    private <T> void sampleChannelSet( String sampleAccession, BiConsumer<GeoChannel, T> property, int channel, T value ) {
        GeoSample sample = results.getSampleMap().get( sampleAccession );
        GeoChannel entity = sample.getChannel( channel );
        if ( entity == null ) {
            throw new IllegalArgumentException( "Missing channel " + channel + " for sample " + sampleAccession );
        }
        property.accept( entity, value );
    }

    private <T> void sampleContactSet( String accession, BiConsumer<GeoContact, T> property, T value ) {
        GeoSample sample = results.getSampleMap().get( accession );
        if ( sample == null )
            throw new IllegalArgumentException( "Unknown sample " + accession );
        this.contactSet( sample, property, value );
    }

    private <T> void sampleSet( String accession, BiConsumer<GeoSample, T> property, T value ) {
        GeoSample sample = results.getSampleMap().get( accession );
        if ( sample == null )
            throw new IllegalArgumentException( "Unknown sample " + accession );
        property.accept( sample, value );
    }

    private <T> void seriesAddTo( String accession, BiConsumer<GeoSeries, T> property, T value ) {
        GeoSeries series = results.getSeriesMap().get( accession );
        if ( series == null )
            throw new IllegalArgumentException( "Unknown series " + accession );
        property.accept( series, value );
    }

    private <T> void seriesContactSet( String accession, BiConsumer<GeoContact, T> property, T value ) {
        GeoSeries series = results.getSeriesMap().get( accession );
        if ( series == null )
            throw new IllegalArgumentException( "Unknown series " + accession );
        this.contactSet( series, property, value );
    }

    private <T> void seriesSet( String accession, BiConsumer<GeoSeries, T> property, T value ) {
        GeoSeries series = results.getSeriesMap().get( accession );
        if ( series == null )
            throw new IllegalArgumentException( "Unknown series " + accession );
        property.accept( series, value );
    }

    private boolean startsWithIgnoreCase( String string, String pattern ) {
        // it will never be the same string.
        return string.regionMatches( true, 0, pattern, 0, pattern.length() );
    }

    private void subsetAddToDescription( String accession, String value ) {
        GeoSubset subset = results.getSubsetMap().get( accession );
        if ( subset == null )
            throw new IllegalArgumentException( "Unknown subset " + accession );
        ( ( BiConsumer<GeoSubset, String> ) GeoSubset::addToDescription ).accept( subset, value );
    }

    private <T> void subsetSet( String accession, BiConsumer<GeoSubset, T> property, T value ) {
        GeoSubset subset = results.getSubsetMap().get( accession );
        if ( subset == null )
            throw new IllegalArgumentException( "Unknown subset " + accession );
        property.accept( subset, value );
    }

    /**
     * Check for problems and fix them.
     */
    private void tidyUp() {

        this.checkForAndFixMissingColumnNames();
    }

    private void validate() {
        GeoValues values = results.getSeriesMap().get( currentSeriesAccession ).getValues();
        values.validate();
    }

    private boolean willClobberOtherQuantitationType( String columnName, int actualColumnNumber,
            Map<String, Integer> qtMapForPlatform ) {
        boolean clobbers = false;
        for ( String name : this.currentSample().getColumnNames() ) {
            if ( name.equals( columnName ) )
                continue;
            if ( !qtMapForPlatform.containsKey( name ) )
                continue;
            Integer checkColInd = qtMapForPlatform.get( name );
            if ( checkColInd == actualColumnNumber ) {
                clobbers = true;
                break;
            }
        }
        return clobbers;
    }

}
