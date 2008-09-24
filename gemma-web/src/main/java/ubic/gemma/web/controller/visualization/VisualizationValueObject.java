/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

package ubic.gemma.web.controller.visualization;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class VisualizationValueObject {

    private Collection<GeneExpressionProfile> profiles;
    private ExpressionExperiment ee = null;

    public VisualizationValueObject() {
        super();
        this.profiles = new HashSet<GeneExpressionProfile>();
    }

    public VisualizationValueObject( Collection<DoubleVectorValueObject> vectors ) {
        this();
        
        for ( DoubleVectorValueObject vector : vectors ) {
            if ( this.ee == null ) {
                this.ee = vector.getExpressionExperiment();
            } 
            else if (!( this.ee.equals( vector.getExpressionExperiment() ))) {
                throw new IllegalArgumentException( "All vectors have to have the same ee for this constructor. ee1: "+ this.ee.getId() + "  ee2: " + vector.getExpressionExperiment().getId());
            }

            GeneExpressionProfile profile = new GeneExpressionProfile( vector );
            profiles.add( profile );
        }
    }

    /**
     * @param dvvo
     */

    public VisualizationValueObject( DoubleVectorValueObject dvvo ) {
        this();
        setEE( dvvo.getExpressionExperiment() );
        GeneExpressionProfile profile = new GeneExpressionProfile( dvvo );
        profiles.add( profile );
    }

    // ---------------------------------
    // Getters and Setters
    // ---------------------------------

    public ExpressionExperiment getEE() {
        return ee;
    }

    public void setEE( ExpressionExperiment ee ) {
        this.ee = ee;
    }

    public Collection<GeneExpressionProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles( Collection<GeneExpressionProfile> profiles ) {
        this.profiles = profiles;
    }

    public ExpressionExperiment getEe() {
        return ee;
    }

    public void setEe( ExpressionExperiment ee ) {
        this.ee = ee;
    }

}
