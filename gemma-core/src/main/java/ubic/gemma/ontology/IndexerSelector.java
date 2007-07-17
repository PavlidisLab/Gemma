package ubic.gemma.ontology;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;

public class IndexerSelector implements Selector {

    private static Log log = LogFactory.getLog( IndexerSelector.class.getName() );

    Collection<Property> badPredicates;

    public IndexerSelector() {
        //these are predicates that in general should not be usefull for indexing
        badPredicates = new ArrayList<Property>();
        badPredicates.add( RDFS.comment );
        badPredicates.add( RDFS.seeAlso );
        badPredicates.add( RDFS.isDefinedBy );
    }

    public boolean test( Statement s ) {
        return !( badPredicates.contains( s.getPredicate() ) );
    }

    
    public RDFNode getObject() {
        // TODO Auto-generated method stub
        return null;
    }

    public Property getPredicate() {
        // TODO Auto-generated method stub
        return null;
    }

    public Resource getSubject() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isSimple() {
        // TODO Auto-generated method stub
        return false;
    }

}
