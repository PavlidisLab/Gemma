/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.ontology.providers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.larq.IndexLARQ;

import ubic.basecode.ontology.OntologyLoader;
import ubic.basecode.ontology.model.ObjectPropertyImpl;
import ubic.basecode.ontology.model.OntologyClassRestriction;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologyIndexer;
import ubic.basecode.ontology.search.OntologySearch;
import ubic.gemma.model.common.description.ExternalDatabase;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * Methods to help deal with MESH (Medical Subject Heading) terms. These are provided in PubMed entries as text, but
 * represented in our system as a formal Ontology.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class MeshService {

    private static Log log = LogFactory.getLog( MeshService.class.getName() );

    // This one is gone.
    // private final static String MESH_ONT_URL = "http://www.berkeleybop.org/ontologies/obo-all/mesh/mesh.owl";

    // private final static String MESH_ONT_URL = "http://bike.snu.ac.kr/sites/default/files/meshonto.owl";
    private final static String MESH_ONT_URL = "http://onto.eva.mpg.de/obo/mesh.owl";
    private static final String MESH_INDEX_NAME = "mesh";
    private static OntModel model;
    private static IndexLARQ index;
    private static ExternalDatabase meshdb;

    static {
        model = OntologyLoader.loadPersistentModel( MESH_ONT_URL, false ); // no force.
        index = OntologyIndexer.indexOntology( MESH_INDEX_NAME, model );
        meshdb = ExternalDatabase.Factory.newInstance();
        meshdb.setName( "mesh" );
        meshdb.setWebUri( MESH_ONT_URL );
    }

    /**
     * Cache of mesh -> parent terms
     */
    private static Map<String, Collection<OntologyTerm>> parentsCache = Collections
            .synchronizedMap( new HashMap<String, Collection<OntologyTerm>>() );

    /**
     * Locate OntologyTerm for given plain text; obsolete terms are filtered out.
     * 
     * @param plainText such as "Microsatellite Repeats"
     * @return term that exactly matches the given text, or null if nothing is found that matches exactly. If multiple
     *         terms match exactly, only the first one found in the search is returned (consistent ordering not
     *         guaranteed!)
     */
    public static OntologyTerm find( String plainText ) {
        String munged = munge( plainText );
        Collection<OntologyTerm> name = OntologySearch.matchClasses( model, index, munged );
        log.debug( munged );
        for ( OntologyTerm term : name ) {
            if ( term.getLabel().equals( munged ) ) {
                return term;
            }
        }
        return null;
    }

    public static Collection<OntologyTerm> getAllParents( OntologyTerm entry ) {
        return getAncestors( entry );
    }

    /**
     * @param entry
     * @return
     */
    public static Collection<OntologyTerm> getParents( OntologyTerm entry ) {
        Collection<OntologyTerm> parents = entry.getParents( true );
        Collection<OntologyTerm> results = new HashSet<OntologyTerm>();
        for ( OntologyTerm term : parents ) {

            if ( term instanceof OntologyClassRestriction ) {
                // log.info( "Skipping " + term );
                // OntologyProperty restrictionOn = ( ( OntologyClassRestriction ) term ).getRestrictionOn();
                // if ( restrictionOn.getLabel().equals( "part_of" ) ) {
                // OntologyTerm restrictedTo = ( ( OntologyClassRestriction ) term ).getRestrictedTo();
                // results.add( restrictedTo );
                // }
            } else {
                // log.info( "Adding " + term );
                results.add( term );
            }
        }

        return results;
    }

    /**
     * @return the has_qualifier ObjectProperty that can be used to form statements about MESH term instances.
     */
    public static ubic.basecode.ontology.model.ObjectProperty hasQualifier() {
        Property property = model.createProperty( "http://purl.org/obo/owl/MESH#hasQualifier" ); // FIXME not valid
        RDFNode node = property.inModel( model );
        model.setStrictMode( false );
        return new ObjectPropertyImpl( node.as( com.hp.hpl.jena.ontology.ObjectProperty.class ) );
    }

    /**
     * @return the isMajorHeading ObjectProperty that can be used to form statements about MESH term instances.
     */
    public static ubic.basecode.ontology.model.DatatypeProperty isMajorHeading() {
        Property property = model.createProperty( "http://purl.org/obo/owl/MESH#isMajorHeading" ); // FIXME not valid
        RDFNode node = property.inModel( model );
        model.setStrictMode( false );
        return new ubic.basecode.ontology.model.DatatypePropertyImpl(
                node.as( com.hp.hpl.jena.ontology.DatatypeProperty.class ) );
    }

    /**
     * @param entry
     * @param includePartOf
     * @return
     */
    private static Collection<OntologyTerm> getAncestors( OntologyTerm entry ) {

        Collection<OntologyTerm> ancestors = parentsCache.get( entry.getUri() );
        if ( ancestors == null ) {
            ancestors = new HashSet<OntologyTerm>();

            Collection<OntologyTerm> parents = getParents( entry );
            if ( parents != null ) {
                for ( OntologyTerm parent : parents ) {
                    ancestors.add( parent );
                    ancestors.addAll( getAncestors( parent ) );
                }
            }

            ancestors = Collections.unmodifiableCollection( ancestors );
            parentsCache.put( entry.getUri(), ancestors );
        }
        return new HashSet<OntologyTerm>( ancestors );
    }

    private static String munge( String plainText ) {
        String[] fields = plainText.split( "," );
        if ( fields.length == 1 ) {
            return plainText.toLowerCase().trim().replaceAll( " ", "_" );
        } else if ( fields.length == 2 ) {
            // swap them around
            return fields[1].toLowerCase().trim().replaceAll( " ", "_" ) + "_"
                    + fields[0].toLowerCase().trim().replaceAll( " ", "_" );
        } else {
            return plainText.toLowerCase().trim().replaceAll( "[, ]", "_" );
        }
    }

}
