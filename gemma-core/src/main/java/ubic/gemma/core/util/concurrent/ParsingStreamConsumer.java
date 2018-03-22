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
package ubic.gemma.core.util.concurrent;

import ubic.gemma.core.loader.util.parser.Parser;

import java.io.IOException;
import java.io.InputStream;

/**
 * See http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html
 *
 * @author pavlidis
 */
public class ParsingStreamConsumer<T> extends Thread {
    private final InputStream is;
    private final Parser<T> parser;

    public ParsingStreamConsumer( Parser<T> parser, InputStream is ) {
        this.is = is;
        this.parser = parser;
    }

    @Override
    public void run() {
        try {
            parser.parse( is );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}
