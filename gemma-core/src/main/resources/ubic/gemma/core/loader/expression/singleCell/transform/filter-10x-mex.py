import sys
import cellranger
import cellranger.cell_calling
import cellranger.cell_calling_helpers
from cellranger.mtx_to_matrix_converter import load_mtx

if len(sys.argv) != 3:
    print('Usage: python filter-10x-mtx.py input_file output_file')
    sys.exit(1)

input_file, output_file = sys.argv[1], sys.argv[2]

print('Reading 10x MEX from ' + input_file + '...')
data = load_mtx(input_file)

# TODO: filter data
print('Filtering low quality cells...')

print('Writing result to ' + output_file + '...')
data.save_mex(output_file)