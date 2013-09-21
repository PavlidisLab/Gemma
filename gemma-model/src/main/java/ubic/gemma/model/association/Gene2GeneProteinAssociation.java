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

import org.apache.commons.lang3.reflect.FieldUtils;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;

/**
 * 
 */
public abstract class Gene2GeneProteinAssociation extends Gene2GeneAssociation {

    /**
     * Constructs new instances of {@link ubic.gemma.model.association.Gene2GeneProteinAssociation}.
     */
    public static final class Factory {

        public static Gene2GeneProteinAssociation newInstance( Gene firstGene, Gene secondGene,
                DatabaseEntry databaseEntry, byte[] evidenceVector, Double confidenceScore ) {
            Gene2GeneProteinAssociation entity = new Gene2GeneProteinAssociationImpl();

            try {
                FieldUtils.writeField( entity, "secondGene", secondGene, true );
                FieldUtils.writeField( entity, "firstGene", firstGene, true );
                FieldUtils.writeField( entity, "databaseEntry", databaseEntry, true );
                FieldUtils.writeField( entity, "evidenceVector", evidenceVector, true );
                FieldUtils.writeField( entity, "confidenceScore", confidenceScore, true );

            } catch ( IllegalAccessException e ) {
                System.err.println( e );
            }
            return entity;
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8142881537025422087L;
    final private Double confidenceScore = null;

    final private byte[] evidenceVector = null;

    final private DatabaseEntry databaseEntry = null;

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
    public DatabaseEntry getDatabaseEntry() {
        return this.databaseEntry;
    }

    /**
     * 
     */
    public byte[] getEvidenceVector() {
        return this.evidenceVector;
    }

}