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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;

/**
 * Holds a complete copy of the MgedOntology in memory. This gets loaded on startup.
 * As the MgedOntology is the framework ontology i've added a feature so that the Ontology can be changed dynamically
 * via the web front end. 
 * 
 * @author klc
 * @version $Id: MgedOntologyService.java
 * @spring.bean id="mgedOntologyService"
 * @spring.property name="bioMaterialService" ref ="bioMaterialService"
 */

public class MgedOntologyService extends AbstractOntologyService {

    protected static final Log log = LogFactory.getLog( MgedOntologyService.class );

    protected static String ontology_startingPoint;
    private static BioMaterialService bioMaterialService;

    public MgedOntologyService() {
        super();
        ontology_startingPoint = getOntologyStartingPoint();
    }
    
    public void saveStatement( VocabCharacteristic vc, Collection<Long> bioMaterialIdList ) {

        log.info( "Vocab Characteristic: " + vc.getDescription() );
        log.info( "Biomaterial ID List: " + bioMaterialIdList );

        Set<Characteristic> chars = new HashSet<Characteristic>();
        chars.add( ( Characteristic ) vc );
        Collection<BioMaterial> biomaterials = bioMaterialService.load( bioMaterialIdList );

        for ( BioMaterial bioM : biomaterials ) {
            bioM.setCharacteristics( chars );
            bioMaterialService.update( bioM );

        }

    }

    public Collection<OntologyTreeNode> getBioMaterialTerms() {

        if ( !ready.get() ) return null;

        Collection<OntologyTreeNode> nodes = new ArrayList<OntologyTreeNode>();

        OntologyTerm term = terms.get( ontology_startingPoint );

        nodes.add( buildTreeNode( term ) );
        return nodes;
    }
    
    
    
    /**
     * 
     * Will attempt to load a different ontology into the MGED ontology service
     * @param ontologyURL
     * @param startingPointURL
     */
    public void loadNewOntology( String ontologyURL, String startingPointURL ) {

        if ( running.get() ) return;

        
        ontology_URL = ontologyURL;
        MgedOntologyService.ontology_startingPoint = startingPointURL;

        ready = new AtomicBoolean( false );
        running = new AtomicBoolean( false );

        init();

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

    
    
    /**
     * @param bioMaterialService the bioMaterialService to set
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        MgedOntologyService.bioMaterialService = bioMaterialService;
    }

    @Override
    protected  OntModel loadModel( String url, OntModelSpec spec ) throws IOException {
       return OntologyLoader.loadMemoryModel( url, spec );
    }

    protected String getOntologyStartingPoint() {
        return "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#BioMaterialPackage";
    }

    @Override
    protected String getOntologyUrl() {       
        return "http://mged.sourceforge.net/ontologies/MGEDOntology.owl";
    }

}
