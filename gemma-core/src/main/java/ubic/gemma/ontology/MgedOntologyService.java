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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author klc
 */
public class MgedOntologyService {

    protected static final Log logger = LogFactory.getLog( MgedOntologyService.class );

    // public

    public OntologyDataList getTerm(int start, int count, String orderBy ) {

//        if ( uri == null ) return new OntologyData();
//
//        OntologyTerm term = OntologyTools.getOntologyTerm( uri );
//        if ( term == null ) return new OntologyData();
//
//        OntologyData od = new OntologyData( term.getUri(), term.getTerm(), term.getComment(), term.getChildren( true ) );
//
//        return od;
        OntologyDataList res = new OntologyDataList();
        Collection<OntologyData> children = new ArrayList<OntologyData>();
        children.add(new OntologyData(99,"term99", "desc99"));
        children.add(new OntologyData(98, "term98", "desc98"));
        
        Object[] ontos = { new OntologyData(1,"term1","desc1"), new OntologyData(2,"term2","desc2"),new OntologyData(3,"term3","desc3")};
        res.setData( ontos );
        res.setTotalSize( 3 );
        
        return res;
    }
    
    public Collection<OntologyTreeNode> getBioMaterialTerms(){
        
        Collection<OntologyTreeNode> nodes = new ArrayList<OntologyTreeNode>();
        
        OntologyTreeNode ontologyTreeNodeA = new OntologyTreeNode( "foo" );
        
        ontologyTreeNodeA.appendChild( new OntologyTreeNode("oobly") );
        
        nodes.add( ontologyTreeNodeA );
        nodes.add( new OntologyTreeNode( "bar" ) );
        return nodes;
    }

}
