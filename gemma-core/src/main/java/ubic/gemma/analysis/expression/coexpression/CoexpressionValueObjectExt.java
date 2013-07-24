/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.analysis.expression.coexpression;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * Implementation note: This has very abbreviated field names to reduce the size of strings sent to browsers.
 * 
 * @author luke
 * @version $Id$
 */
public class CoexpressionValueObjectExt implements Comparable<CoexpressionValueObjectExt> {

    private Boolean containsMyData = false;
    private String datasetVector;
    private GeneValueObject foundGene;
    private Boolean foundRegulatesQuery = false;
    private String gene2GeneProteinAssociationStringUrl;
    private String gene2GeneProteinInteractionConfidenceScore;
    private String gene2GeneProteinInteractionEvidence;
    private Integer goSim;
    private Integer maxGoSim;
    private Integer negSupp;
    private Integer nonSpecNegSupp;
    private Integer nonSpecPosSupp;
    private Integer numTestedIn;
    private Integer posSupp;
    private GeneValueObject queryGene;
    private Boolean queryRegulatesFound = false;
    private Double queryGeneNodeDegree;
    private Double foundGeneNodeDegree;

    private String sortKey;

    private Collection<Long> supportingExperiments;

    private Integer supportKey;

    @Override
    public int compareTo( CoexpressionValueObjectExt arg0 ) {
        return this.getSortKey().compareTo( arg0.getSortKey() );
    }

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
        CoexpressionValueObjectExt other = ( CoexpressionValueObjectExt ) obj;
        if ( this.sortKey == null ) {
            if ( other.sortKey != null ) return false;
        } else if ( !sortKey.equals( other.sortKey ) ) {            
            return false;
        } else if (!this.queryGene.getOfficialSymbol().equals( other.queryGene.getOfficialSymbol() )) {
            return false;
        }
        return true;
    }
    public Boolean getContainsMyData() {
        return containsMyData;
    }

    public String getDatasetVector() {
        return datasetVector;
    }

    /**
     * @return the coexpressed gene.
     */
    public GeneValueObject getFoundGene() {
        return foundGene;
    }

    public Double getFoundGeneNodeDegree() {
        return foundGeneNodeDegree;
    }

    public Boolean getFoundRegulatesQuery() {
        return foundRegulatesQuery;
    }

    /**
     * @return the gene2GeneProteinAssociationStringUrl
     */
    public String getGene2GeneProteinAssociationStringUrl() {
        return gene2GeneProteinAssociationStringUrl;
    }

    /**
     * @return the gene2GeneProteinInteractionConfidenceScore
     */
    public String getGene2GeneProteinInteractionConfidenceScore() {
        return gene2GeneProteinInteractionConfidenceScore;
    }

    /**
     * @return the gene2GeneProteinInteractionEvidence
     */
    public String getGene2GeneProteinInteractionEvidence() {
        return gene2GeneProteinInteractionEvidence;
    }

    public Integer getGoSim() {
        return goSim;
    }

    public Integer getMaxGoSim() {
        return maxGoSim;
    }

    public Integer getNegSupp() {
        return negSupp;
    }

    public Integer getNonSpecNegSupp() {
        return nonSpecNegSupp;
    }

    public Integer getNonSpecPosSupp() {
        return nonSpecPosSupp;
    }

    public Integer getNumTestedIn() {
        return numTestedIn;
    }

    public Integer getPosSupp() {
        return posSupp;
    }

    public GeneValueObject getQueryGene() {
        return queryGene;
    }

    public Double getQueryGeneNodeDegree() {
        return queryGeneNodeDegree;
    }

    public Boolean getQueryRegulatesFound() {
        return queryRegulatesFound;
    }

    public String getSortKey() {
        return sortKey;
    }

    public Collection<Long> getSupportingExperiments() {
        return supportingExperiments;
    }

    public Integer getSupportKey() {
        return supportKey;
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
        result = prime * result + ( ( sortKey == null ) ? 0 : sortKey.hashCode() );
        return result;
    }

    public void setContainsMyData( Boolean containsMyData ) {
        this.containsMyData = containsMyData;
    }

    public void setDatasetVector( String datasetVector ) {
        this.datasetVector = datasetVector;
    }

    public void setFoundGene( GeneValueObject foundGene ) {
        this.foundGene = foundGene;
    }

    public void setFoundGeneNodeDegree( Double foundGeneNodeDegree ) {
        this.foundGeneNodeDegree = foundGeneNodeDegree;
    }

    public void setFoundRegulatesQuery( Boolean foundRegulatesQuery ) {
        this.foundRegulatesQuery = foundRegulatesQuery;
    }

    /**
     * @param gene2GeneProteinAssociationStringUrl the gene2GeneProteinAssociationStringUrl to set
     */
    public void setGene2GeneProteinAssociationStringUrl( String gene2GeneProteinAssociationStringUrl ) {
        this.gene2GeneProteinAssociationStringUrl = gene2GeneProteinAssociationStringUrl;
    }

    /**
     * @param gene2GeneProteinInteractionConfidenceScore the gene2GeneProteinInteractionConfidenceScore to set
     */
    public void setGene2GeneProteinInteractionConfidenceScore( String gene2GeneProteinInteractionConfidenceScore ) {
        this.gene2GeneProteinInteractionConfidenceScore = gene2GeneProteinInteractionConfidenceScore;
    }

    /**
     * @param gene2GeneProteinInteractionEvidence the gene2GeneProteinInteractionEvidence to set
     */
    public void setGene2GeneProteinInteractionEvidence( String gene2GeneProteinInteractionEvidence ) {
        this.gene2GeneProteinInteractionEvidence = gene2GeneProteinInteractionEvidence;
    }

    public void setGoSim( Integer goSim ) {
        this.goSim = goSim;
    }

    public void setMaxGoSim( Integer maxGoSim ) {
        this.maxGoSim = maxGoSim;
    }

    public void setNegSupp( Integer negSupp ) {
        this.negSupp = negSupp;
    }

    public void setNonSpecNegSupp( Integer nonSpecNegSupp ) {
        this.nonSpecNegSupp = nonSpecNegSupp;
    }

    public void setNonSpecPosSupp( Integer nonSpecPosSupp ) {
        this.nonSpecPosSupp = nonSpecPosSupp;
    }

    public void setNumTestedIn( Integer numTestedIn ) {
        this.numTestedIn = numTestedIn;
    }

    public void setPosSupp( Integer posSupp ) {
        this.posSupp = posSupp;
    }

    public void setQueryGene( GeneValueObject queryGene ) {
        this.queryGene = queryGene;
    }

    public void setQueryGeneNodeDegree( Double queryGeneNodeDegree ) {
        this.queryGeneNodeDegree = queryGeneNodeDegree;
    }

    public void setQueryRegulatesFound( Boolean queryRegulatesFound ) {
        this.queryRegulatesFound = queryRegulatesFound;
    }

    public void setSortKey() {
        this.sortKey = String.format( "%06f%s", 1.0 / Math.abs( getSupportKey() ), getFoundGene().getOfficialSymbol() );
    }

    public void setSortKey( String sortKey ) {
        this.sortKey = sortKey;
    }

    public void setSupportingExperiments( Collection<Long> supportingExperiments ) {
        this.supportingExperiments = supportingExperiments;
    }

    public void setSupportKey( Integer supportKey ) {
        this.supportKey = supportKey;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if ( this.getPosSupp() > 0 ) {
            buf.append( getSupportRow( getPosSupp(), "+" ) );
        }
        if ( getNegSupp() > 0 ) {
            if ( buf.length() > 0 ) buf.append( "\n" );
            buf.append( getSupportRow( getNegSupp(), "-" ) );
        }
        return buf.toString();
    }

    private String getSupportRow( Integer links, String sign ) {
        String[] fields = new String[] { queryGene.getOfficialSymbol(), foundGene.getOfficialSymbol(),
                links.toString(), sign };
        return StringUtils.join( fields, "\t" );
    }

}
