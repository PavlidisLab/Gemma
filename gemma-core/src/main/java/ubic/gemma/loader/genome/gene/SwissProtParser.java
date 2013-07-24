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
package ubic.gemma.loader.genome.gene;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.loader.util.parser.RecordParser;

/**
 * This does a very minimal parse of Swissprot records, just to get mRNAs associated with a single protein.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SwissProtParser extends RecordParser<Object> {

    Collection<Object> results = new HashSet<Object>();

    public SwissProtParser() {
        this.setRecordSeparator( "//" );
    }

    @Override
    protected void addResult( Object obj ) {
        results.add( obj );
    }

    @Override
    public Collection<Object> getResults() {
        return results;
    }

    @Override
    public Object parseOneRecord( String record ) {
        if ( StringUtils.isBlank( record ) ) return null;

        String[] rows = StringUtils.split( record, "\n" );

        for ( String row : rows ) {
            String[] fields = StringUtils.split( row, null, 2 );
            if ( fields.length != 2 ) {
                continue;
            }

            String tag = fields[0];
            String value = fields[1];

            if ( tag.equals( "ID" ) ) {
                String id = StringUtils.split( value, null, 2 )[0];
                log.debug( id );
            } else if ( tag.equals( "AC" ) ) {
                // swissprot accessions
                String[] accessions = StringUtils.split( value, " ;" );
                log.debug( StringUtils.join( accessions, " " ) );
            } else if ( tag.equals( "OX" ) ) {
                // taxon
                String[] nf = StringUtils.split( value, "=", 2 );
                String taxid = nf[1];
                taxid = taxid.replaceFirst( ";.*$", "" );
            } else if ( tag.equals( "GN" ) ) {
                // gene
                // Name=YWHAB;
                String[] nf = StringUtils.split( value, "=", 2 );
                String name = nf[1];
                name = name.replaceFirst( ";.*$", "" );
                log.debug( name );
            } else if ( tag.equals( "CC" ) ) {

            } else if ( tag.equals( "DE" ) ) {

            } else if ( tag.equals( "DR" ) ) {
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

                    log.debug( nucleotide + " --> " + protein );

                } else {
                    continue;
                }

            } else if ( tag.equals( "DT" ) ) {

            } else if ( tag.equals( "FT" ) ) {

            } else if ( tag.equals( "KW" ) ) {

            } else if ( tag.equals( "OC" ) ) {

            } else if ( tag.equals( "OG" ) ) {

            } else if ( tag.equals( "OS" ) ) {

            } else if ( tag.equals( "RA" ) ) {

            } else if ( tag.equals( "RC" ) ) {

            } else if ( tag.equals( "RG" ) ) {

            } else if ( tag.equals( "RL" ) ) {

            } else if ( tag.equals( "RN" ) ) {

            } else if ( tag.equals( "RP" ) ) {

            } else if ( tag.equals( "RT" ) ) {

            } else if ( tag.equals( "RX" ) ) {

            } else if ( tag.equals( "SQ" ) ) {

            } else {
                // sequence data.
                continue;
            }

        }

        return null;
    }
}
