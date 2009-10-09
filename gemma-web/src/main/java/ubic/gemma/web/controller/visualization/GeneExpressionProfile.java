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

import java.util.Collection;

import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * Expression data for one probe; designed for conveying small amounts of data to clients. NOTE the data are
 * standardized.
 * 
 * @author kelsey
 * @version $Id$
 */
public class GeneExpressionProfile {

    private boolean allMissing = true;
    private String color = "black";
    private double[] profile;
    private Integer factor;

    /*
     * This is a collection because probes are not specific.
     */
    private Collection<GeneValueObject> genes;
    private DesignElement probe;
    private Double pValue = null;

    public GeneExpressionProfile( DoubleVectorValueObject vector ) {
        this( vector, null, null, null );
    }

    public GeneExpressionProfile( DoubleVectorValueObject vector, String color, Integer factor, Double pValue ) {
        this.genes = GeneValueObject.convert2GeneValueObjects( vector.getGenes() );
        this.probe = vector.getDesignElement();
        this.probe.setArrayDesign( null );
        this.factor = factor;
        this.pValue = pValue;

        if ( color != null ) {
            this.color = color;
        }

        this.profile = vector.standardize();

        int i = 0;
        // Also test to make sure all the data isn't NAN
        for ( Double d : this.profile ) {
            if ( !d.equals( Double.NaN ) ) this.allMissing = false;
            i++;
        }

    }

    public String getColor() {
        return color;
    }

    public Integer getFactor() {
        return factor;
    }

    public Collection<GeneValueObject> getGenes() {
        return genes;
    }

    public DesignElement getProbe() {
        return probe;
    }

    public Double getPValue() {
        return pValue;
    }

    /**
     * @return the allMissing
     */
    public boolean isAllMissing() {
        return allMissing;
    }

    public void setColor( String color ) {
        this.color = color;
    }

    /**
     * @return the profile
     */
    public double[] getProfile() {
        return profile;
    }

    /**
     * @param profile the profile to set
     */
    public void setProfile( double[] profile ) {
        this.profile = profile;
    }

    public void setFactor( Integer factor ) {
        this.factor = factor;
    }

    public void setGenes( Collection<GeneValueObject> genes ) {
        this.genes = genes;
    }

    public void setProbe( DesignElement probe ) {
        this.probe = probe;
    }

    public void setPValue( Double value ) {
        pValue = value;
    }

}
