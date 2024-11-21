package ubic.gemma.web.taglib;

import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.HtmlUtils;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.web.util.WebEntityUrlBuilder;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import java.util.LinkedHashMap;
import java.util.Map;

import static ubic.gemma.web.taglib.TagWriterUtils.writeAttributes;

/**
 * Tag that generates an HTML link for a given {@link Identifiable} entity.
 * @author poirigui
 * @see WebEntityUrlBuilder
 */
public class EntityLinkTag extends HtmlEscapingAwareTag implements DynamicAttributes {

    private WebEntityUrlBuilder entityUrlBuilder;

    private Identifiable entity;
    private boolean external;
    @Nullable
    private String title;
    @Nullable
    private BioAssayDimension dimension;
    private final Map<String, Object> dynamicAttributes = new LinkedHashMap<>();

    private TagWriter tagWriter;

    @Override
    protected int doStartTagInternal() throws Exception {
        if ( entityUrlBuilder == null ) {
            entityUrlBuilder = getRequestContext().getWebApplicationContext().getBean( WebEntityUrlBuilder.class );
        }
        tagWriter = new TagWriter( pageContext );
        String uri;
        if ( external ) {
            uri = entityUrlBuilder.fromHostUrl().entity( entity ).web().toUriString();
        } else {
            uri = entityUrlBuilder.fromContextPath().entity( entity ).web().toUriString();
        }
        if ( dimension != null && ( entity instanceof ExpressionExperimentSubSet
                || entity instanceof BioAssay
                || entity instanceof BioMaterial ) ) {
            uri += "&dimension=" + dimension.getId();
        }
        tagWriter.startTag( "a" );
        tagWriter.writeAttribute( "href", htmlEscape( uri ) );
        tagWriter.writeOptionalAttributeValue( "title", htmlEscape( title ) );
        writeAttributes( dynamicAttributes, isHtmlEscape(), tagWriter );
        tagWriter.forceBlock();
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        tagWriter.endTag();
        return EVAL_PAGE;
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

    public void setDimension( @Nullable BioAssayDimension dimension ) {
        this.dimension = dimension;
    }

    @Override
    public void setDynamicAttribute( String uri, String localName, Object value ) {
        dynamicAttributes.put( localName, value );
    }

    private String htmlEscape( String s ) {
        return isHtmlEscape() ? HtmlUtils.htmlEscape( s ) : s;
    }
}
