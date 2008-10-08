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

import ubic.basecode.dataStructure.DoublePoint;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.Gene;

/**
 * @author kelsey
 * @version $Id$
 */
public class GeneExpressionProfile {

    /*
     * This is a collection because probes are not specific.
     */
    Collection<Gene> genes;
    List<DoublePoint> points;
    DesignElement probe;
    private String color = "black";

    public GeneExpressionProfile( DoubleVectorValueObject vector, String color ) {
        this.genes = vector.getGenes();
        this.probe = vector.getDesignElement();
        this.points = new ArrayList<DoublePoint>();

        if ( color != null ) {
            this.color = color;
        }

        double[] data = vector.standardize();
        int i = 0;
        for ( Double d : data ) {
            points.add( new DoublePoint( i, d ) );
            i++;
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

    public void setPoint( List<DoublePoint> points ) {
        this.points = points;
    }

    public DesignElement getProbe() {
        return probe;
    }

    public void setProbe( DesignElement probe ) {
        this.probe = probe;
    }

}
