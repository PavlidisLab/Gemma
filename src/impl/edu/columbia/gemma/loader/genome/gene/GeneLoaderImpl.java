/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.genome.gene;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.loader.loaderutils.Persister;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="geneLoader"
 * @spring.property name="geneDao" ref="geneDao"
 */
public class GeneLoaderImpl implements Persister {
    protected static final Log log = LogFactory.getLog( GeneLoaderImpl.class );

    private GeneDao geneDao;

    /**
     * Persist genes in collection.
     * 
     * @param col
     */
    public Collection<Object> persist( Collection<Object> col ) {
        for ( Object object : col ) {
            this.persist( object );
        }
        return col;
    }

    /**
     * Persist gene.
     * 
     * @param gene
     */
    public Object persist( Object obj ) {
        assert obj instanceof Gene;
        return geneDao.findOrCreate( ( Gene ) obj );
    }

    /**
     * @return Returns the geneDao.
     */
    public GeneDao getGeneDao() {
        return geneDao;
    }

    /**
     * 
     */
    public void removeAll() {
        assert geneDao != null;
        Collection col = geneDao.findAllGenes();
        geneDao.remove( col );
    }

    public void removeAll( Collection col ) {
        Iterator iter = col.iterator();
        while ( iter.hasNext() ) {
            Gene g = ( Gene ) iter.next();
            this.getGeneDao().remove( g );
        }
    }

    /**
     * @param geneDao The geneDao to set.
     */
    public void setGeneDao( GeneDao geneDao ) {
        this.geneDao = geneDao;
    }
}
