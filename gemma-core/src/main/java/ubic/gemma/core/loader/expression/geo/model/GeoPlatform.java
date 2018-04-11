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
package ubic.gemma.core.loader.expression.geo.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.core.loader.expression.geo.model.GeoDataset.PlatformType;
import ubic.gemma.core.loader.expression.geo.util.GeoConstants;

/**
 * Bean describing a microarray platform in GEO
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
public class GeoPlatform extends GeoData {

    private static final String DISTRIBUTION_VIRTUAL = "virtual";

    /**
     * detect more exon arrays; values are the platform we will actually used
     */
    private static final Map<String, String> exonPlatformGeoIds = new HashMap<>();
    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog( GeoPlatform.class.getName() );

    static {
        try (BufferedReader in = new BufferedReader( new InputStreamReader(
                GeoPlatform.class.getResourceAsStream( "/ubic/gemma/core/affy.exonarrays.txt" ) ) )) {
            while ( in.ready() ) {
                String geoId = in.readLine().trim();

                if ( geoId.startsWith( "#" ) || geoId.isEmpty() ) {
                    continue;
                }

                String[] f = geoId.split( "=" );

                if ( f.length < 2 ) {
                    continue;
                }

                GeoPlatform.exonPlatformGeoIds.put( f[0], f[1] );
            }
        } catch ( Exception e ) {
            GeoPlatform.log.warn( "List of exon array IDs could not be loaded: " + e.getMessage() );
        }
    }

    private final Collection<String> designElements = new HashSet<>();
    /**
     * Store information on the platform here. Map of designElements to other information. This has to be lists so the
     * values "line up".
     */
    private final Map<String, List<String>> platformInformation = new HashMap<>();
    /**
     * Map of original probe names provided by GEO to the names in Gemma (if this platform is already there). This is
     * needed because probe names are sometimes changed after import. This map must be populated prior to import of the
     * data.
     */
    private final Map<String, String> probeNamesInGemma = new HashMap<>();
    private Collection<String> catalogNumbers = new HashSet<>();
    private String coating = "";
    private Collection<String> contributer = new HashSet<>();
    private String description = "";
    private String distribution = "";
    private String lastUpdateDate = "";
    private String manufactureProtocol = "";
    private String manufacturer = "";
    private Collection<String> organisms = new HashSet<>();
    private List<List<String>> platformData = new ArrayList<>();
    private Collection<Integer> pubMedIds = new HashSet<>();
    private String sample = "DNA";
    private String supplementaryFile = "";
    private String support = "";
    private GeoDataset.PlatformType technology = null;
    /**
     * Will be set to false during parsing if data are missing.
     */
    private boolean useDataFromGEO = true;
    private Collection<String> webLinks = new HashSet<>();

    /**
     * Refers to a list of platforms for which the data from GEO is usually not usable and/or which we always reanalyze
     * from CEL files - exon arrays and newer gene arrays
     * 
     * @param geoPlatformId (GPL)
     * @return true if the platform is affymetrix exon array.
     */
    public static boolean isAffymetrixExonArray( String geoPlatformId ) {
        return GeoPlatform.exonPlatformGeoIds.containsKey( geoPlatformId );
    }

    /**
     * @param geoPlatformId (GPL)
     * @return short name (GPLXXXX) of platform we would actually use, or null if not found
     */
    public static String exonArrayToGeneLevelArray( String geoPlatformId ) {
        return GeoPlatform.exonPlatformGeoIds.get( geoPlatformId );
    }

    /**
     * Add a value to a column. A special case is when the column is of the probe ids (design element name).
     *
     * @param columnName column name
     * @param value value
     */
    public void addToColumnData( String columnName, String value ) {
        if ( !platformInformation.containsKey( columnName ) ) {
            if ( GeoPlatform.log.isDebugEnabled() )
                GeoPlatform.log.debug( "Adding " + columnName + " to " + this.getGeoAccession() );
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
                throw new IllegalStateException(
                        "In platform " + geoAccession + ": Column " + columnName + " contains the value " + value
                                + " twice; check the GEO file for validity!" );
                // return;
            }
            designElements.add( value );
        }

        List<String> columnData = this.getColumnData( columnName );
        if ( columnData == null ) {
            return;
        }
        columnData.add( value );
    }

    /**
     * @param s description
     */
    public void addToDescription( String s ) {
        this.description = this.description + " " + s;
    }

    /**
     * @param org organisation
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
     * @param catalogNumbers The catalogNumbers to set.
     */
    public void setCatalogNumbers( Collection<String> catalogNumbers ) {
        this.catalogNumbers = catalogNumbers;
    }

    /**
     * @return Returns the coating.
     */
    public String getCoating() {
        return this.coating;
    }

    /**
     * @param coating The coating to set.
     */
    public void setCoating( String coating ) {
        this.coating = coating;
    }

    /**
     * @param columnNames column names
     * @return List of Lists of Strings
     */
    public List<List<String>> getColumnData( Collection<String> columnNames ) {
        List<List<String>> results = new ArrayList<>();
        for ( String columnName : columnNames ) {
            List<String> columnData = this.getColumnData( columnName );
            if ( columnData == null )
                continue;
            results.add( columnData );
        }
        return results;
    }

    public List<String> getColumnData( String columnName ) {
        if ( !platformInformation.containsKey( columnName ) ) {
            GeoPlatform.log.warn( "No platform information for column=" + columnName );
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
     * @param contributer The contributer to set.
     */
    public void setContributer( Collection<String> contributer ) {
        this.contributer = contributer;
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
     * @param distribution The distribution to set.
     */
    public void setDistribution( String distribution ) {
        this.distribution = distribution;
    }

    /**
     * @return the name of the column that has the 'ids' for the design elements on this platform. Usually this is "ID".
     */
    public String getIdColumnName() {
        Collection<String> columnNames = this.getColumnNames();
        int index = 0;
        for ( String string : columnNames ) {
            if ( GeoConstants.likelyId( string ) ) {
                GeoPlatform.log.debug( string + " appears to indicate the array element identifier in column " + index
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

    public void setLastUpdateDate( String lastUpdateDate ) {
        this.lastUpdateDate = lastUpdateDate;
    }

    /**
     * @return Returns the manufactureProtocol.
     */
    public String getManufactureProtocol() {
        return this.manufactureProtocol;
    }

    /**
     * @param manufactureProtocol The manufactureProtocol to set.
     */
    public void setManufactureProtocol( String manufactureProtocol ) {
        this.manufactureProtocol = manufactureProtocol;
    }

    /**
     * @return Returns the manufacturer.
     */
    public String getManufacturer() {
        return this.manufacturer;
    }

    /**
     * @param manufacturer The manufacturer to set.
     */
    public void setManufacturer( String manufacturer ) {
        this.manufacturer = manufacturer;
    }

    /**
     * @return Returns the organisms.
     */
    public Collection<String> getOrganisms() {
        return this.organisms;
    }

    public void setOrganisms( Collection<String> organism ) {
        this.organisms = organism;
    }

    /**
     * @return Returns the platformData.
     */
    public List<List<String>> getPlatformData() {
        return this.platformData;
    }

    /**
     * @param platformData The platformData to set.
     */
    public void setPlatformData( List<List<String>> platformData ) {
        this.platformData = platformData;
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
     * @param pubMedIds The pubMedIds to set.
     */
    public void setPubMedIds( Collection<Integer> pubMedIds ) {
        this.pubMedIds = pubMedIds;
    }

    /**
     * @return String
     */
    public String getSample() {
        return sample;
    }

    public void setSample( String sample ) {
        this.sample = sample;
    }

    /**
     * @return String
     */
    public String getSupplementaryFile() {
        return supplementaryFile;
    }

    public void setSupplementaryFile( String supplementaryFile ) {
        this.supplementaryFile = supplementaryFile;
    }

    /**
     * @return Returns the support.
     */
    public String getSupport() {
        return this.support;
    }

    /**
     * @param support The support to set.
     */
    public void setSupport( String support ) {
        this.support = support;
    }

    /**
     * @return Returns the technology.
     */
    public GeoDataset.PlatformType getTechnology() {
        return this.technology;
    }

    /**
     * @param technology The technology to set.
     */
    public void setTechnology( GeoDataset.PlatformType technology ) {
        this.technology = technology;
    }

    /**
     * @return Returns the webLinks.
     */
    public Collection<String> getWebLinks() {
        return this.webLinks;
    }

    /**
     * @param webLinks The webLinks to set.
     */
    public void setWebLinks( Collection<String> webLinks ) {
        this.webLinks = webLinks;
    }

    /**
     * Normally only set this if "false". Default is true, but will be overridden for certain typs of platforms such as
     * MPSS (rna-seq), SAGE or Exon arrays.
     *
     * @param b new value
     */
    public void setUseDataFromGEO( boolean b ) {
        this.useDataFromGEO = b;
    }

    /**
     * @return true if the data uses a platform that, generally, we can use the data from, if available. Will be false
     *         for MPSS, SAGE and transcript- or exon- level exon array data.
     */
    public boolean useDataFromGeo() {

        if ( !this.useDataFromGEO )
            return false;

        if ( technology == null ) {
            throw new IllegalStateException( "Don't call until the technology type is filled in" );
        }

        if ( GeoPlatform.DISTRIBUTION_VIRTUAL.equals( this.distribution ) || technology.equals( PlatformType.MPSS )
                || technology.equals( PlatformType.SAGE ) || technology.equals( PlatformType.SAGENlaIII ) || technology
                        .equals( PlatformType.SAGERsaI )
                || technology.equals( PlatformType.SAGESau3A ) || technology
                        .equals( PlatformType.other ) ) {
            return false;
        }

        if ( StringUtils.isBlank( this.getTitle() ) ) {
            throw new IllegalStateException(
                    "Can't figure out suitability of data until platform title is filled in." );
        }

        // these are the three gene-level representations of affy exon platforms. We can use these data, even if we replace it, eventually. However, the data aren't always there.
        // See also DataUpdater.prepareTargetPlatformForExonArrays
        // Sometimes the data are there, sometimes it isn't.
        return !technology.equals( PlatformType.inSituOligonucleotide )
                || ( !GeoPlatform.isAffymetrixExonArray( this.getGeoAccession() ) && !this.getTitle().toLowerCase()
                        .contains( "exon" ) )
                || this.getGeoAccession().equals( "GPL6096" ) || this.getGeoAccession()
                        .equals( "GPL6244" )
                || this.getGeoAccession().equals( "GPL6247" );

    }
}
