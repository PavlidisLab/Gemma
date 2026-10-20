package ubic.gemma.core.ontology.providers;

/**
 * Marker for an {@link OntologyService} that can produce a corpus-tailored slim
 * variant of its source via OWL-API STAR module extraction. Implemented by
 * {@code ChebiOntologyService} and {@code MondoOntologyService}; consumed by
 * the {@code POST /admin/ontologies/{name}/rebuild-slim} REST endpoint so the
 * controller can address any slimmable service without knowing the concrete type.
 *
 * <p>Two methods, both already on the concrete services:
 * <ul>
 *   <li>{@link #triggerSlimRebuildAsync()} — spawn a daemon thread to (re)build
 *       the slim cache from the on-disk source. Returns {@code true} if a new
 *       build started; {@code false} if one was already in flight.</li>
 *   <li>{@link #isSlimRebuildInFlight()} — status probe for the daemon thread.</li>
 * </ul>
 *
 * <p>The slim file + sidecar meta live at
 * {@code ${ontology.cache.dir}/ontology/<cacheName>-slim.{owl,meta.json}},
 * derived from the service's {@code getCacheName()}.
 */
public interface SlimmableOntologyService extends OntologyService {

    /**
     * Spawn a daemon thread to rebuild the slim cache from the current corpus seed set.
     * Returns immediately. Subsequent calls while a rebuild is in flight return
     * {@code false} without starting a duplicate thread.
     *
     * @throws IllegalStateException if the service isn't loaded yet, or if the slim
     *         plumbing (extractor / resolver / cache dir) isn't wired on the bean.
     */
    boolean triggerSlimRebuildAsync();

    /**
     * @return {@code true} if a slim-rebuild daemon thread is currently running on this bean.
     */
    boolean isSlimRebuildInFlight();
}
