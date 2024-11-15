package ubic.gemma.web.taglib;

import lombok.Setter;
import org.springframework.util.Assert;
import org.springframework.web.servlet.tags.RequestContextAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
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
public class CharacteristicTag extends RequestContextAwareTag {

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
                    tagWriter.writeAttribute( "href", characteristic.getValueUri() );
                    tagWriter.writeAttribute( "target", "_blank" );
                    tagWriter.writeAttribute( "rel", "noreferred noopener" );
                } else {
                    tagWriter.writeAttribute( "href", gemBrowUrl + "/#/q/" + urlEncode( characteristic.getValueUri() ) );
                }
                tagWriter.writeOptionalAttributeValue( "title", characteristic.getCategory() );
            }
            tagWriter.startTag( "span" );
            tagWriter.appendValue( characteristic.getValue() );
            tagWriter.endTag();
            if ( characteristic.getValueUri() != null ) {
                tagWriter.endTag();
            }
        } else if ( characteristic.getCategory() != null ) {
            if ( characteristic.getCategoryUri() != null ) {
                tagWriter.startTag( "a" );
                if ( external ) {
                    tagWriter.writeAttribute( "href", characteristic.getCategoryUri() );
                    tagWriter.writeAttribute( "target", "_blank" );
                    tagWriter.writeAttribute( "rel", "noreferred noopener" );
                } else {
                    tagWriter.writeAttribute( "href", gemBrowUrl + "/#/q/" + urlEncode( characteristic.getCategoryUri() ) );
                }
            }
            tagWriter.startTag( "span" );
            tagWriter.appendValue( characteristic.getCategory() );
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

    private String urlEncode( String s ) {
        try {
            return URLEncoder.encode( s, StandardCharsets.UTF_8.name() );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }
}
