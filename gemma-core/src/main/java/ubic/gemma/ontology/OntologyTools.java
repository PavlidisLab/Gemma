/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.ontology;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import net.sf.ehcache.CacheException;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.ontology.cache.OntologyCache;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Paul
 * @version $Id$
 * @deprecated
 */
@Deprecated
public class OntologyTools {

    private static OntologyCache cache;

    static {
        cache = new OntologyCache();
    }

    /**
     * @param characteristic
     * @return A text version of the term underlying the characteristic.
     */
    public static String getLabel( VocabCharacteristic characteristic ) {
        return getOntologyTerm( characteristic ).getTerm();
    }

    public static OntologyIndividual getOntologyIndividual( String uri ) {
        try {
            if ( cache.get( uri ) != null ) {
                return ( OntologyIndividual ) cache.get( uri );
            }

            return ( OntologyIndividual ) cache.get( uri );
        } catch ( IllegalStateException e ) {
            throw new RuntimeException( e );
        } catch ( CacheException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param uri
     * @return
     */
    public static OntologyProperty getOntologyProperty( String uri ) {
        try {
            if ( cache.get( uri ) != null ) {
                return ( OntologyProperty ) cache.get( uri );
            }

            return ( OntologyProperty ) cache.get( uri );
        } catch ( IllegalStateException e ) {
            throw new RuntimeException( e );
        } catch ( CacheException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param uri
     * @return
     */
    public static OntologyTerm getOntologyTerm( String uri ) {
        try {
            if ( cache.get( uri ) != null ) {
                return ( OntologyTerm ) cache.get( uri );
            }

            return ( OntologyTerm ) cache.get( uri );
        } catch ( IllegalStateException e ) {
            throw new RuntimeException( e );
        } catch ( CacheException e ) {
            throw new RuntimeException( e );
        }
    }

    public static OntologyTerm getOntologyTerm( String uri, OntModel model ) {
        OntClass ontClass = model.getOntClass( uri );
        return new OntologyTermImpl( ontClass, null );
    }

    /**
     * @param characteristic
     * @return
     */
    public static OntologyTerm getOntologyTerm( VocabCharacteristic characteristic ) {
        String uri = characteristic.getValueUri();
        return getOntologyTerm( uri );
    }

    /**
     * @param characteristics
     * @return
     */
    public static Collection<OntologyTerm> getOntologyTerms( Collection<VocabCharacteristic> characteristics ) {
        Collection<OntologyTerm> result = new HashSet<OntologyTerm>();
        for ( VocabCharacteristic vc : characteristics ) {
            result.add( getOntologyTerm( vc ) );
        }
        return result;
    }

    /**
     * Given a variable for a sparql query and a solution/result set it will give you a string to represent it.
     * 
     * @param var
     * @param soln
     * @return String to represent the var
     */

    public static String varToString( String var, QuerySolution soln ) {
        try {
            Resource r = soln.getResource( var );
            if ( r == null ) return null;
            return r.toString();
        } catch ( ClassCastException c ) {
            Literal l = soln.getLiteral( var );
            return l.getString();
        }
    }

    protected static void initOntology( InputStream is, String name, OntModelSpec spec ) {
        Collection<OntologyResource> terms;
        terms = OntologyLoader.initialize( name, OntologyLoader.loadMemoryModel( is, name, spec ) );

        for ( OntologyResource term : terms ) {
            if ( term == null ) continue; // why does this happen?
            cache.put( term );
        }
    }

    protected static void initOntology( String url ) {
        Collection<OntologyResource> terms;
        terms = OntologyLoader.initialize( url, OntologyLoader.loadMemoryModel( url ) );
        for ( OntologyResource term : terms ) {
            cache.put( term );
        }
    }

}
