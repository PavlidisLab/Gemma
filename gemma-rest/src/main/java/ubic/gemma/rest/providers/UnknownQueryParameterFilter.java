/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
 */
package ubic.gemma.rest.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.model.Parameter.Source;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.rest.annotations.AllowsUnknownQueryParameters;
import ubic.gemma.rest.util.UnknownQueryParameterException;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rejects a request that carries a query parameter the matched resource method cannot bind, rather than ignoring it.
 * <p>
 * JAX-RS drops an undeclared query parameter silently, which turns a typo or a wrong parameter name into a
 * confidently-wrong answer instead of an error: {@code GET /datasets?ids=1,2,3&limit=200} dropped {@code ids} and
 * answered with the whole corpus, reported under a {@code totalElements} covering every dataset. There is no JAX-RS
 * or Jersey setting for this (Jersey 3.1 exposes none), so it is enforced here.
 *
 * <h2>Why the accepted set cannot go stale</h2>
 * The accepted set is not written down anywhere. It is read, per route, from
 * {@link Invocable#getParameters()} — the very {@link Parameter} objects Jersey uses to bind values into the
 * method's arguments. Adding a {@code @QueryParam} to a resource method therefore widens what this filter accepts in
 * the same edit, and there is no second list to update. Consequences that follow from reading Jersey's own model
 * rather than re-deriving one by reflection:
 * <ul>
 *     <li>{@code @BeanParam} is expanded, because Jersey models it as a {@link Parameter.BeanParameter} holding the
 *         nested parameters (none are used in this module today; the recursion is what keeps that true if one is
 *         added).</li>
 *     <li>Parameters inherited from a superclass or interface are already resolved into the invocable, so a route
 *         that declares its parameters on a base type is not wrongly narrowed.</li>
 *     <li>Anything Jersey binds from somewhere other than the query string — path, header, cookie, matrix, entity —
 *         is simply not in the accepted set and is not looked for in the query string either.</li>
 * </ul>
 *
 * <h2>What is deliberately not checked</h2>
 * <ul>
 *     <li>A method annotated {@link AllowsUnknownQueryParameters}, or declared in a class so annotated: it reads the
 *         query string itself, so no declared set describes what it accepts.</li>
 *     <li>A method handed the raw {@link UriInfo}, for the same reason — it can read any parameter, so no parameter
 *         can be shown to be ignored. Derived from the signature rather than from a list, so a new pass-through
 *         route is covered the moment it is written.</li>
 *     <li>{@code OPTIONS}, so a CORS preflight — which the browser sends to the same URL, query string included —
 *         can never be answered with a 400. {@code CorsFilter} short-circuits preflights ahead of Jersey today; this
 *         does not depend on that staying true.</li>
 *     <li>Every request, when {@code gemma.rest.rejectUnknownQueryParameters} is {@code false}. The setting is the
 *         rollback: it takes a restart, not a redeploy.</li>
 * </ul>
 *
 * @author gemma
 */
@Provider
@Component
public class UnknownQueryParameterFilter implements ContainerRequestFilter {

    private static final Log log = LogFactory.getLog( UnknownQueryParameterFilter.class );

    /**
     * Accepted query-parameter names per resource method. Keyed on the definition {@link Method}, which is stable
     * for the life of the resource model, so the per-request cost is a lookup and a set difference.
     */
    private final Map<Method, Set<String>> acceptedByMethod = new ConcurrentHashMap<>();

    private final boolean enabled;

    /**
     * The inline default matters: the mocked REST test contexts resolve placeholders from a fixed list rather than
     * from {@code default.properties}, so a bare {@code ${...}} here fails container startup for every JerseyTest
     * in the module, not just a test of this filter.
     */
    @Autowired
    public UnknownQueryParameterFilter( @Value("${gemma.rest.rejectUnknownQueryParameters:true}") boolean enabled ) {
        this.enabled = enabled;
        if ( !enabled ) {
            log.warn( "Unknown query parameters will be ignored rather than rejected; "
                    + "set gemma.rest.rejectUnknownQueryParameters=true to reject them." );
        }
    }

    @Override
    public void filter( ContainerRequestContext requestContext ) {
        if ( !enabled ) {
            return;
        }
        // a CORS preflight carries the query string of the request it is asking about
        if ( HttpMethod.OPTIONS.equals( requestContext.getMethod() ) ) {
            return;
        }
        UriInfo uriInfo = requestContext.getUriInfo();
        if ( !( uriInfo instanceof ExtendedUriInfo ) ) {
            // no matched-method information to check against; never reject on a guess
            return;
        }
        Set<String> submitted = uriInfo.getQueryParameters().keySet();
        if ( submitted.isEmpty() ) {
            return;
        }
        ResourceMethod matched = ( ( ExtendedUriInfo ) uriInfo ).getMatchedResourceMethod();
        if ( matched == null ) {
            // sub-resource locator still being resolved, or no method matched
            return;
        }
        Invocable invocable = matched.getInvocable();
        Method definitionMethod = invocable.getDefinitionMethod();
        if ( isExempt( invocable ) ) {
            return;
        }
        Set<String> accepted = acceptedByMethod.computeIfAbsent( definitionMethod, m -> collectQueryParameterNames( invocable ) );
        List<String> unknown = new ArrayList<>();
        for ( String name : submitted ) {
            if ( !accepted.contains( name ) ) {
                unknown.add( name );
            }
        }
        if ( unknown.isEmpty() ) {
            return;
        }
        // WARN rather than DEBUG: while this is newly enforced, the log is the record of which client sent what.
        log.warn( String.format( "Rejecting %s %s: unknown query parameter(s) %s (accepted: %s); user-agent: %s",
                requestContext.getMethod(), uriInfo.getPath(), unknown,
                accepted.isEmpty() ? "none" : accepted,
                requestContext.getHeaderString( "User-Agent" ) ) );
        throw new UnknownQueryParameterException( unknown, accepted );
    }

    private boolean isExempt( Invocable invocable ) {
        Method definitionMethod = invocable.getDefinitionMethod();
        if ( definitionMethod.isAnnotationPresent( AllowsUnknownQueryParameters.class )
                || definitionMethod.getDeclaringClass().isAnnotationPresent( AllowsUnknownQueryParameters.class ) ) {
            return true;
        }
        Method handlingMethod = invocable.getHandlingMethod();
        if ( handlingMethod != null && ( handlingMethod.isAnnotationPresent( AllowsUnknownQueryParameters.class )
                || handlingMethod.getDeclaringClass().isAnnotationPresent( AllowsUnknownQueryParameters.class ) ) ) {
            return true;
        }
        // a method holding the raw UriInfo can read any query parameter, so none of them is provably ignored
        for ( Parameter parameter : invocable.getParameters() ) {
            if ( parameter.getSource() == Source.CONTEXT && UriInfo.class.isAssignableFrom( parameter.getRawType() ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Every name the method can bind from the query string, taken from Jersey's own parameter model.
     */
    private static Set<String> collectQueryParameterNames( Invocable invocable ) {
        Set<String> names = new LinkedHashSet<>();
        collectInto( invocable.getParameters(), names );
        return Collections.unmodifiableSet( names );
    }

    private static void collectInto( Collection<Parameter> parameters, Set<String> names ) {
        for ( Parameter parameter : parameters ) {
            if ( parameter.getSource() == Source.QUERY ) {
                names.add( parameter.getSourceName() );
            } else if ( parameter instanceof Parameter.BeanParameter ) {
                collectInto( ( ( Parameter.BeanParameter ) parameter ).getParameters(), names );
            }
        }
    }
}
