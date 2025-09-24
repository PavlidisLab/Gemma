# Glossary

You may encounter some of these terms when using Gemma.

- **Array Design, Platform**: A microarray design or other expression platform. For example, the HG-U133A is a specific Array Design.
- **BioMaterial**: An RNA sample, along with the description of where and how it was obtained and prepared. For data collected on ratiometric arrays, this may reflect a combination of two RNA samples, one of which was used as a reference. (Near-synonym: **sample**)
- **BioAssay, Assay**: The combination of a BioMaterial with an ArrayDesign Represents a RNA sample run on a single microarray. (Near-synonym: **sample**)
- **Characteristic, Annotation, Tag**: A descriptive annotation applied to a dataset or sample. For example, “Organism Part: Liver” is a characteristic which pairs the MGED Ontology term “Organism part” with the value “Liver”. Characteristics are also used to describe Experimental Factors and Factor Values.
- **Data vector**: Typically refers to an expression profile for one probe in one dataset, though the precise definition depends on context.
- **Experimental design**: Describes the organization of samples in an Expression Experiment.
- **Experimental factor**: A value which was varied (or variable) in an experiment and which is considered part of the experimental design. Each sample in the study will have a factor value for each factor. Factors can be categorical (e.g., “tissue”) or continuous (e.g., “Weight in grams”).
- **Expression Experiment, Experiment, Dataset, Study**: A group of BioAssays, for example as grouped together in a single GEO accession.
- **Expression Experiment Set, Dataset set**: A grouping of Expression Experiments.
- **Factor value**: See experimental factor for an explanation.
- **Preferred quantitation type**: The quantitation type that is used when we want to speak of “expression levels” or “expression measurements”. The values for the preferred quantitation type are the ones that are actually used in most analyses or visualizations. Each study should have only one preferred quantitation type.
- **Processed expression data**: The data for the preferred quantitation type.
- **Quantitation type**: Describes values measured on in bioassays, either coming from the original scan or derived values. For example, “Signal”, “Background” or “Absent/Present call” might be descriptions of quantitation types.
- **Sample**: In typical usage, synonymous with BioAssay. May refer to a BioMaterial. Hopefully the intended meaning should be clear from the context (or not important to distinguish).
