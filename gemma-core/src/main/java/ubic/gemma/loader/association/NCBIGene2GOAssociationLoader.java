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
package ubic.gemma.loader.association;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.persistence.PersisterHelper;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGene2GOAssociationLoader {

    protected static final Log log = LogFactory.getLog( NCBIGene2GOAssociationLoader.class );

    private PersisterHelper persisterHelper;

    protected Collection<Gene2GOAssociation> load( Collection<Collection<Gene2GOAssociation>> g2GoCol ) {
        Collection<Gene2GOAssociation> results = new HashSet<Gene2GOAssociation>();
        int count = 0;
        int geneCount = 0;
        for ( Collection<Gene2GOAssociation> ob : g2GoCol ) {
            if ( ++geneCount % 1000 == 0 ) {
                log.info( "Persisted Gene to GO associations for " + geneCount + " genes" );
            }
            for ( Gene2GOAssociation association : ob ) {
                if ( ++count % 1000 == 0 ) {
                    log.info( "Persisted " + count + " Gene to GO associations" );
                }
                results.add( load( association ) );
            }
        }
        return results;
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param entity
     * @return
     */
    protected Gene2GOAssociation load( Gene2GOAssociation entity ) {
        assert entity.getGene() != null;
        assert entity.getOntologyEntry() != null;
        return ( Gene2GOAssociation ) persisterHelper.persist( entity );
    }
}
