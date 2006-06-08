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
package ubic.gemma.jni.cluster;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * @author keshav
 * @version $Id$
 */
public class NCluster {

    public native int[][] computeCompleteLinkage( int elements, double matrix[][] );

    static {
        Configuration config = null;
        try {
            config = new PropertiesConfiguration( "Gemma.properties" );
        } catch ( ConfigurationException e ) {
            System.err.println( "Could not read properites file " + config );
            e.printStackTrace();
        }

        String baseDir = ( String ) config.getProperty( "gemma.baseDir" );
        String localBasePath = ( String ) config.getProperty( "cluster.dll.path" );
        System.load( baseDir + localBasePath );
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {// TODO move me to the test suite.
        NCluster cluster = new NCluster();
        double[][] values = new double[5][2];

        double[] t0 = { 1, 0 };
        double[] t1 = { 4, 0 };
        double[] t2 = { 5, 0 };
        double[] t3 = { 9, 0 };
        double[] t4 = { 10, 0 };

        values[0] = t0;
        values[1] = t1;
        values[2] = t2;
        values[3] = t3;
        values[4] = t4;

        cluster.computeCompleteLinkage( 5, values );
    }

}
