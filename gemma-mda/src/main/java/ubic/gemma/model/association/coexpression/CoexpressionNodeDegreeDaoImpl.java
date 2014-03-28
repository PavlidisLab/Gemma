/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.model.association.coexpression;

import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.AbstractDao;

/**
 * @author paul
 * @version $Id$
 */
@Component
public class CoexpressionNodeDegreeDaoImpl extends AbstractDao<GeneCoexpressionNodeDegree> implements
        CoexpressionNodeDegreeDao {

    @Autowired
    public CoexpressionNodeDegreeDaoImpl( SessionFactory sessionFactory ) {
        super( GeneCoexpressionNodeDegreeImpl.class );
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public void deleteFor( Gene gene ) {
        List<GeneCoexpressionNodeDegree> existing = this.getHibernateTemplate().findByNamedParam(
                "from GeneCoexpressionNodeDegreeImpl n where n.gene = :g", "g", gene );
        if ( existing.isEmpty() ) return;
        this.remove( existing );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeDao#findOrCreate(ubic.gemma.model.genome.
     * Gene)
     */
    @Override
    @Transactional
    public GeneCoexpressionNodeDegree findOrCreate( Gene gene ) {
        List<GeneCoexpressionNodeDegree> existing = this.getHibernateTemplate().findByNamedParam(
                " from GeneCoexpressionNodeDegreeImpl n where n.geneId = :g", "g", gene.getId() );
        if ( existing.isEmpty() ) {
            GeneCoexpressionNodeDegree nd = GeneCoexpressionNodeDegree.Factory.newInstance( gene );
            return this.create( nd );
        }
        return existing.get( 0 );

    }

}
