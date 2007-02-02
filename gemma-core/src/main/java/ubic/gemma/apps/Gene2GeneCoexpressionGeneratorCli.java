/*
 * The Gemma project.
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

package ubic.gemma.apps;

import java.io.IOException;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Given an array design creates a Gene Ontology Annotation file
 * <p>
 * 
 * @author klc
 */

public class Gene2GeneCoexpressionGeneratorCli extends AbstractSpringAwareCLI {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        Option annotationFileOption = OptionBuilder.hasArg().withArgName( "Annotation file name" ).withDescription(
                "Optional: The name of the Annotation file to be generated" ).withLongOpt( "annotation" ).create( 'f' );

        addOption( annotationFileOption );

    }

    public static void main( String[] args ) {
        ArrayDesignGOAnnotationGeneratorCli p = new ArrayDesignGOAnnotationGeneratorCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Gene 2 Gene Coexpression Caching tool ", args );
        if ( err != null ) return err;

        return null;
    }

    /**
     * @throws IOException process the current AD
     */

    protected void processOptions() {
        super.processOptions();

        if ( this.hasOption( 'f' ) ) {
            // this.fileName = this.getOptionValue( 'f' );
        }
    }

}