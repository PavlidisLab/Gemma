/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DoesNotNeedAttentionEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FactorValueNeedsAttentionEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.NeedsAttentionEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Spring Service base class for <code>FactorValueService</code>, provides access
 * to all services and entities referenced by this service.
 * </p>
 *
 * @author pavlidis
 * @author keshav
 * @see FactorValueService
 */
@Service
public class FactorValueServiceImpl extends AbstractFilteringVoEnabledService<FactorValue, FactorValueValueObject>
        implements FactorValueService {

    private final ExpressionExperimentService expressionExperimentService;
    private final AuditTrailService auditTrailService;
    private final AuditEventService auditEventService;
    private final FactorValueDao factorValueDao;
    private final StatementDao statementDao;

    @Autowired
    public FactorValueServiceImpl( ExpressionExperimentService expressionExperimentService, AuditTrailService auditTrailService, AuditEventService auditEventService, FactorValueDao factorValueDao, StatementDao statementDao ) {
        super( factorValueDao );
        this.expressionExperimentService = expressionExperimentService;
        this.auditTrailService = auditTrailService;
        this.auditEventService = auditEventService;
        this.factorValueDao = factorValueDao;
        this.statementDao = statementDao;
    }

    @Override
    @Transactional(readOnly = true)
    public FactorValue loadWithExperimentalFactor( Long id ) {
        FactorValue fv = load( id );
        if ( fv != null ) {
            Hibernate.initialize( fv.getExperimentalFactor() );
        }
        return fv;
    }

    @Override
    @Transactional(readOnly = true)
    public FactorValue loadWithExperimentalFactorOrFail( Long id ) {
        FactorValue fv = loadOrFail( id );
        Hibernate.initialize( fv.getExperimentalFactor() );
        return fv;
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public FactorValue loadWithOldStyleCharacteristics( Long id, boolean readOnly ) {
        return factorValueDao.loadWithOldStyleCharacteristics( id, readOnly );
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public Map<Long, Integer> loadIdsWithNumberOfOldStyleCharacteristics( Set<Long> excludedIds ) {
        return factorValueDao.loadIdsWithNumberOfOldStyleCharacteristics( excludedIds );
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public Collection<FactorValue> findByValue( String valuePrefix ) {
        return this.factorValueDao.findByValue( valuePrefix );
    }

    @Override
    @Transactional
    public Statement createStatement( FactorValue factorValue, Statement statement ) {
        Assert.notNull( factorValue.getId(), "The factor value must be persistent." );
        Assert.isNull( statement.getId(), "The statement to be added must not be persistent." );
        // note here that once the statement is persisted, it is compared by ID, so it won't clash with any other
        // identical statement
        statement = this.statementDao.create( statement );
        factorValue.getCharacteristics().add( statement );
        factorValueDao.update( factorValue );
        return statement;
    }

    @Override
    @Transactional
    public Statement saveStatement( FactorValue fv, Statement statement ) {
        Assert.notNull( fv.getId(), "The factor value must be persistent." );
        if ( statement.getId() != null && !fv.getCharacteristics().contains( statement ) ) {
            throw new IllegalArgumentException( "The given persistent statement is not associated with the factor value." );
        }
        // same for createStatement() not applies here and in addition, if the statement already belongs to the FV, this
        // is a noop
        if ( statement.getId() == null ) {
            statement = statementDao.create( statement );
        }
        fv.getCharacteristics().add( statement );
        factorValueDao.update( fv );
        return statement;
    }

    @Override
    @Transactional
    public Statement saveStatementIgnoreAcl( FactorValue fv, Statement statement ) {
        Assert.notNull( fv.getId(), "The factor value must be persistent." );
        if ( statement.getId() != null && !fv.getCharacteristics().contains( statement ) ) {
            throw new IllegalArgumentException( "The given persistent statement is not associated with the factor value." );
        }
        // same for createStatement() not applies here and in addition, if the statement already belongs to the FV, this
        // is a noop
        if ( statement.getId() == null ) {
            statement = statementDao.create( statement );
        }
        fv.getCharacteristics().add( statement );
        // careful here!
        factorValueDao.updateIgnoreAcl( fv );
        return statement;
    }

    @Override
    @Transactional
    public void removeStatement( FactorValue fv, Statement statement ) {
        Assert.notNull( fv.getId(), "The factor value must be persistent." );
        Assert.notNull( statement.getId(), "The statement to be removed must be persistent." );
        if ( !fv.getCharacteristics().remove( statement ) ) {
            throw new IllegalArgumentException( String.format( "%s is not associated with %s", statement, fv ) );
        }
        this.factorValueDao.update( fv );
        // now we can safely delete it
        this.statementDao.remove( statement );
    }

    @Override
    @Transactional
    public void remove( FactorValue entity ) {
        super.remove( ensureInSession( entity ) );
    }

    @Override
    @Transactional
    public void remove( Collection<FactorValue> entities ) {
        super.remove( ensureInSession( entities ) );
    }

    @Override
    @Transactional
    public void markAsNeedsAttention( FactorValue factorValue, String note ) {
        Assert.isTrue( !factorValue.getNeedsAttention(), "This FactorValue already needs attention." );
        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( factorValue );
        factorValue.setNeedsAttention( true );
        factorValueDao.update( factorValue );
        if ( ee != null ) {
            auditTrailService.addUpdateEvent( ee, FactorValueNeedsAttentionEvent.class, String.format( "%s needs attention: %s", factorValue, note ) );
        }
    }

    @Override
    @Transactional
    public void clearNeedsAttentionFlag( FactorValue factorValue, String note ) {
        Assert.isTrue( factorValue.getNeedsAttention(), "This FactorValue does not need attention." );
        ExpressionExperiment ee = expressionExperimentService.findByFactorValue( factorValue );
        factorValue.setNeedsAttention( false );
        factorValueDao.update( factorValue );
        if ( ee != null ) {
            boolean needsAttention = ee.getCurationDetails().getNeedsAttention();
            AuditEvent ae = auditEventService.getLastEvent( ee, NeedsAttentionEvent.class,
                    Collections.singleton( FactorValueNeedsAttentionEvent.class ) );
            AuditEvent nae = auditEventService.getLastEvent( ee, DoesNotNeedAttentionEvent.class );
            // ensure that the last NeedsAttentionEvent hasn't been fixed already
            boolean hasNeedsAttentionEvent = ae != null && ( nae == null || ae.getDate().after( nae.getDate() ) );
            // check if all the FVs are OK
            boolean hasFactorValueThatNeedsAttention = ee.getExperimentalDesign().getExperimentalFactors().stream()
                    .flatMap( ef -> ef.getFactorValues().stream() )
                    .anyMatch( FactorValue::getNeedsAttention );
            if ( needsAttention && !hasNeedsAttentionEvent && !hasFactorValueThatNeedsAttention ) {
                auditTrailService.addUpdateEvent( ee, DoesNotNeedAttentionEvent.class, note );
            }
        }
    }
}