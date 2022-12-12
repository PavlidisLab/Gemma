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

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledDao;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author pavlidis
 */
@Repository
public class BioAssayDaoImpl extends AbstractFilteringVoEnabledDao<BioAssay, BioAssayValueObject> implements BioAssayDao {

    @Autowired
    private ArrayDesignDao arrayDesignDao;

    @Autowired
    public BioAssayDaoImpl( SessionFactory sessionFactory ) {
        super( OBJECT_ALIAS, BioAssay.class, sessionFactory );
    }

    @Override
    public BioAssay find( BioAssay bioAssay ) {
        return ( BioAssay ) BusinessKey
                .createQueryObject( this.getSessionFactory().getCurrentSession(), bioAssay )
                .uniqueResult();
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
                        "select distinct b from BioAssay b inner join b.accession a where a.accession = :accession" )
                .setParameter( "accession", accession ).list();
    }

    @Override
    public void thaw( final BioAssay bioAssay ) {
        try {
            Session session = getSessionFactory().getCurrentSession();
            reattach( bioAssay );
            Hibernate.initialize( bioAssay.getArrayDesignUsed() );
            Hibernate.initialize( bioAssay.getOriginalPlatform() );
            BioMaterial bm = bioAssay.getSampleUsed();
            reattach( bm );
            Hibernate.initialize( bm );
            Hibernate.initialize( bm.getBioAssaysUsedIn() );
            Hibernate.initialize( bm.getFactorValues() );
            session.evict( bm );
            session.evict( bioAssay );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'BioAssayDao.thawRawAndProcessed(BioAssay bioAssay)' --> " + th, th );
        }
    }

    @Override
    public Collection<BioAssay> thaw( Collection<BioAssay> bioAssays ) {
        if ( bioAssays.isEmpty() )
            return bioAssays;
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct b from BioAssay b left join fetch b.arrayDesignUsed"
                        + " left join fetch b.sampleUsed bm"
                        + " left join bm.factorValues left join bm.bioAssaysUsedIn where b.id in (:ids) " )
                .setParameterList( "ids", EntityUtils.getIds( bioAssays ) )
                .list();
    }

    /**
     * Method that allows specification of FactorValueBasicValueObject in the bioMaterialVOs
     *
     * @param entities the bio assays to convert into a VO
     * @param basic true to use FactorValueBasicValueObject, false to use classic FactorValueValueObject
     * @return a collection of bioAssay value objects
     */
    @Override
    //TODO remove when FactorValueValueObject usage is phased out
    public List<BioAssayValueObject> loadValueObjects( Collection<BioAssay> entities, Map<Long, ArrayDesignValueObject> arrayDesignValueObjects, boolean basic ) {
        List<BioAssayValueObject> vos = new LinkedList<>();
        for ( BioAssay e : entities ) {
            vos.add( new BioAssayValueObject( e, arrayDesignValueObjects, basic ) );
        }
        return vos;
    }

    @Override
    protected BioAssayValueObject doLoadValueObject( BioAssay entity ) {
        return new BioAssayValueObject( entity, null, false );
    }

    @Override
    public Slice<BioAssayValueObject> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        throw new NotImplementedException( "Filtering BioAssay VOs is not supported." );
    }

    @Override
    public List<BioAssayValueObject> loadValueObjectsPreFilter( @Nullable Filters filters, @Nullable Sort sort ) {
        throw new NotImplementedException( "Filtering BioAssay VOs is not supported." );
    }
}
