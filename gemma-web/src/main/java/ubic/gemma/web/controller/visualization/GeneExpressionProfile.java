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
package ubic.gemma.web.controller.visualization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.DoublePoint;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.Gene;

/**
 * @author kelsey
 * @version $Id$
 */
public class GeneExpressionProfile {

    private static Log log = LogFactory.getLog( GeneExpressionProfile.class );

    /*
     * This is a collection because probes are not specific.
     */
    Collection<Gene> genes;
    List<DoublePoint> points;
    DesignElement probe;
    int factor;
    private String color = "black";

    public GeneExpressionProfile( DoubleVectorValueObject vector, String color, int factor ) {
        this.genes = vector.getGenes();
        this.probe = vector.getDesignElement();
        this.probe.setArrayDesign( null );
        this.points = new ArrayList<DoublePoint>();
        this.factor = factor;
        
        if ( color != null ) {
            this.color = color;
        }

        double[] data = vector.standardize();
        int i = 0;
        // Also test to make sure all the data isn't NAN
        Boolean allNan = true;
        for ( Double d : data ) {
            
            if (!d.equals( Double.NaN))
                allNan = false;
            
            // TESTING: simulate missing data.
            // if ( RandomUtils.nextDouble() < 0.1 || i == 0) {
            // points.add( new DoublePoint( i, Double.NaN ) );
            // } else {
            points.add( new DoublePoint( i, d ) );
            // }
            i++;
        }
        // If all nan change points to null;
        if (allNan){
            points = null;
            //log.info( "All Nan removed from: " + this.probe);
        }
    }

    public String getColor() {
        return color;
    }

    public void setColor( String color ) {
        this.color = color;
    }

    public Collection<Gene> getGenes() {
        return genes;
    }

    public void setGenes( Collection<Gene> genes ) {
        this.genes = genes;
    }

    public List<DoublePoint> getPoints() {
        return this.points;
    }

    public void setPoints( List<DoublePoint> points ) {
        this.points = points;
    }

    public DesignElement getProbe() {
        return probe;
    }

    public void setProbe( DesignElement probe ) {
        this.probe = probe;
    }

    public int getFactor() {
        return factor;
    }

    public void setFactor( int factor ) {
        this.factor = factor;
    }

}
