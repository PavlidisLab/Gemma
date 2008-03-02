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
package ubic.gemma.web.controller.coexpressionSearch;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.genome.Gene;

/**
 * @author luke
 */
public class CoexpressionValueObjectExt {
    
    private Gene queryGene;
    private Gene foundGene;
    private String sortKey;
    private Integer supportKey;
    private Integer positiveLinks;
    private Integer negativeLinks;
    private Integer nonSpecificPositiveLinks;
    private Integer nonSpecificNegativeLinks;
    private Boolean hybridizesWithQueryGene;
    private Integer numDatasetsLinkTestedIn;
    private Integer goOverlap;
    private Integer possibleOverlap;
    private Long[] testedDatasetVector;
    private Long[] supportingDatasetVector;
    
    public Gene getQueryGene() {
        return queryGene;
    }
    
    public void setQueryGene( Gene queryGene ) {
        this.queryGene = queryGene;
    }
    
    public Gene getFoundGene() {
        return foundGene;
    }
    
    public void setFoundGene( Gene foundGene ) {
        this.foundGene = foundGene;
    }
    
    public String getSortKey() {
        return sortKey;
    }
    
    public void setSortKey() {
        this.sortKey = String.format( "%06f%s", 1.0/getSupportKey(), getFoundGene().getOfficialSymbol() );
    }

    public Integer getSupportKey() {
        return supportKey;
    }

    public void setSupportKey( Integer supportKey ) {
        this.supportKey = supportKey;
    }

    public Integer getPositiveLinks() {
        return positiveLinks;
    }

    public void setPositiveLinks( Integer positiveLinks ) {
        this.positiveLinks = positiveLinks;
    }

    public Integer getNegativeLinks() {
        return negativeLinks;
    }

    public void setNegativeLinks( Integer negativeLinks ) {
        this.negativeLinks = negativeLinks;
    }

    public Integer getNonSpecificPositiveLinks() {
        return nonSpecificPositiveLinks;
    }

    public void setNonSpecificPositiveLinks( Integer nonSpecificPositiveLinks ) {
        this.nonSpecificPositiveLinks = nonSpecificPositiveLinks;
    }

    public Integer getNonSpecificNegativeLinks() {
        return nonSpecificNegativeLinks;
    }

    public void setNonSpecificNegativeLinks( Integer nonSpecificNegativeLinks ) {
        this.nonSpecificNegativeLinks = nonSpecificNegativeLinks;
    }

    public Boolean getHybridizesWithQueryGene() {
        return hybridizesWithQueryGene;
    }

    public void setHybridizesWithQueryGene( Boolean hybridizesWithQueryGene ) {
        this.hybridizesWithQueryGene = hybridizesWithQueryGene;
    }

    public Integer getNumDatasetsLinkTestedIn() {
        return numDatasetsLinkTestedIn;
    }

    public void setNumDatasetsLinkTestedIn( Integer numDatasetsLinkTestedIn ) {
        this.numDatasetsLinkTestedIn = numDatasetsLinkTestedIn;
    }
    
    public Integer getGoOverlap() {
        return goOverlap;
    }
    
    public void setGoOverlap( Integer goOverlap ) {
        this.goOverlap = goOverlap;
    }
    
    public Integer getPossibleOverlap() {
        return possibleOverlap;
    }
    
    public void setPossibleOverlap( Integer possibleOverlap ) {
        this.possibleOverlap = possibleOverlap;
    }

    public Long[] getTestedDatasetVector() {
        return testedDatasetVector;
    }

    public void setTestedDatasetVector( Long[] testedDatasetVector ) {
        this.testedDatasetVector = testedDatasetVector;
    }

    public Long[] getSupportingDatasetVector() {
        return supportingDatasetVector;
    }

    public void setSupportingDatasetVector( Long[] supportingDatasetVector ) {
        this.supportingDatasetVector = supportingDatasetVector;
    }
    
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if ( getPositiveLinks() > 0 ) {
            buf.append( getSupportRow( getPositiveLinks(), "+" ) );
        }
        if ( getNegativeLinks() > 0 ) {
            if ( buf.length() > 0 )
                buf.append( "\n" );
            buf.append( getSupportRow( getNegativeLinks(), "-" ) );
        }
        return buf.toString();
    }    
    
    private String getSupportRow( Integer links, String sign ) {
        String[] fields = new String[] {
            queryGene.getOfficialSymbol(),
            foundGene.getOfficialSymbol(),
            links.toString(),
            sign
        };
        return StringUtils.join( fields, "\t" );
    }
    
}
 