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

import ubic.gemma.model.genome.Gene;

/**
 * @author luke
 */
public class ExtCoexpressionValueObject {
    
    private Gene queryGene;
    private Gene foundGene;
    private Integer supportKey;
    private Integer positiveLinks;
    private Integer negativeLinks;
    private Integer numDatasetsLinkTestedIn;
    private Integer goOverlap;
    private Integer possibleOverlap;
    private byte[] testedDatasetVector;
    private byte[] supportingDatasetVector;
    
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

    public byte[] getTestedDatasetVector() {
        return testedDatasetVector;
    }

    public void setTestedDatasetVector( byte[] testedDatasetVector ) {
        this.testedDatasetVector = testedDatasetVector;
    }

    public byte[] getSupportingDatasetVector() {
        return supportingDatasetVector;
    }

    public void setSupportingDatasetVector( byte[] supportingDatasetVector ) {
        this.supportingDatasetVector = supportingDatasetVector;
    }
    
}
