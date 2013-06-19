Ext.namespace('Gemma');

/**
 * 
 * @type {Function}
 */
Gemma.CoexpressionGridRecordConstructor = Ext.data.Record.create([{
      name : "queryGene",
      sortType : function(gene) {
         return gene.officialSymbol;
      }
   }, {
      name : "foundGene",
      sortType : function(gene) {
         return gene.officialSymbol;
      }
   }, {
      name : "sortKey",
      type : "string"
   }, {
      name : "supportKey",
      type : "int",
      sortType : Ext.data.SortTypes.asInt,
      sortDir : "DESC"
   }, {
      name : "posSupp",
      type : "int"
   }, {
      name : "negSupp",
      type : "int"
   }, {
      name : "numTestedIn",
      type : "int"
   }, {
      name : "nonSpecPosSupp",
      type : "int"
   }, {
      name : "nonSpecNegSupp",
      type : "int"
   }, {
      name : "hybWQuery",
      type : "boolean"
   }, {
      name : "goSim",
      type : "int"
   }, {
      name : "maxGoSim",
      type : "int"
   }, {
      name : "datasetVector",
      type : "string"
   }, {
      name : "supportingExperiments"
   }, {
      name : "gene2GeneProteinAssociationStringUrl",
      type : "string"
   }, {
      name : "gene2GeneProteinInteractionEvidence",
      type : "string"
   }, {
      name : "gene2GeneProteinInteractionConfidenceScore",
      type : "string"
   }, {
      name : "foundGeneNodeDegree",
      type : "float"
   }, {
      name : "queryGeneNodeDegree",
      type : "float"
   }, {
      name : "containsMyData",
      type : "boolean"
   }, {
      name : "foundRegulatesQuery",
      type : "boolean"
   }, {
      name : "queryRegulatesFound",
      type : "boolean"
   }]);
