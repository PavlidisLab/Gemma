/**
 * This package contains classes and interfaces related to R integration in the Gemma project.
 * <p>
 * <ul>
 * <li>{@link ubic.gemma.core.util.r.RClient}, a high-level client for interacting with R.</li>
 * <li>{@link ubic.gemma.core.util.StandaloneREngine}, an extension of {@link org.rosuda.REngine.REngine} that supports</li>
 * communicating with R using a UNIX domain socket.
 * </ul>
 * This is intended to be a complete replacement for {@link ubic.basecode.util.r} that should eventually be moved
 * there.
 * @author poirigui
 */
@ParametersAreNonnullByDefault
package ubic.gemma.core.util.r;

import javax.annotation.ParametersAreNonnullByDefault;