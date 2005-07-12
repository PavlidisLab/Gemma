package edu.columbia.gemma.loader.expression.geo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.util.StringUtil;

import edu.columbia.gemma.loader.expression.geo.model.GeoContact;
import edu.columbia.gemma.loader.expression.geo.model.GeoData;
import edu.columbia.gemma.loader.expression.geo.model.GeoPlatform;
import edu.columbia.gemma.loader.expression.geo.model.GeoSample;
import edu.columbia.gemma.loader.expression.geo.model.GeoSeries;

public class GeoFamilyParser {

    /**
     * 
     */
    private static final String FIELD_DELIM = "\t";

    private static Log log = LogFactory.getLog( GeoFamilyParser.class.getName() );

    private boolean inDatabase = false;
    private boolean inSample = false;
    private boolean inPlatform = false;
    private boolean inSeries = false;
    private boolean inPlatformTable = false;
    private boolean inSampleTable = false;
    private boolean inSeriesTable = false;

    private Map<String, GeoSample> sampleMap;
    private String currentPlatformAccession;
    private String currentSeriesAccession;
    private String currentSampleAccession;
    private Map<String, GeoPlatform> platformMap;
    private Map<String, GeoSeries> seriesMap;

    // TODO: Move all of these into the respective objects.
    private Map<String, String> platformColumns;
    private Map<String, String> sampleColumns;
    private Map<String, String> seriesColumns;

    public Map getSamples() {
        return this.sampleMap;
    }

    public Map getPlatforms() {
        return this.platformMap;
    }

    public Map getSeries() {
        return this.seriesMap;
    }

    public GeoFamilyParser() {
        sampleMap = new HashMap<String, GeoSample>();
        platformMap = new HashMap<String, GeoPlatform>();
        seriesMap = new HashMap<String, GeoSeries>();
        platformColumns = new HashMap<String, String>();
        sampleColumns = new HashMap<String, String>();
    }

    /**
     * @param is
     * @throws IOException
     */
    public void parse( InputStream is ) throws IOException {
        if ( is == null ) {
            throw new IOException( "Inputstream was null" );
        }

        if ( is.available() == 0 ) {
            throw new IOException( "No bytes to read from the annotation file." );
        }

        BufferedReader dis = new BufferedReader( new InputStreamReader( is ) );

        String line = "";
        while ( ( line = dis.readLine() ) != null ) {
            parseLine( line );
        }
    }

    /**
     * @param line
     */
    private void parseLine( String line ) {
        if ( line.length() == 0 ) return;
        if ( line.startsWith( "^" ) ) {
            if ( startsWithIgnoreCase( line, "^DATABASE" ) ) {
                inDatabase = true;
                inSample = false;
                inPlatform = false;
                inSeries = false;
            } else if ( startsWithIgnoreCase( line, "^SAMPLE" ) ) {
                inSample = true;
                inDatabase = false;
                inPlatform = false;
                inSeries = false;
                String value = extractValue( line );
                currentSampleAccession = value;
                sampleMap.put( value, new GeoSample() );
                log.debug( "In sample" );
            } else if ( startsWithIgnoreCase( line, "^PLATFORM" ) ) {
                inPlatform = true;
                inDatabase = false;
                inSample = false;
                inSeries = false;
                String value = extractValue( line );
                currentPlatformAccession = value;
                platformMap.put( value, new GeoPlatform() );
                log.debug( "In platform" );
            } else if ( startsWithIgnoreCase( line, "^SERIES" ) ) {
                inSeries = true;
                inPlatform = false;
                inSample = false;
                inDatabase = false;
                String value = extractValue( line );
                currentSeriesAccession = value;
                seriesMap.put( value, new GeoSeries() );
                log.debug( "In series" );
            } else {
                throw new IllegalStateException( "Unknown flag: " + line );
            }
        } else {
            parseRegularLine( line );
        }
    }

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
                    sampleSet( currentSampleAccession, "channelCount", Integer.parseInt( value ) );
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
                    seriesSet( currentSeriesAccession, "seriesId", value ); // can be many?
                } else {
                    throw new IllegalStateException( "Unknown flag: " + line );
                }

            } else if ( inSeries ) {
                if ( startsWithIgnoreCase( line, "!Series_title" ) ) {

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
                } else {
                    throw new IllegalStateException( "Unknown flag: " + line );
                }
            } else if ( inDatabase ) {
                // we are going to ignore these lines.
            } else if ( inPlatform ) {
                if ( startsWithIgnoreCase( line, "!Platform_title" ) ) {
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
            }
        } else if ( line.startsWith( "#" ) ) {
            if ( inPlatform ) {
                platformMap.get( currentPlatformAccession ).getColumnNames().add(
                        extractKeyValue( platformColumns, line ) );
            } else if ( inSample ) {
                sampleMap.get( currentSampleAccession ).getColumnNames().add( extractKeyValue( sampleColumns, line ) );
            } else if ( inSeries ) {
                seriesMap.get( currentSeriesAccession ).getColumnNames().add( extractKeyValue( seriesColumns, line ) );
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
            } else {
                throw new IllegalStateException( "Wrong state to deal with '" + line + "'" );
            }
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
        value = StringUtils.strip( key );
        log.debug( "Extracted key: " + key );
        map.put( key, value );
        return key;
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
     * @param line
     */
    private void parsePlatformLine( String line ) {
        String[] tokens = line.split( FIELD_DELIM );

        Map<String, List<String>> platformDataMap = platformMap.get( currentPlatformAccession ).getData();

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
            e.printStackTrace();
        } catch ( IllegalArgumentException e ) {
            e.printStackTrace();
        } catch ( NoSuchMethodException e ) {
            e.printStackTrace();
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

    private void platformContactSet( String accession, String property, Object value ) {
        GeoPlatform platform = platformMap.get( accession );
        contactSet( platform, property, value );
    }

    private void seriesContactSet( String accession, String property, Object value ) {
        GeoSeries series = seriesMap.get( accession );
        contactSet( series, property, value );
    }

    private void sampleContactSet( String accession, String property, Object value ) {
        GeoSample sample = sampleMap.get( accession );
        contactSet( sample, property, value );
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
     * @param contact
     * @param property
     * @param value
     */
    private void contactSet( GeoContact contact, String property, Object value ) {
        if ( contact == null ) throw new IllegalArgumentException();
        try {
            BeanUtils.setProperty( contact, property, value );
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
     * @param line
     * @return
     */
    private String extractValue( String line ) {
        int eqIndex = line.indexOf( '=' );
        if ( eqIndex < 0 ) return null;
        return StringUtils.strip( line.substring( eqIndex + 1 ) );
    }

}
