/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * TODO Document me
 * 
 * @author paul
 * @version $Id$
 */
public class IndexerSelector implements Selector {

    Collection<Property> badPredicates;

    public IndexerSelector() {
        // these are predicates that in general should not be usefull for indexing
        badPredicates = new ArrayList<Property>();
        badPredicates.add( RDFS.comment );
        badPredicates.add( RDFS.seeAlso );
        badPredicates.add( RDFS.isDefinedBy );
    }

    public RDFNode getObject() {
        return null;
    }

    public Property getPredicate() {
        return null;
    }

    public Resource getSubject() {
        return null;
    }

    public boolean isSimple() {
        return false;
    }

    public boolean test( Statement s ) {
        return !( badPredicates.contains( s.getPredicate() ) );
    }

}
