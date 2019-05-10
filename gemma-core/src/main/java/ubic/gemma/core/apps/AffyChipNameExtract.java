/*
 * The gemma-core project
 * 
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.core.apps;

import ubic.basecode.util.FileTools;
import ubic.gemma.core.loader.expression.arrayDesign.AffyChipTypeExtractor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Purely a testing tool to parse CEL files to extract the 'chip name', so we can gather them.
 * 
 * Should probably be in GemmaAnalysis but that is badly broken.
 * 
 * You just run this like
 * 
 * $GEMMACMD chipnameExtract [filenames]
 * 
 * It doesn't handle the regular argument setup, wasn't worth the trouble.
 * 
 * @author paul
 */
public class AffyChipNameExtract extends ArrayDesignSequenceManipulatingCli {
    public static void main( String[] args ) {
        AffyChipNameExtract d = new AffyChipNameExtract();
        executeCommand( d, args );
    }

    @Override
    public String getCommandName() {
        return "chipnameExtract";
    }

    @Override
    protected Exception doWork( String[] args ) {

        for ( String f : args ) {
            try (InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f )) {

                String type = AffyChipTypeExtractor.extract( is );
                System.err.println( type + "\t" + f );
            } catch ( IOException e ) {
                System.err.println( "Failed to find chip type: " + "\t" + f + " " + e.getMessage() );
            }

        }

        return null;
    }

}
