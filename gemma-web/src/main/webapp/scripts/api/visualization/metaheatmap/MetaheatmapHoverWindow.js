Ext.namespace( 'Gemma.Metaheatmap' );

// a window for displaying details as elements of the image are
// hovered over

Gemma.Metaheatmap.HoverWindow = Ext
   .extend(
      Ext.Window,
      {

         // height : 200,
         width : 350,
         closable : false,
         shadow : false,
         border : false,
         bodyBorder : false,
         // hidden : true,
         // bodyStyle : 'padding: 7px',

         isDocked : false, // 

         tplWriteMode : 'overwrite',

         initComponent : function() {
            Gemma.Metaheatmap.HoverWindow.superclass.initComponent.apply( this, arguments );

            this.tpl = this.initTemplate_();
         },

         /**
          * @private
          * @memberOf Gemma.Metaheatmap.HoverWindow
          */
         initTemplate_ : function() {
            if ( Gemma.Metaheatmap.Config.USE_GENE_COUNTS_FOR_ENRICHMENT ) {
               return new Ext.XTemplate(
                  '<span style="font-size: 12px ">',
                  '<tpl for=".">',
                  '<tpl if="type==\'condition\'">', // condition
                  '<b>Experiment</b>: {datasetShortName}, {datasetName}<br>',
                  '<b>Condition</b>: {contrastFactorValue} vs {baselineFactorValue} ({factorCategory})<br> ',
                  '<b>Baseline</b>: {baselineFactorValue} <br> ',
                  '<b>Enrichment</b>: {numDiffExpressed} out of {numInSet} genes are differentially expressed with p-value {ora:sciNotation} <br> ',
                  '<b>Specificity</b>: {specificityPercent}% of genes were differentially expressed under this condition ({totalDiffExpressed} out of {totalOnArray})<br><br> ',
                  '</tpl>', '<tpl if="type==\'minipie\'">', // minipie
                  '{percentDiffExpressed} of genes are differentially expressed.<br>',
                  '({totalDiffExpressed} of {totalOnArray}) Click for details.', '</tpl>', '<tpl if="type==\'gene\'">', // gene
                  '<b>Gene</b>: {geneSymbol} {geneFullName}<br>',
                  '<b>Meta P value</b>: {geneMetaPvalue:sciNotation} based on {metaPvalueCount} p-values.<br>',
                  '</tpl>', '<tpl if="type==\'cell\'">', // cell
                  '<b>Gene</b>: {geneSymbol} {geneFullName}<br>',
                  '<b>Experiment</b>: {datasetShortName}, {datasetName}<br>',
                  '<b>Condition</b>: {contrastFactorValue} vs {baselineFactorValue} ({factorCategory})<br>',
                  '<b>Baseline</b>: {baselineFactorValue} <br>',
                  '<b>Number of elements</b>: {numberOfProbesDiffExpressed} / {numberOfProbes} <br>',
                  '<b>q-value</b>: {correctedPValue:sciNotation}<br>', '<b>p-value</b>: {pvalue:sciNotation}<br>',
                  '<b>log fold change</b>: {foldChange:logRatioNotation}', '</tpl>', '</tpl></span>' );
            } else {
               return new Ext.XTemplate(
                  '<span style="font-size: 12px ">',
                  '<tpl for=".">',
                  '<tpl if="type==\'condition\'">', // condition
                  '<b>Experiment</b>: {datasetShortName}, {datasetName}<br>',
                  '<b>Condition</b>: {contrastFactorValue} vs {baselineFactorValue} ({factorCategory})<br> ',
                  '<b>Baseline</b>: {baselineFactorValue} <br> ',
                  '<b>Enrichment</b>: {numDiffExpressed} out of {numInSet} elements are differentially expressed with p-value {ora:sciNotation} <br> ',
                  '<b>Specificity</b>: {specificityPercent}% of elements were differentially expressed under this condition ({totalDiffExpressed} out of {totalOnArray})<br><br> ',
                  '</tpl>', '<tpl if="type==\'minipie\'">', // minipie
                  '{percentDiffExpressed} of elements are differentially expressed.<br>',
                  '({totalDiffExpressed} of {totalOnArray}) Click for details.', '</tpl>', '<tpl if="type==\'gene\'">', // gene
                  '<b>Gene</b>: {geneSymbol} {geneFullName}<br>',
                  '<b>Meta P value</b>: {geneMetaPvalue:sciNotation} based on {metaPvalueCount} p-values.<br>',
                  '</tpl>', '<tpl if="type==\'cell\'">', // cell
                  '<b>Gene</b>: {geneSymbol} {geneFullName}<br>',
                  '<b>Experiment</b>: {datasetShortName}, {datasetName}<br>',
                  '<b>Condition</b>: {contrastFactorValue} vs {baselineFactorValue} ({factorCategory})<br>',
                  '<b>Baseline</b>: {baselineFactorValue} <br>',
                  '<b>Number of elements</b>: {numberOfProbesDiffExpressed} / {numberOfProbes} <br>',
                  '<b>q-value</b>: {correctedPValue:sciNotation}<br>', '<b>p-value</b>: {pvalue:sciNotation}<br>',
                  '<b>log fold change</b>: {foldChange}', '</tpl>', '</tpl></span>' );
            }
         },

         /**
          * @private
          */
         onRender : function() {
            Gemma.Metaheatmap.HoverWindow.superclass.onRender.apply( this, arguments );
         }

      } );

Ext.reg( 'Metaheatmap.HoverWindow', Gemma.Metaheatmap.HoverWindow );
