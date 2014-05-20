/*
 * The gemma-web project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.web.util;

import net.jawr.web.resource.bundle.postprocess.BundleProcessingStatus;
import net.jawr.web.resource.bundle.postprocess.ResourceBundlePostProcessor;

/**
 * JAWR postprocessor to remove console.log and debugger breakpoint statements. Configured in jawr.properties
 * <p>
 * See {@link https://jawr.java.net/docs/postprocessors.html}
 * 
 * @author paul
 * @version $Id$
 */
public class MinifierPostProcessor implements ResourceBundlePostProcessor {

    private static final String CONSOLE_LOG_REGEX = "console\\.log\\([^)]*\\);";
    private static final String DEBUGGER = "debugger;";

    public MinifierPostProcessor() {
    }

    @Override
    public StringBuffer postProcessBundle( BundleProcessingStatus status, StringBuffer bundleString ) {
        StringBuffer ret = new StringBuffer();
        ret.append( clean( bundleString.toString() ) );
        return ret;
    }

    private String clean( String string ) {
        return string.replaceAll( CONSOLE_LOG_REGEX, "" ).replaceAll( DEBUGGER, "" );
    }
}
