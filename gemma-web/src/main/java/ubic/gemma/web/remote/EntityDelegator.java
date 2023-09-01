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

import org.springframework.util.Assert;
import ubic.gemma.model.common.Identifiable;

/**
 * Bean to expose for remote access via AJAX, when all that is needed is the ID and a way to know what the class is.
 * @param <T> the type of entity being delegated
 * @author Paul
 */
public class EntityDelegator<T extends Identifiable> {

    private Long id;

    private String classDelegatingFor;

    public EntityDelegator() {
    }

    public EntityDelegator( T entity ) {
        Assert.notNull( entity.getId(), "The entity being delegated must have a non-null ID." );
        this.id = entity.getId();
        this.classDelegatingFor = entity.getClass().getName();
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

    /**
     * Check if the entity delegator holds the given type.
     */
    public boolean holds( Class<?> type ) {
        return type.getName().equals( this.classDelegatingFor ) || type.getSimpleName().equals( this.classDelegatingFor );
    }

    @Override
    public String toString() {
        return String.format( "%s Id=%d", classDelegatingFor, id );
    }
}
