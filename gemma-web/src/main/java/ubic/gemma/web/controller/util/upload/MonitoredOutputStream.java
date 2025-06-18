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
 * Credit:
 *   If you're nice, you'll leave this bit:
 *
 *   Class by Pierre-Alexandre Losson -- http://www.telio.be/blog
 *   email : plosson@users.sourceforge.net
 */
package ubic.gemma.web.controller.util.upload;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream that puts information on how many bytes have been read into a OutputStreamListener.
 *
 * @author Original : plosson on 05-janv.-2006 10:46:33
 * @author pavlidis
 *
 */
public class MonitoredOutputStream extends OutputStream {
    private final OutputStream target;
    private final OutputStreamListener listener;

    /**
     */
    public MonitoredOutputStream( OutputStream target, OutputStreamListener listener ) {
        this.target = target;
        this.listener = listener;
        this.listener.start();
    }

    @Override
    public void close() throws IOException {
        target.close();
        listener.done();
    }

    @Override
    public void flush() throws IOException {
        target.flush();
    }

    @Override
    public void write( byte[] b ) throws IOException {
        target.write( b );
        listener.bytesRead( b.length );
    }

    @Override
    public void write( byte[] b, int off, int len ) throws IOException {
        target.write( b, off, len );
        listener.bytesRead( len );
    }

    @Override
    public void write( int b ) throws IOException {
        target.write( b );
        listener.bytesRead( 1 );
    }
}
