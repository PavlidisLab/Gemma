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
package ubic.gemma.core.analysis.report;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.SecurityService;
import ubic.gemma.core.security.authentication.ManualAuthenticationService;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.*;

/**
 * Service to collect data on objects that are new in the system.
 * <p>
 * Every report is computed under an anonymous security context, so the ACL post-filter on
 * {@link AuditEventService#getNewSinceDate} / {@link AuditEventService#getUpdatedSinceDate}
 * leaves only what a logged-out visitor can see.
 * <p>
 * <b>Caching is the caller's job.</b> These reports used to be written to disk as a Java
 * object stream under {@code $gemma.appdata.home/WhatsNew/} by a nightly Quartz job, because
 * generating one is slow. That cache outlived its only reader when gemma-web was retired
 * (commit {@code bb154eee88}) — the job kept writing files nobody loaded, and the read path
 * carried a deserialization allow-list to defend a format nothing parsed. The caching
 * requirement is real, so it moved rather than disappeared: {@link HomeStats} carries the
 * corpus-wide counts, refreshed daily and persisted as JSON by {@link HomeStatsRefresher}.
 *
 * @author pavlidis
 */
@Component("whatsNewService")
public class WhatsNewServiceImpl implements WhatsNewService {

    private static final Log log = LogFactory.getLog( WhatsNewServiceImpl.class.getName() );

    @Autowired
    private AuditEventService auditEventService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private ManualAuthenticationService manualAuthenticationService;

    @Override
    @Transactional(readOnly = true)
    public WhatsNew getDailyReport() {
        return this.getReport( DateUtils.addDays( new Date(), -1 ) );
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsNew getWeeklyReport() {
        return this.getReport( DateUtils.addWeeks( new Date(), -1 ) );
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsNew getReport( Date since ) {
        return this.asAnonymousUser( () -> this.buildReport( since ) );
    }

    @Override
    @Transactional(readOnly = true)
    public long countNewExpressionExperiments( Date since ) {
        // getNewSinceDate is @PostFilter'd on READ, so running it anonymously yields exactly the
        // experiments a logged-out visitor can see. Counting the result is the whole method —
        // the point is to skip buildReport's platform / taxon / biomaterial passes.
        return this.asAnonymousUser(
                () -> ( long ) auditEventService.getNewSinceDate( ExpressionExperiment.class, since ).size() );
    }

    /**
     * Run a report computation from the perspective of an anonymous user.
     */
    private <T> T asAnonymousUser( java.util.concurrent.Callable<T> work ) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication( manualAuthenticationService.authenticateAnonymously() );
        try {
            return new DelegatingSecurityContextCallable<>( work, context ).call();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private WhatsNew buildReport( Date date ) {
        WhatsNew wn = new WhatsNew( date );

        Collection<Auditable> updatedObjects = new HashSet<>();
        updatedObjects.addAll( auditEventService.getUpdatedSinceDate( ArrayDesign.class, date ) );
        updatedObjects.addAll( auditEventService.getUpdatedSinceDate( ExpressionExperiment.class, date ) );
        wn.setUpdatedObjects( updatedObjects );
        WhatsNewServiceImpl.log.info( wn.getUpdatedObjects().size() + " updated objects since " + date );

        Collection<Auditable> newObjects = new HashSet<>();
        newObjects.addAll( auditEventService.getNewSinceDate( ArrayDesign.class, date ) );
        newObjects.addAll( auditEventService.getNewSinceDate( ExpressionExperiment.class, date ) );
        wn.setNewObjects( newObjects );
        WhatsNewServiceImpl.log.info( wn.getNewObjects().size() + " new objects since " + date );

        Collection<ExpressionExperiment> updatedExpressionExperiments = this.getExpressionExperiments( updatedObjects );
        Collection<ExpressionExperiment> newExpressionExperiments = this.getExpressionExperiments( newObjects );
        Collection<ArrayDesign> updatedArrayDesigns = this.getArrayDesigns( updatedObjects );
        Collection<ArrayDesign> newArrayDesigns = this.getArrayDesigns( newObjects );

        // don't show things that are "new" as "updated" too (if they were updated after being loaded)
        updatedExpressionExperiments.removeAll( newExpressionExperiments );
        updatedArrayDesigns.removeAll( newArrayDesigns );

        // build new and updated counts by taxon to display in data summary widget on front page
        wn.setNewEEIdsPerTaxon( this.getExpressionExperimentIdsByTaxon( newExpressionExperiments ) );
        wn.setUpdatedEEIdsPerTaxon( this.getExpressionExperimentIdsByTaxon( updatedExpressionExperiments ) );

        wn.setNewBioMaterialCount( this.getBioMaterialCount( newExpressionExperiments ) );

        return wn;
    }

    /**
     * @param items a collection of objects that may include array designs
     * @return the array design subset of the collection passed in
     */
    private Collection<ArrayDesign> getArrayDesigns( Collection<Auditable> items ) {

        Collection<ArrayDesign> ads = new HashSet<>();
        for ( Auditable auditable : items ) {
            if ( auditable instanceof ArrayDesign ) {
                ads.add( ( ArrayDesign ) auditable );
            }
        }
        return ads;
    }

    /**
     * @param ees a collection of expression experiments
     * @return the number of biomaterials in all the expression experiments passed in
     */
    private long getBioMaterialCount( Collection<ExpressionExperiment> ees ) {
        long count = 0;
        for ( ExpressionExperiment ee : ees ) {
            count += this.expressionExperimentService.getBioMaterialCount( ee );
        }
        return count;
    }

    /**
     * Give breakdown by taxon. "Private" experiments are not included.
     */
    private Map<Taxon, Collection<Long>> getExpressionExperimentIdsByTaxon( Collection<ExpressionExperiment> ees ) {
        /*
         * Sort taxa by name.
         */
        SortedMap<Taxon, Collection<Long>> eesPerTaxon = new TreeMap<>( Comparator.comparing( Taxon::getScientificName, Comparator.nullsLast( Comparator.naturalOrder() ) ) );

        if ( ees.isEmpty() )
            return eesPerTaxon;

        Collection<ExpressionExperiment> publicEEs = securityService.choosePublic( ees );

        Map<ExpressionExperiment, Taxon> taxa = expressionExperimentService.getTaxa( publicEEs );

        // invert the map.
        for ( Map.Entry<ExpressionExperiment, Taxon> entry : taxa.entrySet() ) {
            ExpressionExperiment ee = entry.getKey();
            Taxon t = entry.getValue();
            Collection<Long> ids;
            if ( eesPerTaxon.containsKey( t ) ) {
                ids = eesPerTaxon.get( t );
            } else {
                ids = new ArrayList<>();
            }
            ids.add( ee.getId() );
            eesPerTaxon.put( t, ids );
        }
        return eesPerTaxon;
    }

    /**
     * @param items a collection of objects that may include expression experiments
     * @return the expression experiment subset of the collection passed in
     */
    private Collection<ExpressionExperiment> getExpressionExperiments( Collection<Auditable> items ) {

        Collection<ExpressionExperiment> ees = new HashSet<>();
        for ( Auditable auditable : items ) {
            if ( auditable instanceof ExpressionExperiment ) {
                ees.add( ( ExpressionExperiment ) auditable );
            }
        }
        return ees;
    }
}
