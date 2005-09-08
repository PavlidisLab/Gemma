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
package edu.columbia.gemma.loader.association;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.association.Gene2GOAssociation;
import edu.columbia.gemma.association.Gene2GOAssociationDao;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;
import edu.columbia.gemma.loader.loaderutils.Persister;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="gene2GOAssociationLoader"
 * @spring.property name="gene2GOAssociationDao" ref="gene2GOAssociationDao"
 */
public class Gene2GOAssociationLoaderImpl implements Persister {

    protected static final Log log = LogFactory.getLog( Gene2GOAssociationLoaderImpl.class );

    private Gene2GOAssociationDao gene2GOAssociationDao;

    /**
     * @param oeCol
     */
    public void persist( Collection<Object> g2GoCol ) {

        log.info( "persisting Gemma objects (if object exists it will not be persisted) ..." );

        Collection<Gene2GOAssociation> g2GoColFromDatabase = getGene2GOAssociationDao().findAllGene2GOAssociations();

        int count = 0;
        for ( Object ob : g2GoCol ) {
            assert gene2GOAssociationDao != null;
            assert ob instanceof Gene2GOAssociation;
            Gene2GOAssociation g2Go = ( Gene2GOAssociation ) ob;

            if ( g2GoColFromDatabase.size() == 0 ) {

                persist( g2Go );
                count++;
                ParserAndLoaderTools.objectsPersistedUpdate( count, 1000, "Gene2GOAssociation objects" );

            } else {
                for ( Gene2GOAssociation g2GoFromDatabase : g2GoColFromDatabase ) {
                    if ( ( !g2Go.getGene().equals( g2GoFromDatabase.getGene() ) )
                            && ( !g2Go.getAssociatedOntologyEntry().equals(
                                    g2GoFromDatabase.getAssociatedOntologyEntry() ) ) ) {
                        persist( g2Go );
                        count++;
                        ParserAndLoaderTools.objectsPersistedUpdate( count, 1000, "Gene2GOAssociation objects" );
                    }
                }
            }
        }
    }

    /**
     * @param object
     */
    public void persist( Object object ) {
        assert object instanceof Gene2GOAssociation;
        Gene2GOAssociation g2Go = ( Gene2GOAssociation ) object;
        getGene2GOAssociationDao().create( g2Go );
    }

    /**
     * @param gene2GOAssociation
     */
    public void create( Gene2GOAssociation gene2GOAssociation ) {
        getGene2GOAssociationDao().create( gene2GOAssociation );
    }

    /**
     * @return Returns the gene2GOAssociationDao.
     */
    public Gene2GOAssociationDao getGene2GOAssociationDao() {
        return gene2GOAssociationDao;
    }

    /**
     * @param gene2GOAssociationDao The gene2GOAssociationDao to set.
     */
    public void setGene2GOAssociationDao( Gene2GOAssociationDao gene2GOAssociationDao ) {
        this.gene2GOAssociationDao = gene2GOAssociationDao;
    }

}
