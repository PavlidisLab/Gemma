/**
 * Mass-indexer driver around Hibernate Search 7's
 * {@link org.hibernate.search.mapper.orm.massindexing.MassIndexer}. Exposes
 * {@link ubic.gemma.core.search.indexer.IndexerService} for callers (CLI, scheduled
 * task) that need to rebuild the on-disk Lucene index for one or more
 * {@code @Indexed} entity roots.
 * <p>
 * See SEARCH_RECCE.md Section 4 Step 4.
 */
package ubic.gemma.core.search.indexer;
