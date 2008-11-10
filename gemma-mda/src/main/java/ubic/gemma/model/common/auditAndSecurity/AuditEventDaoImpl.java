/*
 * The Gemma project.
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
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;
import java.util.HashSet;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import ubic.gemma.model.common.Auditable;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent
 * @author pavlidis
 * @version $Id$
 */
public class AuditEventDaoImpl extends ubic.gemma.model.common.auditAndSecurity.AuditEventDaoBase {

    /**
     * Classes that we track for 'updated since'. This is used for "What's new" functionality.
     */
    private static String[] AUDITABLES_TO_TRACK_FOR_WHATSNEW = {
    // "ubic.gemma.model.expression.analysis.ExpressionAnalysisImpl",
            "ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl",
            // "ubic.gemma.model.common.description.BibliographicReferenceImpl",
            // "ubic.gemma.model.common.auditAndSecurity.ContactImpl",
            "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" };

    /**
     * Note that this only returns selected classes of auditables.
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#getNewSinceDate(java.util.Date)
     * @return Collection of Auditables
     */
    @Override
    @SuppressWarnings("unchecked")
    protected java.util.Collection<Auditable> handleGetNewSinceDate( java.util.Date date ) {
        Collection<Auditable> result = new HashSet<Auditable>();
        for ( String clazz : AUDITABLES_TO_TRACK_FOR_WHATSNEW ) {
            String queryString = "select distinct adb from "
                    + clazz
                    + " adb inner join adb.auditTrail atr inner join atr.events as ae where ae.date > :date and ae.action='C'";
            try {
                org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
                queryObject.setParameter( "date", date );
                result.addAll( queryObject.list() );
            } catch ( org.hibernate.HibernateException ex ) {
                throw super.convertHibernateAccessException( ex );
            }
        }
        return result;
    }

    /**
     * Note that this only returns selected classes of auditables.
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#getUpdatedSinceDate(java.util.Date)
     * @return Collection of Auditables
     */
    @Override
    @SuppressWarnings("unchecked")
    protected java.util.Collection handleGetUpdatedSinceDate( java.util.Date date ) {
        Collection<Auditable> result = new HashSet<Auditable>();
        for ( String clazz : AUDITABLES_TO_TRACK_FOR_WHATSNEW ) {
            String queryString = "select distinct adb from "
                    + clazz
                    + " adb inner join adb.auditTrail atr inner join atr.events as ae where ae.date > :date and ae.action='U'";
            try {
                org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
                queryObject.setParameter( "date", date );
                result.addAll( queryObject.list() );
            } catch ( org.hibernate.HibernateException ex ) {
                throw super.convertHibernateAccessException( ex );
            }
        }
        return result;
    }

    @Override
    protected void handleThaw( final AuditEvent auditEvent ) throws Exception {
        this.getHibernateTemplate().execute( new HibernateCallback() {
            public Object doInHibernate( Session session ) throws HibernateException {
                /*
                 * FIXME this check really won't work. This thaw will not operate correctly if the event isn't already
                 * associated with the session.
                 */
                if ( session.get( AuditEventImpl.class, auditEvent.getId() ) == null ) {
                    session.lock( auditEvent, LockMode.NONE );
                }
                Hibernate.initialize( auditEvent );
                Hibernate.initialize( auditEvent.getPerformer() );
                return null;
            }
        } );

    }

}