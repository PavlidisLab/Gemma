/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import ubic.gemma.util.AbstractCLI;

/**
 * Generic command line information for Gemma. This doesn't do anything but print some help.
 * 
 * @author paul
 * @version $Id$
 */
public class GemmaCLI {

    private static final String[] apps = new String[] { "ubic.gemma.apps.ArrayDesignProbeMapperCli",
            "ubic.gemma.apps.ArrayDesignSequenceAssociationCli", "ubic.gemma.apps.ArrayDesignRepeatScanCli",
            "ubic.gemma.apps.LoadExpressionDataCli", "ubic.gemma.apps.LoadSimpleExpressionDataCli",
            "ubic.gemma.apps.ArrayDesignBlatCli", "ubic.gemma.apps.ArrayDesignAnnotationFileCli",
            "ubic.gemma.apps.ProcessedDataComputeCLI", "ubic.gemma.apps.TwoChannelMissingValueCLI",
            "ubic.gemma.apps.DeleteExperimentCli", "ubic.gemma.apps.VectorMergingCli",
            "ubic.gemma.apps.ArrayDesignMergeCli", "ubic.gemma.apps.LinkAnalysisCli",
            "ubic.gemma.apps.DifferentialExpressionAnalysisCli",
            "ubic.gemma.apps.ExpressionExperimentPlatformSwitchCli", "ubic.gemma.apps.ExpressionDataCorrMatCli",
            "ubic.gemma.apps.ArrayDesignSubsumptionTesterCli", "ubic.gemma.apps.RNASeqDataAddCli",
            "ubic.gemma.apps.AffyDataFromCelCli" };

    /**
     * @param args
     */
    public static void main( String[] args ) {
        System.err.println( "============ Gemma command line tools ============" );

        System.err
                .print( "You've evoked the Gemma CLI in a mode that doesn't do anything.\n"
                        + "To operate Gemma tools, run a command like:\n\njava [jre options] -classpath ${GEMMA_LIB} <classname> [options]\n\n"
                        + "You can use gemmaCli.sh as a shortcut\n\n"
                        + "Here is a list of the classnames for some available tools:\n\n" );
        Arrays.sort( apps );
        for ( String a : apps ) {
            String desc = "";
            try {
                Class<?> aclazz = Class.forName( a );
                Object cliinstance = aclazz.newInstance();
                Method method = aclazz.getMethod( "getShortDesc", new Class[] {} );
                desc = ( String ) method.invoke( cliinstance, new Object[] {} );
            } catch ( ClassNotFoundException e ) {
                e.printStackTrace();
            } catch ( IllegalArgumentException e ) {
                e.printStackTrace();
            } catch ( IllegalAccessException e ) {
                e.printStackTrace();
            } catch ( InvocationTargetException e ) {
                e.printStackTrace();
            } catch ( SecurityException e ) {
                e.printStackTrace();
            } catch ( NoSuchMethodException e ) {
                e.printStackTrace();
            } catch ( InstantiationException e ) {
                e.printStackTrace();
            }

            System.err.println( a + " : " + desc );
        }
        System.err.println( "\nTo get help for a specific tool, use \n\ngemmaCli.sh <classname> --help" );
        System.err.print( "\n" + AbstractCLI.FOOTER + "\n=========================================\n" );
    }
}
