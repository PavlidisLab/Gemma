package ubic.gemma.web.taglib.displaytag;

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
 * @version $Id$
 */
public class OntologyWrapper extends TableDecorator {

    Log log = LogFactory.getLog( this.getClass() );

    // public String getValue() {
    // OntologyEntry oe = (OntologyEntry ) getCurrentRowObject();
    // return oe.getValue();
    // }
    //
    // public String getDescription() {
    // OntologyEntry oe = (OntologyEntry ) getCurrentRowObject();
    // return oe.getDescription();
    // }
    //
    // public String getCategory() {
    // OntologyEntry oe = (OntologyEntry ) getCurrentRowObject();
    // return oe.getCategory();
    // }

    public String getAccession() {
        OntologyEntry oe = ( OntologyEntry ) getCurrentRowObject();

        return "<a target='_blank' href='http://amigo.geneontology.org/cgi-bin/amigo/go.cgi?view=details&query="
                + oe.getAccession() + "'>" + oe.getAccession() + "</a>";

    }

}
