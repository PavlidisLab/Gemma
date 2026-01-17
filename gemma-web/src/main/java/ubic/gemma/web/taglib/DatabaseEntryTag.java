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

import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.core.loader.util.ExternalDatabaseUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.common.description.ExternalDatabases;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;
import java.net.URL;

/**
 * @author keshav
 */
public class DatabaseEntryTag extends AbstractHtmlElementTag {

    @Nullable
    private DatabaseEntryValueObject databaseEntry;

    @Override
    public int doStartTagInternal() throws JspException {
        TagWriter tagWriter = new TagWriter( pageContext );

        if ( this.databaseEntry == null ) {
            tagWriter.startTag( "i" );
            tagWriter.appendValue( "No accession available" );
            tagWriter.endTag();
            return SKIP_BODY;
        }

        String contextPath = pageContext.getServletContext().getContextPath();

        tagWriter.startTag( "span" );
        writeOptionalAttributes( tagWriter );

        tagWriter.appendValue( htmlEscape( databaseEntry.getAccession() ) );

        if ( databaseEntry.getExternalDatabase() != null ) {
            URL externalUrl = ExternalDatabaseUtils.getUrl( databaseEntry );
            String databaseLogo;
            if ( ExternalDatabases.GEO.equalsIgnoreCase( databaseEntry.getExternalDatabase().getName() ) ) {
                databaseLogo = contextPath + "/images/logo/geo-logo.png";
            } else if ( ExternalDatabases.ARRAY_EXPRESS.equalsIgnoreCase( databaseEntry.getExternalDatabase().getName() ) ) {
                databaseLogo = "/images/logo/arrayexpress-logo.png";
            } else if ( ExternalDatabases.BIO_STUDIES.equalsIgnoreCase( databaseEntry.getExternalDatabase().getName() ) ) {
                databaseLogo = "/images/logo/biostudies-logo.png";
            } else if ( ExternalDatabases.SRA.equalsIgnoreCase( databaseEntry.getExternalDatabase().getName() ) ) {
                databaseLogo = "/images/logo/sra-logo.png";
            } else if ( ExternalDatabases.CELLXGENE.equalsIgnoreCase( databaseEntry.getExternalDatabase().getName() ) ) {
                databaseLogo = "/images/logo/cellxgene-logo.png";
            } else if ( ExternalDatabases.PUBMED.equalsIgnoreCase( databaseEntry.getExternalDatabase().getName() ) ) {
                databaseLogo = "/images/logo/pubmed-logo-blue.png";
            } else if ( ExternalDatabases.ARXIV.equalsIgnoreCase( databaseEntry.getExternalDatabase().getName() ) ) {
                databaseLogo = "/images/logo/arxiv-logo.png";
            } else if ( ExternalDatabases.BIORXIV.equalsIgnoreCase( databaseEntry.getExternalDatabase().getName() ) ) {
                databaseLogo = "/images/logo/biorxiv-logo.png";
            } else {
                databaseLogo = null;
            }
            tagWriter.appendValue( " " );
            if ( externalUrl != null ) {
                if ( databaseLogo != null ) {
                    tagWriter.startTag( "a" );
                    tagWriter.writeAttribute( "href", externalUrl.toString() );
                    tagWriter.writeAttribute( "target", "_blank" );
                    tagWriter.writeAttribute( "rel", "noopener noreferrer" );
                    writeDatabaseLogo( databaseEntry.getExternalDatabase().getName(), databaseLogo, tagWriter );
                    tagWriter.endTag(); // </a>
                } else {
                    tagWriter.appendValue( "(" );
                    tagWriter.startTag( "a" );
                    tagWriter.writeAttribute( "href", externalUrl.toString() );
                    tagWriter.writeAttribute( "target", "_blank" );
                    tagWriter.writeAttribute( "rel", "noopener noreferrer" );
                    tagWriter.appendValue( htmlEscape( databaseEntry.getExternalDatabase().getName() ) );
                    tagWriter.appendValue( " " );
                    tagWriter.startTag( "i" );
                    tagWriter.writeAttribute( "class", "fa fa-external-link" );
                    tagWriter.endTag( true );
                    tagWriter.endTag(); // </a>
                    tagWriter.appendValue( ")" );
                }
            } else if ( databaseLogo != null ) {
                writeDatabaseLogo( databaseEntry.getExternalDatabase().getName(), databaseLogo, tagWriter );
            } else {
                tagWriter.appendValue( "(" + htmlEscape( databaseEntry.getExternalDatabase().getName() ) + ")" );
            }
        }

        tagWriter.endTag();

        return Tag.SKIP_BODY;
    }

    private void writeDatabaseLogo( String databaseName, String databaseLogo, TagWriter tagWriter ) throws JspException {
        tagWriter.startTag( "img" );
        tagWriter.writeAttribute( "src", databaseLogo );
        tagWriter.writeAttribute( "height", "16" );
        tagWriter.writeAttribute( "alt", htmlEscape( databaseName ) + " logo" );
        tagWriter.endTag(); // </img>
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
