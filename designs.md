# Experimental Designs

## Table of contents

- [Experimental Designs](#experimental-designs)
  * [About](#about)
  * [Browsing the design](#browsing-the-design)
  * [Creating a design for your experiments](#creating-a-design-for-your-experiments)
    + [File upload](#file-upload)
    + [Editing through the website UI](#editing-through-the-website-ui)

## About

An experimental design describes the characteristics of each sample as is relevant to the analysis of the experiment. Experimental designs are not populated for all studies, as this has to be done manually in many cases. Some studies were annotated by GEO curators and those annotations appear in Gemma.

An experimental design is organized around Experimental Factors (EF), listed near the top of the page. A factor is a known variable in an experiment, such as "age", "genotype" or "treatment". An experiment can have any number of factors, but most have only 1-3.

Each sample has a specific Factor Value for each Factor. For example, for "genotype", the available values might be "wild-type" and "mutant". The available factor values for a given EF are shown in the middle of the page. For a parameter like "age", the values might be continuous values (numbers) like "10.1 years".

Factors can also be continuous. In this case they take numeric values and often represent something that was measured by the experimenter, not under direct control. Thus “incubation temperature” should probably be defined as categorical (e.g. 37C vs 42C), while “postmorterm interval” is probably continuous. However, it sometimes makes sense to bin continuous values so they can be treated as categorical. This often comes up in clinical studies. For example, “age” might be treated as categorical (“Young” vs. “Old”) rather than as continuous, even if age was simply a demographic data point collected during the study. It depends on the demands of the study.

Currently Gemma does not directly handle continuous values in statistical analyses. We suggest using categorical values until we add analytical procedures to handle continuous ones. In addition Gemma can currently only use up to two factors in analysis, so defining ten won’t buy you much (for now).
The samples in the experiment (called Biomaterials in Gemma) each have values for each factor (the value can be ‘missing’, which you shouldn’t define as a separate value). Thus in our example we might have two samples which have the values “B6” “stomach” and some that are “B6” “esophagus”.

Because experimental designs are curated manually (either by us or by the original source, such as GEO), it is certainly possible for errors to exist. If you spot a problem, please let us know (see the Contacts & Credits section on the main page.

## Browsing the design
To find the experimental design page, select your dataset of interest from the dataset browser. Then click on the Experimental Design tab at the top of the page. From here, you can click the Show Details icon which will take you to the Experimental Design Details page. The two tabs on this page are Design Setup, which provides the experimental factors and factor values used (For definitions, see the Glossary section on the main page) and Sample Details, which provides a complete list of the samples in the study and the parameters that apply to each sample.

## Creating a design for your experiments

We provide two methods for entering experimental designs. First, you can upload a simple text file. Second, you can enter it using the web interface. The web interface can then be used to make edits if you made any mistakes.

### File upload

For complex experimental designs, it may be easier to use this method rather than entering it by hand. For large data sets with continuous measures such as pH or masses, this is particularly useful.

When you first view the page, you will be given a link to a template file for you to fill in. To see how this file should then be set up, it may be helpful to view the files for some experiments that were already loaded.

Basically, you fill in the table (using Excel or similar) and save as text, and upload it to the Gemma site.

(Note: You cannot combine the upload and manual creation methods; that is, once the design is defined you cannot upload additional information from a file.)

### Editing through the website UI

#### 1- Define and edit experimental factors
Experimental factors are managed in the top half of the “Design setup” tab. To add a factor, click “Add new” and fill in the form. The new factor appears on the table. The hardest part about this step is choosing the category. If you aren’t sure, “Treatment” is a good catchall; for tissue use “Organism Part”. For time, use “Sampling time point”. For description you should use something like “drug vs. control”. The description is particularly useful if you have multiple factors with the same category (like two different treatment factors), so you can tell them apart later.

Currently Gemma uses categories defined from the MGED Ontology (MO). In some cases we abuse or stretch the definition of the term. In the future we will likely replace MO with another ontology for describing experiments.
You can edit or delete factors. To edit a field, double-click on it. Save your changes with the ‘save’ button. If you want to revert your changes (BEFORE clicking ‘save’), click ‘undo’. Once you ‘save’, ‘undo’ will not work. To delete a factor, select it in the table and click ‘delete’. You will be asked to confirm, as this cannot be undone.

Note that deleting a factor deletes all of its associated factor values and the relationships they had to the samples.

#### 2 - Define and edit factor values
Factor values are managed in the “Design setup” tab, “factor values” section (bottom half). This only applies to categorical factors. For continuous-valued ones, this tool is disabled and you must enter values via the “sample details” tab.

To add a factor value:

- Click “Create”. The value is displayed with a placeholder text (like “undefined”).
- Click on the ‘+’ to show the data entry area.
- (Optional) Double-click on the category area to change the category (by default the same category as the factor is used).
- Double-click on the value area and set the value by typing. This is explained in more detail below.

To delete a factor value, check its box and click the ‘delete’ button. You will be asked to confirm. Any associations that factor value had with samples will be erased as well.

##### More detail on setting values

Factor values can be complicated, but for most purposes you can follow a simple approach. Enter a value such as “liver” or “drug” (or “control group”). When you type in the value field, you will be given suggestions for words to use. Words in **bold** are ones that already appear in our system (and so might be good to use). Words in _green_ are from formal ontologies (and so might also be good to use). Words that are in _**bold green**_ are even more preferred. But if you don’t see the term you want, just type one in.

Once you’ve found the term you want, click ‘save’ to apply your changes.

Advanced: Things can be complicated if you want to describe your factor values using ontology terms, which we encourage you to do, but you don’t need to.

Say you want to express the idea of “asprin 1mg/kg”. Obviously there won’t be a single ontology term to match that. The idea is that rather than supplying a free text description “asprin 1mg/kg” you would break this down into two characteristics: one for the drug “asprin” and one for the dose (in principle you would want to separate the dose value from the unit, but that’s beyond the scope of this tool).

You can add additional characteristics to a factor value by using the lower half of the toolbar which says “Add characteristic to: “.

#### 3 - Apply the factor values to the samples
This is done exclusively through the “Sample details” tab. This tab shows a table of the samples in your study, with columns for each factor (if this doesn’t show up correctly, make sure you saved your factors and factor values, and/or refresh the page).

The basic idea is to fill in the table. This can be done one field at a time (slow, but useful for corrections) or in bulk.

Whichever method you use, you must click ‘save’ to make your changes permanent. Until this is done, changed items in the table are marked with a little red triangle. Don’t forget to click “save” before navigating away! You can also click ‘undo’ to set things back – but not after you click ‘save’.

**Entering values one at a time**
Double-click on the cell you want to edit. You will be able to choose or enter a value. Then click “save”.

**Entering values in batch mode**
1. Select the factor and factor value you want to apply from the toolbar.
2. Select the rows of the table you want to change (using click+shift or click+ctrl to select multiple rows)
3. Click the “apply” button.
4. Click “save” to make the changes permanent.
