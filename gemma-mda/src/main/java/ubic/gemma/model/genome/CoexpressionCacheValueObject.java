/*
 * The Gemma project.
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
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
    private String geneType;

    private double pvalue;

    private Gene queryGene;

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

    public String getGeneType() {
        return geneType;
    }

    public double getPvalue() {
        return pvalue;
    }

    public Gene getQueryGene() {
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

    public void setGeneType( String geneType ) {
        this.geneType = geneType;
    }

    public void setPvalue( double pvalue ) {
        this.pvalue = pvalue;
    }

    public void setQueryGene( Gene queryGene ) {
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
