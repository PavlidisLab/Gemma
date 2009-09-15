/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
import java.util.Date;
import java.util.HashSet;

/**
 * Used to contain GEO summary information from the 'Browse' views.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoRecord extends GeoData {

    /**
     * 
     */
    private static final long serialVersionUID = 2060148205381855991L;

    private int numSamples;
    private String contactName;
    private Date releaseDate;
    private Collection<String> organisms;
    private Collection<Long> correspondingExperiments;

    /*
     * How many times a curator has already looked at the details. this helps us track data sets we've already examined
     * for usefulness.
     */
    private int previousClicks = 0;

    public int getPreviousClicks() {
        return previousClicks;
    }

    public void setPreviousClicks( int previousClicks ) {
        this.previousClicks = previousClicks;
    }

    public boolean isUsable() {
        return usable;
    }

    public void setUsable( boolean usable ) {
        this.usable = usable;
    }

    /*
     * Curator judgement about whether this is loadable. False indicates a problem.
     */
    private boolean usable = true;

    public GeoRecord() {
        super();
        this.organisms = new HashSet<String>();
        this.correspondingExperiments = new HashSet<Long>();
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName( String contactName ) {
        this.contactName = contactName;
    }

    public int getNumSamples() {
        return numSamples;
    }

    public void setNumSamples( int numSamples ) {
        this.numSamples = numSamples;
    }

    public Collection<String> getOrganisms() {
        return organisms;
    }

    public void setOrganisms( Collection<String> organisms ) {
        this.organisms = organisms;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate( Date releaseDate ) {
        this.releaseDate = releaseDate;
    }

    @Override
    public String toString() {
        return super.toString() + " " + this.getTitle();
    }

    public Collection<Long> getCorrespondingExperiments() {
        return correspondingExperiments;
    }

    public void setCorrespondingExperiments( Collection<Long> correspondingExperiments ) {
        this.correspondingExperiments = correspondingExperiments;
    }

}
