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
 * Bean describing a microarray platform in GEO
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoPlatform extends GeoData {

    private static Log log = LogFactory.getLog( GeoPlatform.class.getName() );

    /**
     * Store information on the platform here. Map of designElements to other information.
     */
    private Map<String, List<String>> data = new HashMap<String, List<String>>();

    private Collection<String> catalogNumbers = new HashSet<String>();

    private String coating = "";

    private Collection<String> contributer = new HashSet<String>();

    private String description = "";

    private String distribution = "";

    private String manufactureProtocol = "";

    private String manufacturer = "";

    private Collection<String> organisms = new HashSet<String>();

    private List<List> platformData = new ArrayList<List>();

    private Collection<Integer> pubMedIds = new HashSet<Integer>();

    private String support = "";

    private GeoDataset.PlatformType technology;

    private Collection<String> webLinks = new HashSet<String>();

    /**
     * @param s
     */
    public void addToDescription( String s ) {
        this.description = this.description + " " + s;
    }

    /**
     * @param org
     */
    public void addToOrganisms( String org ) {
        this.organisms.add( org );
    }

    /**
     * @param designElement
     * @return
     */
    public List<String> getColumnData( String columnName ) {
        assert data.size() != 0;
        return data.get( columnName );
    }

    /**
     * @param designElement
     * @param value
     */
    public void addToColumnData( String columnName, String value ) {
        if ( !data.containsKey( columnName ) ) {
            log.info( "Adding " + columnName + " to " + this.getGeoAccession() );
            data.put( columnName, new ArrayList<String>() );
        }
        getColumnData( columnName ).add( value );
    }

    /**
     * @return Returns the catalogNumbers.
     */
    public Collection<String> getCatalogNumbers() {
        return this.catalogNumbers;
    }

    /**
     * @return Returns the coating.
     */
    public String getCoating() {
        return this.coating;
    }

    /**
     * @return Returns the contributer.
     */
    public Collection<String> getContributer() {
        return this.contributer;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @return Returns the descriptions.
     */
    public String getDescriptions() {
        return this.description;
    }

    /**
     * @return Returns the distribution.
     */
    public String getDistribution() {
        return this.distribution;
    }

    /**
     * @return Returns the manufactureProtocol.
     */
    public String getManufactureProtocol() {
        return this.manufactureProtocol;
    }

    /**
     * @return Returns the manufacturer.
     */
    public String getManufacturer() {
        return this.manufacturer;
    }

    /**
     * @return Returns the organisms.
     */
    public Collection<String> getOrganisms() {
        return this.organisms;
    }

    /**
     * @return Returns the platformData.
     */
    public List<List> getPlatformData() {
        return this.platformData;
    }

    /**
     * @return Returns the pubMedIds.
     */
    public Collection<Integer> getPubMedIds() {
        return this.pubMedIds;
    }

    /**
     * @return Returns the support.
     */
    public String getSupport() {
        return this.support;
    }

    /**
     * @return Returns the technology.
     */
    public GeoDataset.PlatformType getTechnology() {
        return this.technology;
    }

    /**
     * @return Returns the webLinks.
     */
    public Collection<String> getWebLinks() {
        return this.webLinks;
    }

    /**
     * @param catalogNumbers The catalogNumbers to set.
     */
    public void setCatalogNumbers( Collection<String> catalogNumbers ) {
        this.catalogNumbers = catalogNumbers;
    }

    /**
     * @param coating The coating to set.
     */
    public void setCoating( String coating ) {
        this.coating = coating;
    }

    /**
     * @param contributer The contributer to set.
     */
    public void setContributer( Collection<String> contributer ) {
        this.contributer = contributer;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @param distribution The distribution to set.
     */
    public void setDistribution( String distribution ) {
        this.distribution = distribution;
    }

    /**
     * @param manufactureProtocol The manufactureProtocol to set.
     */
    public void setManufactureProtocol( String manufactureProtocol ) {
        this.manufactureProtocol = manufactureProtocol;
    }

    /**
     * @param manufacturer The manufacturer to set.
     */
    public void setManufacturer( String manufacturer ) {
        this.manufacturer = manufacturer;
    }

    /**
     * @param organisms The organisms to set.
     */
    public void setOrganisms( Collection<String> organism ) {
        this.organisms = organism;
    }

    /**
     * @param platformData The platformData to set.
     */
    public void setPlatformData( List<List> platformData ) {
        this.platformData = platformData;
    }

    /**
     * @param pubMedIds The pubMedIds to set.
     */
    public void setPubMedIds( Collection<Integer> pubMedIds ) {
        this.pubMedIds = pubMedIds;
    }

    /**
     * @param support The support to set.
     */
    public void setSupport( String support ) {
        this.support = support;
    }

    /**
     * @param technology The technology to set.
     */
    public void setTechnology( GeoDataset.PlatformType technology ) {
        this.technology = technology;
    }

    /**
     * @param webLinks The webLinks to set.
     */
    public void setWebLinks( Collection<String> webLinks ) {
        this.webLinks = webLinks;
    }

}
