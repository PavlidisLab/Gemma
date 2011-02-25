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
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * Expression data for one probe; designed for conveying small amounts of data to clients. NOTE the data are
 * standardized by default.
 * 
 * @author kelsey
 * @version $Id$
 */
public class GeneExpressionProfile {

    private boolean allMissing = true;

    /**
     * A hint about what color to use to display this vector in visualizations.
     */
    private String color = "black";

    private double[] profile;

    /**
     * Whether the vector is adjusted to mean=0, variance=1
     */
    private boolean standardized = true;

    /**
     * A value indicating 'importance', which can be used to influence display in visualizations. BADLY NAMED.
     */
    private Integer factor;

    /*
     * This is a collection because probes are not specific.
     */
    private Collection<GeneValueObject> genes;
    private CompositeSequence probe;
    private Double pValue = null;

    /**
     * @param vector
     */
    public GeneExpressionProfile( DoubleVectorValueObject vector ) {
        this( vector, null, null, null, true );
    }

    /**
     * @param vector
     * @param color
     * @param factor
     * @param pValue
     */
    public GeneExpressionProfile( DoubleVectorValueObject vector, String color, Integer factor, Double pValue ) {
        this( vector, color, factor, pValue, true );
    }

    public GeneExpressionProfile( DoubleVectorValueObject vector, String color, Integer factor, Double pValue,
            boolean standardize ) {
        this.genes = GeneValueObject.convert2ValueObjects( vector.getGenes() );
        this.probe = vector.getDesignElement();
        this.probe.setArrayDesign( null );
        this.factor = factor;
        this.pValue = pValue;

        if ( color != null ) {
            this.color = color;
        }

        this.standardized = standardize;

        if ( this.standardized ) {
            this.profile = vector.standardize();
        } else {
            this.profile = vector.getData();
        }

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

    public CompositeSequence getProbe() {
        return probe;
    }

    /**
     * @return the profile
     */
    public double[] getProfile() {
        return profile;
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

    /**
     * @return the standardized
     */
    public boolean isStandardized() {
        return standardized;
    }

    public void setColor( String color ) {
        this.color = color;
    }

    public void setFactor( Integer factor ) {
        this.factor = factor;
    }

    public void setGenes( Collection<GeneValueObject> genes ) {
        this.genes = genes;
    }

    public void setProbe( CompositeSequence probe ) {
        this.probe = probe;
    }

    /**
     * @param profile the profile to set
     */
    public void setProfile( double[] profile ) {
        this.profile = profile;
    }

    public void setPValue( Double value ) {
        pValue = value;
    }

}
