package ubic.gemma.web.taglib;

import lombok.Setter;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.web.util.WebEntityUrlBuilder;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;

/**
 * Tag that generates an HTML link for a given {@link Identifiable} entity.
 * @author poirigui
 * @see WebEntityUrlBuilder
 */
@Setter
public class EntityLinkTag extends AbstractHtmlElementTag {

    private transient WebEntityUrlBuilder entityUrlBuilder;

    private Identifiable entity;
    private boolean external;
    @Nullable
    private BioAssayDimension dimension;

    private transient TagWriter tagWriter;

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
        writeOptionalAttributes( tagWriter );
        tagWriter.forceBlock();
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        tagWriter.endTag();
        return EVAL_PAGE;
    }
}
