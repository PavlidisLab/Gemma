/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.loader.expression.geo.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 */
public class GeoLoaderCli extends AbstractSpringAwareCLI {

    private Collection<String> ges;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        this.addOption( OptionBuilder.hasArg().withArgName( "series id" ).withDescription(
                "GSE id to load e.g. GSE1001" ).create( 's' ) );
        this.addOption( OptionBuilder.hasArg().withArgName( "file name" ).withDescription( "File with list of GSE ids" )
                .create( "f" ) );

        super.requireLogin();

    }

    /**
     * @param fileName
     * @return
     * @throws IOException
     */
    private Collection<String> readExpressionExperimentListFileToStrings( String fileName ) throws IOException {
        Collection<String> eeNames = new HashSet<String>();
        BufferedReader in = new BufferedReader( new FileReader( fileName ) );
        while ( in.ready() ) {
            String line = in.readLine().trim();
            String[] toks = StringUtils.splitPreserveAllTokens( line, "\t" );
            if ( toks.length == 0 ) continue;
            String eeName = toks[0];
            if ( eeName.startsWith( "#" ) ) {
                continue;
            }
            eeNames.add( eeName );
        }
        return eeNames;
    }

    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( "geo loader", args );

        GeoDatasetService loader = ( GeoDatasetService ) this.getBean( "geoDatasetService" );

        for ( String gse : ges ) {
            log.info( "***** Loading: " + gse + " ******" );
            try {
                Collection<?> results = loader.fetchAndLoad( gse, false, false, true, true, false, false );
                for ( Object object : results ) {
                    successObjects.add( ges + ": " + object );
                }
            } catch ( Exception e ) {
                log.error( e, e );
                this.errorObjects.add( ges );
            }
        }

        super.summarizeProcessing();

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( this.hasOption( 'f' ) ) {
            try {
                this.ges = readExpressionExperimentListFileToStrings( this.getOptionValue( "f" ) );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } else if ( this.hasOption( 's' ) ) {
            this.ges = new HashSet<String>();
            this.ges.add( this.getOptionValue( 's' ) );
        } else {
            throw new IllegalArgumentException( "You must specify data sets" );
        }

    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        GeoLoaderCli l = new GeoLoaderCli();
        l.doWork( args );
    }

}
