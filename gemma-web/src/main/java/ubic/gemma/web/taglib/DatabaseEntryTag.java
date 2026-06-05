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

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.tags.form.TagWriter;
import ubic.gemma.core.loader.util.ExternalDatabaseUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.web.assets.StaticAssetResolver;
import ubic.gemma.web.util.ExternalDatabaseWebUtils;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.Tag;

/**
 * @author keshav
 */
public class DatabaseEntryTag extends AbstractHtmlElementTag {

    private transient StaticAssetResolver staticAssetResolver;

    @Nullable
    private DatabaseEntryValueObject databaseEntry;

    @Override
    public int doStartTagInternal() throws JspException {
        if ( staticAssetResolver == null ) {
            staticAssetResolver = getRequestContext().getWebApplicationContext().getBean( StaticAssetResolver.class );
        }

        TagWriter tagWriter = new TagWriter( pageContext );

        if ( this.databaseEntry == null ) {
            tagWriter.startTag( "i" );
            tagWriter.appendValue( "No accession available" );
            tagWriter.endTag();
            return SKIP_BODY;
        }

        tagWriter.startTag( "span" );
        writeOptionalAttributes( tagWriter );

        boolean hasLabel = StringUtils.isNotBlank( databaseEntry.getLabel() );

        if ( hasLabel ) {
            tagWriter.appendValue( htmlEscape( databaseEntry.getLabel() ) );
            tagWriter.appendValue( " " );
        }

        if ( databaseEntry.getExternalDatabase() != null ) {
            String externalUri = ExternalDatabaseUtils.getUri( databaseEntry );
            String databaseLogo = ExternalDatabaseWebUtils.getLogo( databaseEntry.getExternalDatabase() );
            if ( externalUri != null && isHttpUrl( externalUri ) ) {
                if ( databaseLogo != null ) {
                    tagWriter.startTag( "a" );
                    tagWriter.writeAttribute( "href", externalUri );
                    tagWriter.writeAttribute( "target", "_blank" );
                    tagWriter.writeAttribute( "rel", "noopener noreferrer" );
                    writeDatabaseLogo( databaseEntry.getExternalDatabase().getName(), databaseLogo, tagWriter );
                    tagWriter.endTag(); // </a>
                } else {
                    if ( hasLabel ) {
                        tagWriter.appendValue( "(" );
                    }
                    tagWriter.startTag( "a" );
                    tagWriter.writeAttribute( "href", externalUri );
                    tagWriter.writeAttribute( "target", "_blank" );
                    tagWriter.writeAttribute( "rel", "noopener noreferrer" );
                    tagWriter.appendValue( htmlEscape( databaseEntry.getExternalDatabase().getName() ) );
                    tagWriter.appendValue( " " );
                    tagWriter.startTag( "i" );
                    tagWriter.writeAttribute( "class", "fa fa-external-link" );
                    tagWriter.endTag( true );
                    tagWriter.endTag(); // </a>
                    if ( hasLabel ) {
                        tagWriter.appendValue( ")" );
                    }
                }
            } else if ( databaseLogo != null ) {
                writeDatabaseLogo( databaseEntry.getExternalDatabase().getName(), databaseLogo, tagWriter );
            } else {
                if ( hasLabel ) {
                    tagWriter.appendValue( "(" );
                }
                tagWriter.appendValue( "(" + htmlEscape( databaseEntry.getExternalDatabase().getName() ) + ")" );
                if ( hasLabel ) {
                    tagWriter.appendValue( ")" );
                }
            }
        }

        tagWriter.endTag();

        return Tag.SKIP_BODY;
    }

    private boolean isHttpUrl( String uri ) {
        return uri.startsWith( "http://" ) || uri.startsWith( "https://" );
    }

    private void writeDatabaseLogo( String databaseName, String databaseLogo, TagWriter tagWriter ) throws JspException {
        tagWriter.startTag( "img" );
        tagWriter.writeAttribute( "src", staticAssetResolver.resolveUrl( databaseLogo ) );
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
