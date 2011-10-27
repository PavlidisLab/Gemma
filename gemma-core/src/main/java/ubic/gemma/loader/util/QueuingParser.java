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
package ubic.gemma.loader.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;

/**
 * Defines a class that produces object that can be consumed by other classes.
 * 
 * @author pavlidis
 * @version $Id$
 */
public interface QueuingParser<T> {

    /**
     * Parse an input stream, storing the results in the passed queue (which can be used by a consumer)
     * 
     * @param inputStream
     * @param queue
     * @throws IOException
     */
    public void parse( InputStream inputStream, BlockingQueue<T> queue ) throws IOException;

}
