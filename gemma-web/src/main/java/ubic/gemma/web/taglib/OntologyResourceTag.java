package ubic.gemma.web.taglib;

import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.HtmlUtils;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.gemma.web.controller.ontology.OntologyController;

import java.util.regex.Pattern;

public class OntologyResourceTag extends HtmlEscapingAwareTag {

    private OntologyResource resource;

    @Override
    protected int doStartTagInternal() throws Exception {
        TagWriter tagWriter = new TagWriter( pageContext );
        String contextPath = getRequestContext().getContextPath();
        if ( resource.getUri() == null ) {
            tagWriter.startTag( "span" );
            tagWriter.appendValue( htmlEscape( resource.getLabel() ) );
            tagWriter.endTag();
        } else {
            tagWriter.startTag( "a" );
            tagWriter.writeAttribute( "href", htmlEscape( resource.getUri()
                    .replaceFirst( "^" + Pattern.quote( OntologyController.TGFVO_URI_PREFIX ), contextPath + "/ont/TGFVO/" )
                    .replaceFirst( "^" + Pattern.quote( OntologyController.TGEMO_URI_PREFIX ), contextPath + "/ont/" ) ) );
            tagWriter.appendValue( htmlEscape( resource.getLabel() ) );
            tagWriter.endTag();
        }
        return SKIP_BODY;
    }

    public void setResource( OntologyResource resource ) {
        this.resource = resource;
    }

    private String htmlEscape( String s ) {
        return isHtmlEscape() ? HtmlUtils.htmlEscape( s ) : s;
    }
}
