package ubic.gemma.web.remote;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.convert.BeanConverter;
import org.directwebremoting.dwrp.ObjectOutboundVariable;
import org.directwebremoting.extend.MarshallException;
import org.directwebremoting.extend.OutboundContext;
import org.directwebremoting.extend.OutboundVariable;
import org.directwebremoting.extend.Property;

import ubic.gemma.ontology.OntologyClassRestriction;
import ubic.gemma.ontology.OntologyTerm;

public class OntologyTermConverter extends BeanConverter {

    private static Log log = LogFactory.getLog( OntologyTermConverter.class.getName() );

    public OutboundVariable convertOutbound( Object data, OutboundContext outctx ) throws MarshallException {
        // Where we collect out converted children
        Map<String, OutboundVariable> ovs = new TreeMap<String, OutboundVariable>();

        // We need to do this before collecing the children to save recurrsion
        ObjectOutboundVariable ov = new ObjectOutboundVariable( outctx );
        outctx.put( data, ov );

        try {
            Map properties = getPropertyMapFromObject( data, true, false );
            props: for ( Iterator it = properties.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = ( Map.Entry ) it.next();
                String name = ( String ) entry.getKey();
                Property property = ( Property ) entry.getValue();

                Object value = property.getValue( data );

                // Loop detection:
                // Detects self referential loops
                //
                if ( data instanceof OntologyTerm && value instanceof Collection ) {// Look for loops in the class
                                                                                    // restrictions of the ontology term
                    OntologyTerm term = ( OntologyTerm ) data;
                    for ( Object o : ( Collection ) value ) {

                        if ( o instanceof OntologyClassRestriction ) {

                            OntologyClassRestriction restriction = ( OntologyClassRestriction ) o;
                            OntologyTerm restrictedTo = restriction.getRestrictedTo();
                            
                            if ((restrictedTo != null) && (term != null) && restrictedTo.equals( term ) ) { // is it a self referential loop?
                                log.info( "Loop detected" );
                                // todo: put something special in the returned object so that the java scrip gui knows
                                // and displays the loop correctly
                                continue props; // if a loop is detected don't try to convert it!
                            }
                        }
                    }
                }
                OutboundVariable nested = getConverterManager().convertOutbound( value, outctx );

                ovs.put( name, nested );
            }
        } catch ( MarshallException ex ) {
            throw ex;
        } catch ( Exception ex ) {
            throw new MarshallException( data.getClass(), ex );
        }

        ov.init( ovs, getJavascript() );

        return ov;
    }

}
