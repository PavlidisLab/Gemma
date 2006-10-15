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
package ubic.gemma.visualization;

import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;

/**
 * Provides information to visualize the ExpressionDataMatrix over http. This class is useful when using the
 * HtmlMatrixVisualizerTag to render the matrix in the browser.
 * 
 * @author keshav
 * @version $Id$
 */
public class HttpExpressionDataMatrixVisualizer extends DefaultExpressionDataMatrixVisualizer {

    private static final long serialVersionUID = -1502097627023644747L;

    private String protocol = "http";

    private String server = "localhost";

    private int port = 8080;

    /**
     * Do not instantiate. This is to be "inpected" by java constructs that require an official java bean. When we say
     * inspected, we mean it is never actually invoked but the signature checked using a string comparison. Invocation
     * will result in a RuntimeException
     */
    public HttpExpressionDataMatrixVisualizer() {
        super();
        throw new RuntimeException( "cannot instantiate using no-arg constructor" );
    }

    /**
     * @param expressionDataMatrix
     * @param imageFile
     */
    public HttpExpressionDataMatrixVisualizer( ExpressionDataMatrix expressionDataMatrix, String imageFile ) {
        super( expressionDataMatrix, imageFile );
    }

    /**
     * @param protocol
     * @param server
     * @param port
     */
    public HttpExpressionDataMatrixVisualizer( ExpressionDataMatrix expressionDataMatrix, String protocol,
            String server, int port, String imageFile ) {
        super( expressionDataMatrix, imageFile );

        this.protocol = protocol;
        this.server = server;
        this.port = port;
    }

    /**
     * @return Returns the protocol.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @param protocol The protocol to set.
     */
    public void setProtocol( String protocol ) {
        this.protocol = protocol;
    }

    /**
     * @return Returns the server.
     */
    public String getServer() {
        return server;
    }

    /**
     * @param server The server to set.
     */
    public void setServer( String server ) {
        this.server = server;
    }

    /**
     * @return Returns the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port The port to set.
     */
    public void setPort( int port ) {
        this.port = port;
    }
}
