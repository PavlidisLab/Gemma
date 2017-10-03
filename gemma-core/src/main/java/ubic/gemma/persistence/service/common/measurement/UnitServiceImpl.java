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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.measurement.Unit;

import java.util.Collection;

/**
 * @author paul
 * @see UnitService
 */
@Service
public class UnitServiceImpl implements UnitService {

    @Autowired
    private UnitDao unitDao;

    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public Collection<Unit> create( Collection<Unit> entities ) {
        return unitDao.create( entities );
    }

    @Override
    @Transactional
    public Unit create( Unit unit ) {
        return unitDao.create( unit );
    }

    @Override
    @Transactional(readOnly = true)
    public Unit find( Unit unit ) {
        return unitDao.find( unit );
    }

    @Override
    @Transactional
    public Unit findOrCreate( Unit unit ) {
        return unitDao.findOrCreate( unit );
    }

    @Override
    @Transactional(readOnly = true)
    public Unit load( Long id ) {
        return unitDao.load( id );
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Collection<Unit> loadAll() {
        return unitDao.loadAll();
    }

    @Override
    @Transactional
    public void remove( Collection<Unit> entities ) {
        unitDao.remove( entities );
    }

    @Override
    @Transactional
    public void remove( Long id ) {
        unitDao.remove( id );
    }

    @Override
    @Transactional
    public void remove( Unit unit ) {
        unitDao.remove( unit );
    }

    @Override
    @Transactional
    public void update( Collection<Unit> entities ) {
        unitDao.update( entities );
    }

    @Override
    @Transactional
    public void update( Unit unit ) {
        unitDao.update( unit );
    }

    UnitDao getUnitDao() {
        return unitDao;
    }

}
