/*
 * The gemma-model project
 * 
 * Copyright (c) 2015 University of British Columbia
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

package ubic.gemma.model.expression.experiment;

/**
 * Simplified representation of Geeq
 * 
 * @author paul
 * @version $Id$
 */
public class GeeqValueObject {

    /**
     * Experiment that this is for
     */
    private Long eeId;

    private String eeShortName;

    /**
     * ID of the GEEQ
     */
    private Long id;

    private double quality;

    private double suitability;

    public GeeqValueObject( Geeq entity ) {
        this.suitability = entity.getSuitability();
        this.id = entity.getId();
        /*
         * FIXME need the experiment information.
         */
    }

    public Long getEeId() {
        return eeId;
    }

    public String getEeShortName() {
        return eeShortName;
    }

    public Long getId() {
        return id;
    }

    public double getQuality() {
        return quality;
    }

    public double getSuitability() {
        return suitability;
    }

    public void setEeId( Long eeId ) {
        this.eeId = eeId;
    }

    public void setEeShortName( String eeShortName ) {
        this.eeShortName = eeShortName;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setQuality( double quality ) {
        this.quality = quality;
    }

    public void setSuitability( double suitability ) {
        this.suitability = suitability;
    }

}
