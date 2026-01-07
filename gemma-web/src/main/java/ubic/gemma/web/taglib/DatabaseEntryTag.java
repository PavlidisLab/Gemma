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
package ubic.gemma.web.taglib;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.common.description.ExternalDatabases;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * @author keshav
 */
@CommonsLog
@SuppressWarnings("unused") // Frontend use
public class DatabaseEntryTag extends TagSupport {

    @Nullable
    private DatabaseEntryValueObject databaseEntry;

    @Override
    public int doStartTag() throws JspException {

        DatabaseEntryTag.log.debug( "start tag" );

        String contextPath = pageContext.getServletContext().getContextPath();

        StringBuilder buf = new StringBuilder();
        if ( this.databaseEntry == null ) {
            buf.append( "<i>No accession available</i>" );
        } else {
            String accession = databaseEntry.getAccession();

            if ( databaseEntry.getExternalDatabase() != null ) {

                if ( databaseEntry.getExternalDatabase().getName().equalsIgnoreCase( ExternalDatabases.GEO ) ) {

                    accession = accession.replaceAll( "\\.[1-9]$", "" );
                    buf.append( accession ).append( "&nbsp;<a title='NCBI page for this entry'" )
                            .append( " target='_blank' href='https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" )
                            .append( accession ).append( "'><img src='" ).append( contextPath )
                            .append( "/images/logo/geoTiny.png' /></a>" );
                } else if ( databaseEntry.getExternalDatabase().getName().equalsIgnoreCase( "ArrayExpress" ) ) {
                    buf.append( accession ).append( "&nbsp;<a title='ArrayExpress page for this entry'" ).append(
                                    " target='_blank' href='https://www.ebi.ac.uk/microarray-as/aer/result?queryFor=Experiment&eAccession=" )
                            .append( accession ).append( "'><img src='" ).append( contextPath )
                            .append( "/images/logo/arrayExpressTiny.png' /></a>" );
                } else {
                    buf.append( accession ).append( "(" ).append( databaseEntry.getExternalDatabase().getName() )
                            .append( ":" ).append( ")" );
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
        return Tag.SKIP_BODY;
    }

    @Override
    public int doEndTag() {

        DatabaseEntryTag.log.debug( "end tag" );

        return Tag.EVAL_PAGE;
    }

    public void setDatabaseEntry( @Nullable DatabaseEntry databaseEntry ) {
        if ( databaseEntry == null ) {
            // if it is a user-owned data set.
            this.databaseEntry = null;
            return;
        }
        this.databaseEntry = new DatabaseEntryValueObject( databaseEntry );
    }

    public void setDatabaseEntryValueObject( DatabaseEntryValueObject databaseEntry ) {
        this.databaseEntry = databaseEntry;
    }

}
