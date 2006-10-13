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
package ubic.gemma.apps;

import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class ArrayDesignSequenceManipulatingCli extends AbstractSpringAwareCLI {

    protected void unlazifyArrayDesign( final ArrayDesign arrayDesign ) {
        // unlazify the arrayDesign.
        HibernateDaoSupport hds = new HibernateDaoSupport() {
        };

        hds.setSessionFactory( ( SessionFactory ) this.getBean( "sessionFactory" ) );

        HibernateTemplate templ = hds.getHibernateTemplate();

        log.info( "Unlazifying ArrayDesign..." );
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( arrayDesign, LockMode.READ );
                arrayDesign.getCompositeSequences().size();
                for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
                    if ( cs.getBiologicalCharacteristic() == null ) continue;
                    cs.getBiologicalCharacteristic().getTaxon();
                }
                return null;
            }
        }, true );
    }
}
