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

import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.expression.geo.GeoSampleCorrespondence;

import java.util.*;

/**
 * Represents a set of GEO samples that were submitted together. In many cases this corresponds to a full study, but for
 * studies that used more than one type of microarray (e.g., A and B chips in Affy sets), there will be two series.
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
@CommonsLog
public class GeoSeries extends GeoData {

    private static final long serialVersionUID = -1058350558444775537L;

    private String seriesId;
    private String status;
    private String submissionDate;
    private String platformId;
    private final Collection<GeoSample> samples;
    private final Collection<SeriesType> seriesTypes = new HashSet<>();
    private final Collection<String> subSeries;
    private final Map<Integer, GeoVariable> variables;
    private Collection<GeoContact> contributers;
    private Collection<GeoDataset> dataSets;
    private boolean isSubSeries = false;
    private boolean isSuperSeries = false;
    private Collection<String> keyWords;
    private String lastUpdateDate = "";
    private String overallDesign = "";
    private Collection<String> pubmedIds;
    private Map<Integer, GeoReplication> replicates;
    private GeoSampleCorrespondence sampleCorrespondence;
    private String summary = "";
    private String supplementaryFile = "";
    private GeoValues values;
    private Collection<String> webLinks;

    public GeoSeries() {
        keyWords = new HashSet<>();
        pubmedIds = new HashSet<>();
        variables = new HashMap<>();
        replicates = new HashMap<>();
        webLinks = new HashSet<>();
        contributers = new HashSet<>();
        samples = new HashSet<>();
        dataSets = new HashSet<>();
        values = new GeoValues();
        subSeries = new HashSet<>();
    }

    /**
     * See also GeoDataset.convertStringToExperimentType
     *
     * @param  string series type string
     * @return        series type object
     */
    public static SeriesType convertStringToSeriesType( String string ) {
        if ( string.equalsIgnoreCase( "Expression profiling by array" ) ) {
            return SeriesType.geneExpressionByArray;
        } else if ( string.equalsIgnoreCase( "Methylation profiling by high throughput sequencing" ) ) {
            return SeriesType.methylationArraybased;
        } else if ( string.equalsIgnoreCase( "Genome binding/occupancy profiling by high throughput sequencing" ) ) {
            return SeriesType.genomeBindingBySequencing;
        } else if ( string.equalsIgnoreCase( "Expression profiling by high throughput sequencing" ) ) {
            return SeriesType.geneExpressionBySequencing;
        } else if ( string.equalsIgnoreCase( "Non-coding RNA profiling by array" ) ) {
            return SeriesType.nonCodingRNAProfilingArraybased;
        } else if ( string.equals( "Other" ) ) {
            return SeriesType.other;
        } else if ( string.equalsIgnoreCase( "Third-party reanalysis" ) ) {
            return SeriesType.thirdPartyReanalysis;
        } else if ( string.equalsIgnoreCase( "Genome binding/occupancy profiling by genome tiling array" ) ) {
            return SeriesType.genomeBindingByArray;
        } else if ( string.equalsIgnoreCase( "Genome variation profiling by array" ) ) {
            return SeriesType.genomeVariationByArray;
        } else if ( string.equalsIgnoreCase( "Non-coding RNA profiling by high throughput sequencing" ) ) {
            return SeriesType.nonCodingRNAProfilingBySequencing;
        } else if ( string.equalsIgnoreCase( "Methylation profiling by genome tiling array" ) ) {
            return SeriesType.methylationByGenomeTiling;
        } else if ( string.equalsIgnoreCase( "Genome variation profiling by genome tiling array" ) ) {
            return SeriesType.genomeVariationByGenomeTiling;
        } else if ( string.equalsIgnoreCase( "different tissues" ) || string
                .equalsIgnoreCase( "cell_type_comparison_design" ) ) {
            return SeriesType.other;
        } else if ( string.equals( "other" ) || string.equalsIgnoreCase( "different tissues" ) || string
                .equalsIgnoreCase( "cell_type_comparison_design; disease state; cell line; tissue type" )
                || string
                        .equalsIgnoreCase( "time-course" )
                || string.equalsIgnoreCase( "Dual-label cDNA microarray" ) || string
                        .equalsIgnoreCase( "SuperSeries" )
                || string.equalsIgnoreCase( "Logical set" ) || string
                        .equalsIgnoreCase( "DNA Oligonucleotide Array" )
                || string
                        .equalsIgnoreCase( "expression profiling; time course analysis; infection response" ) ) {
            // these are possibilities that linger in tests. A pesky one is 'other', since that used to mean something
            // different than 'Other' (note capitalization). The old meaning is still expression arrays.
            return SeriesType.geneExpressionByArray;
        } else if ( string.equalsIgnoreCase( "Expression profiling by SAGE" ) ) {
            return SeriesType.geneExpressionBySAGE;
        } else {
            // there are too many hanging around, it's actually not so bad to guess.
            log.warn( "Unknown series type '" + string + "', assuming is expression arrays" );
            return SeriesType.geneExpressionByArray;
            //
            // throw new IllegalArgumentException( "Unknown series type '" + string + "'" );
        }
    }

    public void addContributer( GeoContact contributer ) {
        this.contributers.add( contributer );
    }

    public void addDataSet( GeoDataset dataset ) {
        dataset.addSeries( this );
        this.dataSets.add( dataset );
    }

    public void addSample( GeoSample sample ) {
        this.samples.add( sample );
    }

    /**
     * Add a group of samples to this series.
     *
     * @param s samples
     */
    public void addSamples( Collection<GeoSample> s ) {
        this.samples.addAll( s );
    }

    public void addSubSeries( String value ) {
        this.subSeries.add( value );
    }

    public void addToKeyWords( String keyword ) {
        this.keyWords.add( keyword );
    }

    /**
     * @param id in format "1239954" or "1239954,2194919", etc. The latter will be split into two.
     */
    public void addToPubmedIds( String id ) {
        String[] ids = id.split( "," );
        assert ids.length > 0;
        this.pubmedIds.addAll( Arrays.asList( ids ) );
        assert this.pubmedIds.size() > 0;
    }

    public void addToSeriesTypes( SeriesType type ) {
        this.seriesTypes.add( type );
    }

    /**
     * @param text to add onto the summary. A space is added to the end of the previous summary first.
     */
    public void addToSummary( String text ) {
        this.summary = this.summary + " " + text;
    }

    public void addToVariables( Integer number, GeoVariable variable ) {
        this.variables.put( number, variable );
    }

    /**
     * @return Returns the contributers.
     */
    public Collection<GeoContact> getContributers() {
        return this.contributers;
    }

    /**
     * @param contributers The contributers to set.
     */
    public void setContributers( Collection<GeoContact> contributers ) {
        this.contributers = contributers;
    }

    public Collection<GeoDataset> getDatasets() {
        return this.dataSets;
    }

    /**
     * @return Returns the type.
     */
    public Collection<String> getKeyWords() {
        return this.keyWords;
    }

    /**
     * @param type The type to set.
     */
    public void setKeyWords( Collection<String> type ) {
        this.keyWords = type;
    }

    /**
     * Returns the date the series was last updated.
     *
     * @return String
     */
    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    /**
     * @param lastUpdateDate the date the series was last updated.
     */
    public void setLastUpdateDate( String lastUpdateDate ) {
        this.lastUpdateDate = lastUpdateDate;
    }

    /**
     * @return Returns the overallDesign.
     */
    public String getOverallDesign() {
        return this.overallDesign;
    }

    /**
     * @param overallDesign The overallDesign to set.
     */
    public void setOverallDesign( String overallDesign ) {
        this.overallDesign = overallDesign;
    }

    /**
     * @return Returns the pubmedIds.
     */
    public Collection<String> getPubmedIds() {
        return this.pubmedIds;
    }

    /**
     * @param pubmedIds The pubmedIds to set.
     */
    public void setPubmedIds( Collection<String> pubmedIds ) {
        this.pubmedIds = pubmedIds;
    }

    /**
     * @return Returns the replicates.
     */
    public Map<Integer, GeoReplication> getReplicates() {
        return this.replicates;
    }

    /**
     * @param replicates The replicates to set.
     */
    public void setReplicates( Map<Integer, GeoReplication> replicates ) {
        this.replicates = replicates;
    }

    /**
     * @return Returns the sampleCorrespondence.
     */
    public GeoSampleCorrespondence getSampleCorrespondence() {
        return this.sampleCorrespondence;
    }

    /**
     * @param sampleCorrespondence The sampleCorrespondence to set.
     */
    public void setSampleCorrespondence( GeoSampleCorrespondence sampleCorrespondence ) {
        this.sampleCorrespondence = sampleCorrespondence;
    }

    public Collection<GeoSample> getSamples() {
        return this.samples;
    }

    public Collection<SeriesType> getSeriesTypes() {
        return seriesTypes;
    }

    /**
     * @return the subSeries
     */
    public Collection<String> getSubSeries() {
        return subSeries;
    }

    /**
     * @return Returns the summaries.
     */
    public String getSummaries() {
        return this.summary;
    }

    public void setSummaries( String summary ) {
        this.summary = summary;
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

    public GeoValues getValues() {
        return values;
    }

    public void setValues( GeoValues values ) {
        this.values = values;
    }

    /**
     * Get a subset of the values. This is only used for 'splitting' a series.
     *
     * @param  s Samples to include data from.
     * @return   geo values
     */
    public GeoValues getValues( Collection<GeoSample> s ) {
        return values.subset( s );
    }

    /**
     * @return Returns the variables.
     */
    public Map<Integer, GeoVariable> getVariables() {
        return this.variables;
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
     * @return the isSubSeries
     */
    public boolean isSubSeries() {
        return isSubSeries;
    }

    /**
     * @return the isSuperSeries
     */
    public boolean isSuperSeries() {
        return isSuperSeries;
    }

    /**
     * Clean up samples we have decided are ineligible (i.e., non transcriptomic)
     *
     * @param samplesToSkip the samples to remove
     */
    public void removeSamples( Collection<GeoSample> samplesToSkip ) {
        if ( samplesToSkip == null || samplesToSkip.isEmpty() ) {
            return;
        }
        this.samples.removeAll( samplesToSkip );
    }

    /**
     * @param contact The contact to set.
     */
    public void setContact( GeoContact contact ) {
        this.contact = contact;
    }

    public void setDataSets( Collection<GeoDataset> dataSets ) {
        this.dataSets = dataSets;
    }

    /**
     * @param isSubSeries the isSubSeries to set
     */
    public void setIsSubSeries( boolean isSubSeries ) {
        this.isSubSeries = isSubSeries;
    }

    /**
     * @param isSuperSeries the isSuperSeries to set
     */
    public void setIsSuperSeries( boolean isSuperSeries ) {
        this.isSuperSeries = isSuperSeries;
    }

    public enum SeriesType {
        geneExpressionByArray, geneExpressionBySAGE, geneExpressionBySequencing, genomeBindingByArray, genomeBindingBySequencing, genomeVariationByArray, genomeVariationByGenomeTiling, methylationArraybased, methylationByGenomeTiling, nonCodingRNAProfilingArraybased, nonCodingRNAProfilingBySequencing, other, thirdPartyReanalysis,
    }

}
