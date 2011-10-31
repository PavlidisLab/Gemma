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
package ubic.gemma.loader.util.parser;

/**
 * A Parser that processes its input line-by-line. One of the parse methods must be called before data becomes
 * available.
 * 
 * @author pavlidis
 * @version $Id$
 */
public interface LineParser<T> extends Parser<T> {

    public static final int MIN_PARSED_LINES_FOR_UPDATE = 100;

    public static final int PARSE_ALERT_TIME_FREQUENCY_MS = 2000;

    /**
     * Handle the parsing of a single line from the input.
     * 
     * @param line
     */
    abstract T parseOneLine( String line );

}