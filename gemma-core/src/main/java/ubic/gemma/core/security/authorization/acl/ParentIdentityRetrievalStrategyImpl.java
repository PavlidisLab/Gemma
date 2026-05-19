package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.domain.AclObjectIdentity;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
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

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;

/**
 * Use domain-specific logic to resolve parent ACL identities.
 *
 * @author poirigui
 */
@Service
@Slf4j
public class ParentIdentityRetrievalStrategyImpl implements ParentIdentityRetrievalStrategy {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /**
     * Renovations Phase 3: gsec HQL deprecation. Direct JdbcTemplate access to the canonical
     * Spring Security ACL tables ({@code acl_class}, {@code acl_object_identity}) replaces the
     * prior HQL query against gsec's {@code AclObjectIdentity} entity mapping. Initialised lazily
     * from the {@link DataSource} so the field can be {@code @Autowired} without forcing the
     * wiring into a constructor.
     */
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource( DataSource dataSource ) {
        this.jdbcTemplate = new JdbcTemplate( dataSource );
    }

    @Override
    @Transactional(readOnly = true)
    public ObjectIdentity getParentIdentity( Object domainObject ) {
        Assert.isInstanceOf( SecuredChild.class, domainObject, "Domain object must be of type SecuredChild." );
        Assert.notNull( ( ( SecuredChild<?> ) domainObject ).getId(), "Domain object must have a non-null identifier." );

        Class<? extends Securable> parentType;
        Long parentIdentifier;
        if ( ( ( SecuredChild<?> ) domainObject ).getSecurityOwner() != null ) {
            SecuredChild<?> sc = ( SecuredChild<?> ) domainObject;
            // this is necessary because we rely on the class name for querying
            //noinspection unchecked
            parentType = Hibernate.getClass( sc.getSecurityOwner() );
            parentIdentifier = sc.getSecurityOwner().getId();
        } else if ( domainObject instanceof ExperimentalDesign ) {
            ExperimentalDesign design = ( ExperimentalDesign ) domainObject;
            parentType = ExpressionExperiment.class;
            parentIdentifier = expressionExperimentService.findIdByDesign( design );
        } else if ( domainObject instanceof ExperimentalFactor ) {
            ExperimentalFactor factor = ( ExperimentalFactor ) domainObject;
            parentType = ExpressionExperiment.class;
            parentIdentifier = expressionExperimentService.findIdByFactor( factor );
        } else if ( domainObject instanceof FactorValue ) {
            FactorValue factor = ( FactorValue ) domainObject;
            parentType = ExpressionExperiment.class;
            parentIdentifier = expressionExperimentService.findIdByFactorValue( factor );
        } else if ( domainObject instanceof BioAssay ) {
            BioAssay ba = ( BioAssay ) domainObject;
            parentType = ExpressionExperiment.class;
            parentIdentifier = expressionExperimentService.findIdByBioAssay( ba, true );
        } else if ( domainObject instanceof BioMaterial ) {
            BioMaterial bm = ( BioMaterial ) domainObject;
            Collection<Long> eeIds = expressionExperimentService.findIdsByBioMaterial( bm, true );
            parentType = ExpressionExperiment.class;
            if ( eeIds.size() == 1 ) {
                parentIdentifier = eeIds.iterator().next();
            } else if ( eeIds.size() > 1 ) {
                log.warn( "More than one ExpressionExperiment refer to " + bm + ", cannot pick its parent ACL identity." );
                parentIdentifier = null;
            } else {
                log.warn( "Could not find an ExpressionExperiment associated to " + bm + "." );
                parentIdentifier = null;
            }
        } else if ( domainObject instanceof MeanVarianceRelation ) {
            MeanVarianceRelation mvr = ( MeanVarianceRelation ) domainObject;
            parentType = ExpressionExperiment.class;
            parentIdentifier = expressionExperimentService.findIdByMeanVarianceRelation( mvr );
        } else {
            throw new UnsupportedOperationException( "Resolving parent identity for " + domainObject + " is not supported." );
        }

        if ( parentIdentifier != null ) {
            // Phase 3 gsec HQL deprecation: replace the HQL select-by-type+identifier against
            // gsec's AclObjectIdentity entity mapping with raw SQL against the canonical Spring
            // Security tables (acl_object_identity JOIN acl_class). We build a fresh
            // AclObjectIdentity (non-managed) carrying id + type + identifier; downstream callers
            // cast to AclObjectIdentity and read those three fields. The gsec entity mapping is
            // {@code mutable="false"}, so even the prior managed instance did not dirty-flush —
            // mutations flow through JdbcMutableAclService / AclDao, not through Hibernate.
            //language=SQL
            List<AclObjectIdentity> rows = jdbcTemplate.query(
                    "select aoi.id, cls.class, aoi.object_id_identity "
                            + "from acl_object_identity aoi "
                            + "join acl_class cls on aoi.object_id_class = cls.id "
                            + "where cls.class = ? and aoi.object_id_identity = ?",
                    ( rs, rowNum ) -> {
                        AclObjectIdentity oid = new AclObjectIdentity( rs.getString( "class" ), rs.getLong( "object_id_identity" ) );
                        oid.setId( rs.getLong( "id" ) );
                        return oid;
                    },
                    parentType.getName(), parentIdentifier );
            if ( rows.isEmpty() ) {
                return null;
            }
            if ( rows.size() > 1 ) {
                // acl_class.class is UNIQUE, (object_id_class, object_id_identity) is unique per
                // class, so this should never fire — but match the prior HQL uniqueResult()
                // contract by surfacing the violation.
                throw new IllegalStateException( "More than one acl_object_identity row for type="
                        + parentType.getName() + ", identifier=" + parentIdentifier );
            }
            return rows.get( 0 );
        } else {
            log.warn( String.format( "Could not locate parent identifier for %s.", domainObject ) );
            return null;
        }
    }
}
