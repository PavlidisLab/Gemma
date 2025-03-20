Ext.namespace( 'Gemma.LinkRoots' );

Gemma.LinkRoots = {
   expressionExperimentPage : Gemma.CONTEXT_PATH + "/expressionExperiment/showExpressionExperiment.html?id=",
   expressionExperimentSetPage : Gemma.CONTEXT_PATH + "/expressionExperimentSet/showExpressionExperimentSet.html?id=",
   geneSetPage : Gemma.CONTEXT_PATH + "/geneSet/showGeneSet.html?id=",
   genePage : Gemma.CONTEXT_PATH + "/gene/showGene.html?id=",
   genePageNCBI : Gemma.CONTEXT_PATH + "/gene/showGene.html?ncbiid=",
   phenotypePage : Gemma.CONTEXT_PATH + "/phenotypes.html?phenotypeUrlId="
};

(function() {
   Gemma.arrayDesignLink = function( ad ) {
      return "<a ext:qtip='" + ad.name + "' href='" + Gemma.CONTEXT_PATH + "/arrays/showArrayDesign.html?id=" + ad.id + "'>"
         + ad.shortName + "</a>";
   };
})();
