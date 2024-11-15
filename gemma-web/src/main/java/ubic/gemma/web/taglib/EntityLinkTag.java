package ubic.gemma.web.taglib;

import org.springframework.web.servlet.tags.RequestContextAwareTag;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.web.util.WebEntityUrlBuilder;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Tag that generates an HTML link for a given {@link Identifiable} entity.
 * @author poirigui
 * @see WebEntityUrlBuilder
 */
public class EntityLinkTag extends RequestContextAwareTag {

    private Identifiable entity;
    private boolean external;
    @Nullable
    private String title;

    @Override
    protected int doStartTagInternal() throws Exception {
        WebEntityUrlBuilder builder = getRequestContext().getWebApplicationContext().getBean( WebEntityUrlBuilder.class );
        String uri;
        if ( external ) {
            uri = builder.fromHostUrl().entity( entity ).web().toUriString();
        } else {
            uri = builder.fromContextPath().entity( entity ).web().toUriString();
        }
        pageContext.getOut().write( "<a href=\"" + escapeHtml4( uri ) + "\"" );
        if ( title != null ) {
            pageContext.getOut().write( " title=\"" + escapeHtml4( title ) + "\"" );
        }
        pageContext.getOut().write( ">" );
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            pageContext.getOut().write( "</a>" );
            return EVAL_PAGE;
        } catch ( Exception e ) {
            throw new JspTagException( e );
        }
    }

    public void setEntity( Identifiable entity ) {
        this.entity = entity;
    }

    public void setTitle( @Nullable String title ) {
        this.title = title;
    }

    public void setExternal( boolean external ) {
        this.external = external;
    }
}
