package ubic.gemma.web.taglib;

import lombok.Setter;
import org.springframework.util.Assert;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.HtmlUtils;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.web.util.Constants;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Setter
public class CharacteristicTag extends HtmlEscapingAwareTag {

    /**
     * Characteristic to generate the tag for.
     */
    private Characteristic characteristic;

    /**
     * Only render the category part of the characteristic.
     */
    private boolean category;

    /**
     * Generate an external link.
     * <p>
     * The default is to generate an internal link.
     */
    private boolean external;

    @Override
    public int doStartTagInternal() throws JspException {
        Assert.notNull( characteristic, "The characteristic attribute must be set." );
        String gemBrowUrl = ( String ) getAppConfig().get( "gemma.gemBrow.url" );
        TagWriter tagWriter = new TagWriter( pageContext );
        if ( !category && characteristic.getValue() != null ) {
            if ( characteristic.getValueUri() != null ) {
                tagWriter.startTag( "a" );
                if ( external ) {
                    tagWriter.writeAttribute( "href", htmlEscape( characteristic.getValueUri() ) );
                    tagWriter.writeAttribute( "target", "_blank" );
                    tagWriter.writeAttribute( "rel", "noreferred noopener" );
                } else {
                    tagWriter.writeAttribute( "href", htmlEscape( gemBrowUrl + "/#/q/" + urlEncode( characteristic.getValueUri() ) ) );
                }
                tagWriter.writeOptionalAttributeValue( "title", htmlEscape( characteristic.getCategory() ) );
            }
            tagWriter.startTag( "span" );
            tagWriter.appendValue( htmlEscape( characteristic.getValue() ) );
            tagWriter.endTag();
            if ( characteristic.getValueUri() != null ) {
                tagWriter.endTag();
            }
        } else if ( characteristic.getCategory() != null ) {
            if ( characteristic.getCategoryUri() != null ) {
                tagWriter.startTag( "a" );
                if ( external ) {
                    tagWriter.writeAttribute( "href", htmlEscape( characteristic.getCategoryUri() ) );
                    tagWriter.writeAttribute( "target", "_blank" );
                    tagWriter.writeAttribute( "rel", "noreferred noopener" );
                } else {
                    tagWriter.writeAttribute( "href", htmlEscape( gemBrowUrl + "/#/q/" + urlEncode( characteristic.getCategoryUri() ) ) );
                }
            }
            tagWriter.startTag( "span" );
            tagWriter.appendValue( htmlEscape( characteristic.getCategory() ) );
            tagWriter.endTag();
            if ( characteristic.getCategoryUri() != null ) {
                tagWriter.endTag();
            }
        } else {
            tagWriter.startTag( "i" );
            tagWriter.appendValue( "Uncategorized" );
            tagWriter.endTag();
        }
        return SKIP_BODY;
    }

    private Map<String, ?> getAppConfig() {
        //noinspection unchecked
        return ( Map<String, ?> ) requireNonNull( pageContext.getAttribute( Constants.CONFIG, PageContext.APPLICATION_SCOPE ) );
    }

    private String htmlEscape( String s ) {
        return isHtmlEscape() ? HtmlUtils.htmlEscape( s ) : s;
    }

    private String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}
