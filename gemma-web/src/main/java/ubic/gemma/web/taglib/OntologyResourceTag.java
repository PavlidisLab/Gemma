package ubic.gemma.web.taglib;

import lombok.Setter;
import org.springframework.util.Assert;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.HtmlUtils;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.gemma.core.ontology.OntologyExternalLinks;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;

@Setter
public class OntologyResourceTag extends HtmlEscapingAwareTag {

    private transient OntologyResource resource;

    @Nullable
    private transient OntologyExternalLinks ontologyExternalLinks;

    @Override
    protected int doStartTagInternal() throws Exception {
        Assert.notNull( resource, "An ontology resource must be set." );
        TagWriter tagWriter = new TagWriter( pageContext );
        String contextPath = getRequestContext().getContextPath();
        writeResource( resource, contextPath, tagWriter );
        return SKIP_BODY;
    }

    void writeResource( OntologyResource resource, String contextPath, TagWriter tagWriter ) throws JspException {
        if ( resource.getUri() == null ) {
            tagWriter.startTag( "span" );
            String label;
            if ( resource.getLabel() != null ) {
                label = resource.getLabel();
            } else if ( resource.getLocalName() != null ) {
                label = resource.getLocalName();
            } else {
                label = resource.getUri();
            }
            tagWriter.appendValue( htmlEscape( label ) );
            tagWriter.endTag();
        } else {
            boolean isExternal = !resource.getUri().startsWith( "http://gemma.msl.ubc.ca/ont/" );
            String href;
            if ( isExternal ) {
                href = getOntologyExternalLinks().getExternalLink( resource );
            } else {
                href = contextPath + "/ont/" + resource.getUri().substring( "http://gemma.msl.ubc.ca/ont/".length() );
            }
            tagWriter.startTag( "a" );
            tagWriter.writeAttribute( "href", htmlEscape( href ) );
            if ( isExternal ) {
                tagWriter.writeAttribute( "target", "_blank" );
                tagWriter.writeAttribute( "rel", "noopener noreferrer" );
            }
            String label;
            if ( resource.getLabel() != null ) {
                label = resource.getLabel();
            } else if ( resource.getLocalName() != null ) {
                label = resource.getLocalName();
            } else {
                label = resource.getUri();
            }
            if ( isExternal ) {
                label += " \uD83D\uDDD7";
            }
            tagWriter.appendValue( htmlEscape( label ) );
            tagWriter.endTag();
        }
    }

    private OntologyExternalLinks getOntologyExternalLinks() {
        if ( ontologyExternalLinks == null ) {
            ontologyExternalLinks = WebApplicationContextUtils
                    .getRequiredWebApplicationContext( pageContext.getServletContext() )
                    .getBean( OntologyExternalLinks.class );
        }
        return ontologyExternalLinks;
    }

    private String htmlEscape( String s ) {
        return isHtmlEscape() ? HtmlUtils.htmlEscape( s ) : s;
    }
}
