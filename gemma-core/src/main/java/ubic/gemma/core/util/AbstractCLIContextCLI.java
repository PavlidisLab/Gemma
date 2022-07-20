/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.core.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Spring configuration for CLI.
 *
 * @author anton date: 18/02/13
 */
public abstract class AbstractCLIContextCLI extends AbstractSpringAwareCLI {

    /**
     * may be tab-delimited, only first column used, commented (#) lines are ignored.
     *
     * @param fileName the file name
     * @return list of ee identifiers
     * @throws IOException in case there is an IO error while reading the file
     */
    protected static List<String> readListFileToStrings( String fileName ) throws IOException {
        List<String> eeNames = new ArrayList<>();
        try ( BufferedReader in = new BufferedReader( new FileReader( fileName ) ) ) {
            while ( in.ready() ) {
                String line = in.readLine().trim();
                if ( line.startsWith( "#" ) ) {
                    continue;
                }
                if ( line.isEmpty() )
                    continue;
                String[] split = StringUtils.split( line, "\t" );
                eeNames.add( split[0] );
            }
            return eeNames;
        }
    }

    protected Taxon setTaxonByName( CommandLine commandLine, TaxonService taxonService ) {
        String taxonName = commandLine.getOptionValue( 't' );
        ubic.gemma.model.genome.Taxon taxon = taxonService.findByCommonName( taxonName );
        if ( taxon == null ) {
            AbstractCLI.log.error( "ERROR: Cannot find taxon " + taxonName );
        }
        return taxon;
    }

    /**
     * @param  name               of the array design to find.
     * @param  arrayDesignService the arrayDesignService to use for the AD retrieval
     * @return                    an array design, if found. Bails otherwise with an error exit code
     */
    protected ArrayDesign locateArrayDesign( String name, ArrayDesignService arrayDesignService ) {

        ArrayDesign arrayDesign = null;

        Collection<ArrayDesign> byname = arrayDesignService.findByName( name.trim().toUpperCase() );
        if ( byname.size() > 1 ) {
            throw new IllegalArgumentException( "Ambiguous name: " + name );
        } else if ( byname.size() == 1 ) {
            arrayDesign = byname.iterator().next();
        }

        if ( arrayDesign == null ) {
            arrayDesign = arrayDesignService.findByShortName( name );
        }

        if ( arrayDesign == null ) {
            AbstractCLI.log.error( "No arrayDesign " + name + " found" );
        }
        return arrayDesign;
    }

    @Override
    protected String[] getAdditionalSpringConfigLocations() {
        return new String[]{"classpath*:ubic/gemma/cliContext-component-scan.xml"};
    }

}
