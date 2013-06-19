/**
 * Wizard tab panel
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.WizardTabPanel = Ext.extend(Ext.TabPanel, {
      activeTab : 0,
      cls : 'wizardTabPanel', // See TabPanel.css for custom css values.
      useCustomInsteadOfTabIcons : true,
      enableOnlyFirstTab : true,
      initComponent : function() {
         if (this.useCustomInsteadOfTabIcons) {
            this.addClass('wizardTabPanelUseCustomIcon');
         }

         var findComponentIndex = function(component) {
            var componentIndex = -1;
            this.items.each(function(item, index, length) {
                  if (component === item) {
                     componentIndex = index;
                  }
               });

            return componentIndex;
         }.createDelegate(this);

         for (var i = 0; i < this.items.length; i++) {
            var item = this.items[i];

            if (i != 0 && this.enableOnlyFirstTab) {
               item.disable();
            }

            if (i < this.items.length - 1 && this.useCustomInsteadOfTabIcons) {
               item.iconCls = 'icon-wizard-next';
            }

            item.on({
                  nextButtonClicked : function(component) {
                     var componentIndex = findComponentIndex(component);

                     if (componentIndex < this.items.length - 1) {
                        this.setActiveTab(componentIndex + 1);
                        this.getActiveTab().enable();
                     }

                     // Disable all the tabs after the next tab.
                     for (var i = componentIndex + 2; i < this.items.length; i++) {
                        this.getComponent(i).disable();
                     }
                  },
                  scope : this
               });
         }

         Gemma.WizardTabPanel.superclass.initComponent.call(this);

         // I need to manually call setActiveTab() to set active tab. Otherwise, getActiveTab() returns null.
         this.setActiveTab(this.activeTab);
      }
   });
