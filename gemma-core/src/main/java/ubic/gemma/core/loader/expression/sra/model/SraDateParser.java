package ubic.gemma.core.loader.expression.sra.model;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SraDateParser extends XmlAdapter<String, Date> {

    private final DateFormat format = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );

    @Override
    public Date unmarshal( String v ) throws Exception {
        synchronized ( format ) {
            return format.parse( v );
        }
    }

    @Override
    public String marshal( Date v ) {
        synchronized ( format ) {
            return format.format( v );
        }
    }
}
