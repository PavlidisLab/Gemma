-- see https://github.com/PavlidisLab/Gemma/issues/341 for more details
update CHARACTERISTIC
set VALUE_URI = replace(VALUE_URI, 'http://purl.obolibrary.org/obo/TGEMO_', 'http://gemma.msl.ubc.ca/ont/TGEMO_')
where VALUE_URI LIKE 'http://purl.obolibrary.org/obo/TGEMO_%';