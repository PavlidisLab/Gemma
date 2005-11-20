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
package edu.columbia.gemma.web.controller.common.auditAndSecurity;

import java.io.Serializable;

/**
 * Command class to handle uploading of a file. (From Appfuse)
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author pavlidis
 * @version $Id$
 */
public class FileUpload implements Serializable {
    private String name;
    private byte[] file;

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public byte[] getFile() {
        return file;
    }

    /**
     * @param name The name to set.
     */
    public void setFile( byte[] file ) {
        /*
         * NOTE: We did not add the xdoclet spring validation (commons validation for spring = spring-modules) tags to
         * this because xdoclet cannot interpret the type byte[].
         */
        this.file = file;
    }

    /**
     * @param name The name to set.
     * @spring.validator type="required"
     * @spring.validator-args arg0resource="uploadForm.name"
     */
    public void setName( String name ) {
        this.name = name;
    }

}
