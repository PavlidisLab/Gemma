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
import org.springframework.util.Assert;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
    public BioMaterial create( BioMaterial entity ) {
        validate( entity );
        return super.create( entity );
    }

    @Override
    public BioMaterial save( BioMaterial entity ) {
        validate( entity );
        return super.save( entity );
    }

    @Override
    public void update( BioMaterial entity ) {
        validate( entity );
        super.update( entity );
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
                log.warn( "BioMaterial query list threw: " + e.getMessage() );
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

    private void validate( BioMaterial bm ) {
        // EF is lazily-loaded, so we use IDs to avoid initializing it
        Set<Long> seenExperimentalFactorIds = new HashSet<>();
        for ( FactorValue fv : bm.getFactorValues() ) {
            // already assumed since
            Assert.notNull( fv.getExperimentalFactor().getId() );
            if ( !seenExperimentalFactorIds.add( fv.getExperimentalFactor().getId() ) ) {
                String affectedFvs = bm.getFactorValues().stream().
                        filter( fv2 -> fv2.getExperimentalFactor().getId().equals( fv.getExperimentalFactor().getId() ) )
                        .map( FactorValue::toString )
                        .collect( Collectors.joining( "\n\t" ) );
                throw new IllegalArgumentException( String.format( "%s has more than one factor values for %s:\n\t%s",
                        bm,
                        IdentifiableUtils.toString( fv.getExperimentalFactor(), ExperimentalFactor.class ),
                        affectedFvs ) );
            }
        }
    }
}