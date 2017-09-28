Ext.namespace( 'Gemma.LinkRoots' );

Gemma.LinkRoots = {
   expressionExperimentPage : ctxBasePath + "/expressionExperiment/showExpressionExperiment.html?id=",
   expressionExperimentSetPage : ctxBasePath + "/expressionExperimentSet/showExpressionExperimentSet.html?id=",
   geneSetPage : ctxBasePath + "/geneSet/showGeneSet.html?id=",
   genePage : ctxBasePath + "/gene/showGene.html?id=",
   genePageNCBI : ctxBasePath + "/gene/showGene.html?ncbiid=",
   phenotypePage : ctxBasePath + "/phenotypes.html?phenotypeUrlId="
};

(function() {
   Gemma.arrayDesignLink = function( ad ) {
      return "<a ext:qtip='" + ad.name + "' href='" + ctxBasePath + "/arrays/showArrayDesign.html?id=" + ad.id + "'>"
         + ad.shortName + "</a>";
   };
})();
