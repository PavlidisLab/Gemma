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

import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;

/**
 * 
 */
public abstract class Gene2GOAssociation extends Gene2OntologyEntryAssociationImpl {

    /**
     * 
     */
    private static final long serialVersionUID = -8503436886033033975L;

    /**
     * Constructs new instances of {@link ubic.gemma.model.association.Gene2GOAssociation}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.association.Gene2GOAssociation}, taking all possible
         * properties (except the identifier(s))as arguments.
         */
        public static Gene2GOAssociation newInstance( Gene gene, VocabCharacteristic ontologyEntry,
                GOEvidenceCode evidenceCode ) {
            final Gene2GOAssociation entity = new ubic.gemma.model.association.Gene2GOAssociationImpl();

            try {
                FieldUtils.writeField( entity, "gene", gene, true );
                FieldUtils.writeField( entity, "ontologyEntry", ontologyEntry, true );
                FieldUtils.writeField( entity, "evidenceCode", evidenceCode, true );
            } catch ( IllegalAccessException e ) {
                e.printStackTrace();
            }

            return entity;
        }

    }

    private final GOEvidenceCode evidenceCode = null;

    /**
     * 
     */
    public GOEvidenceCode getEvidenceCode() {
        return this.evidenceCode;
    }

}