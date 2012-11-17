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
package ubic.gemma.model.genome.gene.phenotype.valueObject;

public class ExternalDatabaseStatisticsValueObject {

    private String name = "";
    private String description = "";
    private String webUri = "";
    private Long numEvidence = 0L;
    private Long numGenes = 0L;
    private Long numPhenotypes = 0L;
    private String lastUpdateDate = "";

    public ExternalDatabaseStatisticsValueObject( String name, String description, String webUri, Long numEvidence,
            Long numGenes, Long numPhenotypes, String lastUpdateDate ) {
        super();
        this.name = name;
        this.description = description;
        this.webUri = webUri;
        this.numEvidence = numEvidence;
        this.numGenes = numGenes;
        this.numPhenotypes = numPhenotypes;
        this.lastUpdateDate = lastUpdateDate;
    }

    public String getName() {
        return this.name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getWebUri() {
        return this.webUri;
    }

    public void setWebUri( String webUri ) {
        this.webUri = webUri;
    }

    public Long getNumEvidence() {
        return this.numEvidence;
    }

    public void setNumEvidence( Long numEvidence ) {
        this.numEvidence = numEvidence;
    }

    public Long getNumGenes() {
        return this.numGenes;
    }

    public void setNumGenes( Long numGenes ) {
        this.numGenes = numGenes;
    }

    public Long getNumPhenotypes() {
        return this.numPhenotypes;
    }

    public void setNumPhenotypes( Long numPhenotypes ) {
        this.numPhenotypes = numPhenotypes;
    }

    public String getLastUpdateDate() {
        return this.lastUpdateDate;
    }

    public void setLastUpdateDate( String lastUpdateDate ) {
        this.lastUpdateDate = lastUpdateDate;
    }

}
