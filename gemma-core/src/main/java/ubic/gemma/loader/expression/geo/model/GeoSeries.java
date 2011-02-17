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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import ubic.gemma.loader.expression.geo.GeoSampleCorrespondence;

/**
 * Represents a set of GEO samples that were submitted together. In many cases this corresponds to a full study, but for
 * studies that used more than one type of microarray (e.g., A and B chips in Affy sets), there will be two series.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoSeries extends GeoData {

    private static final long serialVersionUID = -1058350558444775537L;
    private String summary = "";
    private String overallDesign = "";
    private Collection<String> keyWords;
    private Collection<String> pubmedIds;
    private GeoValues values;
    private Collection<String> webLinks;
    private Collection<GeoContact> contributers;
    private Map<Integer, GeoVariable> variables;
    private Map<Integer, GeoReplication> replicates;
    private GeoSampleCorrespondence sampleCorrespondence;
    private Collection<GeoDataset> dataSets;
    private Collection<GeoSample> samples;
    private String supplementaryFile = "";
    private String lastUpdateDate = "";
    boolean isSuperSeries = false;
    boolean isSubSeries = false;
    private Collection<String> subSeries;

    public GeoSeries() {
        keyWords = new HashSet<String>();
        pubmedIds = new HashSet<String>();
        variables = new HashMap<Integer, GeoVariable>();
        replicates = new HashMap<Integer, GeoReplication>();
        webLinks = new HashSet<String>();
        contributers = new HashSet<GeoContact>();
        samples = new HashSet<GeoSample>();
        dataSets = new HashSet<GeoDataset>();
        values = new GeoValues();
        subSeries = new HashSet<String>();
    }

    /**
     * @param contributer
     */
    public void addContributer( GeoContact contributer ) {
        this.contributers.add( contributer );
    }

    /**
     * @param dataset
     */
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
     * @param s
     */
    public void addSamples( Collection<GeoSample> s ) {
        this.samples.addAll( s );
    }

    /**
     * @param keyword
     */
    public void addToKeyWords( String keyword ) {
        this.keyWords.add( keyword );
    }

    /**
     * @param id in format "1239954" or "1239954,2194919", etc. The latter will be split into two.
     */
    public void addToPubmedIds( String id ) {
        String[] ids = id.split( "," );
        assert ids.length > 0;
        for ( String s : ids ) {
            this.pubmedIds.add( s );
        }
        assert this.pubmedIds.size() > 0;
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
     * @return
     */
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
     * Returns the date the series was last updated.
     * 
     * @return String
     */
    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    /**
     * @return Returns the overallDesign.
     */
    public String getOverallDesign() {
        return this.overallDesign;
    }

    /**
     * @return Returns the pubmedIds.
     */
    public Collection<String> getPubmedIds() {
        return this.pubmedIds;
    }

    /**
     * @return Returns the replicates.
     */
    public Map<Integer, GeoReplication> getReplicates() {
        return this.replicates;
    }

    /**
     * @return Returns the sampleCorrespondence.
     */
    public GeoSampleCorrespondence getSampleCorrespondence() {
        return this.sampleCorrespondence;
    }

    public Collection<GeoSample> getSamples() {
        return this.samples;
    }

    /**
     * @return Returns the summaries.
     */
    public String getSummaries() {
        return this.summary;
    }

    /**
     * @return String
     */
    public String getSupplementaryFile() {
        return supplementaryFile;
    }

    public GeoValues getValues() {
        return values;
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
     * @param contact The contact to set.
     */
    public void setContact( GeoContact contact ) {
        this.contact = contact;
    }

    /**
     * @param contributers The contributers to set.
     */
    public void setContributers( Collection<GeoContact> contributers ) {
        this.contributers = contributers;
    }

    public void setDataSets( Collection<GeoDataset> dataSets ) {
        this.dataSets = dataSets;
    }

    /**
     * @param type The type to set.
     */
    public void setKeyWords( Collection<String> type ) {
        this.keyWords = type;
    }

    /**
     * Sets the date the series was last updated.
     * 
     * @param lastUpdateDate
     */
    public void setLastUpdateDate( String lastUpdateDate ) {
        this.lastUpdateDate = lastUpdateDate;
    }

    /**
     * @param overallDesign The overallDesign to set.
     */
    public void setOverallDesign( String overallDesign ) {
        this.overallDesign = overallDesign;
    }

    /**
     * @param pubmedIds The pubmedIds to set.
     */
    public void setPubmedIds( Collection<String> pubmedIds ) {
        this.pubmedIds = pubmedIds;
    }

    /**
     * @param replicates The replicates to set.
     */
    public void setReplicates( Map<Integer, GeoReplication> replicates ) {
        this.replicates = replicates;
    }

    /**
     * @param sampleCorrespondence The sampleCorrespondence to set.
     */
    public void setSampleCorrespondence( GeoSampleCorrespondence sampleCorrespondence ) {
        this.sampleCorrespondence = sampleCorrespondence;
    }

    /**
     * @param isSubSeries the isSubSeries to set
     */
    public void setIsSubSeries( boolean isSubSeries ) {
        this.isSubSeries = isSubSeries;
    }

    /**
     * @param summaries The summaries to set.
     */
    public void setSummaries( String summary ) {
        this.summary = summary;
    }

    /**
     * @param isSuperSeries the isSuperSeries to set
     */
    public void setIsSuperSeries( boolean isSuperSeries ) {
        this.isSuperSeries = isSuperSeries;
    }

    /**
     * @param supplementaryFile
     */
    public void setSupplementaryFile( String supplementaryFile ) {
        this.supplementaryFile = supplementaryFile;
    }

    public void setValues( GeoValues values ) {
        this.values = values;
    }

    /**
     * @param webLinks The webLinks to set.
     */
    public void setWebLinks( Collection<String> webLinks ) {
        this.webLinks = webLinks;
    }

    /**
     * @param value
     */
    public void addSubSeries( String value ) {
        this.subSeries.add( value );
    }

    /**
     * @return the subSeries
     */
    public Collection<String> getSubSeries() {
        return subSeries;
    }

}
