## Running a Search
### Setting up the query
1. Select whether you want to search for Differential Expression or Coexpression. See figure 1 below.

{% include image.html url="/assets/img/search1.png" description="Figure 1 - Initial state of the expression search form" %}

2. Select genes to find differential expression data for. 
This can be done by selecting a pre-made group or by searching for experiments and using the results.

  1. Selecting a pre-made group
    - When you click in the gene search box, you may be prompted with a list of gene groups.
    - Prompted groups can include groups made by Gemma administrators, groups you have saved (if you are logged in) and temporary (unsaved) groups
  2. Searching for genes
    - Genes can be found by key word, which will match to gene symbols, names, ontology associations and group names
    - Use quotation marks to search for phrases, e.g _"cell junction"_.
  3. Using a list of symbols or NCBI IDs
    - If you already know which genes you want to use for your search, you can paste them in by symbol or NCBI ID
    - Click on the button just to the left of the gene search box to open a window to paste in your list (See figure 1, box B).
    - Each gene should be on one line.
    
3. Select experiments from which we’ll pull expression data.

  1. Selecting a pre-made group:
    - When you click in the experiment search box, you will be prompted with a list of experiment groups.
    - Prompted groups can include groups made by Gemma administrators, groups you have saved (if you are logged in) and temporary (unsaved) groups.
  2. Searching for experiments:
    - Gemma has thousands of experiments for you to use.
    - Try searching these experiments by keyword (_hippocampus, autism_), array design (_GPL96_) or GEO id (_GSE28521_).
    - Keyword searches look for matches in experiment accessions, names, descriptions, array designs, curator-assigned tags and group names.
    - You can search using word roots, like "hypothala".
    
### Viewing, refining and saving your selections
The following applies to both gene and experiment selections

Once you have made a selection, you can add to it using the search boxes in the selection preview area.

To remove elements from your selection or to save your selection, use the edit/save icon in the top right of the preview areas (See figure 2, box A).

{% include image.html url="/assets/img/search2.png" description="Figure 2 - post-selection state of the expression search form" %}

For example, let’s say you search for "hippocampus" and select the "hippocampus development" Gene Ontology (GO) group. The gene preview pane now shows you a preview of the genes you have selected. If you want to see all the genes included in this GO group, you can press the "# more…" link at the bottom of the selection pane. (See figure 2, box B) This will bring up a list of the genes in your selection. You can use the "X" buttons on every line to remove genes from your selection or use the search box at the top to add genes.

Now that you have edited your "save" button), it will be permanently available to you. If you aren’t logged in or don’t want to save your changes, you can press the "done" button which will let you use your edited selection for this search. Gemma will remember your edited selection for some time, but we can’t guarantee it will be around for long (if your cache is cleared or we restart our servers, your unsaved selections will be gone). You can have three unsaved edited selections at a time; when you create a fourth, the oldest unsaved edited selection will be replaced.

