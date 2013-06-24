/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.taglib;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Imports text from a resource. The path for the resource is assumed to be "/ubic/gemma/doc/[helpFile]".
 * <p>
 * Example of usage with a helptip:
 * <p>
 * <code>
 * &lt;script>
 var text = '&lt;Gemma:help helpFile="sequenceAnalysisHelp.html"/&gt;';
 function doit(event) {showWideHelpTip(event,text); }
 &lt;/script&gt;
 <br>
 ...
 <br>
 * &lt;a class="helpLink" href="?"
 onclick="showHelpTip(event, text); return false"&gt;
 &lt;img src="/Gemma/images/help.png" /&gt;&lt;/a&gt;
 * </code>
 * <p>
 * Note that the string produced will be broken up with single quotes, so you should wrap your string in single quotes
 * as shown in the example. Doing all of this in the 'onSubmit' method doesn't work in many cases (the javascript parser
 * barfs), so it's better to pull the text in a 'script' block.
 * 
 * @jsp.tag name="help" body-content="empty"
 * @author pavlidis
 * @version $Id$
 */
public class HelpTag extends TagSupport {

    private static final long serialVersionUID = -7035535312981283722L;

    private static Log log = LogFactory.getLog( HelpTag.class.getName() );
    String helpFile;

    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

    @Override
    public int doStartTag() throws JspException {

        StringBuilder buf = new StringBuilder();

        try {
            InputStream io = this.getClass().getResourceAsStream( "/ubic/gemma/doc/" + helpFile );
            if ( io == null ) {
                buf.append( "[Help is not available for this item, possibly due to a configuration error]" );
            }

            BufferedReader br = new BufferedReader( new InputStreamReader( new BufferedInputStream( io ) ) );
            String line = "";

            // we assume the entire thing is embedded in a pair of single quotes.
            while ( ( line = br.readLine() ) != null ) {
                line = line.replaceAll( "\"", "&quot;" );
                line = line.replaceAll( "'", "&#039;" );
                line = line.replaceAll( "\t+", "&nbsp;'+'" );
                // line = line.replaceAll( "\\(", "[" );
                // line = line.replaceAll( "\\)", "]" );
                buf.append( line );
                buf.append( "'+'" );
            }
            br.close();
        } catch ( IOException e ) {
            buf.append( "[Help is not available for this item, possibly due to a configuration error]" );
            log.error( e, e );
        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "helpTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }

    /**
     * @param helpFile
     * @jsp.attribute required="true" rtexprvalue="true"
     */
    public void setHelpFile( String helpFile ) {
        this.helpFile = helpFile;
    }

}
