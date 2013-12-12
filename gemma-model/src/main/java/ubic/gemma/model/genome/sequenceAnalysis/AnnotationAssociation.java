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
package ubic.gemma.model.genome.sequenceAnalysis;

import ubic.gemma.model.common.description.ExternalDatabase;

/**
 * An association between BioSequence and GeneProduct that is provided through an external annotation source, rather
 * than our own sequence analysis. Importantly, the 'overlap', 'score' and other parameters will not be filled in. Also
 * note that in these cases the associated BioSequence may not have actual sequence information filled in. This type of
 * association is used as a "last resort" annotation source for the following types of situations: No sequence
 * information is available; annotations are unavailable (e.g., non-model organisms); or sequences are too short to
 * align using our usual methods (e.g., miRNAs).
 */
public abstract class AnnotationAssociation extends ubic.gemma.model.association.BioSequence2GeneProduct {

    /**
     * Constructs new instances of {@link AnnotationAssociation}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link AnnotationAssociation}.
         */
        public static AnnotationAssociation newInstance() {
            return new AnnotationAssociationImpl();
        }

    }

    private ExternalDatabase source;

    /**
     * <p>
     * The original source of the annotation, such as GEO or flyBase.
     * </p>
     */
    public ExternalDatabase getSource() {
        return this.source;
    }

    public void setSource( ExternalDatabase source ) {
        this.source = source;
    }

}