package ubic.gemma.persistence.service;

import com.google.common.base.Strings;
import gemma.gsec.util.SecurityUtil;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Created by tesarst on 01/06/17.
 * Base DAO providing value object functionality.
 */
public abstract class VoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractDao<O>
        implements BaseVoEnabledDao<O, VO> {

    protected VoEnabledDao( Class<O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    /**
     * Adds all parameters contained in the filters argument to the Query.
     *
     * @param query   the query that needs parameters populated.
     * @param filters filters that provide the parameter values.
     */
    protected static void addRestrictionParameters( Query query, ArrayList<ObjectFilter[]> filters ) {
        if ( !SecurityUtil.isUserAnonymous() && !SecurityUtil.isUserAdmin() ) {
            query.setParameter( "userName", SecurityUtil.getCurrentUsername() );
        }

        if ( filters == null || filters.isEmpty() )
            return;

        for ( ObjectFilter[] filterArray : filters ) {
            if ( filterArray == null || filterArray.length < 1 )
                continue;
            for ( ObjectFilter filter : filterArray ) {
                if ( filter == null )
                    continue;
                if ( Objects.equals( filter.getOperator(), ObjectFilter.in ) ) {
                    query.setParameterList( formParamName( filter ), ( Collection ) filter.getRequiredValue() );
                } else {
                    query.setParameter( formParamName( filter ), filter.getRequiredValue() );
                }
            }
        }
    }

    /**
     * Forms an order by clause for a hibernate query based on given arguments.
     *
     * @param orderByProperty the property the query should be ordered by.
     * @param orderDesc       whether the ordering should be descending or ascending.
     * @return an order by clause. Empty string if the orderByProperty argument is null or empty.
     */
    protected static String formOrderByProperty( String orderByProperty, boolean orderDesc ) {
        if ( Strings.isNullOrEmpty( orderByProperty ) )
            return "";
        return "order by " + orderByProperty + ( orderDesc ? " desc " : " " );
    }

    protected static String formAclSelectClause( String alias, String aoiType ) {
        if ( Strings.isNullOrEmpty( alias ) || Strings.isNullOrEmpty( aoiType ) )
            throw new IllegalArgumentException( "Alias and aoiType can not be empty." );
        return ", AclObjectIdentity as aoi inner join aoi.entries ace inner join aoi.ownerSid sid "
                + "where aoi.identifier = " + alias + ".id and aoi.type = '" + aoiType + "' ";
    }

    /**
     * Creates a CNF restriction clause from the given Filters list.
     *
     * @param filters A list of filtering properties arrays.
     *                Elements in each array will be in a disjunction (OR) with each other.
     *                Arrays will then be in a conjunction (AND) with each other.
     *                I.e. The filter will be in a conjunctive normal form.
     *                <code>[0 OR 1 OR 2] AND [0 OR 1] AND [0 OR 1 OR 3]</code>
     * @return a string containing the clause, without leading "WHERE" keyword.
     */
    protected static String formRestrictionClause( ArrayList<ObjectFilter[]> filters ) {
        String queryString = formAclRestrictionClause();

        if ( filters == null || filters.isEmpty() )
            return queryString;
        StringBuilder conjunction = new StringBuilder();
        for ( ObjectFilter[] filterArray : filters ) {
            if ( filterArray.length == 0 )
                continue;
            StringBuilder disjunction = new StringBuilder();
            boolean first = true;
            for ( ObjectFilter filter : filterArray ) {
                if ( filter == null )
                    continue;
                if ( !first )
                    disjunction.append( "or " );
                if ( filter.getObjectAlias() != null ) {
                    disjunction.append( filter.getObjectAlias() ).append( "." );
                    disjunction.append( filter.getPropertyName() ).append( " " ).append( filter.getOperator() );
                    if ( filter.getOperator().equals( ObjectFilter.in ) ) {
                        disjunction.append( "( :" ).append( formParamName( filter ) ).append( " ) " );
                    } else {
                        disjunction.append( ":" ).append( formParamName( filter ) ).append( " " );
                    }
                }
                first = false;
            }
            String disjunctionString = disjunction.toString();
            if ( !disjunctionString.isEmpty() ) {
                conjunction.append( "and ( " ).append( disjunctionString ).append( " ) " );
            }
        }

        return queryString + conjunction.toString();
    }

    /**
     * Creates a restriction clause to limit the result only to objects the currently logged user can access.
     * Do not forget to populate the :userName parameter for non-admin logged users before using the string
     * to create a Query object.
     *
     * @return a string that can be appended to a query string that was created using {@link this#formAclSelectClause(String, String)}.
     */
    private static String formAclRestrictionClause() {
        String queryString = "";

        // add ACL restrictions
        if ( !SecurityUtil.isUserAnonymous() ) {
            // For administrators no filtering is necessary
            if ( !SecurityUtil.isUserAdmin() ) {
                // For non-admins, pick non-troubled, publicly readable data and data that are readable by them or a group they belong to
                queryString += "and ( (sid.principal = :userName or (ace.sid.id in "
                        // Subselect
                        + "( select sid.id from UserGroup as ug join ug.authorities as ga "
                        + ", AclGrantedAuthoritySid sid where sid.grantedAuthority = CONCAT('GROUP_', ga.authority) "
                        + "and ug.name in ( "
                        // Sub-subselect
                        + "select ug.name from UserGroup ug inner join ug.groupMembers memb where memb.userName = :userName ) "
                        // Sub-subselect end
                        + ") and (ace.mask = 1 or ace.mask = 2) )) "
                        // Subselect end
                        + " or (ace.sid.id = 4 and ace.mask = 1)) ";
            }
        } else {
            // For anonymous users, only pick publicly readable data
            queryString += "and ace.mask = 1 and ace.sid.id = 4 ";
        }
        return queryString;
    }

    /**
     * Forms a parameter name out of the filter object.
     *
     * @param filter the filter to create the parameter name out of.
     * @return a name unique to the provided filter that can be used in a hql query. returned string does not
     * contain the leading ":" character that denotes a parameter keyword in the hql query.
     */
    private static String formParamName( ObjectFilter filter ) {
        return filter.getObjectAlias() + filter.getPropertyName().replace( ".", "" );
    }

    @Override
    public abstract VO loadValueObject( O entity );

    @Override
    public abstract Collection<VO> loadValueObjects( Collection<O> entities );

    /**
     * Should be overridden for any entity that requires special handling of larger amounts of VOs.
     *
     * @return VOs of all instances of the class this DAO manages.
     */
    @Override
    public Collection<VO> loadAllValueObjects() {
        return loadValueObjects( loadAll() );
    }
}
