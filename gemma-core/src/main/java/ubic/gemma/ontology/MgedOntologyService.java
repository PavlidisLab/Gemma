/*
 * The GemmaOnt project
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

package ubic.gemma.ontology;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * Holds a complete copy of the MgedOntology in memory. This gets loaded on startup. As the MgedOntology is the
 * framework ontology i've added a feature so that the Ontology can be changed dynamically via the web front end.
 * 
 * @author klc
 * @version $Id: MgedOntologyService.java
 * @spring.bean id="mgedOntologyService"
 */

public class MgedOntologyService extends AbstractOntologyService {

    public static final String MGED_ONTO_BASE_URL = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl";

    private static final Collection<String> TermsToRemove = Collections.synchronizedList( Arrays.asList( new String[] {
            "BioMaterialPackage", "BioMaterialCharacteristics", "BioMaterial", "BiologicalProperty",
            "ExperimentalDesign", "ExperimentalFactor", "ExperimentalFactorCategory", "Experiment",
            "NormalizationDescriptionType", "NormalizationDescription", "QualityControlDescriptionType" } ) );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.AbstractOntologyService#getOntologyName()
     */
    @Override
    protected String getOntologyName() {
        return "mgedOntology";
    }

    protected static final Log log = LogFactory.getLog( MgedOntologyService.class );

    protected String ontology_startingPoint;

    public MgedOntologyService() {
        super();
        ontology_startingPoint = getOntologyStartingPoint();
    }

    public Collection<OntologyTreeNode> getBioMaterialTreeNodeTerms() {

        if ( !ready.get() ) return null;

        Collection<OntologyTreeNode> nodes = new ArrayList<OntologyTreeNode>();

        OntologyTerm term = terms.get( ontology_startingPoint );

        nodes.add( buildTreeNode( term ) );
        return nodes;
    }

    public Collection<OntologyTerm> getBioMaterialTerms() {

        if ( !ready.get() ) return null;

        OntologyTerm term = terms.get( ontology_startingPoint );
        Collection<OntologyTerm> results = getAllTerms( term );
        results.add( term );

        return results;

    }

    /**
     * @return Returns the Mged Ontology Terms that are usefull for annotating Gemma. Basically the terms in the
     *         bioMaterial package plus some special cases.
     */
    public Collection<OntologyTerm> getUsefulMgedTerms() {
        if ( !ready.get() ) return null;

        Collection<OntologyTerm> results = getBioMaterialTerms();
        results = Collections.synchronizedCollection( results );

        // A bunch of terms not in the biomaterial package that we need. (special cases)
        OntologyTerm term = terms.get( MGED_ONTO_BASE_URL + "#ExperimentPackage" );
        results.addAll( getAllTerms( term ) );

        term = terms.get( MGED_ONTO_BASE_URL + "#MeasurementPackage" );
        results.addAll( getAllTerms( term ) );

        // trim some terms out:
        Collection<OntologyTerm> trimmed = Collections.synchronizedSet( new HashSet<OntologyTerm>() );
        for ( OntologyTerm mgedTerm : results ) {
            if ( !TermsToRemove.contains( mgedTerm.getTerm() ) ) {
                trimmed.add( mgedTerm );
            }
        }

        return trimmed;

    }

    private static Map<String, URL> keyToTermListUrl;
    static {
        keyToTermListUrl = new HashMap<String, URL>();
        keyToTermListUrl.put( "design", MgedOntologyService.class.getResource( "MO.design.categories.txt" ) );
        keyToTermListUrl.put( "experiment", MgedOntologyService.class.getResource( "MO.experiment.categories.txt" ) );
        keyToTermListUrl.put( "factor", MgedOntologyService.class.getResource( "MO.factor.categories.txt" ) );
        keyToTermListUrl.put( "factorvalue", MgedOntologyService.class.getResource( "MO.factorvalue.categories.txt" ) );
    }
    private static Map<String, Collection<OntologyTerm>> keyToTermListCache = new HashMap<String, Collection<OntologyTerm>>();

    /**
     * @param key
     * @return
     */
    public Collection<OntologyTerm> getMgedTermsByKey( String key ) {
        Collection<OntologyTerm> terms = keyToTermListCache.get( key );
        if ( terms == null ) {
            URL termListUrl = keyToTermListUrl.get( key );
            if ( termListUrl == null ) {
                log.warn( "Unknown term list key '" + key + "'; returning general term list" );
                terms = getUsefulMgedTerms();
            } else {
                terms = new HashSet<OntologyTerm>();
                try {
                    Collection<String> wantedTerms = new ArrayList<String>();
                    BufferedReader reader = new BufferedReader( new InputStreamReader( termListUrl.openStream() ) );
                    String line;
                    while ( ( line = reader.readLine() ) != null ) {
                        if ( line.startsWith( "#" ) ) continue;
                        wantedTerms.add( StringUtils.strip( line ) );
                    }
                    reader.close();

                    for ( OntologyTerm term : getUsefulMgedTerms() ) {
                        if ( wantedTerms.contains( term.getTerm() ) ) terms.add( term );
                    }
                } catch ( IOException ioe ) {
                    log.error( "Error reading from term list '" + termListUrl + "'; returning general term list", ioe );
                    terms = getUsefulMgedTerms();
                }
            }
            terms = Collections.unmodifiableCollection( terms );
            keyToTermListCache.put( key, terms );
        }
        return terms;
    }

    /**
     * Will attempt to load a different ontology into the MGED ontology service
     * 
     * @param ontologyURL
     * @param startingPointURL
     */
    public void loadNewOntology( String ontologyURL, String startingPointURL ) {

        if ( running.get() ) return;

        ontology_URL = ontologyURL;
        ontology_startingPoint = startingPointURL;

        ready = new AtomicBoolean( false );
        running = new AtomicBoolean( false );

        init( true );

    }

    protected Collection<OntologyTerm> getAllTerms( OntologyTerm term ) {

        Collection<OntologyTerm> children = term.getChildren( true );

        if ( ( children == null ) || ( children.isEmpty() ) ) return new HashSet<OntologyTerm>();

        Collection<OntologyTerm> grandChildren = new HashSet<OntologyTerm>();
        for ( OntologyTerm child : children ) {
            grandChildren.addAll( getAllTerms( child ) );
        }

        children.addAll( grandChildren );
        return children;

    }

    /**
     * @param node Recursivly builds the tree node structure that is needed by the ext tree
     */
    protected OntologyTreeNode buildTreeNode( OntologyTerm term ) {

        OntologyTreeNode node = new OntologyTreeNode( term );
        node.setLeaf( true );
        Collection<OntologyTerm> children = term.getChildren( true );

        if ( ( children != null ) && ( !children.isEmpty() ) ) {
            // node has children
            node.setAllowChildren( true );
            node.setLeaf( false );

            for ( OntologyTerm child : children ) {
                node.appendChild( buildTreeNode( child ) );
            }
        }

        return node;

    }

    @Override
    protected OntModel loadModel( String url ) {
        return OntologyLoader.loadMemoryModel( url );
    }

    protected String getOntologyStartingPoint() {
        return MGED_ONTO_BASE_URL + "#BioMaterialPackage";
    }

    @Override
    protected String getOntologyUrl() {
        return MGED_ONTO_BASE_URL;
    }

}