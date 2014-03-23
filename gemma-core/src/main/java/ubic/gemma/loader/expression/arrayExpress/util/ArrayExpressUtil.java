package ubic.gemma.loader.expression.arrayExpress.util;

import ubic.gemma.util.NetDatasourceUtil;
import ubic.gemma.util.Settings;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayExpressUtil extends NetDatasourceUtil {

    @Override
    public void init() {
        this.setHost( Settings.getString( "arrayExpress.host" ) );
        assert this.getHost() != null;
    }

}
