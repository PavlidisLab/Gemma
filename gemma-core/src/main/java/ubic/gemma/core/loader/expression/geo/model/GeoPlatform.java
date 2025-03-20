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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import ubic.gemma.core.loader.expression.geo.model.GeoDataset.PlatformType;
import ubic.gemma.core.loader.expression.geo.util.GeoConstants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Bean describing a microarray platform in GEO
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
public class GeoPlatform extends GeoData {

    private static final Set<String> affyExonArrays = new HashSet<>();

    /**
     * keys are platforms names; values are the platform we will actually used (key can equal value)
     */
    private static final Map<String, String> affyPlatformMap = new HashMap<>();
    private static final String DISTRIBUTION_VIRTUAL = "virtual";
    private static final Log log = LogFactory.getLog( GeoPlatform.class.getName() );
    private static final long serialVersionUID = 1L;

    static {

        /*
         * For these platforms we *never* use the data provided by GEO if it's on one of the alternatives to this. These
         * IDs are the gene-level versions. Anything mapping to an alternative of these we won't use, because it's often
         * exon-level data (not always, but in that case it will be an alternative version, which we'd want to replace
         * anyway; so not having imported the data is not a big deal).
         */
        affyExonArrays.add( "GPL6096" ); // MoEx-1_0-st, gene-level version
        affyExonArrays.add( "GPL5175" ); // HuEx-1_0-st, gene-level version
        affyExonArrays.add( "GPL6543" ); // RaEx-1_0-st, gene-level version
        affyExonArrays.add( "GPL17586" ); // HTA-2_0, gene-level version

        try ( BufferedReader in = new BufferedReader( new InputStreamReader( new ClassPathResource( "/ubic/gemma/core/loader/affy.altmappings.txt" ).getInputStream() ) ) ) {
            while ( in.ready() ) {
                String geoId = in.readLine().trim();

                if ( geoId.startsWith( "#" ) || geoId.isEmpty() ) {
                    continue;
                }

                String[] f = geoId.split( "=" );

                if ( f.length < 2 ) {
                    continue;
                }

                GeoPlatform.affyPlatformMap.put( f[0], f[1] );
            }
        } catch ( Exception e ) {
            GeoPlatform.log.warn( "List of exon array IDs could not be loaded: " + e.getMessage() );
        }
    }

    /**
     * @param geoPlatformId (GPL)
     * @return true if we know this to be an exon array - so far as we know.
     */
    public static boolean isAffymetrixExonArray( String geoPlatformId ) {
        return affyExonArrays.contains( GeoPlatform.affyPlatformMap.get( geoPlatformId ) );
    }

    /**
     * @param geoPlatformId (GPL)
     * @return short name (GPLXXXX) of platform we would actually use, or null if not found
     */
    public static String alternativeToProperAffyPlatform( String geoPlatformId ) {
        return GeoPlatform.affyPlatformMap.get( geoPlatformId );
    }

    /**
     *
     * @param geoPlatformId (GPL)
     * @return true if we recognize it as an Affymetrix platform. Depends on our mappings, if an error is spotted let us
     *         know.
     */
    public static boolean isAffyPlatform( String geoPlatformId ) {
        return GeoPlatform.affyPlatformMap.containsKey( geoPlatformId );
    }

    /**
     * Refers to a list of platforms for which the data from GEO is usually not usable and/or which we always reanalyze
     * from CEL files - exon arrays.
     * <p>
     * Logic: if this was run on an Affymetrix exon array we won't use the data from GEO, even if it was already using
     * the gene-level version of the platform, because there are several variant versions that just muck up the system
     * with useless probes (we have gone back and forth on this a bit...)
     * <p>
     * Note that we endeavour to reanalyze all Affy data sets at the CEL file level.
     *
     * @param geoPlatformId (GPL)
     * @return true if the platform is affymetrix exon array.
     */
    public static boolean isGEOAffyDataUsable( String geoPlatformId ) {

        if ( !isAffyPlatform( geoPlatformId ) ) {
            throw new IllegalArgumentException(
                    "Not an Affy platform, so far as we know, check that it's an Affy platform first" );
        }

        String platformToUse = affyPlatformMap.get( geoPlatformId );

        if ( affyExonArrays.contains( platformToUse ) ) {
            return false;
        }

        // it's already on the right platform, though we may replace the data later.
        return platformToUse.equals( geoPlatformId );

    }

    public String getId() {
        return id;
    }

    public void setId( String id ) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus( String status ) {
        this.status = status;
    }

    public String getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate( String submissionDate ) {
        this.submissionDate = submissionDate;
    }

    public String getWebLink() {
        return webLink;
    }

    public void setWebLink( String webLink ) {
        this.webLink = webLink;
    }

    public Map<String, List<String>> getPlatformInformation() {
        return platformInformation;
    }

    public boolean isUseDataFromGEO() {
        return useDataFromGEO;
    }

    private String id;
    private String status;
    private String submissionDate;
    private String webLink;
    private Collection<String> catalogNumbers = new HashSet<>();
    private String coating = "";
    private Collection<String> contributer = new HashSet<>();
    private String description = "";
    private final Collection<String> designElements = new HashSet<>();
    private String distribution = "";
    private String lastUpdateDate = "";
    private String manufactureProtocol = "";
    private String manufacturer = "";
    private Collection<String> organisms = new HashSet<>();
    private List<List<String>> platformData = new ArrayList<>();
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
     * Add a value to a column. A special case is when the column is of the probe ids (design element name).
     *
     * @param columnName column name
     * @param value value
     */
    public void addToColumnData( String columnName, String value ) {
        if ( !platformInformation.containsKey( columnName ) ) {
            if ( GeoPlatform.log.isDebugEnabled() )
                GeoPlatform.log.debug( "Adding " + columnName + " to " + this.getGeoAccession() );
            platformInformation.put( columnName, new ArrayList<>() );
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
                        "In platform " + getGeoAccession() + ": Column " + columnName + " contains the value " + value
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
     * @return Returns the coating.
     */
    public String getCoating() {
        return this.coating;
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

    public void setSample( String sample ) {
        this.sample = sample;
    }

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
     * Normally only set this if "false". Default is true, but will be overridden for certain typs of platforms such as
     * MPSS (rna-seq), SAGE or Exon arrays.
     *
     * @param b new value
     */
    public void setUseDataFromGEO( boolean b ) {
        this.useDataFromGEO = b;
    }

    /**
     * @param webLinks The webLinks to set.
     */
    public void setWebLinks( Collection<String> webLinks ) {
        this.webLinks = webLinks;
    }

    /**
     * @return true if the data uses a platform that, generally, we can use the data from, if available. Will be false
     *         for MPSS, SAGE and transcript- or exon- level exon array data. Note that sometimes this will result in us
     *         not using data that was sort of okay - for example, an alternative CDF/MPS version of an Affy Exon array
     *         at
     *         the gene level. But we'd want to reanalyze it from the official CDF/MPS anyway.
     */
    public boolean useDataFromGeo() {

        // if it was SAGE or something we may have already figured it out.
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

        if ( !isAffyPlatform( this.getGeoAccession() ) ) {
            // no further objections.
            return true;
        }

        // If true, we can use these data, even if we replace it from CEL files, eventually. 
        return isGEOAffyDataUsable( this.getGeoAccession() );

    }

}
