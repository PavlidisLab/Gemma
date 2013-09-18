/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.web.remote;

import ubic.gemma.util.EntityUtils;

/**
 * Bean to expose for remote access via AJAX, when all that is needed is the ID and a way to know what the class is.
 * 
 * @author Paul
 * @version $Id$
 */
public class EntityDelegator {

    private Long id;

    private String classDelegatingFor;

    public EntityDelegator() {
    }

    public EntityDelegator( Object entity ) {
        this.id = EntityUtils.getId( entity );
        this.classDelegatingFor = entity.getClass().getSimpleName();
    }

    public String getClassDelegatingFor() {
        return classDelegatingFor;
    }

    public Long getId() {
        return id;
    }

    public void setClassDelegatingFor( String classDelegatingFor ) {
        this.classDelegatingFor = classDelegatingFor;
    }

    public void setId( Long id ) {
        this.id = id;
    }

}
