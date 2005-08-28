/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
import java.util.HashSet;
import java.util.List;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoSample extends GeoData {
    String title;

    List<GeoChannel> channelData;
    String hybProtocol;
    String dataProcessing;
    String scanProtocol;
    String description;
    String platformId; // refers to a platform object.

    Collection<GeoPlatform> platforms;

    // SAGE items.
    String anchor;
    int tagCount;
    int tagLength;

    public GeoSample() {
        channelData = new ArrayList<GeoChannel>();
        this.addChannel();
        contact = new GeoContact();
        platforms = new HashSet<GeoPlatform>();
    }

    public void addPlatform( GeoPlatform platform ) {
        this.platforms.add( platform );
    }

    public Collection<GeoPlatform> getPlatforms() {
        return this.platforms;
    }

    /**
     * @return Returns the anchor.
     */
    public String getAnchor() {
        return this.anchor;
    }

    /**
     * @param anchor The anchor to set.
     */
    public void setAnchor( String anchor ) {
        this.anchor = anchor;
    }

    /**
     * @return Returns the channelCount.
     */
    public int getChannelCount() {
        return this.channelData.size();
    }

    /**
     * @return Returns the channelData.
     */
    public List<GeoChannel> getChannelData() {
        return this.channelData;
    }

    /**
     * @param channelData The channelData to set.
     */
    public void setChannelData( List<GeoChannel> channelData ) {
        this.channelData = channelData;
    }

    public GeoChannel getChannel( int i ) {
        if ( i <= 0 || i > channelData.size() )
            throw new IllegalArgumentException( "Invalid channel index " + i + ", only " + channelData.size()
                    + " channels available." );
        return channelData.get( i - 1 );
    }

    public void addChannel() {
        this.channelData.add( new GeoChannel() );
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
     * @return Returns the platformId.
     */
    public String getPlatformId() {
        return this.platformId;
    }

    /**
     * @param platformId The platformId to set.
     */
    public void setPlatformId( String platformId ) {
        this.platformId = platformId;
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
     * @return Returns the tagCount.
     */
    public int getTagCount() {
        return this.tagCount;
    }

    /**
     * @param tagCount The tagCount to set.
     */
    public void setTagCount( int tagCount ) {
        this.tagCount = tagCount;
    }

    /**
     * @return Returns the tagLength.
     */
    public int getTagLength() {
        return this.tagLength;
    }

    /**
     * @param tagLength The tagLength to set.
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

}
