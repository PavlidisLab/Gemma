/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

package ubic.gemma.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * A collection of functions that are useful for maintaining the database. This is intended
 * for denormalized tables and statistics tables that need to be generated periodically.
 * @author jsantos
 *
 */
public class TableMaintenanceUtil extends HibernateDaoSupport {
    private static Log log = LogFactory.getLog( TableMaintenanceUtil.class.getName() );
    
    /**
     * Function to drop and regenerate the Gene2Cs entries. Gene2Cs is a denormalized join table that
     * allows for a quick link between Genes and CompositeSequences
     *
     */
    public void generateGene2CsEntries () {

        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( Session session ) throws HibernateException {

                log.info( "Deleting all entries for Gene2Cs." );
                String queryString = "TRUNCATE TABLE GENE2CS";
                org.hibernate.SQLQuery queryObject;

                queryObject = session.createSQLQuery( queryString ); // for native query.
                queryObject.executeUpdate();
                
                
                log.info( "Recreating all entries for Gene2Cs." );
                queryString = "INSERT INTO GENE2CS (GENE, CS) SELECT gene.ID AS GENE, cs.ID AS CS " +
                        " FROM CHROMOSOME_FEATURE AS gene, CHROMOSOME_FEATURE AS geneprod,BIO_SEQUENCE2_GENE_PRODUCT AS bsgp,COMPOSITE_SEQUENCE cs " +
                        " WHERE gene.CLASS <> 'GeneProduct' and geneprod.GENE_FK = gene.ID and bsgp.GENE_PRODUCT_FK = geneprod.ID and " +
                        " bsgp.BIO_SEQUENCE_FK = cs.BIOLOGICAL_CHARACTERISTIC_FK;";
                queryObject = session.createSQLQuery( queryString ); // for native query.
                queryObject.executeUpdate();
                log.info( "Done regenerating Gene2Cs." );
                
                session.flush();
                session.clear();
                return null;
            }
        }, true );
    }


}
