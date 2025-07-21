/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.common;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class AbstractDescribable extends AbstractIdentifiable implements Describable {

    private String name;
    @Nullable
    private String description;

    @Override
    public String getName() {
        return this.name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return this.description;
    }

    public void setDescription( @Nullable String description ) {
        this.description = description;
    }

    @Override
    public int hashCode() {
        return Objects.hash( getName() );
    }

    @Override
    public String toString() {
        return super.toString() + ( this.getName() == null ? "" : " Name=" + this.getName() );
    }
}