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
package edu.columbia.gemma.loader.expression.geo.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Represents a sample (GSM) in GEO. The channels correspond to BioMaterials; the sample itself corresponds to a
 * BioAssay in Gemma. Some fields are only relevant for SAGE.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoSample extends GeoData {

    private String title = "";
    private List<GeoChannel> channels;
    private String hybProtocol = "";
    private String dataProcessing = "";
    private String scanProtocol = "";
    private String description = "";

    Collection<GeoPlatform> platforms;

    Collection<GeoReplication> replicates;
    Collection<GeoVariable> variables;

    /*
     * quantitationType -> designelement -> value
     */
    Map<String, Map<String, String>> data = new HashMap<String, Map<String, String>>();

    /**
     * @param designElement
     * @param quantitationType
     * @param value
     */
    public void addDatum( String designElement, String quantitationType, String value ) {
        if ( !data.containsKey( quantitationType ) ) {
            data.put( quantitationType, new HashMap<String, String>() );
        }
        data.get( quantitationType ).put( designElement, value );
    }

    /**
     * @param designElement
     * @param quantitationType
     * @return
     */
    public String getDatum( String designElement, String quantitationType ) {
        if ( !data.containsKey( quantitationType ) ) {
            throw new IllegalArgumentException( "No such quantitation type \"" + quantitationType + "\"" );
        }
        return data.get( quantitationType ).get( designElement );
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
     * @return Returns the title.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * @param title The title to set.
     */
    public void setTitle( String title ) {
        this.title = title;
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

}
