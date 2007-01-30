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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.AuditableImpl;
import ubic.gemma.util.ReflectionUtil;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent
 * @author pavlidis
 * @version $Id$
 */
public class AuditEventDaoImpl extends ubic.gemma.model.common.auditAndSecurity.AuditEventDaoBase {

    /**
     * FIXME this isn't complete.
     */
    private static String[] AUDITABLES = { "ubic.gemma.model.expression.analysis.ExpressionAnalysisImpl",
            "ubic.gemma.model.expression.arrayDesign.ArrayDesignImpl",
            "ubic.gemma.model.common.description.BibliographicReferenceImpl",
            "ubic.gemma.model.common.auditAndSecurity.ContactImpl",
            "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl" };

    private static Log log = LogFactory.getLog( AuditEventDaoImpl.class.getName() );

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#getUpdatedSinceDate(java.util.Date)
     * @return Collection of Auditables
     */
    @SuppressWarnings("unchecked")
    protected java.util.Collection handleGetUpdatedSinceDate( java.util.Date date ) {
        // first get the audit trails for the objects. Then we have to get the auditables.

        String queryString = "select atr from AuditTrailImpl as atr inner join atr.events as ae where ae.date > :date and ae.action='U'";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "date", date );
            Collection<AuditTrail> trails = queryObject.list();
            return getAuditablesForTrails( trails );
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * @param trails
     * @return
     */
    @SuppressWarnings("unchecked")
    private java.util.Collection getAuditablesForTrails( Collection<AuditTrail> trails ) {
        Collection<Auditable> result = new HashSet<Auditable>();
        if ( trails == null || trails.size() == 0 ) return result;

        /*
         * This gives a list of trails for objects...now we have to get the owning auditables.
         */
        for ( String clazz : AUDITABLES ) {
            String auditableQuery = "from " + clazz + " as a where a.auditTrail in (:ats)";
            log.debug( auditableQuery );
            org.hibernate.Query auditableQueryObj = super.getSession( false ).createQuery( auditableQuery );
            auditableQueryObj.setParameterList( "ats", trails );
            Collection<Auditable> auditables = auditableQueryObj.list();
            if ( auditables != null && auditables.size() > 0 ) {
                result.addAll( auditables );
            }
        }

        return result;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEventDao#getNewSinceDate(java.util.Date)
     * @return Collection of Auditables
     */
    @SuppressWarnings("unchecked")
    protected java.util.Collection handleGetNewSinceDate( java.util.Date date ) {
        String queryString = "select atr from AuditTrailImpl as atr inner join atr.events as ae where ae.date > :date and ae.action='C'";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "date", date );
            Collection<AuditTrail> trails = queryObject.list();
            return getAuditablesForTrails( trails );
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

}