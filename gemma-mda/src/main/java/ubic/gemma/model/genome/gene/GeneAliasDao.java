/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.genome.gene;

import org.springframework.stereotype.Repository;

/**
 * @see ubic.gemma.model.genome.gene.GeneAlias
 */
@Repository
public interface GeneAliasDao {

    /**
     * Creates a new instance of ubic.gemma.model.genome.gene.GeneAlias and adds from the passed in
     * <code>entities</code> collection
     * 
     * @param entities the collection of ubic.gemma.model.genome.gene.GeneAlias instances to create.
     * @return the created instances.
     */
    public java.util.Collection create( java.util.Collection entities );

    /**
     * Creates an instance of ubic.gemma.model.genome.gene.GeneAlias and adds it to the persistent store.
     */
    public ubic.gemma.model.genome.gene.GeneAlias create( ubic.gemma.model.genome.gene.GeneAlias geneAlias );

    /**
     * Loads an instance of ubic.gemma.model.genome.gene.GeneAlias from the persistent store.
     */
    public ubic.gemma.model.genome.gene.GeneAlias load( java.lang.Long id );

    /**
     * Loads all entities of type {@link ubic.gemma.model.genome.gene.GeneAlias}.
     * 
     * @return the loaded entities.
     */
    public java.util.Collection loadAll();

    /**
     * Removes the instance of ubic.gemma.model.genome.gene.GeneAlias having the given <code>identifier</code> from the
     * persistent store.
     */
    public void remove( java.lang.Long id );

    /**
     * Removes all entities in the given <code>entities<code> collection.
     */
    public void remove( java.util.Collection entities );

    /**
     * Removes the instance of ubic.gemma.model.genome.gene.GeneAlias from the persistent store.
     */
    public void remove( ubic.gemma.model.genome.gene.GeneAlias geneAlias );

    /**
     * Updates all instances in the <code>entities</code> collection in the persistent store.
     */
    public void update( java.util.Collection entities );

    /**
     * Updates the <code>geneAlias</code> instance in the persistent store.
     */
    public void update( ubic.gemma.model.genome.gene.GeneAlias geneAlias );

}
