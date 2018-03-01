/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.loader.util.fetcher;

import ubic.gemma.model.common.description.LocalFile;

import java.io.File;
import java.util.Collection;

/**
 * Interface for downloading via http files and unarchiving them
 *
 * @author ldonnison
 */
public interface HttpArchiveFetcherInterface extends Fetcher {

    File unPackFile( Collection<LocalFile> localFile );

}
