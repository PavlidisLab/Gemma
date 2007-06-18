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

        return Property.Factory.newInstance();
    }

    public OutboundVariable convertOutbound( Object data, OutboundContext outctx ) throws MarshallException {
        return new SimpleOutboundVariable( '\'' + data.toString() + '\'', outctx, true );
    }

}
