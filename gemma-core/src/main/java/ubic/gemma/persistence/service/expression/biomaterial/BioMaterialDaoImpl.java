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
package ubic.gemma.persistence.service.expression.biomaterial;

import org.hibernate.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author pavlidis
 * @see BioMaterial
 */
@Repository
public class BioMaterialDaoImpl extends AbstractVoEnabledDao<BioMaterial, BioMaterialValueObject>
        implements BioMaterialDao {

    private static final int MAX_REPS = 5;

    @Autowired
    public BioMaterialDaoImpl( SessionFactory sessionFactory ) {
        super( BioMaterial.class, sessionFactory );
    }

    @Override
    public BioMaterial find( BioMaterial bioMaterial ) {
        AbstractDao.log.debug( "Start find" );
        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( BioMaterial.class );

        BusinessKey.addRestrictions( queryObject, bioMaterial );

        // This part is involved in a weird race condition that I could not get to a bottom of, so this is a hack-fix for now - tesarst, 2018-May-2
        List results = null;
        int rep = 0;
        while ( results == null && rep < BioMaterialDaoImpl.MAX_REPS ) {
            try {
                results = queryObject.list();
                rep++;
            } catch ( ObjectNotFoundException e ) {
                AbstractDao.log.warn( "BioMaterial query list threw: " + e.getMessage() );
            }
        }

        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + BioMaterial.class.getName()
                                + "' was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        AbstractDao.log.debug( "Done with find" );
        return ( BioMaterial ) result;
    }

    @Override
    public BioMaterial copy( final BioMaterial bioMaterial ) {

        BioMaterial newMaterial = BioMaterial.Factory.newInstance();
        newMaterial.setDescription( bioMaterial.getDescription() + " [Created by Gemma]" );
        newMaterial.setCharacteristics( bioMaterial.getCharacteristics() );
        newMaterial.setSourceTaxon( bioMaterial.getSourceTaxon() );

        newMaterial.setTreatments( bioMaterial.getTreatments() );
        newMaterial.setFactorValues( bioMaterial.getFactorValues() );

        newMaterial.setName( "Modeled after " + bioMaterial.getName() );
        newMaterial = this.findOrCreate( newMaterial );
        return newMaterial;

    }

    @Override
    public Collection<BioMaterial> findByExperiment( ExpressionExperiment experiment ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct bm from ExpressionExperiment e join e.bioAssays b join b.sampleUsed bm where e = :ee" )
                .setParameter( "ee", experiment ).list();
    }

    @Override
    public Collection<BioMaterial> findByFactorValue( FactorValue fv ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct b from BioMaterial b join b.factorValues fv where fv = :f" )
                .setParameter( "f", fv ).list();
    }

    @Override
    public ExpressionExperiment getExpressionExperiment( Long bioMaterialId ) {
        return ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct e from ExpressionExperiment e inner join e.bioAssays ba inner join ba.sampleUsed bm where bm.id =:bmid " )
                .setParameter( "bmid", bioMaterialId ).uniqueResult();
    }

    @Override
    public void thaw( final BioMaterial bioMaterial ) {
        Session session = this.getSessionFactory().getCurrentSession();
        session.buildLockRequest( LockOptions.NONE ).lock( bioMaterial );
        Hibernate.initialize( bioMaterial );
        Hibernate.initialize( bioMaterial.getSourceTaxon() );
        Hibernate.initialize( bioMaterial.getBioAssaysUsedIn() );
        Hibernate.initialize( bioMaterial.getTreatments() );
        Hibernate.initialize( bioMaterial.getFactorValues() );
    }

    @Override
    public Collection<BioMaterial> thaw( Collection<BioMaterial> bioMaterials ) {
        if ( bioMaterials.isEmpty() )
            return bioMaterials;
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct b from BioMaterial b left join fetch b.sourceTaxon left join fetch b.bioAssaysUsedIn"
                        + " left join fetch b.treatments left join fetch b.factorValues where b.id in (:ids)" )
                .setParameterList( "ids", EntityUtils.getIds( bioMaterials ) ).list();
    }

    @Override
    public BioMaterialValueObject loadValueObject( BioMaterial entity ) {
        return new BioMaterialValueObject( entity );
    }

}