#
# Sort an AnnData object by sample
#

import sys
import os
import resource


def memory_limit(ratio):
    """
    Cap the memory this process can commit to a fraction of the host's available memory, so that an oversized
    dataset fails with a MemoryError instead of taking the host down.

    RLIMIT_DATA is used rather than RLIMIT_AS: it counts private writable mappings (the heap and the anonymous
    mappings backing NumPy arrays), whereas RLIMIT_AS also counts address space that is reserved but never used,
    such as the per-thread malloc arenas that threads create.

    Either limit also counts the stack and buffers that OpenBLAS reserves for each of its threads, one per core,
    which is several GiB on a many-core host before any data is loaded; hitting the limit while those threads start
    crashes the interpreter instead of raising a MemoryError. This script does not need BLAS, so it is restricted to
    a single thread.

    The ratio can be overridden with GEMMA_PYTHON_MEMORY_LIMIT_RATIO; set it to 0 to disable the limit. This is a
    no-op where /proc/meminfo is not available (e.g. macOS).
    """
    os.environ.setdefault('OPENBLAS_NUM_THREADS', '1')
    os.environ.setdefault('OMP_NUM_THREADS', '1')
    ratio = float(os.environ.get('GEMMA_PYTHON_MEMORY_LIMIT_RATIO', ratio))
    if ratio <= 0:
        return
    try:
        with open('/proc/meminfo') as f:
            available_memory = next(int(line.split()[1]) * 1024 for line in f if line.startswith('MemAvailable:'))
    except (OSError, StopIteration):
        print('Could not determine available memory from /proc/meminfo, not limiting memory usage.')
        return
    limit = int(available_memory * ratio)
    soft, hard = resource.getrlimit(resource.RLIMIT_DATA)
    # never raise an existing limit
    if soft != resource.RLIM_INFINITY:
        limit = min(limit, soft)
    if hard != resource.RLIM_INFINITY:
        limit = min(limit, hard)
    print('Limiting memory usage to %.1f GiB.' % (limit / 1024 ** 3))
    resource.setrlimit(resource.RLIMIT_DATA, (limit, hard))


memory_limit(.9)

try:
    import numpy as np
    import anndata
    from scipy.sparse import isspmatrix_csr
except ImportError as e:
    print('You need anndata and scipy to run this script. Install it with "pip install anndata".')
    raise e

try:
    input_file, output_file, sample_column_name = sys.argv[1:]
except Exception as e:
    print('Usage: python sort-by-sample-anndata.py input_file output_file sample_column_name')
    raise e


def permute_csr_columns(m, inv_perm, chunk_size=1 << 26):
    """
    Permute the columns of a CSR matrix in place such that old column j becomes column inv_perm[j].

    This only rewrites the column indices, in bounded chunks, instead of copying the whole matrix.
    """
    inv_perm = inv_perm.astype(m.indices.dtype, copy=False)
    for start in range(0, len(m.indices), chunk_size):
        chunk = m.indices[start:start + chunk_size]
        chunk[:] = inv_perm[chunk]
    # the flag is cached by scipy and would make sort_indices() a no-op
    m.has_sorted_indices = False
    m.sort_indices()


print("Reading HDF5 from " + input_file + "...")
df = anndata.read_h5ad(input_file)
print("Sorting by " + sample_column_name + "...")
# positions in the order produced by sort_values(); the sort depends only on the column values, so this is the same
# order as sorting df.var itself
perm = df.var[[sample_column_name]].reset_index(drop=True).sort_values(sample_column_name).index.to_numpy()
inv_perm = np.empty_like(perm)
inv_perm[perm] = np.arange(len(perm))
# CSR matrices are detached and permuted in place to avoid copying them; everything else (var, varm, varp, dense or
# CSC matrices) is reordered by AnnData itself
X = df.X if isspmatrix_csr(df.X) else None
if X is not None:
    df.X = None
layer_names = list(df.layers)
csr_layers = {}
for layer in list(df.layers):
    if isspmatrix_csr(df.layers[layer]):
        csr_layers[layer] = df.layers[layer]
        del df.layers[layer]
# we need a copy, otherwise we would be sorting a view
df = df[:, perm].copy()
if X is not None:
    print("Sorting CSR matrix indices from /X...")
    permute_csr_columns(X, inv_perm)
    df.X = X
for layer, m in csr_layers.items():
    print("Sorting CSR matrix indices from /layers/" + layer + "...")
    permute_csr_columns(m, inv_perm)
    df.layers[layer] = m
# restore the original order of the layers
df.layers = {layer: df.layers[layer] for layer in layer_names}
print("Writing result to " + output_file + "...")
df.write_h5ad(output_file)
