package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.domain.AclObjectIdentity;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;

/**
 * Use domain-specific logic to resolve parent ACL identities.
 *
 * @author poirigui
 */
@Service
@CommonsLog
public class ParentIdentityRetrievalStrategyImpl implements ParentIdentityRetrievalStrategy {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Override
    @Transactional(readOnly = true)
    public ObjectIdentity getParentIdentity( ObjectIdentity aoi ) {
        //noinspection unchecked
        Class<? extends SecuredChild<?>> clazz = sessionFactory.getClassMetadata( aoi.getType() ).getMappedClass();
        Class<? extends Securable> parentType;
        Long parentIdentifier;
        if ( ExperimentalDesign.class.isAssignableFrom( clazz ) ) {
            ExperimentalDesign design = ( ExperimentalDesign ) sessionFactory.getCurrentSession()
                    .get( ExperimentalDesign.class, aoi.getIdentifier() );
            if ( design == null ) {
                log.warn( "Could not find " + clazz.getSimpleName() + " with ID " + aoi.getIdentifier() + "." );
                return null;
            }
            ExpressionExperiment ee = expressionExperimentService.findByDesign( design );
            parentType = ExpressionExperiment.class;
            parentIdentifier = ee != null ? ee.getId() : null;
        } else if ( ExperimentalFactor.class.isAssignableFrom( clazz ) ) {
            ExperimentalFactor factor = ( ExperimentalFactor ) sessionFactory.getCurrentSession()
                    .get( ExperimentalFactor.class, aoi.getIdentifier() );
            if ( factor == null ) {
                log.warn( "Could not find " + clazz.getSimpleName() + " with ID " + aoi.getIdentifier() + "." );
                return null;
            }
            ExpressionExperiment ee = expressionExperimentService.findByFactor( factor );
            parentType = ExpressionExperiment.class;
            parentIdentifier = ee != null ? ee.getId() : null;
        } else if ( FactorValue.class.isAssignableFrom( clazz ) ) {
            FactorValue factor = ( FactorValue ) sessionFactory.getCurrentSession()
                    .get( FactorValue.class, aoi.getIdentifier() );
            if ( factor == null ) {
                log.warn( "Could not find " + clazz.getSimpleName() + " with ID " + aoi.getIdentifier() + "." );
                return null;
            }
            ExpressionExperiment ee = expressionExperimentService.findByFactorValue( factor );
            parentType = ExpressionExperiment.class;
            parentIdentifier = ee != null ? ee.getId() : null;
        } else if ( BioAssay.class.isAssignableFrom( clazz ) ) {
            BioAssay ba = ( BioAssay ) sessionFactory.getCurrentSession()
                    .get( BioAssay.class, aoi.getIdentifier() );
            if ( ba == null ) {
                log.warn( "Could not find " + clazz.getSimpleName() + " with ID " + aoi.getIdentifier() + "." );
                return null;
            }
            ExpressionExperiment ee = expressionExperimentService.findByBioAssay( ba, true );
            parentType = ExpressionExperiment.class;
            parentIdentifier = ee != null ? ee.getId() : null;
        } else if ( BioMaterial.class.isAssignableFrom( clazz ) ) {
            BioMaterial bm = ( BioMaterial ) sessionFactory.getCurrentSession()
                    .get( BioMaterial.class, aoi.getIdentifier() );
            if ( bm == null ) {
                log.warn( "Could not find " + clazz.getSimpleName() + " with ID " + aoi.getIdentifier() + "." );
                return null;
            }
            Collection<ExpressionExperiment> ees = expressionExperimentService.findByBioMaterial( bm, true );
            parentType = ExpressionExperiment.class;
            if ( ees.size() == 1 ) {
                parentIdentifier = ees.iterator().next().getId();
            } else if ( ees.size() > 1 ) {
                log.warn( "More than one ExpressionExperiment refer to " + bm + ", cannot pick its parent ACL identity." );
                parentIdentifier = null;
            } else {
                log.warn( "Could not find " + clazz.getSimpleName() + " with ID " + aoi.getIdentifier() + "." );
                parentIdentifier = null;
            }
        } else if ( MeanVarianceRelation.class.isAssignableFrom( clazz ) ) {
            MeanVarianceRelation mvr = ( MeanVarianceRelation ) sessionFactory.getCurrentSession()
                    .get( MeanVarianceRelation.class, aoi.getIdentifier() );
            if ( mvr == null ) {
                log.warn( "Could not find " + clazz.getSimpleName() + " with ID " + aoi.getIdentifier() + "." );
                return null;
            }
            ExpressionExperiment ee = expressionExperimentService.findByMeanVarianceRelation( mvr );
            parentType = ExpressionExperiment.class;
            parentIdentifier = ee != null ? ee.getId() : null;
        } else {
            // try automated!
            SecuredChild<?> sc = ( SecuredChild<?> ) sessionFactory.getCurrentSession()
                    .get( clazz, aoi.getIdentifier() );
            if ( sc == null ) {
                log.warn( "Could not find " + clazz.getSimpleName() + " with ID " + aoi.getIdentifier() + "." );
                return null;
            }
            if ( sc.getSecurityOwner() != null ) {
                // this is necessary because we rely on the class name for querying
                //noinspection unchecked
                parentType = Hibernate.getClass( sc.getSecurityOwner() );
                parentIdentifier = sc.getSecurityOwner().getId();
            } else {
                log.warn( String.format( "Cannot resolve parent ACL identity for %s Id=%s: its security owner is not populated.",
                        clazz.getSimpleName(), aoi.getIdentifier() ) );
                parentType = null;
                parentIdentifier = null;
            }
        }

        if ( parentIdentifier != null ) {
            return ( AclObjectIdentity ) sessionFactory.getCurrentSession()
                    .createQuery( "select aoi from AclObjectIdentity aoi where aoi.type = :type and aoi.identifier = :identifier" )
                    .setParameter( "type", parentType.getName() )
                    .setParameter( "identifier", parentIdentifier )
                    .uniqueResult();
        } else {
            log.warn( String.format( "Could not locate parent identifier for %s Id=%s.", clazz.getSimpleName(), aoi.getIdentifier() ) );
            return null;
        }
    }
}
