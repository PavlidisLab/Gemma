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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.expression.geo.model.GeoDataset.PlatformType;
import ubic.gemma.loader.expression.geo.util.GeoConstants;

/**
 * Bean describing a microarray platform in GEO
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoPlatform extends GeoData {

    private static final String DISTRIBUTION_VIRTUAL = "virtual";

    private static final long serialVersionUID = 1L;

    private static Log log = LogFactory.getLog( GeoPlatform.class.getName() );

    /**
     * Store information on the platform here. Map of designElements to other information. This has to be lists so the
     * values "line up".
     */
    private Map<String, List<String>> platformInformation = new HashMap<String, List<String>>();

    /**
     * Map of original probe names provided by GEO to the names in Gemma (if this platform is already there). This is
     * needed because probe names are sometimes changed after import. This map must be popoulated prior to import of the
     * data.
     */
    private Map<String, String> probeNamesInGemma = new HashMap<String, String>();

    private Collection<String> catalogNumbers = new HashSet<String>();

    private String coating = "";

    private Collection<String> contributer = new HashSet<String>();

    private String description = "";

    private String distribution = "";

    private String manufactureProtocol = "";

    private String manufacturer = "";

    private Collection<String> organisms = new HashSet<String>();

    private List<List<String>> platformData = new ArrayList<List<String>>();

    private Collection<Integer> pubMedIds = new HashSet<Integer>();

    private String support = "";

    private GeoDataset.PlatformType technology = null;

    private Collection<String> webLinks = new HashSet<String>();

    private String sample = "DNA";

    private String lastUpdateDate = "";

    private String supplementaryFile = "";

    private Collection<String> designElements = new HashSet<String>();

    /**
     * Add a value to a column. A special case is when the column is of the probe ids (design element name).
     * 
     * @param columnName
     * @param value
     */
    public void addToColumnData( String columnName, String value ) {
        if ( !platformInformation.containsKey( columnName ) ) {
            if ( log.isDebugEnabled() ) log.debug( "Adding " + columnName + " to " + this.getGeoAccession() );
            platformInformation.put( columnName, new ArrayList<String>() );
        }

        // don't add design elements twice. Occurs in corrupt files, but see bug 2054
        if ( GeoConstants.likelyId( columnName ) ) {
            if ( designElements.contains( value ) ) {

                /*
                 * This is not easily recoverable, because all the other columns will have the wrong number of items.
                 */

                // log.warn( "Column " + columnName + " contains the value " + value
                // + " twice; check the GEO file for validity!" );
                throw new IllegalStateException( "In platform " + geoAccession + ": Column " + columnName
                        + " contains the value " + value + " twice; check the GEO file for validity!" );
                // return;
            }
            designElements.add( value );
        }

        List<String> columnData = getColumnData( columnName );
        if ( columnData == null ) {
            return;
        }
        columnData.add( value );
    }

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
     * @param columnNames
     * @return List of Lists of Strings
     */
    public List<List<String>> getColumnData( Collection<String> columnNames ) {
        List<List<String>> results = new ArrayList<List<String>>();
        for ( String columnName : columnNames ) {
            List<String> columnData = this.getColumnData( columnName );
            if ( columnData == null ) continue;
            results.add( columnData );
        }
        return results;
    }

    /**
     * @param designElement
     * @return
     */
    public List<String> getColumnData( String columnName ) {
        if ( !platformInformation.containsKey( columnName ) ) {
            log.warn( "No platform information for column=" + columnName );
            return null;
        }
        // assert platformInformation.size() != 0 : this + " has no platformInformation at all!";
        // assert platformInformation.containsKey( columnName ) : this + " has no platformInformation for '" +
        // columnName
        // + "'";
        return platformInformation.get( columnName );
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

    public Collection<String> getDesignElements() {
        return designElements;
    }

    /**
     * @return Returns the distribution.
     */
    public String getDistribution() {
        return this.distribution;
    }

    /**
     * Get the name of the column that has the 'ids' for the design elements on this platform. Usually this is "ID".
     * 
     * @param platform
     * @return
     */
    public String getIdColumnName() {
        Collection<String> columnNames = this.getColumnNames();
        int index = 0;
        for ( String string : columnNames ) {
            if ( GeoConstants.likelyId( string ) ) {
                log.debug( string + " appears to indicate the array element identifier in column " + index
                        + " for platform " + this );
                return string;
            }
            index++;
        }
        return null;
    }

    /**
     * @return String
     */
    public String getLastUpdateDate() {
        return lastUpdateDate;
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
    public List<List<String>> getPlatformData() {
        return this.platformData;
    }

    public Map<String, String> getProbeNamesInGemma() {
        return probeNamesInGemma;
    }

    /**
     * @return Returns the pubMedIds.
     */
    public Collection<Integer> getPubMedIds() {
        return this.pubMedIds;
    }

    /**
     * @return String
     */
    public String getSample() {
        return sample;
    }

    /**
     * @return String
     */
    public String getSupplementaryFile() {
        return supplementaryFile;
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
     * @param lastUpdateDate
     */
    public void setLastUpdateDate( String lastUpdateDate ) {
        this.lastUpdateDate = lastUpdateDate;
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
    public void setPlatformData( List<List<String>> platformData ) {
        this.platformData = platformData;
    }

    /**
     * @param pubMedIds The pubMedIds to set.
     */
    public void setPubMedIds( Collection<Integer> pubMedIds ) {
        this.pubMedIds = pubMedIds;
    }

    /**
     * @param sample
     */
    public void setSample( String sample ) {
        this.sample = sample;
    }

    /**
     * @param supplementaryFile
     */
    public void setSupplementaryFile( String supplementaryFile ) {
        this.supplementaryFile = supplementaryFile;
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

    /**
     * @return true if the data uses a platform that, generally, we can use the data from. Will be false for MPSS, SAGE
     *         and Exon array data.
     */
    public boolean useDataFromGeo() {

        if ( technology == null ) {
            throw new IllegalStateException( "Don't call until the technology type is filled in" );
        }

        if ( DISTRIBUTION_VIRTUAL.equals( this.distribution ) || technology.equals( PlatformType.MPSS )
                || technology.equals( PlatformType.SAGE ) || technology.equals( PlatformType.SAGENlaIII )
                || technology.equals( PlatformType.SAGERsaI ) || technology.equals( PlatformType.SAGESau3A )
                || technology.equals( PlatformType.other ) ) {
            return false;
        }

        if ( StringUtils.isBlank( getTitle() ) ) {
            throw new IllegalStateException( "Can't figure out suitability of data until platform title is filled in." );
        }

        if ( technology.equals( PlatformType.inSituOligonucleotide ) && ( getTitle().contains( "Exon" ) ) ) {
            // This is not expected to be very robust, but there's not much else to go on, apparently.
            return false;
        }

        return true;
    }
}
