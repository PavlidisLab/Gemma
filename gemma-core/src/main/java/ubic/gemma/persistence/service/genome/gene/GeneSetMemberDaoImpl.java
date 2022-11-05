/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.persistence.service.genome.gene;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;

/**
 * @author kelsey
 */
@Repository
public class GeneSetMemberDaoImpl extends AbstractDao<GeneSetMember> implements BaseDao<GeneSetMember> {

    @Autowired
    public GeneSetMemberDaoImpl( SessionFactory sessionFactory ) {
        super( GeneSetMember.class, sessionFactory );
    }

    @Override
    @Transactional
    public void create( final Collection<GeneSetMember> entities ) {
        super.create( entities );
    }

    @Override
    @Transactional
    public void update( final Collection<GeneSetMember> entities ) {
        super.update( entities );
    }

}
