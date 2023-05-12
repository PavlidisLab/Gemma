package ubic.gemma.persistence.util;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.model.Securable;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.*;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;

/**
 * Utilities for integrating ACLs with Hibernate {@link Criteria} API.
 * @author poirigui
 */
public class AclCriteriaUtils {

    /**
     * Form a restriction clause for ACL.
     *
     * @see AclQueryUtils#formAclRestrictionClause()
     */
    public static Criterion formAclRestrictionClause( String aoiIdColumn, Class<? extends Securable> aoiType ) {
        if ( StringUtils.isBlank( aoiIdColumn ) ) {
            throw new IllegalArgumentException( "Object identity column cannot be empty." );
        }
        DetachedCriteria dc = DetachedCriteria.forClass( AclObjectIdentity.class, "aoi" )
                .createAlias( "aoi.ownerSid", "sid" )
                .setProjection( Projections.property( "aoi.identifier" ) )
                .add( Restrictions.eqProperty( "aoi.identifier", aoiIdColumn ) )
                .add( Restrictions.eq( "aoi.type", aoiType.getCanonicalName() ) );

        // if this is called before a user login (i.e. in AbstractCriteriaFilteringVoEnabledDao), treat a missing
        // authentication as an anonymous login
        boolean hasAuthentication = SecurityContextHolder.getContext().getAuthentication() != null;

        if ( !hasAuthentication || !SecurityUtil.isUserAdmin() ) {
            dc.createAlias( "aoi.entries", "ace" );
        }

        int readMask = BasePermission.READ.getMask();
        int writeMask = BasePermission.WRITE.getMask();
        if ( !hasAuthentication || SecurityUtil.isUserAnonymous() ) {
            // the object is public
            dc.add( Restrictions.conjunction()
                    // FIXME: ace2_ is generated, but sqlRestriction can only replace the root alias
                    .add( new SQLCriterionWithAceSidAliasSubstitution( "{aceSid}.SID_FK in (" + AclQueryUtils.ANONYMOUS_SID_SQL + ")", new Object[0], new Type[0] ) )
                    .add( new BitwiseAnd( "ace.mask", readMask ) ) );
        } else if ( !SecurityUtil.isUserAdmin() ) {
            String userName = SecurityUtil.getCurrentUsername();
            dc.add( Restrictions.disjunction()
                    // user own the object
                    .add( Restrictions.eq( "sid.principal", userName ) )
                    // user has specific rights to the object
                    .add( Restrictions.conjunction()
                            .add( new SQLCriterionWithAceSidAliasSubstitution( "{aceSid}.SID_FK in (" + AclQueryUtils.CURRENT_USER_SIDS_SQL.replace( ":" + AclQueryUtils.USER_NAME_PARAM, "?" ) + ")", new Object[] { userName }, new Type[] { StringType.INSTANCE } ) )
                            .add( new BitwiseAnd( "ace.mask", readMask | writeMask ) ) )
                    // the object is public
                    .add( Restrictions.conjunction()
                            .add( new SQLCriterionWithAceSidAliasSubstitution( "{aceSid}.SID_FK in (" + AclQueryUtils.ANONYMOUS_SID_SQL + ")", new Object[0], new Type[0] ) )
                            .add( new BitwiseAnd( "ace.mask", readMask ) ) ) );
        }

        return Subqueries.propertyIn( aoiIdColumn, dc );
    }

    private static class BitwiseAnd implements Criterion {

        private final String prop;
        private final Object value;

        public BitwiseAnd( String prop, Object value ) {
            this.prop = prop;
            this.value = value;
        }

        @Override
        public String toSqlString( Criteria criteria, CriteriaQuery criteriaQuery ) throws HibernateException {
            SessionFactoryImplementor sessionFactoryImplementor = criteriaQuery.getFactory();
            return criteriaQuery.getFactory().getSqlFunctionRegistry()
                    .findSQLFunction( "bitwise_and" )
                    .render( new IntegerType(),
                            Arrays.asList( criteriaQuery.getColumn( criteria, prop ), criteriaQuery.getTypedValue( criteria, prop, value ) ),
                            sessionFactoryImplementor );
        }

        @Override
        public TypedValue[] getTypedValues( Criteria criteria, CriteriaQuery criteriaQuery ) throws HibernateException {
            return new TypedValue[0];
        }
    }

    /**
     * Extend {@link SQLCriterion} to also substitute selected non-root aliases.
     */
    private static class SQLCriterionWithAceSidAliasSubstitution extends SQLCriterion {

        protected SQLCriterionWithAceSidAliasSubstitution( String sql, Object[] values, Type[] types ) {
            super( sql, values, types );
        }

        @Override
        public String toSqlString( Criteria criteria, CriteriaQuery criteriaQuery ) throws HibernateException {
            return super.toSqlString( criteria, criteriaQuery )
                    .replace( "{aceSid}", criteriaQuery.getSQLAlias( criteria, "ace.sid" ) );
        }
    }
}
