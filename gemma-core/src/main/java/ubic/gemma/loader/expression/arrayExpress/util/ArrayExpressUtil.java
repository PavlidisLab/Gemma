package ubic.gemma.loader.expression.arrayExpress.util;

import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.NetDatasourceUtil;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayExpressUtil extends NetDatasourceUtil {

    @Override
    public void init() {
        this.setHost( ConfigUtils.getString( "arrayExpress.host" ) );
        assert this.getHost() != null;
    }

}
