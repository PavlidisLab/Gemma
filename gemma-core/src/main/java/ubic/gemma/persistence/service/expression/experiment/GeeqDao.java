/*
 * The gemma project
 * 
 * Copyright (c) 2015 University of British Columbia
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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.experiment.GeeqImpl;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.BaseDao;

/**
 * 
 * @author paul
 */
@Repository
public class GeeqDao extends AbstractDao<GeeqImpl> implements BaseDao<GeeqImpl> {

    @Autowired
    public GeeqDao( SessionFactory sessionFactory ) {
        super( GeeqImpl.class, sessionFactory );
    }
}
