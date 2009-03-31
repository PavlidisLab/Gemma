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
package ubic.gemma.loader.expression.arrayExpress;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Parses the flat files from ArrayExpress. Format is a little complicated. Some have reporters, some
 * compositeSequences.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignParser extends BasicLineParser<CompositeSequence> {

    Collection<CompositeSequence> results = new HashSet<CompositeSequence>();

    @Override
    protected void addResult( CompositeSequence obj ) {
        results.add( obj );
    }

    @Override
    public Collection<CompositeSequence> getResults() {
        return results;
    }

    boolean isSpotted = false;
    Integer reporterNameField = null;
    Integer csNameField = null;
    Integer csIdentifierField = null;
    Integer reporterIdentifierField = null;
    Integer sequenceField = null;
    Integer reporterDescriptionField = null;
    Integer csDescriptionField = null;

    boolean useReporterId = false;

    /**
     * @param useReporterId the useReporterId to set
     */
    public void setUseReporterId( boolean useReporterId ) {
        this.useReporterId = useReporterId;
    }

    @SuppressWarnings("null")
    public CompositeSequence parseOneLine( String line ) {

        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
        if ( fields.length < 2 ) return null;

        if ( csNameField == null ) {
            parseHeader( fields );
            return null;
        }

        CompositeSequence cs = CompositeSequence.Factory.newInstance();

        String csName = null;
        // String reporterName = null;
        String csDescription = null;
        String reporterDescription = null;
        // String sequence = null;
        String reporterIdentifier = null;
        String csIdentifier = null;

        if ( csIdentifierField != null ) {
            csIdentifier = fields[csIdentifierField];
        }

        if ( csNameField != null ) {
            csName = fields[csNameField];
        } else if ( csIdentifier != null ) {
            csName = csIdentifier;
        }

        // if ( reporterNameField != null ) {
        // reporterName = fields[reporterNameField];
        // }

        if ( csDescriptionField != null ) {
            csDescription = fields[csDescriptionField];
        }

        if ( reporterDescriptionField != null ) {
            reporterDescription = fields[reporterDescriptionField];
        }

        if ( csIdentifierField != null ) {
            csIdentifier = getUnqualifiedIdentifier( fields[csIdentifierField] );
        }

        if ( reporterIdentifierField != null ) {
            reporterIdentifier = getUnqualifiedIdentifier( fields[reporterIdentifierField] );
        }

        // if ( sequenceField != null ) {
        // sequence = fields[sequenceField];
        // }

        String probeName = null;
        String probeDescription = null;

        if ( this.useReporterId || csName.equals( "-" ) ) {
            probeName = reporterIdentifier;
        } else {
            probeName = csIdentifier;
        }

        if ( csDescription != null ) {
            probeDescription = csDescription.equals( "-" ) ? "" : csDescription;
        } else if ( reporterDescription != null ) {
            probeDescription = reporterDescription.equals( "-" ) ? "" : reporterDescription;
        }

        cs.setName( probeName );
        cs.setDescription( probeDescription );
        return cs;
    }

    private String getUnqualifiedIdentifier( String identifier ) {
        return identifier.substring( identifier.lastIndexOf( ':' ) + 1, identifier.length() );
    }

    private void parseHeader( String[] fields ) {
        for ( int i = 0; i < fields.length; i++ ) {

            String field = fields[i];

            if ( field.equalsIgnoreCase( "CompositeSequence Name" ) ) {
                csNameField = i;
            } else if ( field.equalsIgnoreCase( "Reporter Identifier" ) ) {
                reporterIdentifierField = i;
            } else if ( field.equalsIgnoreCase( "CompositeSequence Identifier" ) ) {
                csIdentifierField = i;
            } else if ( field.equalsIgnoreCase( "Reporter Name" ) ) {
                reporterNameField = i;
            } else if ( field.equalsIgnoreCase( "Reporter actual Sequence" ) ) {
                sequenceField = i;
            } else if ( field.equalsIgnoreCase( "Reporter Comment" ) ) {
                reporterDescriptionField = i;
            } else if ( field.equalsIgnoreCase( "CompositeSequence Comment" ) ) {
                csDescriptionField = i;
            } else if ( field.equalsIgnoreCase( "MetaColumn" ) ) {
                isSpotted = true;
            } else if ( field.equalsIgnoreCase( "" ) ) {
            }

        }

    }

}
