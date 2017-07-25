package ubic.gemma.persistence.service;

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

    @Override
    public abstract VO loadValueObject( O entity );

    @Override
    public abstract Collection<VO> loadValueObjects( Collection<O> entities );

    /**
     * Should be overridden for any entity that requires special handling of larger amounts of VOs.
     * @return VOs of all instances of the class this DAO manages.
     */
    @Override
    public Collection<VO> loadAllValueObjects() {
        return loadValueObjects( loadAll() );
    }

    /**
     * Adds all parameters contained in the filters argument to the Query.
     *
     * @param query   the query that needs parameters populated.
     * @param filters filters that provide the parameter values.
     */
    protected static void addRestrictionParameters( Query query, ArrayList<ObjectFilter[]> filters ) {
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
     * Creates a CNF restriction clause from the given Filters list.
     *
     * @param filters A list of filtering properties arrays.<br/>
     *                Elements in each array will be in a disjunction (OR) with each other.<br/>
     *                Arrays will then be in a conjunction (AND) with each other.<br/>
     *                I.e. The filter will be in a conjunctive normal form.<br/>
     *                <code>[0 OR 1 OR 2] AND [0 OR 1] AND [0 OR 1 OR 3]</code><br/><br/>
     * @return a string containing the clause, without leading "WHERE" keyword.
     */
    protected static String formRestrictionClause( ArrayList<ObjectFilter[]> filters ) {

        if ( filters == null || filters.isEmpty() )
            return "";
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
                        disjunction.append( ":" ).append( formParamName( filter ) );
                    }
                }
                first = false;
            }
            String disjunctionString = disjunction.toString();
            if ( !disjunctionString.isEmpty() ) {
                conjunction.append( "and ( " ).append( disjunctionString ).append( " ) " );
            }
        }

        return conjunction.toString();
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
}
