package ubic.gemma.core.loader.expression.arrayExpress.util;

import ubic.gemma.core.util.NetDatasourceUtil;
import ubic.gemma.core.config.Settings;

/**
 * @author pavlidis
 *
 */
public class ArrayExpressUtil extends NetDatasourceUtil {

    @Override
    public void init() {
        this.setHost( Settings.getString( "arrayExpress.host" ) );
        this.setLogin( Settings.getString( "arrayExpress.user" ) );
        this.setPassword( Settings.getString( "arrayExpress.password" ) );
        assert this.getHost() != null;
    }

}
