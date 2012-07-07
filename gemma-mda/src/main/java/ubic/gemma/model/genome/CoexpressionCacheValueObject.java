/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.genome;

import java.io.Serializable;

/**
 * Used to cache low-level results
 * 
 * @author paul
 * @version $Id$
 */
public class CoexpressionCacheValueObject implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long coexpressedGene;
    private Long coexpressedProbe;
    private Long expressionExperiment;

    private Long queryGene;

    private Long queryProbe;

    private double score;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CoexpressionCacheValueObject other = ( CoexpressionCacheValueObject ) obj;
        if ( coexpressedGene == null ) {
            if ( other.coexpressedGene != null ) return false;
        } else if ( !coexpressedGene.equals( other.coexpressedGene ) ) return false;
        if ( coexpressedProbe == null ) {
            if ( other.coexpressedProbe != null ) return false;
        } else if ( !coexpressedProbe.equals( other.coexpressedProbe ) ) return false;
        if ( expressionExperiment == null ) {
            if ( other.expressionExperiment != null ) return false;
        } else if ( !expressionExperiment.equals( other.expressionExperiment ) ) return false;
        if ( queryGene == null ) {
            if ( other.queryGene != null ) return false;
        } else if ( !queryGene.equals( other.queryGene ) ) return false;
        if ( queryProbe == null ) {
            if ( other.queryProbe != null ) return false;
        } else if ( !queryProbe.equals( other.queryProbe ) ) return false;
        return true;
    }

    public Long getCoexpressedGene() {
        return coexpressedGene;
    }

    public Long getCoexpressedProbe() {
        return coexpressedProbe;
    }

    public Long getExpressionExperiment() {
        return expressionExperiment;
    }

    public Long getQueryGene() {
        return queryGene;
    }

    public Long getQueryProbe() {
        return queryProbe;
    }

    public double getScore() {
        return score;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( coexpressedGene == null ) ? 0 : coexpressedGene.hashCode() );
        result = prime * result + ( ( coexpressedProbe == null ) ? 0 : coexpressedProbe.hashCode() );
        result = prime * result + ( ( expressionExperiment == null ) ? 0 : expressionExperiment.hashCode() );
        result = prime * result + ( ( queryGene == null ) ? 0 : queryGene.hashCode() );
        result = prime * result + ( ( queryProbe == null ) ? 0 : queryProbe.hashCode() );
        return result;
    }

    public void setCoexpressedGene( Long coexpressedGene ) {
        this.coexpressedGene = coexpressedGene;
    }

    public void setCoexpressedProbe( Long coexpressedProbe ) {
        this.coexpressedProbe = coexpressedProbe;
    }

    public void setExpressionExperiment( Long expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public void setQueryGene( Long queryGene ) {
        this.queryGene = queryGene;
    }

    public void setQueryProbe( Long queryProbe ) {
        this.queryProbe = queryProbe;
    }

    public void setScore( double score ) {
        this.score = score;
    }

    @Override
    public String toString() {
        return this.queryGene + " coexpressed with gene=" + this.coexpressedGene + " in " + expressionExperiment
                + " with probe " + coexpressedProbe;
    }

}
