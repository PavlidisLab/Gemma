# Search query syntax

The search query accepts the following syntax:

|              |                             |                                                                                                                                 |
|--------------|-----------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| Conjunction  | `alpha AND beta AND gamma`  | Results must contain "alpha", "beta" and "gamma".                                                                               |
| Disjunction  | `alpha OR beta OR gamma`    | Results must contain either "alpha", "beta" or "gamma". This is the default when multiple terms are supplied.                   | 
| Grouping     | `(alpha OR beta) AND gamma` | Results must contain one of "alpha" or "beta" and also "gamma".                                                                 |
| Exact Search | `"alpha beta gamma"`        | Results must contain the exact phrase "alpha beta gamma".                                                                       |
| Field        | `shortName:GSE00001`        | Datasets with short name GSE00001. <details><summary>List of supported dataset fields</summary>{searchableProperties}</details> |
| Prefix       | `alpha*`                    | Results must start with "alpha".                                                                                                |
| Wildcard     | `BRCA?`                     | Results can contain any letter for the `?`. In this example, BRCA1 and BRCA2 would be matched.                                  | 
| Fuzzy        | `alpha~`                    | Results can approximate "alpha". In this example, "aleph" would be accepted.                                                    |
| Boosting     | `alpha^2 beta`              | Results mentioning "alpha" are ranked higher over those containing only "beta".                                                 |
| Require      | `+alpha beta`               | Results must mention "alpha" and optionally "beta".                                                                             |
| Escape       | `\+alpha`                   | Results must mention "+alpha". Any special character from the search syntax can be escaped by prepending it with "\".           | 
