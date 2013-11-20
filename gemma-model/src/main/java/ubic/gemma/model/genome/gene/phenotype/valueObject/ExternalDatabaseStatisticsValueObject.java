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

import java.util.Date;

public class ExternalDatabaseStatisticsValueObject implements Comparable<ExternalDatabaseStatisticsValueObject> {

    private String name = "";
    private String description = "";
    private String webUri = "";
    private Long numEvidence = 0L;
    private Long numGenes = 0L;
    private Long numPhenotypes = 0L;
    private Long numPublications = 0L;
    private Date lastUpdateDate = null;
    private String pathToDownloadFile = "";

    public ExternalDatabaseStatisticsValueObject() {
        super();
    }

    public ExternalDatabaseStatisticsValueObject( String name, String description, String webUri, Long numEvidence,
            Long numGenes, Long numPhenotypes, Long numPublications, Date lastUpdateDate, String pathToDownloadFile ) {
        super();
        this.name = name;
        this.description = description;
        this.webUri = webUri;
        this.numEvidence = numEvidence;
        this.numGenes = numGenes;
        this.numPhenotypes = numPhenotypes;
        this.numPublications = numPublications;
        this.lastUpdateDate = lastUpdateDate;
        this.pathToDownloadFile = pathToDownloadFile;
    }

    public void addNumPublications( Long addNumPublications ) {
        this.numPublications += addNumPublications;
    }

    @Override
    public int compareTo( ExternalDatabaseStatisticsValueObject externalDatabaseStatisticsValueObject ) {

        if ( this.webUri.equals( "" ) ) {
            return 1;
        }

        return this.getName().toLowerCase().compareTo( externalDatabaseStatisticsValueObject.getName().toLowerCase() );
    }

    public String getDescription() {
        return this.description;
    }

    public Date getLastUpdateDate() {
        return this.lastUpdateDate;
    }

    public String getName() {
        return this.name;
    }

    public Long getNumEvidence() {
        return this.numEvidence;
    }

    public Long getNumGenes() {
        return this.numGenes;
    }

    public Long getNumPhenotypes() {
        return this.numPhenotypes;
    }

    public Long getNumPublications() {
        return this.numPublications;
    }

    public String getWebUri() {
        return this.webUri;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setLastUpdateDate( Date lastUpdateDate ) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setNumEvidence( Long numEvidence ) {
        this.numEvidence = numEvidence;
    }

    public void setNumGenes( Long numGenes ) {
        this.numGenes = numGenes;
    }

    public void setNumPhenotypes( Long numPhenotypes ) {
        this.numPhenotypes = numPhenotypes;
    }

    public void setNumPublications( Long numPublications ) {
        this.numPublications = numPublications;
    }

    public void setWebUri( String webUri ) {
        this.webUri = webUri;
    }

    public String getPathToDownloadFile() {
        return pathToDownloadFile;
    }

    public void setPathToDownloadFile( String pathToDownloadFile ) {
        this.pathToDownloadFile = pathToDownloadFile;
    }

}
