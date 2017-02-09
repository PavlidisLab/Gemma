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

import ubic.gemma.model.common.description.DatabaseEntry;

/**
 * An association between a BioSequence and a GeneProduct based on external database identifiers.
 */
public abstract class ReferenceAssociation extends BioSequence2GeneProduct {

    /**
     * 
     */
    private static final long serialVersionUID = -6338026603382275762L;

    /**
     * Constructs new instances of {@link ReferenceAssociation}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ReferenceAssociation}.
         */
        public static ReferenceAssociation newInstance() {
            return new ReferenceAssociationImpl();
        }

    }

    private DatabaseEntry referencedDatabaseEntry;

    /**
     * 
     */
    public DatabaseEntry getReferencedDatabaseEntry() {
        return this.referencedDatabaseEntry;
    }

    public void setReferencedDatabaseEntry( DatabaseEntry referencedDatabaseEntry ) {
        this.referencedDatabaseEntry = referencedDatabaseEntry;
    }

}