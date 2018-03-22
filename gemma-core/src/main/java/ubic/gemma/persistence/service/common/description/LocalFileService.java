/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.common.description;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.persistence.service.BaseService;

import java.io.IOException;
import java.util.Collection;

/**
 * @author kelsey
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public interface LocalFileService extends BaseService<LocalFile> {

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_EDIT" })
    LocalFile copyFile( LocalFile sourceFile, LocalFile targetFile ) throws IOException;

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void deleteFile( LocalFile localFile ) throws IOException;

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    LocalFile find( LocalFile localFile );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    LocalFile findOrCreate( LocalFile localFile );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<LocalFile> loadAll();

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( LocalFile localFile );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    LocalFile findByPath( java.lang.String path );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    LocalFile save( LocalFile localFile );

}
