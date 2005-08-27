package edu.columbia.gemma.loader.expression.geo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.util.FileTools;
import baseCode.util.StringUtil;
import edu.columbia.gemma.loader.expression.geo.model.GeoContact;
import edu.columbia.gemma.loader.expression.geo.model.GeoData;
import edu.columbia.gemma.loader.expression.geo.model.GeoDataset;
import edu.columbia.gemma.loader.expression.geo.model.GeoPlatform;
import edu.columbia.gemma.loader.expression.geo.model.GeoSample;
import edu.columbia.gemma.loader.expression.geo.model.GeoSeries;
import edu.columbia.gemma.loader.expression.geo.model.GeoSubset;
import edu.columbia.gemma.loader.expression.geo.model.GeoVariable;
import edu.columbia.gemma.loader.loaderutils.Parser;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoFamilyParser implements Parser {

    /**
     * 
     */
    private static final String FIELD_DELIM = "\t";

    private static Log log = LogFactory.getLog( GeoFamilyParser.class.getName() );

    private String currentDatasetAccession;
    private String currentPlatformAccession;
    private String currentSampleAccession;
    private String currentSeriesAccession;
    private String currentSubsetAccession;
    private Map<String, String> datasetColumns;

    private Map<String, GeoDataset> datasetMap;
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

    private Map<String, String> platformColumns;

    private Map<String, GeoPlatform> platformMap;

    private Map<String, String> sampleColumns;

    private Map<String, GeoSample> sampleMap;

    private Map<String, String> seriesColumns;

    private Map<String, GeoSeries> seriesMap;

    private Map<String, GeoSubset> subsetMap;

    public GeoFamilyParser() {
        sampleMap = new HashMap<String, GeoSample>();
        platformMap = new HashMap<String, GeoPlatform>();
        seriesMap = new HashMap<String, GeoSeries>();
        platformColumns = new HashMap<String, String>();
        sampleColumns = new HashMap<String, String>();
        seriesColumns = new HashMap<String, String>();
        datasetColumns = new HashMap<String, String>();
        datasetMap = new HashMap<String, GeoDataset>();
        subsetMap = new HashMap<String, GeoSubset>();
    }

    /**
     * @return
     */
    public Map<String, GeoDataset> getDatasets() {
        return this.datasetMap;
    }

    /**
     * @return
     */
    public Map<String, GeoPlatform> getPlatforms() {
        return this.platformMap;
    }

    /**
     * @return
     */
    public Map<String, GeoSample> getSamples() {
        return this.sampleMap;
    }

    /**
     * @return
     */
    public Map<String, GeoSeries> getSeries() {
        return this.seriesMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Parser#parse(java.io.File)
     */
    public void parse( File f ) throws IOException {
        this.parse( new FileInputStream( f ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Parser#parse(java.io.InputStream)
     */
    public void parse( InputStream is ) throws IOException {
        if ( is == null ) {
            throw new IOException( "Inputstream was null" );
        }

        if ( is.available() == 0 ) {
            throw new IOException( "No bytes to read from the input stream." );
        }

        BufferedReader dis = new BufferedReader( new InputStreamReader( is ) );

        log.info( "Parsing...." );
        String line = "";
        int count = 0;
        while ( ( line = dis.readLine() ) != null ) {
            parseLine( line );
            count++;
        }
        log.debug( "Parsed " + count + " lines." );

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Parser#parse(java.lang.String)
     */
    public void parse( String fileName ) throws IOException {
        InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( fileName );
        parse( is );
    }

    /**
     * @param target
     * @param property
     * @param value
     */
    private void addTo( Object target, String property, Object value ) {
        try {
            Method adder = target.getClass().getMethod( "addTo" + StringUtil.upperCaseFirstLetter( property ),
                    new Class[] { value.getClass() } );
            adder.invoke( target, new Object[] { value } );
        } catch ( SecurityException e ) {
            log.error( e, e );
        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
        } catch ( NoSuchMethodException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( e, e );
        }
    }

    /**
     * @param contact
     * @param property
     * @param value
     */
    private void contactSet( GeoContact contact, String property, Object value ) {
        if ( contact == null ) throw new IllegalArgumentException();
        try {
            BeanUtils.setProperty( contact, property, value );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( e, e );
        }
    }

    /**
     * @param object
     * @param property
     * @param value
     */
    private void contactSet( Object object, String property, Object value ) {
        if ( object instanceof GeoContact ) {
            contactSet( ( GeoContact ) object, property, value );
        } else if ( object instanceof GeoData ) {
            GeoContact contact = ( ( GeoData ) object ).getContact();
            contactSet( contact, property, value );
        }
    }

    /**
     * @param accession
     * @param property
     * @param value
     */
    private void datasetSet( String accession, String property, Object value ) {
        GeoDataset dataset = datasetMap.get( accession );
        if ( dataset == null ) throw new IllegalArgumentException( "Unknown dataset " + accession );
        try {
            BeanUtils.setProperty( dataset, property, value );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( InvocationTargetException e ) {
            log.error( e, e );
        }
    }

    /**
     * @param line
     * @return
     */
    private int extractChannelNumber( String line ) {
        int chIndex = line.lastIndexOf( "_ch" );
        if ( chIndex < 0 ) return 1; // that's okay, there is only one channel.
        String candidateInt = line.substring( chIndex + 3, chIndex + 4 );
        try {
            return Integer.parseInt( candidateInt );
        } catch ( NumberFormatException e ) {
            return 1;
        }
    }

    /**
     * @param map
     * @param line
     */
    private String extractKeyValue( Map<String, String> map, String line ) {
        if ( !line.startsWith( "#" ) ) throw new IllegalArgumentException( "Wrong type of line" );

        String fixed = line.substring( line.indexOf( '#' ) + 1 );

        String[] tokens = fixed.split( "=", 2 );
        if ( tokens.length != 2 ) {
            throw new IllegalArgumentException( "Wrong type of line: " + line );
        }
        String key = tokens[0];
        String value = tokens[1];
        key = StringUtils.strip( key );
        value = StringUtils.strip( value );
        log.debug( "Extracted key: " + key + ", value: " + value );
        map.put( key, value );
        return key;
    }

    /**
     * @param line
     * @return
     */
    private String extractValue( String line ) {
        int eqIndex = line.indexOf( '=' );
        if ( eqIndex < 0 ) return null;
        return StringUtils.strip( line.substring( eqIndex + 1 ) );
    }

    /**
     * @param line
     * @return
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
     * @param line
     */
    private void parseLine( String line ) {
        if ( line.length() == 0 ) return;
        if ( line.startsWith( "^" ) ) {
            if ( startsWithIgnoreCase( line, "^DATABASE" ) ) {
                inDatabase = true;
                inSubset = false;
                inDataset = false;
                inSample = false;
                inPlatform = false;
                inSeries = false;
            } else if ( startsWithIgnoreCase( line, "^SAMPLE" ) ) {
                inSample = true;
                inSubset = false;
                inDataset = false;
                inDatabase = false;
                inPlatform = false;
                inSeries = false;
                String value = extractValue( line );
                currentSampleAccession = value;
                if ( sampleMap.containsKey( value ) ) return;
                GeoSample sample = new GeoSample();
                sample.setGeoAccesssion( value );
                sampleMap.put( value, sample );
                log.debug( "In sample " + sample );
            } else if ( startsWithIgnoreCase( line, "^PLATFORM" ) ) {
                inPlatform = true;
                inSubset = false;
                inDataset = false;
                inDatabase = false;
                inSample = false;
                inSeries = false;
                String value = extractValue( line );
                currentPlatformAccession = value;
                if ( platformMap.containsKey( value ) ) return;
                GeoPlatform platform = new GeoPlatform();
                platform.setGeoAccesssion( value );
                platformMap.put( value, platform );
                log.debug( "In platform " + platform );
            } else if ( startsWithIgnoreCase( line, "^SERIES" ) ) {
                inSubset = false;
                inDataset = false;
                inSeries = true;
                inPlatform = false;
                inSample = false;
                inDatabase = false;
                String value = extractValue( line );
                currentSeriesAccession = value;
                if ( seriesMap.containsKey( value ) ) return;
                GeoSeries series = new GeoSeries();
                series.setGeoAccesssion( value );

                seriesMap.put( value, series );
                log.debug( "In series " + series );
            } else if ( startsWithIgnoreCase( line, "^DATASET" ) ) {
                inSubset = false;
                inDataset = true;
                inSeries = false;
                inPlatform = false;
                inSample = false;
                inDatabase = false;
                String value = extractValue( line );
                currentDatasetAccession = value;
                if ( datasetMap.containsKey( value ) ) return;
                GeoDataset ds = new GeoDataset();
                ds.setGeoAccesssion( value );

                datasetMap.put( value, ds );
                log.debug( "In dataset " + ds );
            } else if ( startsWithIgnoreCase( line, "^SUBSET" ) ) {
                inSubset = true;
                inDataset = false;
                inSeries = false;
                inPlatform = false;
                inSample = false;
                inDatabase = false;
                String value = extractValue( line );
                currentSubsetAccession = value;
                if ( subsetMap.containsKey( value ) ) return;
                GeoSubset ss = new GeoSubset();
                ss.setGeoAccesssion( value );

                subsetMap.put( value, ss );
                log.debug( "In subset " + ss );
            } else {
                throw new IllegalStateException( "Unknown flag: " + line );
            }
        } else {
            parseRegularLine( line );
        }
    }

    /**
     * @param line
     */
    private void parsePlatformLine( String line ) {
        String[] tokens = line.split( FIELD_DELIM );

        Map<String, List<String>> platformDataMap = platformMap.get( currentPlatformAccession ).getData();

        if ( platformMap.get( currentPlatformAccession ).getColumnNames().size() != tokens.length ) {
            log.error( "Incorrect number of tokens from '" + line + "' (" + tokens.length + ", expected "
                    + platformMap.get( currentPlatformAccession ).getColumnNames().size() + ")" );
            return;
        }

        for ( int i = 0; i < tokens.length; i++ ) {
            String token = tokens[i];
            String columnName = platformMap.get( currentPlatformAccession ).getColumnNames().get( i );
            if ( !platformDataMap.containsKey( columnName ) ) {
                platformDataMap.put( columnName, new ArrayList<String>() );
            }
            platformDataMap.get( columnName ).add( token );
        }
    }

    /**
     * @param line
     */
    private void parseRegularLine( String line ) {
        if ( line.startsWith( "!" ) ) {
            String value = extractValue( line );
            if ( inSample ) {
                if ( startsWithIgnoreCase( line, "!sample_table_begin" ) ) {
                    inSampleTable = true;
                } else if ( startsWithIgnoreCase( line, "!sample_table_end" ) ) {
                    inSampleTable = false;
                } else if ( startsWithIgnoreCase( line, "!Sample_title" ) ) {
                    sampleSet( currentSampleAccession, "title", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_geo_accession" ) ) {
                    currentSampleAccession = value;
                    sampleMap.put( currentSampleAccession, new GeoSample() );
                } else if ( startsWithIgnoreCase( line, "!Sample_status" ) ) {
                    sampleSet( currentSampleAccession, "status", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_submission_date" ) ) {
                    sampleSet( currentSampleAccession, "submissionDate", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_type" ) ) {
                    sampleSet( currentSampleAccession, "type", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_channel_count" ) ) {
                    int numExtraChannelsNeeded = Integer.parseInt( value ) - 1;
                    for ( int i = 0; i < numExtraChannelsNeeded; i++ ) {
                        sampleMap.get( currentSampleAccession ).addChannel();
                    }
                    sampleSet( currentSampleAccession, "channelCount", new Integer( Integer.parseInt( value ) ) );
                } else if ( startsWithIgnoreCase( line, "!Sample_source_name" ) ) {
                    int channel = extractChannelNumber( line );
                    sampleChannelSet( currentSampleAccession, "sourceName", channel, value );
                } else if ( startsWithIgnoreCase( line, "!Sample_organism" ) ) {
                    int channel = extractChannelNumber( line );
                    sampleChannelSet( currentSampleAccession, "organism", channel, value );
                } else if ( startsWithIgnoreCase( line, "!Sample_biomaterial_provider" ) ) {
                    int channel = extractChannelNumber( line );
                    sampleChannelSet( currentSampleAccession, "bioMaterialProvider", channel, value );
                } else if ( startsWithIgnoreCase( line, "!Sample_treatment_protocol" ) ) {
                    int channel = extractChannelNumber( line );
                    sampleChannelAddTo( currentSampleAccession, "treatmentProtocol", channel, value );
                } else if ( startsWithIgnoreCase( line, "!Sample_molecule" ) ) {
                    int channel = extractChannelNumber( line );
                    sampleChannelSet( currentSampleAccession, "molecule", channel, value );
                } else if ( startsWithIgnoreCase( line, "!Sample_growth_protocol" ) ) {
                    int channel = extractChannelNumber( line );
                    sampleChannelAddTo( currentSampleAccession, "growthProtocol", channel, value );
                } else if ( startsWithIgnoreCase( line, "!sample_extract_protocol" ) ) {
                    int channel = extractChannelNumber( line );
                    sampleChannelAddTo( currentSampleAccession, "extractProtocol", channel, value );
                } else if ( startsWithIgnoreCase( line, "!Sample_hyb_protocol" ) ) {
                    sampleAddTo( currentSampleAccession, "hybProtocol", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_scan_protocol" ) ) {
                    sampleAddTo( currentSampleAccession, "scanProtocol", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_data_processing" ) ) {
                    sampleAddTo( currentSampleAccession, "dataProcessing", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_description" ) ) {
                    sampleAddTo( currentSampleAccession, "description", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_label" ) ) {
                    int channel = extractChannelNumber( line );
                    sampleChannelSet( currentSampleAccession, "label", channel, value );
                } else if ( startsWithIgnoreCase( line, "!Sample_characteristics" ) ) {
                    int channel = extractChannelNumber( line );
                    GeoSample sample = sampleMap.get( currentSampleAccession );
                    sample.getChannel( channel ).addCharacteristic( value );
                } else if ( startsWithIgnoreCase( line, "!Sample_platform_id" ) ) {
                    sampleSet( currentSampleAccession, "id", value );
                    if ( platformMap.containsKey( value ) ) {
                        sampleMap.get( currentSampleAccession ).addPlatform( platformMap.get( value ) );
                    }
                } else if ( startsWithIgnoreCase( line, "!Sample_contact_name" ) ) {
                    sampleContactSet( currentSampleAccession, "name", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_contact_email" ) ) {
                    sampleContactSet( currentSampleAccession, "email", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_contact_institute" ) ) {
                    sampleContactSet( currentSampleAccession, "institute", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_contact_laboratory" ) ) {
                    sampleContactSet( currentSampleAccession, "laboratory", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_contact_department" ) ) {
                    sampleContactSet( currentSampleAccession, "department", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_contact_address" ) ) {
                    sampleContactSet( currentSampleAccession, "address", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_contact_city" ) ) {
                    sampleContactSet( currentSampleAccession, "city", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_contact_phone" ) ) {
                    sampleContactSet( currentSampleAccession, "phone", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_contact_web_link" ) ) {
                    sampleContactSet( currentSampleAccession, "webLink", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_contact_fax" ) ) {
                    sampleContactSet( currentSeriesAccession, "fax", value );
                } else if ( startsWithIgnoreCase( line, "!Sample_series_id" ) ) {
                    if ( seriesMap.containsKey( value ) ) {
                        this.seriesMap.get( value ).addSample( this.sampleMap.get( currentSampleAccession ) );
                    }
                    seriesSet( currentSeriesAccession, "seriesId", value ); // can be many?
                } else {
                    throw new IllegalStateException( "Unknown flag: " + line );
                }

            } else if ( inSeries ) {
                if ( startsWithIgnoreCase( line, "!Series_title" ) ) {
                    seriesSet( currentSeriesAccession, "title", value );
                } else if ( startsWithIgnoreCase( line, "!Series_geo_accession" ) ) {
                    currentSeriesAccession = value;
                } else if ( startsWithIgnoreCase( line, "!Series_status" ) ) {
                    seriesSet( currentSeriesAccession, "status", value );
                } else if ( startsWithIgnoreCase( line, "!Series_submission_date" ) ) {
                    seriesSet( currentSeriesAccession, "submissionDate", value );
                } else if ( startsWithIgnoreCase( line, "!Series_pubmed_id" ) ) {
                    seriesAddTo( currentSeriesAccession, "pubmedIds", value );
                } else if ( startsWithIgnoreCase( line, "!Series_summary" ) ) {
                    seriesAddTo( currentSeriesAccession, "summary", value );
                } else if ( startsWithIgnoreCase( line, "!Series_type" ) ) {
                    seriesSet( currentSeriesAccession, "type", value );
                } else if ( startsWithIgnoreCase( line, "!Series_contributor" ) ) {
                    seriesMap.get( currentSeriesAccession ).getContributers().add( value );
                } else if ( startsWithIgnoreCase( line, "!Series_sample_id" ) ) {
                    seriesMap.get( currentSeriesAccession ).getSampleIds().add( value );
                    if ( !this.sampleMap.containsKey( value ) ) {
                        log.debug( "New sample: " + value );
                        GeoSample sample = new GeoSample();
                        sample.setGeoAccesssion( value );
                    }
                    log.debug( "Adding sample: " + value + " to series " + currentSeriesAccession );
                    this.seriesMap.get( currentSeriesAccession ).addSample( this.sampleMap.get( value ) );

                } else if ( startsWithIgnoreCase( line, "!Series_contact_name" ) ) {
                    seriesContactSet( currentSeriesAccession, "name", value );
                } else if ( startsWithIgnoreCase( line, "!Series_contact_email" ) ) {
                    seriesContactSet( currentSeriesAccession, "email", value );
                } else if ( startsWithIgnoreCase( line, "!Series_contact_institute" ) ) {
                    seriesContactSet( currentSeriesAccession, "institute", value );
                } else if ( startsWithIgnoreCase( line, "!Series_contact_laboratory" ) ) {
                    seriesContactSet( currentSeriesAccession, "laboratory", value );
                } else if ( startsWithIgnoreCase( line, "!Series_contact_department" ) ) {
                    seriesContactSet( currentSeriesAccession, "department", value );
                } else if ( startsWithIgnoreCase( line, "!Series_contact_address" ) ) {
                    seriesContactSet( currentSeriesAccession, "address", value );
                } else if ( startsWithIgnoreCase( line, "!Series_contact_city" ) ) {
                    seriesContactSet( currentSeriesAccession, "city", value );
                } else if ( startsWithIgnoreCase( line, "!Series_contact_phone" ) ) {
                    seriesContactSet( currentSeriesAccession, "phone", value );
                } else if ( startsWithIgnoreCase( line, "!Series_contact_fax" ) ) {
                    seriesContactSet( currentSeriesAccession, "fax", value );
                } else if ( startsWithIgnoreCase( line, "!Series_contact_web_link" ) ) {
                    seriesContactSet( currentSeriesAccession, "webLink", value );
                } else if ( startsWithIgnoreCase( line, "!series_platform_id" ) ) {
                    seriesSet( currentSeriesAccession, "platformId", value );
                } else if ( startsWithIgnoreCase( line, "!series_table_begin" ) ) {
                    inSeriesTable = true;
                } else if ( startsWithIgnoreCase( line, "!series_table_end" ) ) {
                    inSeriesTable = false;
                } else if ( startsWithIgnoreCase( line, "!Series_variable_" ) ) {
                    int variable = extractVariableNumber( line );
                    seriesMap.get( currentSeriesAccession ).addToVariables( new GeoVariable() );
                } else if ( startsWithIgnoreCase( line, "!Series_variable_description_" ) ) {
                    int variable = extractVariableNumber( line );
                    // FIXME
                } else if ( startsWithIgnoreCase( line, "!Series_variable_sample_list_" ) ) {
                    int variable = extractVariableNumber( line );
                    // FIXME
                } else if ( startsWithIgnoreCase( line, "!Series_variable_repeats_" ) ) {
                    int variable = extractVariableNumber( line );
                    // FIXME
                } else if ( startsWithIgnoreCase( line, "!Series_variable_repeats_sample_list" ) ) {
                    int variable = extractVariableNumber( line );
                    // FIXME
                } else {
                    throw new IllegalStateException( "Unknown flag: " + line );
                }
            } else if ( inDatabase ) {
                // we are going to ignore these lines.
            } else if ( inPlatform ) {
                if ( startsWithIgnoreCase( line, "!Platform_title" ) ) {
                    platformSet( currentPlatformAccession, "title", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_geo_accession" ) ) {
                    currentPlatformAccession = value;
                } else if ( startsWithIgnoreCase( line, "!Platform_status" ) ) {
                    platformSet( currentPlatformAccession, "status", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_manufacturer" ) ) {
                    platformSet( currentPlatformAccession, "manufacturer", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_manufacture_protocol" ) ) {
                    platformSet( currentPlatformAccession, "manufactureProtocol", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_submission_date" ) ) {
                    platformSet( currentPlatformAccession, "submissionDate", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_technology" ) ) {
                    platformSet( currentPlatformAccession, "technology", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_distribution" ) ) {
                    platformSet( currentPlatformAccession, "distribution", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_organism" ) ) {
                    platformSet( currentPlatformAccession, "organism", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_description" ) ) {
                    platformAddTo( currentPlatformAccession, "description", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_contact_name" ) ) {
                    platformContactSet( currentPlatformAccession, "name", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_contact_email" ) ) {
                    platformContactSet( currentPlatformAccession, "email", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_contact_institute" ) ) {
                    platformContactSet( currentPlatformAccession, "institute", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_contact_laboratory" ) ) {
                    platformContactSet( currentPlatformAccession, "laboratory", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_contact_department" ) ) {
                    platformContactSet( currentPlatformAccession, "department", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_contact_address" ) ) {
                    platformContactSet( currentPlatformAccession, "address", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_contact_city" ) ) {
                    platformContactSet( currentPlatformAccession, "city", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_contact_phone" ) ) {
                    platformContactSet( currentPlatformAccession, "phone", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_contact_web_link" ) ) {
                    platformContactSet( currentPlatformAccession, "webLink", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_contact_fax" ) ) {
                    platformContactSet( currentSeriesAccession, "fax", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_web_link" ) ) {
                    platformSet( currentPlatformAccession, "webLink", value );
                } else if ( startsWithIgnoreCase( line, "!Platform_sample_id" ) ) {
                    platformSet( currentPlatformAccession, "id", value );
                } else if ( startsWithIgnoreCase( line, "!platform_table_begin" ) ) {
                    inPlatformTable = true;
                } else if ( startsWithIgnoreCase( line, "!platform_table_end" ) ) {
                    inPlatformTable = false;
                } else if ( startsWithIgnoreCase( line, "!Platform_series_id" ) ) {
                    // no-op?
                } else {
                    throw new IllegalStateException( "Unknown flag: " + line );
                }
            } else if ( inDataset ) {
                if ( startsWithIgnoreCase( line, "!Dataset_title" ) ) {
                    datasetSet( currentDatasetAccession, "title", value );
                } else if ( startsWithIgnoreCase( line, "!dataset_description" ) ) {
                    datasetSet( currentDatasetAccession, "title", value );
                } else if ( startsWithIgnoreCase( line, "!dataset_maximum_probes" ) ) {
                    datasetSet( currentDatasetAccession, "numProbes", value );
                } else if ( startsWithIgnoreCase( line, "!dataset_order" ) ) {
                    datasetSet( currentDatasetAccession, "order", value );
                } else if ( startsWithIgnoreCase( line, "!dataset_organism" ) ) {
                    datasetSet( currentDatasetAccession, "organism", value );
                } else if ( startsWithIgnoreCase( line, "!dataset_platform" ) ) {
                    if ( !platformMap.containsKey( value ) ) {
                        platformMap.put( value, new GeoPlatform() );
                        platformMap.get( value ).setGeoAccesssion( value );
                    }
                    datasetMap.get( currentDatasetAccession ).setPlatform( platformMap.get( value ) );
                } else if ( startsWithIgnoreCase( line, "!dataset_probe_type" ) ) {
                    datasetSet( currentDatasetAccession, "probeType", value );
                } else if ( startsWithIgnoreCase( line, "!dataset_reference_series" ) ) {
                    if ( !seriesMap.containsKey( value ) ) {
                        log.debug( "Adding series " + value );
                        seriesMap.put( value, new GeoSeries() );
                        seriesMap.get( value ).setGeoAccesssion( value );
                    }
                    datasetMap.get( currentDatasetAccession ).addSeries( seriesMap.get( value ) );
                } else if ( startsWithIgnoreCase( line, "!dataset_total_samples" ) ) {
                    datasetSet( currentDatasetAccession, "numSamples", value );
                } else if ( startsWithIgnoreCase( line, "!dataset_update_date" ) ) {
                    datasetSet( currentDatasetAccession, "updateDate", value );
                } else if ( startsWithIgnoreCase( line, "!dataset_value_type" ) ) {
                    datasetSet( currentDatasetAccession, "valueType", value );
                } else if ( startsWithIgnoreCase( line, "!dataset_completeness" ) ) {
                    datasetSet( currentDatasetAccession, "completeness", value );
                } else if ( startsWithIgnoreCase( line, "!dataset_experiment_type" ) ) {
                    datasetSet( currentDatasetAccession, "experimentType", value );
                } else {
                    throw new IllegalStateException( "Unknown flag: " + line );
                }
            } else if ( inSubset ) {
                if ( startsWithIgnoreCase( line, "!Dataset_title" ) ) {
                    subsetSet( currentSubsetAccession, "title", value );
                } else if ( startsWithIgnoreCase( line, "!subset_dataset_id" ) ) {
                    subsetSet( currentSubsetAccession, "dataSet", value );
                } else if ( startsWithIgnoreCase( line, "!subset_description" ) ) {
                    subsetAddTo( currentSubsetAccession, "description", value );
                } else if ( startsWithIgnoreCase( line, "!subset_sample_id" ) ) {
                    String[] values = value.split( "," );
                    for ( int i = 0; i < values.length; i++ ) {
                        String v = values[i];
                        if ( !sampleMap.containsKey( v ) ) {
                            log.debug( "New sample: " + v );
                            sampleMap.put( v, new GeoSample() );
                            sampleMap.get( v ).setGeoAccesssion( v );
                        }
                        log.debug( "Adding sample: " + v + " to subset " + currentSubsetAccession );
                        subsetMap.get( currentSubsetAccession ).setSample( sampleMap.get( v ) );
                    }

                } else if ( startsWithIgnoreCase( line, "!subset_type" ) ) {
                    subsetSet( currentSubsetAccession, "type", value );
                } else {
                    throw new IllegalStateException( "Unknown flag: " + line );
                }
            } else {
                throw new IllegalStateException( "Unknown flag: " + line );
            }
        } else if ( line.startsWith( "#" ) ) {
            inDatasetTable = false;
            if ( inPlatform ) {
                platformMap.get( currentPlatformAccession ).getColumnNames().add(
                        extractKeyValue( platformColumns, line ) );
            } else if ( inSample ) {
                sampleMap.get( currentSampleAccession ).getColumnNames().add( extractKeyValue( sampleColumns, line ) );
            } else if ( inSeries ) {
                seriesMap.get( currentSeriesAccession ).getColumnNames().add( extractKeyValue( seriesColumns, line ) );
            } else if ( inSubset ) {
                // nothing.
            } else if ( inDataset ) {
                inDatasetTable = true;
                datasetMap.get( currentDatasetAccession ).getColumnNames()
                        .add( extractKeyValue( datasetColumns, line ) );
            } else {
                throw new IllegalStateException( "Wrong state to deal with '" + line + "'" );
            }
        } else {
            if ( inPlatformTable ) {
                parsePlatformLine( line );
            } else if ( inSampleTable ) {
                parseSampleDataLine( line );
            } else if ( inSeriesTable ) {
                parseSeriesDataLine( line );
            } else if ( inDatasetTable ) {
            } else {
                throw new IllegalStateException( "Wrong state to deal with '" + line + "'" );
            }
        }

    }

    /**
     * @param line
     */
    private void parseSampleDataLine( String line ) {
        String[] tokens = line.split( FIELD_DELIM );

        Map<String, List<String>> sampleDataMap = sampleMap.get( currentSampleAccession ).getData();

        for ( int i = 0; i < tokens.length; i++ ) {
            String token = tokens[i];
            String columnName = sampleMap.get( currentSampleAccession ).getColumnNames().get( i );
            if ( !sampleDataMap.containsKey( columnName ) ) {
                sampleDataMap.put( columnName, new ArrayList<String>() );
            }
            sampleDataMap.get( columnName ).add( token );
        }
    }

    /**
     * @param line
     */
    private void parseSeriesDataLine( String line ) {
        String[] tokens = line.split( FIELD_DELIM );

        Map<String, List<String>> seriesDataMap = seriesMap.get( currentSeriesAccession ).getData();

        for ( int i = 0; i < tokens.length; i++ ) {
            String token = tokens[i];
            String columnName = seriesMap.get( currentSeriesAccession ).getColumnNames().get( i );
            if ( !seriesDataMap.containsKey( columnName ) ) {
                seriesDataMap.put( columnName, new ArrayList<String>() );
            }
            seriesDataMap.get( columnName ).add( token );
        }
    }

    /**
     * @param accession
     * @param property
     * @param value
     */
    private void platformAddTo( String accession, String property, Object value ) {
        GeoPlatform platform = platformMap.get( accession );
        if ( platform == null ) throw new IllegalArgumentException( "Unknown platform " + accession );
        addTo( platform, property, value );
    }

    /**
     * @param accession
     * @param property
     * @param value
     */
    private void platformContactSet( String accession, String property, Object value ) {
        GeoPlatform platform = platformMap.get( accession );
        contactSet( platform, property, value );
    }

    /**
     * @param accession
     * @param property
     * @param value
     */
    private void platformSet( String accession, String property, Object value ) {
        GeoPlatform platform = platformMap.get( accession );
        if ( platform == null ) throw new IllegalArgumentException( "Unknown platform " + accession );
        try {
            BeanUtils.setProperty( platform, property, value );
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
        } catch ( InvocationTargetException e ) {
            e.printStackTrace();
        }
    }

    /**
     * @param currentSampleAccession2
     * @param string
     * @param value
     */
    private void sampleAddTo( String accession, String property, Object value ) {
        GeoSample sample = sampleMap.get( accession );
        if ( sample == null ) throw new IllegalArgumentException( "Unknown sample " + accession );
        addTo( sample, property, value );
    }

    /**
     * @param currentSampleAccession2
     * @param string
     * @param channel
     * @param value
     */
    private void sampleChannelAddTo( String sampleAccession, String property, int channel, String value ) {
        GeoSample sample = sampleMap.get( sampleAccession );
        this.addTo( sample.getChannel( channel ), property, value );
    }

    /**
     * @param currentSampleAccession2
     * @param string
     * @param channel
     * @param value
     */
    private void sampleChannelSet( String sampleAccession, String property, int channel, String value ) {
        GeoSample sample = sampleMap.get( sampleAccession );
        try {
            BeanUtils.setProperty( sample.getChannel( channel ), property, value );
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
        } catch ( InvocationTargetException e ) {
            e.printStackTrace();
        }
    }

    /**
     * @param accession
     * @param property
     * @param value
     */
    private void sampleContactSet( String accession, String property, Object value ) {
        GeoSample sample = sampleMap.get( accession );
        contactSet( sample, property, value );
    }

    /**
     * @param accession
     * @param property
     * @param value
     */
    private void sampleSet( String accession, String property, Object value ) {
        GeoSample sample = sampleMap.get( accession );
        if ( sample == null ) throw new IllegalArgumentException( "Unknown sample " + accession );
        try {
            BeanUtils.setProperty( sample, property, value );
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
        } catch ( InvocationTargetException e ) {
            e.printStackTrace();
        }
    }

    /**
     * @param accession
     * @param property
     * @param value
     */
    private void seriesAddTo( String accession, String property, Object value ) {
        GeoSeries series = seriesMap.get( accession );
        if ( series == null ) throw new IllegalArgumentException( "Unknown series " + accession );
        addTo( series, property, value );
    }

    /**
     * @param accession
     * @param property
     * @param value
     */
    private void seriesContactSet( String accession, String property, Object value ) {
        GeoSeries series = seriesMap.get( accession );
        contactSet( series, property, value );
    }

    /**
     * @param accession
     * @param property
     * @param value
     */
    private void seriesSet( String accession, String property, Object value ) {
        GeoSeries series = seriesMap.get( accession );
        if ( series == null ) throw new IllegalArgumentException( "Unknown series " + accession );
        try {
            BeanUtils.setProperty( series, property, value );
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
        } catch ( InvocationTargetException e ) {
            e.printStackTrace();
        }
    }

    /**
     * @param line
     * @param string
     * @return
     */
    private boolean startsWithIgnoreCase( String string, String pattern ) {
        return string.toUpperCase().startsWith( pattern.toUpperCase() );
    }

    /**
     * @param accession
     * @param property
     * @param value
     */
    private void subsetAddTo( String accession, String property, Object value ) {
        GeoSubset subset = subsetMap.get( accession );
        if ( subset == null ) throw new IllegalArgumentException( "Unknown subset " + accession );
        addTo( subset, property, value );
    }

    /**
     * @param accession
     * @param property
     * @param value
     */
    private void subsetSet( String accession, String property, Object value ) {
        GeoSubset subset = subsetMap.get( accession );
        if ( subset == null ) throw new IllegalArgumentException( "Unknown subset " + accession );
        try {
            BeanUtils.setProperty( subset, property, value );
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
        } catch ( InvocationTargetException e ) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Parser#getResults()
     */
    public Collection<Object> getResults() {
        // TODO Auto-generated method stub // FIXME
        throw new UnsupportedOperationException();
    }

}
