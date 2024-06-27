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

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;

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
        Criteria queryObject = this.getSessionFactory().getCurrentSession().createCriteria( BioMaterial.class );

        BusinessKey.addRestrictions( queryObject, bioMaterial );

        // This part is involved in a weird race condition that I could not get to a bottom of, so this is a hack-fix for now - tesarst, 2018-May-2
        BioMaterial result = null;
        int rep = 0;
        while ( result == null && rep < BioMaterialDaoImpl.MAX_REPS ) {
            try {
                result = ( BioMaterial ) queryObject.uniqueResult();
                rep++;
            } catch ( ObjectNotFoundException e ) {
                AbstractDao.log.warn( "BioMaterial query list threw: " + e.getMessage() );
            }
        }

        return result;
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
    public Collection<BioMaterial> findByFactor( ExperimentalFactor experimentalFactor ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct bm from BioMaterial bm join bm.factorValues fv where fv.experimentalFactor = :ef" )
                .setParameter( "ef", experimentalFactor )
                .list();
    }

    @Override
    public ExpressionExperiment getExpressionExperiment( Long bioMaterialId ) {
        return ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct e from ExpressionExperiment e inner join e.bioAssays ba inner join ba.sampleUsed bm where bm.id =:bmid " )
                .setParameter( "bmid", bioMaterialId ).uniqueResult();
    }

    @Override
    public void thaw( final BioMaterial bioMaterial ) {
        Hibernate.initialize( bioMaterial.getSourceTaxon() );
        Hibernate.initialize( bioMaterial.getTreatments() );
        for ( FactorValue fv : bioMaterial.getFactorValues() ) {
            Hibernate.initialize( fv.getExperimentalFactor() );
        }
    }

    @Override
    protected BioMaterialValueObject doLoadValueObject( BioMaterial entity ) {
        return new BioMaterialValueObject( entity );
    }

}