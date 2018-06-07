package ubic.gemma.web.services.rest.util;

import org.hibernate.QueryException;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.args.ArrayEntityArg;
import ubic.gemma.web.services.rest.util.args.FilterArg;
import ubic.gemma.web.services.rest.util.args.IntArg;
import ubic.gemma.web.services.rest.util.args.SortArg;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.ArrayList;

/**
 * Base class for APIs that have an endpoint allowing filtering and sorting of entities.
 *
 * @author tesarst
 */
public abstract class WebServiceWithFiltering<O extends Identifiable, VO extends IdentifiableValueObject<O>, S extends BaseVoEnabledService<O, VO>>
        extends WebService {

    protected S service;

    /**
     * Required by spring
     */
    protected WebServiceWithFiltering() {
    }

    public WebServiceWithFiltering( S service ) {
        this.service = service;
    }

    /**
     * Lists all objects available in gemma.
     *
     * @param filter optional parameter (defaults to empty string) filters the result by given properties.
     *               <p>
     *               Filtering can be done on any* property or nested property that the ExpressionExperiment/ArrayDesign class has (
     *               and is mapped by hibernate ). E.g: 'curationDetails' or 'curationDetails.lastTroubledEvent.date'
     *               </p>
     *               * Any property of a supported type. Currently supported types are:
     *               <ul>
     *               <li>String - property of String type, required value can be any String.</li>
     *               <li>Number - any Number implementation. Required value must be a string parseable to the specific Number type.</li>
     *               <li>Boolean - required value will be parsed to true only if the string matches 'true', ignoring case.</li>
     *               </ul>
     *               Accepted operator keywords are:
     *               <ul>
     *               <li> '=' - equality</li>
     *               <li> '!=' - non-equality</li>
     *               <li> '&lt;' - smaller than</li>
     *               <li> '&gt;' - larger than</li>
     *               <li> '&lt;=' - smaller or equal</li>
     *               <li> '=&gt;' - larger or equal</li>
     *               <li> 'like' - similar string, effectively means 'contains', translates to the sql 'LIKE' operator (given value will be surrounded by % signs)</li>
     *               </ul>
     *               <p>Multiple filters can be chained using 'AND' or 'OR' keywords.</p>
     *               <p>Leave space between the keywords and the previous/next word!</p>
     *               <p>E.g: <code>?filter=property1 &lt; value1 AND property2 like value2</code></p>
     *               <p>
     *               If chained filters are mixed conjunctions and disjunctions, the query must be in conjunctive normal
     *               form (CNF). Parentheses are not necessary - every AND keyword separates blocks of disjunctions.
     *               </p>
     *               Example:
     *               <code>?filter=p1 = v1 OR p1 != v2 AND p2 &lt;=v2 AND p3 &gt; v3 OR p3 &lt; v4</code>
     *               Above query will translate to:
     *               <code>(p1 = v1 OR p1 != v2) AND (p2 &lt;=v2) AND (p3 &gt; v3 OR p3 &lt; v4;)</code>
     *               <p>
     *               Breaking the CNF results in an error.
     *               </p>
     *               <p>
     *               Filter "curationDetails.troubled" will be ignored if user is not an administrator.
     *               </p>
     * @param offset <p>optional parameter (defaults to 0) skips the specified amount of datasets when retrieving them from the database.</p>
     * @param limit  <p>optional parameter (defaults to 20) limits the result to specified amount of datasets. Use 0 for no limit.</p>
     * @param sort   <p>optional parameter (defaults to +id) sets the ordering property and direction.
     *               Format is [+,-][property name]. E.g. "-accession" will translate to descending ordering by the
     *               Accession property.
     *               Note that this does not guarantee the order of the returned entities.
     *               Nested properties are also supported (recursively). E.g. "+curationDetails.lastTroubledEvent.date".</p>
     * @param sr     the http servlet response
     * @return all objects in the database, skipping the first [{@code offset}], and limiting the amount in the result to
     * the value of the {@code limit} parameter.
     */
    protected ResponseDataObject all( FilterArg filter, IntArg offset, IntArg limit, SortArg sort,
            final HttpServletResponse sr ) {
        return this.callService( offset.getValue(), limit.getValue(), sort.getField(), sort.isAsc(),
                filter.getObjectFilters(), sr );
    }

    /**
     * Same as this#all, only also applies an additional filter from the arrayArg, which limits the list to specific set
     * of identifiers.
     *
     * @param arrayArg an array of identifiers to limit the result to.
     * @param filter   see this#all
     * @param offset   see this#all
     * @param limit    see this#all
     * @param sort     see this#all
     * @param sr       see this#all
     * @return see this#all
     */
    protected ResponseDataObject some( ArrayEntityArg<O, VO, S> arrayArg, FilterArg filter, IntArg offset, IntArg limit,
            SortArg sort, final HttpServletResponse sr ) {
        return this.callService( offset.getValue(), limit.getValue(), sort.getField(), sort.isAsc(),
                arrayArg.combineFilters( filter.getObjectFilters(), service ), sr );
    }

    private ResponseDataObject callService( int offset, int limit, String orderBy, boolean asc,
            ArrayList<ObjectFilter[]> filter, final HttpServletResponse sr ) {
        try {
            return Responder.autoCode( service.loadValueObjectsPreFilter( offset, limit, orderBy, asc, filter ), sr );
        } catch ( QueryException e ) {
            //if ( log.isDebugEnabled() ) {
                e.printStackTrace();
            //}
            WellComposedErrorBody error = new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                    FilterArg.ERROR_MSG_MALFORMED_REQUEST );
            return Responder.code( error.getStatus(), error, sr );
        }
    }

}
