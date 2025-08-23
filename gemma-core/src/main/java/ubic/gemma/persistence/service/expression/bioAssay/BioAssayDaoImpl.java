/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.persistence.service.expression.bioAssay;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.AbstractNoopFilteringVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author pavlidis
 */
@Repository
public class BioAssayDaoImpl extends AbstractNoopFilteringVoEnabledDao<BioAssay, BioAssayValueObject> implements BioAssayDao {

    @Autowired
    public BioAssayDaoImpl( SessionFactory sessionFactory ) {
        super( BioAssay.class, sessionFactory );
    }

    @Override
    public BioAssay find( BioAssay bioAssay ) {
        return ( BioAssay ) BusinessKey
                .createQueryObject( this.getSessionFactory().getCurrentSession(), bioAssay )
                .uniqueResult();
    }

    @Nullable
    @Override
    public BioAssay findByShortName( String shortName ) {
        return findOneByProperty( "shortName", shortName );
    }

    @Override
    public Collection<BioAssayDimension> findBioAssayDimensions( BioAssay bioAssay ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select bad from BioAssayDimension bad inner join bad.bioAssays as ba where :bioAssay in ba " )
                .setParameter( "bioAssay", bioAssay ).list();
    }

    @Override
    public Collection<BioAssay> findByAccession( String accession ) {
        if ( StringUtils.isBlank( accession ) )
            return new HashSet<>();

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select b from BioAssay b join b.accession a where a.accession = :accession group by b" )
                .setParameter( "accession", accession ).list();
    }

    @Override
    public Collection<BioAssaySet> getBioAssaySets( BioAssay bioAssay ) {
        Collection<BioAssaySet> results = new HashSet<>();
        //noinspection unchecked
        results.addAll( getSessionFactory().getCurrentSession()
                .createQuery( "select bas from ExpressionExperiment bas join bas.bioAssays ba where ba = :ba group by bas" )
                .setParameter( "ba", bioAssay )
                .list() );
        //noinspection unchecked
        results.addAll( getSessionFactory().getCurrentSession()
                .createQuery( "select bas from ExpressionExperimentSubSet bas join bas.bioAssays ba where ba = :ba group by bas" )
                .setParameter( "ba", bioAssay )
                .list() );
        return results;
    }

    @Override
    public List<BioAssayValueObject> loadValueObjects( Collection<BioAssay> entities, @Nullable Map<ArrayDesign, ArrayDesignValueObject> ad2vo, @Nullable Map<BioAssay, BioAssay> assay2sourceAssayMap, boolean basic, boolean allFactorValues ) {
        List<BioAssayValueObject> vos = new LinkedList<>();
        for ( BioAssay e : entities ) {
            vos.add( new BioAssayValueObject( e, ad2vo, assay2sourceAssayMap != null ? assay2sourceAssayMap.get( e ) : null, basic, allFactorValues ) );
        }
        return vos;
    }

    @Override
    protected BioAssayValueObject doLoadValueObject( BioAssay entity ) {
        return new BioAssayValueObject( entity, null, null, false, false );
    }
}
