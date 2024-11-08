package ubic.gemma.web.taglib;

import lombok.Setter;
import org.springframework.util.Assert;
import ubic.gemma.model.common.description.Characteristic;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

@Setter
public class CharacteristicTag extends TagSupport {

    /**
     * Characteristic to generate the tag for.
     */
    private Characteristic characteristic;

    /**
     * Only render the category part of the characteristic.
     */
    private boolean category;

    @Override
    public int doStartTag() throws JspException {
        Assert.notNull( characteristic, "The characteristic attribute must be set." );
        try {
            if ( !category && characteristic.getValue() != null ) {
                if ( characteristic.getValueUri() != null ) {
                    pageContext.getOut().write( "<a href=\"" + escapeHtml4( characteristic.getValueUri() ) + "\"" );
                    pageContext.getOut().write( " target=\"_blank\" rel=\"noreferred noopener\"" );
                    if ( characteristic.getCategory() != null ) {
                        pageContext.getOut().write( " title=\"" + characteristic.getCategory() + "\"" );
                    }
                    pageContext.getOut().write( ">" );
                }
                pageContext.getOut().write( "<span>" + escapeHtml4( characteristic.getValue() ) + "</span>" );
                if ( characteristic.getValueUri() != null ) {
                    pageContext.getOut().write( "</a>" );
                }
            } else if ( characteristic.getCategory() != null ) {
                if ( characteristic.getCategoryUri() != null ) {
                    pageContext.getOut().write( "<a href=\"" + escapeHtml4( characteristic.getCategoryUri() ) + "\"" );
                    pageContext.getOut().write( " target=\"_blank\" rel=\"noreferred noopener\"" );
                    pageContext.getOut().write( ">" );
                }
                pageContext.getOut().write( "<span>" + escapeHtml4( characteristic.getCategory() ) + "</span>" );
                if ( characteristic.getCategoryUri() != null ) {
                    pageContext.getOut().write( "</a>" );
                }
            } else {
                pageContext.getOut().write( "<i>Uncategorized</i>" );
            }
        } catch ( IOException e ) {
            throw new JspException( e );
        }
        return TagSupport.SKIP_BODY;
    }
}
