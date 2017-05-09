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
package ubic.gemma.persistence.service.common.measurement;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.measurement.Unit;

/**
 * @author paul
 * @version $Id$
 * @see UnitService
 */
@Service
public class UnitServiceImpl implements UnitService {

    @Autowired
    private UnitDao unitDao;

    /**
     * @param entities
     * @return
     * @see UnitDao#create(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public Collection<Unit> create( Collection<Unit> entities ) {
        return ( Collection<Unit> ) unitDao.create( entities );
    }

    /**
     * @param unit
     * @return
     * @see UnitDao#create(ubic.gemma.model.common.measurement.Unit)
     */
    @Override
    @Transactional
    public Unit create( Unit unit ) {
        return unitDao.create( unit );
    }

    /**
     * @param unit
     * @return
     * @see UnitDao#find(ubic.gemma.model.common.measurement.Unit)
     */
    @Override
    @Transactional(readOnly = true)
    public Unit find( Unit unit ) {
        return unitDao.find( unit );
    }

    /**
     * @param unit
     * @return
     * @see UnitDao#findOrCreate(ubic.gemma.model.common.measurement.Unit)
     */
    @Override
    @Transactional
    public Unit findOrCreate( Unit unit ) {
        return unitDao.findOrCreate( unit );
    }

    /**
     * @param id
     * @return
     * @see UnitDao#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public Unit load( Long id ) {
        return unitDao.load( id );
    }

    /**
     * @return
     * @see UnitDao#loadAll()
     */
    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Collection<Unit> loadAll() {
        return ( Collection<Unit> ) unitDao.loadAll();
    }

    /**
     * @param entities
     * @see UnitDao#remove(java.util.Collection)
     */
    @Override
    @Transactional
    public void remove( Collection<Unit> entities ) {
        unitDao.remove( entities );
    }

    /**
     * @param id
     * @see UnitDao#remove(java.lang.Long)
     */
    @Override
    @Transactional
    public void remove( Long id ) {
        unitDao.remove( id );
    }

    /**
     * @param unit
     * @see UnitDao#remove(ubic.gemma.model.common.measurement.Unit)
     */
    @Override
    @Transactional
    public void remove( Unit unit ) {
        unitDao.remove( unit );
    }

    /**
     * @param entities
     * @see UnitDao#update(java.util.Collection)
     */
    @Override
    @Transactional
    public void update( Collection<Unit> entities ) {
        unitDao.update( entities );
    }

    /**
     * @param unit
     * @see UnitDao#update(ubic.gemma.model.common.measurement.Unit)
     */
    @Override
    @Transactional
    public void update( Unit unit ) {
        unitDao.update( unit );
    }

    /**
     * @return the unitDao
     */
    UnitDao getUnitDao() {
        return unitDao;
    }

}
