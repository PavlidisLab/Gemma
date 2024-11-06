/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.model.analysis.expression.coexpression;

import org.apache.commons.lang3.StringUtils;

import javax.persistence.Transient;

/**
 * Tracks the datasets in which coexpression for a gene has been tested. Determining if two genes were tested together
 * requires using the and() method.
 *
 * @author Paul
 */
public class GeneCoexpressionTestedIn extends IdArray {

    /**
     * This serves as the primary key.
     */
    private Long geneId;

    /**
     * we store this separately as a column in the DB, which is why it is a field. Otherwise we infer it from the data.
     */
    private int numDatasetsTestedIn;

    // needed to fulfil javabean contract
    public GeneCoexpressionTestedIn() {
    }

    public GeneCoexpressionTestedIn( Long geneId ) {
        this.geneId = geneId;
    }

    @Override
    public synchronized void addEntity( Long ds ) {
        super.addEntity( ds );
        this.refreshCount();
    }

    @Transient
    @Override
    public int getNumIds() {
        if ( numDatasetsTestedIn > 0 )
            return numDatasetsTestedIn;
        this.refreshCount();
        return numDatasetsTestedIn;
    }

    @Override
    public synchronized void removeEntity( Long ds ) {
        super.removeEntity( ds );
        this.refreshCount();
    }

    @Override
    public String toString() {
        return "GeneCoexpressionTestedIn [geneId=" + geneId + ", numDatasetsTestedIn=" + this.getNumIds() + ", data="
                + StringUtils.join( super.getIds(), "," ) + "]";
    }

    public Long getGeneId() {
        return geneId;
    }

    public int getNumDatasetsTestedIn() {
        return this.getNumIds();
    }

    /**
     * Used for serializing/marshalling only. Do not set this value directly otherwise.
     *
     * @param numDatasetsTestedIn the new value
     */
    public void setNumDatasetsTestedIn( int numDatasetsTestedIn ) {
        this.numDatasetsTestedIn = numDatasetsTestedIn;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        if ( geneId != null )
            return geneId.hashCode();

        result = prime * result + numDatasetsTestedIn;
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        GeneCoexpressionTestedIn other = ( GeneCoexpressionTestedIn ) obj;

        if ( geneId != null ) {
            return this.geneId.equals( other.geneId );
        } else if ( other.geneId != null ) {
            return false;
        }

        return numDatasetsTestedIn == other.numDatasetsTestedIn;
    }

    private void refreshCount() {
        this.numDatasetsTestedIn = super.getNumIds();
    }

}
