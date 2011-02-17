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
package ubic.gemma.model.common.measurement;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
     * @see ubic.gemma.model.common.measurement.UnitDao#create(java.util.Collection)
     */
    public Collection<Unit> create( Collection<Unit> entities ) {
        return ( Collection<Unit> ) unitDao.create( entities );
    }

    /**
     * @param unit
     * @return
     * @see ubic.gemma.model.common.measurement.UnitDao#create(ubic.gemma.model.common.measurement.Unit)
     */
    public Unit create( Unit unit ) {
        return unitDao.create( unit );
    }

    /**
     * @param unit
     * @return
     * @see ubic.gemma.model.common.measurement.UnitDao#find(ubic.gemma.model.common.measurement.Unit)
     */
    public Unit find( Unit unit ) {
        return unitDao.find( unit );
    }

    /**
     * @param unit
     * @return
     * @see ubic.gemma.model.common.measurement.UnitDao#findOrCreate(ubic.gemma.model.common.measurement.Unit)
     */
    public Unit findOrCreate( Unit unit ) {
        return unitDao.findOrCreate( unit );
    }

    /**
     * @return the unitDao
     */
    public UnitDao getUnitDao() {
        return unitDao;
    }

    /**
     * @param id
     * @return
     * @see ubic.gemma.model.common.measurement.UnitDao#load(java.lang.Long)
     */
    public Unit load( Long id ) {
        return unitDao.load( id );
    }

    /**
     * @return
     * @see ubic.gemma.model.common.measurement.UnitDao#loadAll()
     */
    public Collection loadAll() {
        return unitDao.loadAll();
    }

    /**
     * @param entities
     * @see ubic.gemma.model.common.measurement.UnitDao#remove(java.util.Collection)
     */
    public void remove( Collection<Unit> entities ) {
        unitDao.remove( entities );
    }

    /**
     * @param id
     * @see ubic.gemma.model.common.measurement.UnitDao#remove(java.lang.Long)
     */
    public void remove( Long id ) {
        unitDao.remove( id );
    }

    /**
     * @param unit
     * @see ubic.gemma.model.common.measurement.UnitDao#remove(ubic.gemma.model.common.measurement.Unit)
     */
    public void remove( Unit unit ) {
        unitDao.remove( unit );
    }

    /**
     * @param unitDao the unitDao to set
     */
    public void setUnitDao( UnitDao unitDao ) {
        this.unitDao = unitDao;
    }

    /**
     * @param entities
     * @see ubic.gemma.model.common.measurement.UnitDao#update(java.util.Collection)
     */
    public void update( Collection<Unit> entities ) {
        unitDao.update( entities );
    }

    /**
     * @param unit
     * @see ubic.gemma.model.common.measurement.UnitDao#update(ubic.gemma.model.common.measurement.Unit)
     */
    public void update( Unit unit ) {
        unitDao.update( unit );
    }

}
