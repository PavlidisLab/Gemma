package ubic.gemma.web.taglib;

import lombok.Setter;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.HtmlUtils;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueUtils;

@Setter
public class FactorValueTag extends HtmlEscapingAwareTag {

    private FactorValue factorValue;

    @Override
    protected int doStartTagInternal() throws Exception {
        TagWriter writer = new TagWriter( pageContext );
        writer.startTag( "span" );
        String summary = FactorValueUtils.getSummaryString( factorValue );
        writer.appendValue( isHtmlEscape() ? HtmlUtils.htmlEscape( summary ) : summary );
        writer.endTag();
        return SKIP_BODY;
    }
}
