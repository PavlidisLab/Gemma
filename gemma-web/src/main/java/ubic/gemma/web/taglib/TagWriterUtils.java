package ubic.gemma.web.taglib;

import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.jsp.JspException;
import java.util.Map;

public class TagWriterUtils {

    public static void writeAttributes( Map<String, Object> dynamicAttributes, boolean isHtmlEscape, TagWriter tagWriter ) throws JspException {
        for ( Map.Entry<String, Object> attr : dynamicAttributes.entrySet() ) {
            String val = attr.getValue() != null ? attr.getValue().toString() : null;
            if ( isHtmlEscape ) {
                val = HtmlUtils.htmlEscape( val );
            }
            tagWriter.writeAttribute( attr.getKey(), val );
        }
    }

    public static void writeBooleanAttribute( String attributeName, boolean attributeValue, TagWriter tagWriter ) throws JspException {
        if ( attributeValue ) {
            tagWriter.appendValue( attributeName );
        }
    }
}
