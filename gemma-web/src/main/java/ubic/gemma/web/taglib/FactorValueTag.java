package ubic.gemma.web.taglib;

import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.HtmlUtils;
import ubic.gemma.model.expression.experiment.AbstractFactorValueValueObject;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;
import ubic.gemma.model.expression.experiment.FactorValueUtils;

public class FactorValueTag extends HtmlEscapingAwareTag {

    private AbstractFactorValueValueObject factorValue;

    @Override
    protected int doStartTagInternal() throws Exception {
        if ( factorValue.getCharacteristics().size() == 1 ) {
            CharacteristicTag ct = new CharacteristicTag();
            ct.setCharacteristic( factorValue.getCharacteristics().iterator().next() );
            ct.setCategory( false );
            ct.setExternal( true );
            ct.setPageContext( pageContext );
            ct.setParent( getParent() );
            ct.setHtmlEscape( isHtmlEscape() ? "true" : "false" );
            return ct.doStartTagInternal();
        } else {
            TagWriter writer = new TagWriter( pageContext );
            writer.startTag( "span" );
            String summary = FactorValueUtils.getSummaryString( factorValue );
            writer.appendValue( isHtmlEscape() ? HtmlUtils.htmlEscape( summary ) : summary );
            writer.endTag();
            return SKIP_BODY;
        }
    }

    public void setFactorValue( Object fv ) {
        if ( fv instanceof FactorValue ) {
            this.factorValue = new FactorValueBasicValueObject( ( FactorValue ) fv );
        } else if ( fv instanceof AbstractFactorValueValueObject ) {
            this.factorValue = ( AbstractFactorValueValueObject ) fv;
        } else {
            throw new IllegalArgumentException( "Only FactorValue and subclasses of AbstractFactorValueValueObject are supported." );
        }
    }
}
