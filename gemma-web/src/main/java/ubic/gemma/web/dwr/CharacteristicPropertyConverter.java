package ubic.gemma.web.dwr;

import org.directwebremoting.convert.BaseV20Converter;
import org.directwebremoting.dwrp.SimpleOutboundVariable;
import org.directwebremoting.extend.InboundContext;
import org.directwebremoting.extend.InboundVariable;
import org.directwebremoting.extend.MarshallException;
import org.directwebremoting.extend.OutboundContext;
import org.directwebremoting.extend.OutboundVariable;

import ubic.gemma.model.common.description.Property;

public class CharacteristicPropertyConverter extends BaseV20Converter {

    public Object convertInbound( Class paramType, InboundVariable data, InboundContext inctx )
            throws MarshallException {

        //todo:  need to convert the data into a well formed characteristic. 
        //Notes:  this is going to be a recursive process as the characteristic could be quite deep.
        //Context is held onto twice.  Via data (data.context) and passed in (inctx). which one to use. 
        //For sex:
        //data.value = {termUri:reference:c0-e4, object:reference:c0-e5, fieldId:reference:c0-e7} 
        //data.type = Object_Object
        //context.variables  = {c0-e4=string:http%3A%2F%2Fmged.sourceforge.net%2Fontologies%2FMGEDOntology.owl%23Sex, c0-param1=Array:[reference:c0-e8], c0-e1=string:http%3A%2F%2Fmged.sourceforge.net%2Fontologies%2FMGEDOntology.owl%23Sex, c0-e7=string:7768, c0-e8=number:65, c0-param0=Object_Object:{termUri:reference:c0-e1, properties:reference:c0-e2}, c0-e6=string:http%3A%2F%2Fmged.sourceforge.net%2Fontologies%2FMGEDOntology.owl%23male, c0-e3=Object_Object:{termUri:reference:c0-e4, object:reference:c0-e5, fieldId:reference:c0-e7}, c0-e2=Array:[reference:c0-e3], c0-e5=Object_Object:{termUri:reference:c0-e6}}
        //context.variables is a hash map with the keys being the refs values being their actual representations. 
        
        return Property.Factory.newInstance();
    }

    public OutboundVariable convertOutbound( Object data, OutboundContext outctx ) throws MarshallException {
        return new SimpleOutboundVariable( '\'' + data.toString() + '\'', outctx, true );
    }

}
