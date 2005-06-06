package edu.columbia.gemma.loader.association;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GoMappings {
    protected static final Log log = LogFactory.getLog( GoMappings.class );

    public void mapFromHuman() {
        log.info( "Goa Project:  Parsing Human Go File" );
    }

    public void mapFromMouse() {
        log.info( "Goa Project:  Parsing Mouse Go File" );
    }

    public void mapFromRat() {
        log.info( "Goa Project:  Parsing Rat Go File" );
    }

}
