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
package ubic.gemma.util;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;

/**
 * Our \@Service etc. annotations in classes end up generating names with "Impl" on the end, unless we explicitly
 * provide a name. To avoid us having to do that explicit declaration, I wrote this.
 * 
 * @author paul
 * @version $Id$
 */
public class BeanNameGenerator implements org.springframework.beans.factory.support.BeanNameGenerator {

    DefaultBeanNameGenerator gen;

    public BeanNameGenerator() {
        this.gen = new DefaultBeanNameGenerator();

    }

    /**
     * Automagically produce camel-case names for the beans.
     * 
     * @see org.springframework.beans.factory.support.BeanNameGenerator#generateBeanName(org.springframework.beans.factory
     *      .config.BeanDefinition, org.springframework.beans.factory.support.BeanDefinitionRegistry).
     */
    public String generateBeanName( BeanDefinition definition, BeanDefinitionRegistry registry ) {
        String name = this.gen.generateBeanName( definition, registry ).replace( "Impl", "" ).replace( "#0", "" )
                .replaceAll( ".+\\.", "" );

        name = StringUtils.uncapitalize( name );
        return name;
    }

}
