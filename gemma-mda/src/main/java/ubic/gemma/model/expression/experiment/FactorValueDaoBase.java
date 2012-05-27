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
package ubic.gemma.model.expression.experiment;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.FactorValue</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.experiment.FactorValue
 */
public abstract class FactorValueDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport
        implements ubic.gemma.model.expression.experiment.FactorValueDao {

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends FactorValue> create(
            final java.util.Collection<? extends FactorValue> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "FactorValue.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends FactorValue> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#create(int transform,
     *      ubic.gemma.model.expression.experiment.FactorValue)
     */
    @Override
    public FactorValue create( final ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        if ( factorValue == null ) {
            throw new IllegalArgumentException( "FactorValue.create - 'factorValue' can not be null" );
        }
        this.getHibernateTemplate().save( factorValue );
        return factorValue;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#load(int, java.lang.Long)
     */
    @Override
    public FactorValue load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "FactorValue.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.experiment.FactorValueImpl.class, id );
        return ( ubic.gemma.model.expression.experiment.FactorValue ) entity;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#loadAll(int)
     */
    @Override
    public java.util.Collection<? extends FactorValue> loadAll() {
        final java.util.Collection<? extends FactorValue> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.experiment.FactorValueImpl.class );
        return results;
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends FactorValue> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "FactorValue.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends FactorValue> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValueDao#update(ubic.gemma.model.expression.experiment.FactorValue)
     */
    @Override
    public void update( ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        if ( factorValue == null ) {
            throw new IllegalArgumentException( "FactorValue.update - 'factorValue' can not be null" );
        }
        this.getHibernateTemplate().update( factorValue );
    }

}