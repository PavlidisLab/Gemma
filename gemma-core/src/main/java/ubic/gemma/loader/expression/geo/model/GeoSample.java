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
package ubic.gemma.loader.expression.geo.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a sample (GSM) in GEO. The channels correspond to BioMaterials; the sample itself corresponds to a
 * BioAssay in Gemma. Some fields are only relevant for SAGE.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoSample extends GeoData implements Comparable {

    private static Log log = LogFactory.getLog( GeoSample.class.getName() );

    private List<GeoChannel> channels;
    private String hybProtocol = "";
    private String dataProcessing = "";
    private String scanProtocol = "";
    private String description = "";
    private String type = "DNA";
    private String supplementaryFile = "";
    private String lastUpdateDate = "";

    /**
     * This is used to store the title for the sample as found in the GDS file, if it differs from the one in the GSE
     * file
     */
    private String titleInDataset = null;

    private boolean isGenePix = false;

    /**
     * These are ignored by the Gemma system so don't need to be parsed.
     */
    private static Collection<String> skippableQuantitationTypes = new HashSet<String>();

    static {

        // These are from GenePix files. In Stanford files they are named differently than described here:
        // http://www.moleculardevices.com/pages/software/gn_genepix_file_formats.html
        // these are location and spot size information.
        skippableQuantitationTypes.add( "X_COORD" );
        skippableQuantitationTypes.add( "Y_COORD" );
        skippableQuantitationTypes.add( "TOP" );
        skippableQuantitationTypes.add( "BOT" );
        skippableQuantitationTypes.add( "LEFT" );
        skippableQuantitationTypes.add( "RIGHT" );
        skippableQuantitationTypes.add( "DIAMETER" );
        skippableQuantitationTypes.add( "TOT_SPIX" );
        skippableQuantitationTypes.add( "TOT_BPIX" );

        // the following are background-subtracted values that can be easily computed from the raw values
        skippableQuantitationTypes.add( "CH1D_MEAN" );
        skippableQuantitationTypes.add( "CH2D_MEAN" );
        skippableQuantitationTypes.add( "CH1D_MEDIAN" );
        skippableQuantitationTypes.add( "CH2D_MEDIAN" );

        // All the raw 'mean' items are skippable.
        skippableQuantitationTypes.add( "CH1I_MEAN" );
        skippableQuantitationTypes.add( "CH2I_MEAN" );
        skippableQuantitationTypes.add( "CH1B_MEAN" );
        skippableQuantitationTypes.add( "CH2B_MEAN" );
        skippableQuantitationTypes.add( "SUM_MEAN" );
        skippableQuantitationTypes.add( "RAT1_MEAN" );
        skippableQuantitationTypes.add( "RAT2_MEAN" );
        skippableQuantitationTypes.add( "PIX_RAT2_MEAN" );
        skippableQuantitationTypes.add( "CH1IN_MEAN" );
        skippableQuantitationTypes.add( "CH2IN_MEAN" );
        skippableQuantitationTypes.add( "UNF_VALUE" );
        skippableQuantitationTypes.add( "VALUE" );

        // otherwise deemed skippable.
        skippableQuantitationTypes.add( "PERGTBCH1I_1SD" );
        skippableQuantitationTypes.add( "PERGTBCH2I_1SD" );
        skippableQuantitationTypes.add( "PERGTBCH1I_2SD" );
        skippableQuantitationTypes.add( "PERGTBCH2I_2SD" );

    }

    Collection<GeoPlatform> platforms;

    Collection<GeoReplication> replicates;
    Collection<GeoVariable> variables;

    Map<String, Class> quantitationTypeRepresentations = new HashMap<String, Class>();

    /**
     * quantitationType -> designelement -> value
     */
    Map<String, Map<String, Object>> data = new HashMap<String, Map<String, Object>>();

    /* Ignore some data types */
    private boolean shouldAdd( String columnName ) {
        return !( isGenePix && skippableQuantitationTypes.contains( columnName ) );
    }

    /**
     * @param designElement The probe name
     * @param quantitationType The column name used in the SOFT file.
     * @param value The data point to be stored.
     */
    public void addDatum( String designElement, String quantitationType, String value ) {

        if ( !shouldAdd( quantitationType ) ) {
            return;
        }

        if ( !data.containsKey( quantitationType ) ) {
            data.put( quantitationType, new HashMap<String, Object>() );
            inferRepresentation( quantitationType );
            // getColumnNames().add( quantitationType );
        }

        Object convertedValue = convertValue( quantitationType, value );

        /*
         * Make sure we aren't getting duplicate data. This happens in some corrupt files?
         */
        if ( data.get( quantitationType ).containsKey( designElement ) ) {
            throw new IllegalStateException( "There is already a datum for " + designElement + " in "
                    + quantitationType );
        }
        data.get( quantitationType ).put( designElement, convertedValue );
    }

    private Object convertValue( String quantitationType, String value ) {
        Object convertedValue = value; // if it is a string
        Class representation = quantitationTypeRepresentations.get( quantitationType );

        try {
            if ( representation.equals( Double.class ) ) {
                convertedValue = Double.parseDouble( value );
            } else if ( representation.equals( Integer.class ) ) {
                convertedValue = Integer.parseInt( value );
            } else if ( representation.equals( Boolean.class ) ) {
                convertedValue = Boolean.parseBoolean( value );
            }
        } catch ( NumberFormatException e ) {
            convertedValue = handleMissing( representation );
        }
        return convertedValue;
    }

    private Object handleMissing( Class representation ) {
        if ( representation.equals( Double.class ) ) {
            return Double.NaN;
        } else if ( representation.equals( Integer.class ) ) {
            return 0;
        } else if ( representation.equals( Boolean.class ) ) {
            return false;
        } else {
            throw new IllegalArgumentException( "Don't know how to deal with a missing " + representation );
        }
    }

    /**
     * @param quantitationType
     */
    private void inferRepresentation( String quantitationType ) {
        Class representation = Double.class;

        if ( quantitationType.contains( "Probe ID" ) || quantitationType.equalsIgnoreCase( "Probe Set ID" ) ) {
            /*
             * special case...not a quantitation type.
             */
            representation = String.class;
        } else if ( quantitationType.matches( "ABS_CALL" ) ) {
            representation = String.class;
        }

        quantitationTypeRepresentations.put( quantitationType, representation );
    }

    /**
     * Given a column number (count starts from zero) get the name of the corresponding quantitation type for this
     * sample.
     * 
     * @param n
     * @return column name.
     */
    public String getNthQuantitationType( int n ) {
        if ( n < 0 || n > getColumnNames().size() - 1 ) {
            throw new IllegalArgumentException( "Only " + getColumnNames().size() + " columns, requested index " + n );
        }
        return getColumnNames().get( n );
    }

    /**
     * @param designElement
     * @param quantitationType
     * @return
     */
    public Object getDatum( String designElement, String quantitationType ) {
        if ( !data.containsKey( quantitationType ) ) {
            // throw new IllegalArgumentException( "No such quantitation type \"" + quantitationType + "\"" );
            return null;
        }
        return data.get( quantitationType ).get( designElement );
    }

    /**
     * @param designElement
     * @param columnNumber
     * @return
     */
    public Object getDatum( String designElement, int columnNumber ) {
        String quantitationType = getNthQuantitationType( columnNumber );
        return this.getDatum( designElement, quantitationType );
    }

    // SAGE items.
    String anchor;
    int tagCount;
    int tagLength;

    public GeoSample() {
        channels = new ArrayList<GeoChannel>();
        this.addChannel();
        contact = new GeoContact();
        platforms = new HashSet<GeoPlatform>();
        replicates = new HashSet<GeoReplication>();
        variables = new HashSet<GeoVariable>();
    }

    public void addPlatform( GeoPlatform platform ) {
        this.platforms.add( platform );
    }

    public Collection<GeoPlatform> getPlatforms() {
        return this.platforms;
    }

    /**
     * @return Returns the anchor. (SAGE)
     */
    public String getAnchor() {
        return this.anchor;
    }

    /**
     * @param anchor The anchor to set. (SAGE)
     */
    public void setAnchor( String anchor ) {
        this.anchor = anchor;
    }

    /**
     * @return Returns the channelCount.
     */
    public int getChannelCount() {
        return this.channels.size();
    }

    /**
     * @return Returns the channels.
     */
    public List<GeoChannel> getChannels() {
        return this.channels;
    }

    /**
     * @param channels The channels to set.
     */
    public void setChannels( List<GeoChannel> channelData ) {
        this.channels = channelData;
    }

    public GeoChannel getChannel( int i ) {
        if ( i <= 0 || i > channels.size() )
            throw new IllegalArgumentException( "Invalid channel index " + i + ", only " + channels.size()
                    + " channels available." );
        GeoChannel result = channels.get( i - 1 );

        if ( result.getChannelNumber() != i ) {
            throw new IllegalStateException( "Channel number recorded in object was incorrect."
                    + result.getChannelNumber() + " != " + i );
        }
        return result;
    }

    public void addChannel() {
        GeoChannel newCh = new GeoChannel();
        newCh.setChannelNumber( channels.size() + 1 );
        this.channels.add( newCh );
    }

    /**
     * @return Returns the dataProcessing.
     */
    public String getDataProcessing() {
        return this.dataProcessing;
    }

    /**
     * @param dataProcessing The dataProcessing to set.
     */
    public void setDataProcessing( String dataProcessing ) {
        this.dataProcessing = dataProcessing;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription( String description ) {
        this.description = description;
        this.isGenePix = description.contains( "GenePix" );
        if ( isGenePix ) {
            log.warn( "GenePix data detected: Some unused quantitation types may be skipped" );
        }
    }

    /**
     * @return Returns the hybProtocol.
     */
    public String getHybProtocol() {
        return this.hybProtocol;
    }

    /**
     * @param hybProtocol The hybProtocol to set.
     */
    public void setHybProtocol( String hybProtocol ) {
        this.hybProtocol = hybProtocol;
    }

    public void addToHybProtocol( String s ) {
        this.hybProtocol = this.hybProtocol + " " + s;
    }

    public void addToScanProtocol( String s ) {
        this.scanProtocol = this.scanProtocol + " " + s;
    }

    public void addToDataProcessing( String s ) {
        this.dataProcessing = this.dataProcessing + " " + s;
    }

    public void addToDescription( String s ) {
        this.description = this.description + " " + s;
        if ( !isGenePix && description.contains( "GenePix" ) ) { // so we only get the first time around.
            log.warn( "GenePix data detected in " + this + ": Some unused quantitation types may be skipped" );
        }
        this.isGenePix = description.contains( "GenePix" );

    }

    /**
     * @return Returns the scanProtocol.
     */
    public String getScanProtocol() {
        return this.scanProtocol;
    }

    /**
     * @param scanProtocol The scanProtocol to set.
     */
    public void setScanProtocol( String scanProtocol ) {
        this.scanProtocol = scanProtocol;
    }

    /**
     * @return Returns the tagCount. (SAGE)
     */
    public int getTagCount() {
        return this.tagCount;
    }

    /**
     * @param tagCount The tagCount to set. (SAGE)
     */
    public void setTagCount( int tagCount ) {
        this.tagCount = tagCount;
    }

    /**
     * @return Returns the tagLength. (SAGE)
     */
    public int getTagLength() {
        return this.tagLength;
    }

    /**
     * @param tagLength The tagLength to set. (SAGE)
     */
    public void setTagLength( int tagLength ) {
        this.tagLength = tagLength;
    }

    /**
     * @return Returns the replicates.
     */
    public Collection<GeoReplication> getReplicates() {
        return this.replicates;
    }

    /**
     * @return Returns the variables.
     */
    public Collection<GeoVariable> getVariables() {
        return this.variables;
    }

    /**
     * @param variable
     */
    public void addVariable( GeoVariable variable ) {
        this.variables.add( variable );
    }

    /**
     * @param replication
     */
    public void addReplication( GeoReplication replication ) {
        this.replicates.add( replication );
    }

    /**
     * Returns the sample type (ie. DNA, RNA, etc.)
     * 
     * @return String
     */
    public String getType() {
        return this.type;
    }

    /**
     * Sets the sample type (ie. DNA, RNA, etc.)
     * 
     * @param type
     */
    public void setType( String type ) {
        this.type = type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(T)
     */
    public int compareTo( Object o ) {
        return ( ( GeoData ) o ).getGeoAccession().compareTo( this.getGeoAccession() );
    }

    /**
     * @return String
     */
    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    /**
     * @param lastUpdateDate
     */
    public void setLastUpdateDate( String lastUpdateDate ) {
        this.lastUpdateDate = lastUpdateDate;
    }

    /**
     * @return String
     */
    public String getSupplementaryFile() {
        return supplementaryFile;
    }

    /**
     * @param supplementaryFile
     */
    public void setSupplementaryFile( String supplementaryFile ) {
        this.supplementaryFile = supplementaryFile;
    }

    public boolean isGenePix() {
        return isGenePix;
    }

    public String getTitleInDataset() {
        return titleInDataset;
    }

    public void setTitleInDataset( String titleInDataset ) {
        this.titleInDataset = titleInDataset;
    }

}
