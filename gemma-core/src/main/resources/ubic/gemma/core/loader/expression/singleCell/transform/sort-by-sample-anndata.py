#
# Sort an AnnData object by sample
#

import sys
import resource

def memory_limit(ratio):
    with open('/proc/meminfo') as f:
        for line in f:
            if line.startswith('MemAvailable:'):
                available_memory = int(line.split()[1]) * 1024
                break
        else:
            raise RuntimeError('Could not determine available memory from /proc/meminfo')
    limit = int(available_memory * ratio)
    resource.setrlimit(resource.RLIMIT_AS, (limit, limit))

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
    print('Usage: python sort-by-sample-anndata.py input_file output_file')
    raise e

print("Reading HDF5 from " + input_file + "...")
df = anndata.read_h5ad(input_file)
print("Sorting by " + sample_column_name + "...")
sorted_var = df.var.sort_values(sample_column_name)
# perm[new_idx] = old_idx, inv_perm[old_idx] = new_idx
perm = df.var.index.get_indexer(sorted_var.index)
inv_perm = np.argsort(perm)
# Remap column indices in place to avoid allocating making a copy of the matrix.
if isspmatrix_csr(df.X):
    df.X.indices[:] = inv_perm[df.X.indices]
    print("Sorting CSR matrix indices from /X...")
    df.X.sort_indices()
for layer in df.layers:
    if isspmatrix_csr(df.layers[layer]):
        df.layers[layer].indices[:] = inv_perm[df.layers[layer].indices]
        print("Sorting CSR matrix indices from /layers/" + layer + "...")
        df.layers[layer].sort_indices()
df._var = sorted_var
print("Writing result to " + output_file + "...")
df.write_h5ad(output_file)
