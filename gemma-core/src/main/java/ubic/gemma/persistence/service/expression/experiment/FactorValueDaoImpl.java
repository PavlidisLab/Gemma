/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractNoopFilteringVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import javax.annotation.Nullable;
import java.util.*;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.FactorValue</code>.
 * </p>
 */
@Repository
public class FactorValueDaoImpl extends AbstractNoopFilteringVoEnabledDao<FactorValue, FactorValueValueObject>
        implements FactorValueDao {

    @Autowired
    public FactorValueDaoImpl( SessionFactory sessionFactory ) {
        super( FactorValue.class, sessionFactory );
    }

    @Override
    public Collection<FactorValue> findByValue( String valuePrefix ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "from FactorValue where value like :q" )
                .setParameter( "q", valuePrefix + "%" ).list();
    }

    @Override
    public void remove( @Nullable final FactorValue factorValue ) {
        if ( factorValue == null )
            return;

        // detach from the experimental factor
        factorValue.getExperimentalFactor().getFactorValues().remove( factorValue );

        // detach the factor from any sample
        int deleted = this.getSessionFactory().getCurrentSession()
                .createSQLQuery( "delete from BIO_MATERIAL_FACTOR_VALUES bmfv where bmfv.FACTOR_VALUES_FK = :fvId" )
                .addSynchronizedEntityClass( BioMaterial.class )
                .setParameter( "fvId", factorValue.getId() )
                .executeUpdate();
        AbstractDao.log.info( String.format( "%s was detached from %d samples.", factorValue, deleted ) );

        super.remove( factorValue );
    }

    @Override
    public FactorValue find( FactorValue factorValue ) {
        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( FactorValue.class );

        BusinessKey.checkKey( factorValue );

        BusinessKey.createQueryObject( queryObject, factorValue );

        return ( FactorValue ) queryObject.uniqueResult();
    }

    @Override
    public void removeCharacteristic( FactorValue fv, Statement statement ) {
        if ( !fv.getCharacteristics().remove( statement ) ) {
            throw new IllegalArgumentException( String.format( "The statement %s is not associated with the factor value %s", statement, fv ) );
        }
        if ( statement.getObject() != null ) {
            Statement objectAsStatement = ( Statement ) getSessionFactory().getCurrentSession()
                    .get( Statement.class, statement.getObject().getId() );
            if ( objectAsStatement != null && fv.getCharacteristics().contains( objectAsStatement ) ) {
                statement.setObject( null );
            }
        }
        if ( statement.getSecondObject() != null ) {
            Statement secondObjectAsStatement = ( Statement ) getSessionFactory().getCurrentSession()
                    .get( Statement.class, statement.getSecondObject().getId() );
            if ( secondObjectAsStatement != null && fv.getCharacteristics().contains( secondObjectAsStatement ) ) {
                statement.setSecondObject( null );
            }
        }
        getSessionFactory().getCurrentSession().delete( statement );
    }

    @Override
    public Set<Statement> cloneCharacteristics( FactorValue fv ) {
        Collection<Statement> ch = fv.getCharacteristics();
        // pair of original -> clone
        Map<Statement, Statement> clonedStatements = new HashMap<>();
        List<Statement> result = new ArrayList<>( ch.size() );
        for ( Statement s : ch ) {
            result.add( cloneStatement( s, clonedStatements ) );
        }
        return new HashSet<>( result );
    }

    private Statement cloneStatement( Statement s, Map<Statement, Statement> clonedStatements ) {
        // because of how statements (and characteristics) are hashed, the ID must be non-null
        if ( s.getId() == null ) {
            throw new IllegalArgumentException( String.format( "Cannot clone non-persistent %s.", s ) );
        }
        Statement clone = clonedStatements.get( s );
        if ( clone != null ) {
            return clone;
        } else {
            clone = Statement.Factory.newInstance();
            clonedStatements.put( s, clone );
        }
        clone.setName( s.getName() );
        clone.setDescription( s.getDescription() );
        clone.setValue( s.getValue() );
        clone.setValueUri( s.getValueUri() );
        clone.setCategory( s.getCategory() );
        clone.setCategoryUri( s.getCategoryUri() );
        clone.setEvidenceCode( s.getEvidenceCode() );
        clone.setPredicate( s.getPredicate() );
        clone.setPredicateUri( s.getPredicateUri() );
        if ( s.getObject() != null ) {
            if ( s.getObject() instanceof Statement ) {
                clone.setObject( cloneStatement( ( Statement ) s.getObject(), clonedStatements ) );
            } else {
                clone.setObject( cloneCharacteristic( s.getObject() ) );
            }
        }
        clone.setSecondPredicate( s.getSecondPredicate() );
        clone.setSecondPredicateUri( s.getSecondPredicateUri() );
        if ( s.getSecondObject() != null ) {
            if ( s.getSecondObject() instanceof Statement ) {
                clone.setSecondObject( cloneStatement( ( Statement ) s.getSecondObject(), clonedStatements ) );
            } else {
                clone.setSecondObject( cloneCharacteristic( s.getSecondObject() ) );
            }
        }
        return clone;
    }

    private Characteristic cloneCharacteristic( Characteristic c ) {
        if ( c.getId() == null ) {
            throw new IllegalArgumentException( String.format( "Cannot clone non-persistent %s.", c ) );
        }
        return Characteristic.Factory.newInstance( c.getName(), c.getDescription(), c.getValue(), c.getValueUri(),
                c.getCategory(), c.getCategoryUri(), c.getEvidenceCode() );
    }

    @Override
    protected FactorValueValueObject doLoadValueObject( FactorValue entity ) {
        return new FactorValueValueObject( entity );
    }

    private void debug( List<?> results ) {
        StringBuilder sb = new StringBuilder();
        sb.append( "\nFactorValues found:\n" );
        for ( Object object : results ) {
            sb.append( object ).append( "\n" );
        }
        AbstractDao.log.error( sb.toString() );
    }
}