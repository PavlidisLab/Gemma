Ext.namespace('Gemma');

/**
 * need to set geneId as config
 */
Gemma.GeneAllenBrainAtlasImages = Ext.extend(Ext.Panel, {
      geneId : null,
      padding : 10,
      defaults : {
         border : false
      },
      initComponent : function() {

         Gemma.GeneAllenBrainAtlasImages.superclass.initComponent.call(this);
         this.on('render', function() {
               if (!this.loadMask) {
                  this.loadMask = new Ext.LoadMask(this.getEl(), {
                        msg : Gemma.StatusText.Loading.generic,
                        msgCls : 'absolute-position-loading-mask ext-el-mask-msg x-mask-loading'
                     });
               }
               this.loadMask.show();
               GeneController.loadAllenBrainImages(this.geneId, function(imageObjects) {

                     this.loadMask.hide();
                     if (!imageObjects || imageObjects.length === 0) {
                        this.add({
                              html : 'No images available'
                           });

                     } else {

                        var img = imageObjects[0];
                        var homologueText = (img.usingHomologue) ? 'Images are for homologous mouse gene: ' + '<a target="_blank" href="/Gemma/gene/showGene.html?id='
                           + img.abaHomologousMouseGene.id + '">' + img.abaHomologousMouseGene.officialSymbol + ' [' + img.abaHomologousMouseGene.taxonCommonName + ']</a>' : '';
                        this.add({
                              html : '<h3>Allen Brain Atlas expression pattern' + '<a class="helpLink" href="javascript:void(0)"' + 'onclick="showHelpTip(event, \''
                                 + Gemma.HelpText.WidgetDefaults.GeneAllenBrainAtlasImages.helpTT + '\'); return false">' + '<img src="/Gemma/images/help.png" /> </a>'
                                 + '<a title="Go to Allen Brain Atlas details for ' + img.queryGeneSymbol + '" href="' + img.abaGeneURL + '" target="_blank">'
                                 + '<img src="/Gemma/images/logo/aba-icon.png" height="20" width="20" /> </a>' + '</h3>' + '<p>' + homologueText + '<p/>'
                           });
                        var i;
                        for (i = 0; i < imageObjects.length; i++) {
                           img = imageObjects[i];
                           var tmpThumblink = img.downloadExpressionPath.replace("zoom=2", "zoom=0");
                           this.add({
                                 html : '<div style="cursor: pointer; float: left; padding: 8px">' + '<a title="Allen Brain Atlas Image for ' + img.queryGeneSymbol
                                    + ', click to enlarge" ' + 'onClick="Gemma.geneLinkOutPopUp( &#34; ' + img.downloadExpressionPath + ' &#34; )">' + '<img src="' + tmpThumblink
                                    + '" /> </a>' +
                                    // this link is broken:'<img src="'+img.expressionThumbnailUrl+'" /> </a>'+
                                    '</div>'
                              });
                        }
                        this.doLayout();
                     }

                  }.createDelegate(this));
            });
      }
   });
Ext.reg('geneallenbrainatlasimages', Gemma.GeneAllenBrainAtlasImages);

Gemma.geneLinkOutPopUp = function(abaImageUrl) {

   if (abaImageUrl == null)
      return;

   var abaWindowId = "geneDetailsAbaWindow";
   var win = Ext.getCmp(abaWindowId);
   if (win != null) {
      win.close();
   }

   win = new Ext.Window({
      html : "<img src='" + abaImageUrl + "'>",
      id : abaWindowId,
      stateful : false,
      title : "<img height='15'  src='/Gemma/images/abaExpressionLegend.gif'>"
      // ,
      // width : 500,
      // height : 400,
      // autoScroll : true
   });
   win.show(this);

};