Ext.namespace( 'Gemma' );

/**
 * Constructor for making coexpression grid records. CoexpressionValueObjectExt
 * 
 * @type {Function}
 */
Gemma.CoexpressionGridRecordConstructor = Ext.data.Record.create( [ {
   name : "queryGene",
   sortType : function( gene ) {
      return gene.officialSymbol;
   }
}, {
   name : "foundGene",
   sortType : function( gene ) {
      return gene.officialSymbol;
   }
}, {
   name : "sortKey",
   type : "string"
}, {
   name : "support",
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
   name : "supportingExperiments"
}, {
   name : "foundGeneNodeDegree",
   type : "float"
}, {
   name : "queryGeneNodeDegree",
   type : "float"
}, {
   name : "foundGeneNodeDegreeRank",
   type : "float"
}, {
   name : "queryGeneNodeDegreeRank",
   type : "float"
} ] );
