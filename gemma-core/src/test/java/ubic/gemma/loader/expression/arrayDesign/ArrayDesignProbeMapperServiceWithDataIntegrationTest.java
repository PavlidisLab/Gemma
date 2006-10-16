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
package ubic.gemma.loader.expression.arrayDesign;

import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignProbeMapperServiceWithDataIntegrationTest extends BaseSpringContextTest {

    /**
     * @throws Exception
     */
    public final void testProbeMappingForArrayDesign() throws Exception {

        // this test works on HG-U133A with real blat results loaded (pain)
        // load the test chromosome entities
        // load the test sequence data
        // load the test blat results
        // load the test array design (refers to the sequences)
        // need audit trail...

        // process it.
        ArrayDesignService ads = ( ArrayDesignService ) getBean( "arrayDesignService" );
        final ArrayDesign ad = ads.load( 1 );
        assert ad != null;

        // All this to avoid lazy load errors.
        HibernateDaoSupport hds = new HibernateDaoSupport() {
        };
        hds.setSessionFactory( ( SessionFactory ) this.getBean( "sessionFactory" ) );
        HibernateTemplate templ = hds.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( ad, LockMode.READ );
                ad.getCompositeSequences().size();
                for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                    cs.getBiologicalCharacteristic().getTaxon();
                }
                return null;
            }
        }, true );

        Taxon taxon = ( ( TaxonService ) getBean( "taxonService" ) ).findByScientificName( "Homo sapiens" );
        ArrayDesignSequenceAlignmentService aligner = ( ArrayDesignSequenceAlignmentService ) getBean( "arrayDesignSequenceAlignmentService" );
        aligner.processArrayDesign( ad, taxon );

    }
}
