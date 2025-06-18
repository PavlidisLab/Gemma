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

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.Collection;

/**
 * Expression data for one probe; designed for conveying small amounts of data to clients. NOTE the data are
 * standardized by default.
 *
 * @author kelsey
 *
 */
public class GeneExpressionProfile {

    @Override
    public String toString() {

        StringBuilder buf = new StringBuilder();
        for ( double d : profile ) {
            buf.append( String.format( "  %.2g", d ) );
        }

        return "Profile: " + ( genes != null ? "genes=" + StringUtils.join( genes, "," ) : "" )
                + ( profile != null ? "\ndata=" + buf + "\n" : "" );
    }

    private boolean allMissing = true;

    /**
     * A hint about what color to use to display this vector in visualizations.
     */
    private String color = "black";

    private double[] profile;

    /**
     * Whether the vector is adjusted to mean=0, variance=1
     */
    private boolean standardized;

    /**
     * A value indicating 'importance', which can be used to influence display in visualizations. BADLY NAMED.
     */
    private Integer factor;

    /*
     * This is a collection because probes are not specific.
     */
    private Collection<GeneValueObject> genes;
    private CompositeSequenceValueObject probe;
    private Double pValue;
    private Double rank;

    /**
     */
    public GeneExpressionProfile( DoubleVectorValueObject vector ) {
        this( vector, null, null, null, null, true );
        this.rank = vector.getRank();
    }

    /**
     */
    public GeneExpressionProfile( DoubleVectorValueObject vector, Collection<GeneValueObject> genes ) {
        this( vector, genes, null, null, null, true );
        this.rank = vector.getRank();
    }

    /**
     */
    public GeneExpressionProfile( DoubleVectorValueObject vector, Collection<GeneValueObject> genes, String color,
            Integer factor, Double pValue ) {
        this( vector, genes, color, factor, pValue, true );
    }

    /**
     */
    public GeneExpressionProfile( DoubleVectorValueObject vector, Collection<GeneValueObject> genes, String color,
            Integer factor, Double pValue, boolean standardize ) {
        this.genes = genes;
        this.probe = vector.getDesignElement();
        this.probe.setArrayDesign( null );
        this.factor = factor;
        this.pValue = pValue;
        this.rank = vector.getRank();

        if ( color != null ) {
            this.color = color;
        }

        this.standardized = standardize;

        if ( this.standardized ) {
            this.profile = vector.standardize();
        } else {
            this.profile = vector.getData();
        }

        // Also test to make sure all the data isn't NAN
        for ( Double d : this.profile ) {
            if ( !d.equals( Double.NaN ) ) {
                this.allMissing = false;
                break;
            }
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

    public CompositeSequenceValueObject getProbe() {
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

    public Double getRank() {
        return rank;
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

    public void setStandardized( boolean standardized ) {
        this.standardized = standardized;
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

    public void setProbe( CompositeSequenceValueObject probe ) {
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

    public void setRank( Double rank ) {
        this.rank = rank;
    }

}
