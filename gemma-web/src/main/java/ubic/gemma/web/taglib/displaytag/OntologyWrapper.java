package ubic.gemma.web.taglib.displaytag;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.model.common.description.OntologyEntry;

/**
 * used for proper decoration of the ontologyEntry
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author klc
 * @version $Id $
 */
public class OntologyWrapper extends TableDecorator {

    Log log = LogFactory.getLog( this.getClass() );

    public String getValue() {
        Map.Entry oe = ( Map.Entry ) getCurrentRowObject();
        return ( ( OntologyEntry ) oe.getKey() ).getValue();
    }

    public String getDescription() {
        Map.Entry oe = ( Map.Entry ) getCurrentRowObject();
        return ( ( OntologyEntry ) oe.getKey() ).getDescription();
    }

    public String getCategory() {
        Map.Entry oe = ( Map.Entry ) getCurrentRowObject();
        return ( ( OntologyEntry ) oe.getKey() ).getCategory();
    }

    public String getParents() {
        Map.Entry oe = ( Map.Entry ) getCurrentRowObject();
        return ( String ) oe.getValue();
    }

}
