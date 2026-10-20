/*
 * The Gemma project
 *
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.core.security;

/**
 * Defines constants used in GrantedAuthories. An authority is basically a marker of a level of access; in Gemma this
 * corresponds to the authority on a 'group' of users (UserGroup). There are currently three special groups:
 * Administrators, Users, and Agents. Anonymous is another
 * <p>
 * Some of these values are originally set by init-entities.sql
 *
 * @author klc, paul
 * @version $Id: AuthorityConstants.java,v 1.6 2013/08/21 23:22:22 paul Exp $
 * @see org.springframework.security.core.GrantedAuthority
 */
public class AuthorityConstants {

    /**
     * Prefix used for roles.
     * <p>
     * Spring uses {@code ROLE_} by default.
     */
    public static final String ROLE_PREFIX = "GROUP_";

    /**
     * The name of the administrator group authority. All administrators must be in this group.
     */
    public static final String ADMIN_GROUP_AUTHORITY = "GROUP_ADMIN";

    /**
     * The name of the administrator group. Not the same as the group authority!
     */
    public static final String ADMIN_GROUP_NAME = "Administrators";

    /**
     * The name of the default user group authority. All authenticated users should be in this group or in the
     * administrator group.
     */
    public static final String USER_GROUP_AUTHORITY = "GROUP_USER";

    public static final String USER_GROUP_NAME = "Users";

    /**
     * Used when we are running at elevated permissions.
     */
    public static final String RUN_AS_ADMIN_AUTHORITY = "GROUP_RUN_AS_ADMIN";

    /**
     * Used when we are running at elevated permissions.
     */
    public static final String RUN_AS_USER_AUTHORITY = "GROUP_RUN_AS_USER";

    /**
     * Curators do everything an administrator does to DATA — edit designs, change a dataset's
     * visibility, run analyses — and nothing an administrator does to the SERVER or to USER
     * ACCOUNTS. Those two exclusions are enforced at the REST layer, where the routes that
     * manage caches, indices, ontologies, sessions, pipeline batches and user accounts keep
     * {@code hasAuthority('GROUP_ADMIN')}; everything else was widened to
     * {@code hasAuthority('GROUP_CURATOR')}, which an administrator satisfies through the role
     * hierarchy (Paul, 2026-09-05: "really nothing other than user admin, and I guess server ops").
     */
    public static final String CURATOR_GROUP_AUTHORITY = "GROUP_CURATOR";

    public static final String CURATOR_GROUP_NAME = "Curators";

    public static final String AGENT_GROUP_AUTHORITY = "GROUP_AGENT";

    public static final String AGENT_GROUP_NAME = "Agents";

}
