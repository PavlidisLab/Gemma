/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.arrayExpress;

import java.io.IOException;

import org.apache.tools.ant.taskdefs.Untar;
import org.apache.tools.ant.taskdefs.Untar.UntarCompressionMethod;

import edu.columbia.gemma.loader.expression.mage.MageMLConverter;

/**
 * ArrayExpress stores files in an FTP site as tarred-gzipped archives. Each tar file contains the MAGE file and the
 * datacube external files. This class can download an experiment, unpack the tar file, and put the resulting files onto
 * a local filesystem. It then parses and loads up the meta-data.
 * <p>
 * TODO: deal with the raw data.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class DataFileFetcher {

    Untar untarrer;
    MageMLConverter converter;

    public DataFileFetcher() {
        untarrer = new Untar();
        UntarCompressionMethod method = new UntarCompressionMethod();
        method.setValue( "gzip" );
        untarrer.setCompression( method );
        converter = new MageMLConverter();
    }

    /*
     * Have to get the biodatacube, including the dimensions.
     */

}
