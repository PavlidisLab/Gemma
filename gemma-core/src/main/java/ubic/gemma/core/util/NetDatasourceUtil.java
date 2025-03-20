/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import ubic.basecode.util.NetUtils;

import java.io.IOException;

/**
 * Helper methods to get FTP connection.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public abstract class NetDatasourceUtil {

    private final Log log = LogFactory.getLog( this.getClass().getName() );
    private String host;
    private String login = "anonymous";
    private String password = "";

    protected NetDatasourceUtil() {
        this.init();
    }

    public FTPClient connect( int mode ) throws IOException {
        log.info( "Connecting to " + host + " with " + login + " : " + password );
        return NetUtils.connect( mode, host, login, password );
    }

    public void disconnect( FTPClient f ) throws IOException {
        f.disconnect();
    }

    public void setLogin( String login ) {
        this.login = login;
    }

    public void setPassword( String password ) {
        this.password = password;
    }

    public String getHost() {
        return host;
    }

    public void setHost( String host ) {
        this.host = host;
    }

    protected abstract void init();
}
