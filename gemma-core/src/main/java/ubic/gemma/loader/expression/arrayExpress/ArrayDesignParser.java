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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Parses the flat files from ArrayExpress. Format is a little complicated. Some have reporters, some
 * compositeSequences. This is to get the details of the composite sequences, not the array design.
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

    private static Set<String> headingKeys;

    static {
        String[] s = new String[] { "Array Design Name", "Version", "Provider", "Comment", "Printing Protocol",
                "Technology Type", "Surface Type", "Substrate Type", "Term Source File", "Term Source Version",
                "Term Source Name", "Surface Type Term Accession Number", "Surface Type Term Source REF",
                "Technology Type Term Source REF" };
        headingKeys = new HashSet<String>();
        CollectionUtils.addAll( headingKeys, s );
    }

    Integer reporterNameField = null;
    Integer csNameField = null;
    Integer csIdentifierField = null;
    Integer reporterIdentifierField = null;
    Integer sequenceField = null;
    Integer reporterDescriptionField = null;
    Integer csDescriptionField = null;
    boolean readHeader = false;
    boolean useReporterId = false;
    private String taxonName = null;

    /**
     * In case we found one in the file (not always)
     * 
     * @return
     */
    public String getTaxonName() {
        return taxonName;
    }

    /**
     * @param useReporterId the useReporterId to set
     */
    public void setUseReporterId( boolean useReporterId ) {
        this.useReporterId = useReporterId;
    }

    @Override
    public CompositeSequence parseOneLine( String line ) {

        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
        if ( fields.length < 2 ) return null;

        if ( csNameField == null ) {

            if ( readHeader ) {
                throw new IllegalStateException( "Failed to parse the header, probe name field not found" );
            }

            parseHeader( fields );
            return null;
        }

        CompositeSequence cs = CompositeSequence.Factory.newInstance();

        String csName = null;
        String reporterName = null;
        String csDescription = null;
        String reporterDescription = null;
        // String sequence = null;
        String reporterIdentifier = null;
        String csIdentifier = null;

        if ( csIdentifierField != null ) {
            csIdentifier = getUnqualifiedIdentifier( fields[csIdentifierField] );
        }

        if ( csDescriptionField != null ) {
            csDescription = fields[csDescriptionField];
        }

        if ( reporterDescriptionField != null ) {
            reporterDescription = fields[reporterDescriptionField];
        }

        if ( reporterIdentifierField != null ) {
            reporterIdentifier = getUnqualifiedIdentifier( fields[reporterIdentifierField] );
        }

        if ( csNameField != null ) {
            csName = getUnqualifiedIdentifier( fields[csNameField] );
        } else if ( csIdentifier != null ) {
            csName = csIdentifier;
        }

        if ( reporterNameField != null ) {
            reporterName = getUnqualifiedIdentifier( fields[reporterNameField] );
        } else if ( reporterIdentifier != null ) {
            reporterName = reporterIdentifier;
        }

        String probeName = null;
        String probeDescription = null;

        if ( this.useReporterId ) {
            probeName = reporterName;
        } else {
            probeName = csName;
        }

        if ( probeName == null ) {
            throw new IllegalStateException( "Null CS identifier for line: " + line );
        }

        if ( probeName.equals( "-" ) || StringUtils.isBlank( probeName ) ) {
            return null;
        }

        // log.info( probeName );

        if ( csDescription != null ) {
            probeDescription = csDescription.equals( "-" ) ? "" : csDescription;
        } else if ( reporterDescription != null ) {
            probeDescription = reporterDescription.equals( "-" ) ? "" : reporterDescription;
        } else {
            probeDescription = "";
        }

        cs.setName( probeName );
        cs.setDescription( probeDescription );
        return cs;
    }

    private String getUnqualifiedIdentifier( String identifier ) {
        return identifier.substring( identifier.lastIndexOf( ':' ) + 1, identifier.length() );
    }

    @Override
    public void parse( InputStream is ) throws IOException {

        /*
         * Have to parse the header. Old format lacked this.
         */
        br = new BufferedReader( new InputStreamReader( is ) );
        String line = null;
        while ( ( line = br.readLine() ) != null ) {

            if ( StringUtils.isBlank( line ) ) {
                break;
            }

            String lline = line.toLowerCase();

            if ( lline.contains( "compositesequence identifier" ) ) {
                parseHeader( StringUtils.splitPreserveAllTokens( line, '\t' ) );
                break;
            }

            boolean isHeader = false;
            for ( String key : headingKeys ) {
                if ( line.startsWith( key ) ) {
                    isHeader = true;

                    if ( line.startsWith( "Comment[Organism]" ) ) {
                        this.taxonName = line.replaceFirst( "Comment\\[Organism\\]\\s", "" );
                    }
                }
            }

            if ( !isHeader ) {
                log.info( "Not header: " + line );
                break;
            }

        }

        super.parse( is );
    }

    private void parseHeader( String[] fields ) {
        for ( int i = 0; i < fields.length; i++ ) {

            String field = fields[i];
            // log.info( field );
            if ( field.equalsIgnoreCase( "CompositeSequence Name" ) ) {
                csNameField = i;
            } else if ( field.equalsIgnoreCase( "Composite Element Name" ) ) {
                csNameField = i;
            } else if ( field.equalsIgnoreCase( "Reporter Identifier" ) ) {
                reporterIdentifierField = i;
            } else if ( field.startsWith( "Reporter Database Entry" ) ) {
                // don't use
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
            } else if ( field.equalsIgnoreCase( "Control Type" ) ) {
            } else if ( field.startsWith( "Reporter Group" ) ) {
            } else if ( field.startsWith( "Comment" ) ) {
            } else if ( field.equalsIgnoreCase( "Block Column" ) ) {
            } else if ( field.equalsIgnoreCase( "Block Row" ) ) {
            } else if ( field.equalsIgnoreCase( "Row" ) ) {
            } else if ( field.equalsIgnoreCase( "Column" ) ) {
            }

        }

        if ( this.csNameField == null && this.reporterNameField != null ) {
            this.csNameField = this.reporterNameField;
        }

        if ( this.csIdentifierField == null && this.reporterIdentifierField != null ) {
            this.csIdentifierField = this.reporterIdentifierField;
        }

        readHeader = true;

    }

}
