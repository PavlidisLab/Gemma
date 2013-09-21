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
 * An association between a BioSequence and a GeneProduct based on external database identifiers.
 */
public abstract class ReferenceAssociation extends ubic.gemma.model.association.BioSequence2GeneProduct {

    /**
     * Constructs new instances of {@link ubic.gemma.model.association.ReferenceAssociation}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.association.ReferenceAssociation}.
         */
        public static ubic.gemma.model.association.ReferenceAssociation newInstance() {
            return new ubic.gemma.model.association.ReferenceAssociationImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1476215275010160696L;
    private ubic.gemma.model.common.description.DatabaseEntry referencedDatabaseEntry;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ReferenceAssociation() {
    }

    /**
     * 
     */
    public ubic.gemma.model.common.description.DatabaseEntry getReferencedDatabaseEntry() {
        return this.referencedDatabaseEntry;
    }

    public void setReferencedDatabaseEntry( ubic.gemma.model.common.description.DatabaseEntry referencedDatabaseEntry ) {
        this.referencedDatabaseEntry = referencedDatabaseEntry;
    }

}