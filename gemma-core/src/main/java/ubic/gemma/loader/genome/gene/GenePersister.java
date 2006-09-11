/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.loader.genome.gene;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.persistence.Persister;
import ubic.gemma.persistence.PersisterHelper;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="geneLoader"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @deprecated Use the regular persisterHelper directly
 */
public class GenePersister implements Persister {
    protected static final Log log = LogFactory.getLog( GenePersister.class );

    private PersisterHelper persisterHelper;
    private GeneService geneService;

    /**
     * Persist genes in collection.
     * 
     * @param col
     */
    public Collection<Object> persist( Collection<?> col ) {
        Collection<Object> result = new HashSet<Object>();
        for ( Object object : col ) {
            result.add( this.persist( object ) );
        }
        return result;
    }

    /**
     * Persist gene.
     * 
     * @param gene
     */
    public Object persist( Object obj ) {
        assert obj instanceof Gene;
        return persisterHelper.persist( obj );
    }

    /**
     * 
     */
    public void removeAll() {
        Collection col = geneService.findAll();
        geneService.remove( col );
    }

    /**
     * @param col
     */
    public void removeAll( Collection<Gene> col ) {
        geneService.remove( col );
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService service ) {
        this.geneService = service;
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }
}
