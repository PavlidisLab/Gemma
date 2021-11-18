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
package ubic.gemma.core.loader.genome.gene;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.util.parser.RecordParser;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This does a very minimal parse of Swissprot records, just to get mRNAs associated with a single protein.
 *
 * @author pavlidis
 */
public class SwissProtParser extends RecordParser<Object> {

    private final Set<Object> results = new HashSet<>();

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public SwissProtParser() {
        this.setRecordSeparator( "//" );
    }

    @Override
    public Collection<Object> getResults() {
        return results;
    }

    @Override
    public Object parseOneRecord( String record ) {
        if ( StringUtils.isBlank( record ) )
            return null;

        String[] rows = StringUtils.split( record, "\n" );

        for ( String row : rows ) {
            String[] fields = StringUtils.split( row, null, 2 );
            if ( fields.length != 2 ) {
                continue;
            }

            String tag = fields[0];
            String value = fields[1];

            switch ( tag ) {
                case "ID":
                    String id = StringUtils.split( value, null, 2 )[0];
                    RecordParser.log.debug( id );
                    break;
                case "AC":
                    // swissprot accessions
                    String[] accessions = StringUtils.split( value, " ;" );
                    RecordParser.log.debug( StringUtils.join( accessions, " " ) );
                    break;
                case "OX": {
                    break;
                }
                case "GN": {
                    // gene
                    // Name=YWHAB;
                    String[] nf = StringUtils.split( value, "=", 2 );
                    String name = nf[1];
                    name = name.replaceFirst( ";.*$", "" );
                    RecordParser.log.debug( name );
                    break;
                }
                case "CC":
                    break;
                case "DE":
                    break;
                case "DR":
                    // these are our cross-references

                    String[] subfields = StringUtils.split( value, " ;" );

                    String db = subfields[0];

                    // go on to break it up futher.

                    if ( db.equals( "EMBL" ) ) {

                        String nucleotide = subfields[1].replaceFirst( "\\..+", "" );

                        String protein = subfields[2].replaceFirst( "\\..+", "" );
                        if ( protein.equals( "-" ) ) {
                            protein = null;
                        }

                        RecordParser.log.debug( nucleotide + " --> " + protein );

                    }

                    break;
                case "DT":

                    break;
                case "FT":

                    break;
                case "KW":

                    break;
                case "OC":

                    break;
                case "OG":

                    break;
                case "OS":

                    break;
                case "RA":

                    break;
                case "RC":

                    break;
                case "RG":

                    break;
                case "RL":

                    break;
                case "RN":

                    break;
                case "RP":

                    break;
                case "RT":

                    break;
                case "RX":

                    break;
                case "SQ":

                    break;
                default:
                    // sequence data.
                    break;
            }

        }

        return null;
    }

    @Override
    protected void addResult( Object obj ) {
        results.add( obj );
    }
}
