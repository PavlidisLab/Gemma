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
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.util.Slice;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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

    private final FactorValueDao factorValueDao;
    private final StatementDao statementDao;

    @Autowired
    public FactorValueServiceImpl( FactorValueDao factorValueDao, StatementDao statementDao ) {
        super( factorValueDao );
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

    @Nonnull
    @Override
    @Transactional(readOnly = true)
    public <T extends Exception> FactorValue loadWithExperimentalFactorOrFail( Long id, Function<String, T> exceptionSupplier ) throws T {
        FactorValue fv = loadOrFail( id, exceptionSupplier );
        Hibernate.initialize( fv.getExperimentalFactor() );
        return fv;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<FactorValue, Characteristic> getExperimentalFactorCategoriesIgnoreAcls( Collection<FactorValue> factorValues ) {
        return factorValueDao.getExperimentalFactorCategories( factorValues );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<FactorValue, ExpressionExperiment> getExpressionExperimentsIgnoreAcls( Collection<FactorValue> factorValues ) {
        return factorValueDao.getExpressionExperimentsIgnoreAcls( factorValues );
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
    @Transactional(readOnly = true)
    public Collection<FactorValue> loadIgnoreAcls( Set<Long> ids ) {
        return factorValueDao.load( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<FactorValue> loadAll( int offset, int limit ) {
        return factorValueDao.loadAll( offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> loadAllIds() {
        return factorValueDao.loadAllIds();
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<Long> loadAllIds( int offset, int limit ) {
        return factorValueDao.loadAllIds( offset, limit );
    }

    @Override
    @Deprecated
    @Transactional(readOnly = true)
    public Collection<FactorValue> findByValueStartingWith( String valuePrefix, int maxResults ) {
        return this.factorValueDao.findByValueStartingWith( valuePrefix, maxResults );
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
            fv.getCharacteristics().add( statement );
        }
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
            fv.getCharacteristics().add( statement );
        }
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
}