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

package ubic.gemma.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import ubic.gemma.web.util.MessageUtil;

/**
 * Extend this to create a simple Single or MultiActionController; includes configuration for sending email and setting
 * messages in the session. Use the \@Controller and \@RequestMapping annotations to configure subclasses.
 *
 * @author keshav
 */
public abstract class BaseController {

    protected Log log = LogFactory.getLog( getClass().getName() );

    @Autowired
    protected MessageSource messageSource;

    @Autowired
    protected MessageUtil messageUtil;
}
