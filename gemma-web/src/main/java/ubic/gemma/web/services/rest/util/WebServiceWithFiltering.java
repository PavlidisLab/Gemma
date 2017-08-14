package ubic.gemma.web.services.rest.util;

import org.hibernate.QueryException;
import ubic.gemma.web.services.rest.util.args.FilterArg;
import ubic.gemma.web.services.rest.util.args.IntArg;
import ubic.gemma.web.services.rest.util.args.SortArg;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.text.ParseException;

public abstract class WebServiceWithFiltering extends WebService {

    /**
     * Lists all objects available in gemma.
     *
     * @param filter optional parameter (defaults to empty string) filters the result by given properties.
     *               <br/>
     *               <p>
     *               Filtering can be done on any* property or nested property that the ExpressionExperiment class has (
     *               and is mapped by hibernate ). E.g: 'curationDetails' or 'curationDetails.lastTroubledEvent.date'
     *               </p><p>
     *               * Any property of a supported type. Currently supported types are:
     *               <ul>
     *               <li>String - property of String type, required value can be any String.</li>
     *               <li>Number - any Number implementation. Required value must be a string parseable to the specific Number type.</li>
     *               <li>Boolean - required value will be parsed to true only if the string matches 'true', ignoring case.</li>
     *               </ul>
     *               </p><p>
     *               Accepted operator keywords are:
     *               <ul>
     *               <li> '=' - equality</li>
     *               <li> '!=' - non-equality</li>
     *               <li> '<' - smaller than</li>
     *               <li> '>' - larger than</li>
     *               <li> '<=' - smaller or equal</li>
     *               <li> '=>' - larger or equal</li>
     *               <li> 'like' - similar string, effectively means 'contains', translates to the sql 'LIKE' operator (given value will be surrounded by % signs)</li>
     *               </ul>
     *               Multiple filters can be chained using 'AND' or 'OR' keywords.<br/>
     *               Leave space between the keywords and the previous/next word! <br/>
     *               E.g: <code>?filter=property1 < value1 AND property2 like value2</code>
     *               </p><p>
     *               If chained filters are mixed conjunctions and disjunctions, the query must be in conjunctive normal
     *               form (CNF). Parentheses are not necessary - every AND keyword separates blocks of disjunctions.
     *               </p><p>
     *               Example:<br/>
     *               <code>?filter=p1 = v1 OR p1 != v2 AND p2 <= v2 AND p3 > v3 OR p3 < v4</code><br/>
     *               Above query will translate to: <br/>
     *               <code>(p1 = v1 OR p1 != v2) AND (p2 <= v2) AND (p3 > v3 OR p3 < v4;)</code>
     *               </p><p>
     *               Breaking the CNF results in an error.
     *               </p>
     *               <p>
     *               Filter "curationDetails.troubled" will be ignored if user is not an administrator.
     *               </p>
     *               <br/>
     * @param offset <p>optional parameter (defaults to 0) skips the specified amount of datasets when retrieving them from the database.</p>
     * @param limit  <p>optional parameter (defaults to 20) limits the result to specified amount of datasets. Use 0 for no limit.</p>
     * @param sort   <p>optional parameter (defaults to +id) sets the ordering property and direction.<br/>
     *               Format is [+,-][property name]. E.g. "-accession" will translate to descending ordering by the
     *               Accession property.<br/>
     *               Note that this does not guarantee the order of the returned entities.<br/>
     *               Nested properties are also supported (recursively). E.g. "+curationDetails.lastTroubledEvent.date".<br/></p>
     * @return all objects in the database, skipping the first [{@code offset}], and limiting the amount in the result to
     * the value of the {@code limit} parameter.
     */
    protected ResponseDataObject all( FilterArg filter, IntArg offset, IntArg limit, SortArg sort,
            final HttpServletResponse sr ) {
        try {
            return loadVOsPreFilter( filter, offset, limit, sort, sr );
        } catch ( QueryException | ParseException e ) {
            if ( log.isDebugEnabled() ) {
                e.printStackTrace();
            }
            WellComposedErrorBody error = new WellComposedErrorBody( Response.Status.BAD_REQUEST,
                    FilterArg.ERROR_MSG_MALFORMED_REQUEST );
            WellComposedErrorBody.addExceptionFields( error, e );
            return Responder.code( error.getStatus(), error, sr );
        }
    }

    /**
     * Calls the appropriate service to load a filtered collection of objects.
     *
     * @throws ParseException in case the filter argument could not be converted into object filter.
     */
    protected abstract ResponseDataObject loadVOsPreFilter( FilterArg filter, IntArg offset, IntArg limit, SortArg sort,
            final HttpServletResponse sr ) throws ParseException;

}
