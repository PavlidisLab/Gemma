/*
 * The Gemma-Production project
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

/**
 * 
 */

package ubic.gemma.web.controller.visualization;

import java.util.ArrayList;
import java.util.Collection;

import ubic.basecode.dataStructure.DoublePoint;
import ubic.basecode.dataStructure.Point;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

public class VisualizationValueObject {

    private Collection<DoublePoint> cordinates;
    private Collection<Gene> genes;
    private ExpressionExperiment ee;

    
    public VisualizationValueObject(){
        super();
    }
    
    public VisualizationValueObject( Collection<DoublePoint> xy, Collection<Gene> g, ExpressionExperiment ee ) {
        this();

        this.cordinates = xy;
        this.genes = g;
        this.ee = ee;

    }
    
    /**
     * 
     * @param dvvo
     */
    
    public VisualizationValueObject(DoubleVectorValueObject dvvo){
        this();
        
        setEE( dvvo.getExpressionExperiment() );
        setGenes( dvvo.getGenes() );
        
        Collection<DoublePoint> points = new ArrayList<DoublePoint>();
        double[] data = dvvo.getNormalizedDEDV();
        int i = 0;
        for(Double d : data){
            points.add( new DoublePoint(i,d) );
            i++;
        }

        
    }

    // ---------------------------------
    // Getters and Setters
    // ---------------------------------

    public Collection<DoublePoint> getCordinates() {
        return cordinates;
    }

    public void setCordinates( Collection<DoublePoint> cordinates ) {
        this.cordinates = cordinates;
    }

    public Collection<Gene> getGenes() {
        return genes;
    }

    public void setGenes( Collection<Gene> genes ) {
        this.genes = genes;
    }

    public ExpressionExperiment getEE() {
        return ee;
    }

    public void setEE( ExpressionExperiment ee ) {
        this.ee = ee;
    }

}
