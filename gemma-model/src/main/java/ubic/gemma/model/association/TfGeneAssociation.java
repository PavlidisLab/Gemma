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
 * An association between a DNA-binding protein gene and a putative target gene. By convention, the first gene is the
 * TF, the second gene is the target.
 */
public abstract class TfGeneAssociation extends ubic.gemma.model.association.Gene2GeneAssociation {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4563819092987249428L;

    private ubic.gemma.model.common.description.DatabaseEntry databaseEntry;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public TfGeneAssociation() {
    }

    /**
     * 
     */
    public ubic.gemma.model.common.description.DatabaseEntry getDatabaseEntry() {
        return this.databaseEntry;
    }

    public void setDatabaseEntry( ubic.gemma.model.common.description.DatabaseEntry databaseEntry ) {
        this.databaseEntry = databaseEntry;
    }

}