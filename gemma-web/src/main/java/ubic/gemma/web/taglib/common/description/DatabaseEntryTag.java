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
package ubic.gemma.web.taglib.common.description;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.DatabaseEntryValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;

/**
 * @author keshav
 * @version $Id$
 */
public class DatabaseEntryTag extends TagSupport {

    /**
     * 
     */
    private static final long serialVersionUID = -8225561718129593445L;

    private static Log log = LogFactory.getLog( DatabaseEntryTag.class );

    private DatabaseEntryValueObject databaseEntry;

    /**
     * @param databaseEntry
     */
    public void setDatabaseEntry( DatabaseEntry databaseEntry ) {
        this.databaseEntry = new DatabaseEntryValueObject( databaseEntry );
    }

    public void setDatabaseEntryValueObject( DatabaseEntryValueObject databaseEntry ) {
        this.databaseEntry = databaseEntry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {

        log.debug( "end tag" );

        return EVAL_PAGE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        log.debug( "start tag" );

        StringBuilder buf = new StringBuilder();
        if ( this.databaseEntry == null ) {
            buf.append( "No accession" );
        } else {
            String accession = databaseEntry.getAccession();

            if ( databaseEntry.getExternalDatabase() != null ) {

                if ( databaseEntry.getExternalDatabase().getName().equalsIgnoreCase( "GEO" ) ) {

                    accession = accession.replaceAll( "\\.[1-9]$", "" );
                    buf.append( accession + "&nbsp;<a title='NCBI page for this entry'"
                            + " target='_blank' href='http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" + accession
                            + "'><img src='/Gemma/images/logo/geoTiny.png' /></a>" );
                } else if ( databaseEntry.getExternalDatabase().getName().equalsIgnoreCase( "ArrayExpress" ) ) {
                    buf.append( accession
                            + "&nbsp;<a title='ArrayExpress page for this entry'"
                            + " target='_blank' href='http://www.ebi.ac.uk/microarray-as/aer/result?queryFor=Experiment&eAccession="
                            + accession + "'><img src='/Gemma/images/logo/arrayExpressTiny.png' /></a>" );
                } else {
                    buf.append( accession + "(" + databaseEntry.getExternalDatabase().getName() + ":" + ")" );
                }
            } else {
                buf.append( accession );
            }
        }

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( this.getClass().getName() + ex.getMessage() );
        }
        return SKIP_BODY;
    }

}
