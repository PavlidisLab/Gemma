/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.association;

/**
 * 
 */
public abstract class Gene2GeneProteinAssociation extends ubic.gemma.model.association.Gene2GeneAssociation {

    /**
     * Constructs new instances of {@link ubic.gemma.model.association.Gene2GeneProteinAssociation}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.association.Gene2GeneProteinAssociation}.
         */
        public static ubic.gemma.model.association.Gene2GeneProteinAssociation newInstance() {
            return new ubic.gemma.model.association.Gene2GeneProteinAssociationImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8142881537025422087L;
    private Double confidenceScore;

    private byte[] evidenceVector;

    private ubic.gemma.model.common.description.DatabaseEntry databaseEntry;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Gene2GeneProteinAssociation() {
    }

    /**
     * 
     */
    public Double getConfidenceScore() {
        return this.confidenceScore;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.description.DatabaseEntry getDatabaseEntry() {
        return this.databaseEntry;
    }

    /**
     * 
     */
    public byte[] getEvidenceVector() {
        return this.evidenceVector;
    }

    public void setConfidenceScore( Double confidenceScore ) {
        this.confidenceScore = confidenceScore;
    }

    public void setDatabaseEntry( ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        this.databaseEntry = databaseEntry;
    }

    public void setEvidenceVector( byte[] evidenceVector ) {
        this.evidenceVector = evidenceVector;
    }

}