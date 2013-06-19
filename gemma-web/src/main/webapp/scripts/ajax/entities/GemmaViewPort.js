/**
 * @author thea
 * @version $Id$
 */
Ext.namespace('Gemma');

/**
 * main panel should be set as config value: centerPanelConfig ex: new Gemma.GemmaViewPort({ centerPanelConfig: new
 * Gemma.GenePage({ geneId: Ext.get("gene").getValue() }) });
 */
Gemma.GemmaViewPort = Ext.extend(Ext.Viewport, {
      layout : 'border',
      defaultCenter : {
         xtype : 'panel',
         html : 'must be set in config'
      },
      centerPanelConfig : null,
      initComponent : function() {
         this.centerPanel = (this.centerPanelConfig) ? this.centerPanelConfig : this.defaultCenter;
         Gemma.GemmaViewPort.superclass.initComponent.call(this);

         this.add([{
               xtype : 'gemmaNavHeader',
               region : 'north'
            }]);
         this.setCenterRegion(this.centerPanel);
      },
      setCenterRegion : function(panel) {
         Ext.apply(panel, {
               region : 'center'
            });
         this.add(panel);
      },
      onRender : function(ct, position) {

         Gemma.GemmaViewPort.superclass.onRender.call(this, ct, position);
         // HACK
         var createdJunk = document.getElementById('page');
         if (createdJunk && createdJunk.parentNode && createdJunk.parentNode.hasChildNodes) {
            createdJunk.style.display = "none";
            // createdJunk.parentNode.removeChild(createdJunk);
         }
      }
   });
