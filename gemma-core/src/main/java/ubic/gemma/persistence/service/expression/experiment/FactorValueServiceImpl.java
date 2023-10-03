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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;

import java.util.Collection;

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
    @Deprecated
    @Transactional(readOnly = true)
    public Collection<FactorValue> findByValue( String valuePrefix ) {
        return this.factorValueDao.findByValue( valuePrefix );
    }

    @Override
    @Transactional
    public void remove( FactorValue entity ) {
        super.remove( ensureInSession( entity ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Statement loadStatement( Long statementId ) {
        return statementDao.load( statementId );
    }

    @Override
    @Transactional
    public Statement createStatement( FactorValue factorValue, Statement statement ) {
        if ( statement.getId() != null ) {
            throw new IllegalArgumentException( "The statement must not be persistent in order to be added to a given factor value." );
        }
        statement = this.statementDao.create( statement );
        // note here that once the statement is persisted, it is compared by ID, so it won't clash with any other
        // identical statement
        factorValue.getCharacteristics().add( statement );
        return statement;
    }

    @Override
    @Transactional
    public void removeStatement( FactorValue fv, Statement statement ) {
        fv = ensureInSession( fv );

        if ( !fv.getCharacteristics().remove( statement ) ) {
            throw new IllegalArgumentException( String.format( "%s is not associated with %s", statement, fv ) );
        }

        // now we can safely delete it
        this.statementDao.remove( statement );
    }

    @Override
    @Transactional
    public void remove( Collection<FactorValue> entities ) {
        super.remove( ensureInSession( entities ) );
    }
}