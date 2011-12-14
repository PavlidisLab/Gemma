Ext.namespace('Gemma.Tutorial');

/**
 * Format of param array's elements must be:
 * 	
 *  var tipDefinition = [];
	tipDefinition.push({
		element: this.visualizationPanel,
		title: 'Forth',
		text: 'Look out--here comes science!  <br><a href="#">Next</a>',
		tipConfig:{
			anchor: 'left'
		}
	});
 * 
 * set config
	stateId: [...],
	for this particular tutorial to stay hidden once closed
 * 
 * @param {Object[]} tipDefinition
 */
Gemma.Tutorial.ControlPanel = Ext.extend(Ext.Panel, {
	tips: [],
	targetEls: [],
	currIndex: 0,
	padding: 10,
	bodyStyle: 'background-color: #FFFDE9;line-height:22px',
	defaults: {
		bodyStyle: 'background: transparent;margin-left:auto;margin-right:auto;width:600px;',
		border: false,
		padding: 5
	},
		
	//stateful: true,
	// what describes the state of this panel - in this case it is the "hidden" field
	getState: function(){
		return {
			hidden: this.hidden
		};
	},
	// specify when the state should be saved - in this case after panel was hidden or shown
	stateEvents: ['hide', 'show'],
	
	initComponent: function(){
		this.currIndex = 0;
		Gemma.Tutorial.ControlPanel.superclass.initComponent.call(this);
		this.add([{
			layout: 'hbox',
			flex: 1,
			height: 30,
			items: [{
				html: 'You are now using a tutorial. How fun! click the "next" and "previous" buttons.',
				border: false,
				bodyStyle: 'background: transparent;',
				flex: 1
			}, {
				xtype: 'button',
				cls: 'transparent-btn',
				tooltip: 'Close this tutorial',
				icon: '/Gemma/images/icons/cross.png',
				handler: this.hideTutorial,
				scope: this,
				flex: 0
			}]
		}, {
			ref: 'controlBtns',
			layout: {
				type: 'hbox',
				pack: 'end'
			},
			
			items: [{
				xtype: 'button',
				ref: 'prevBtn',
				text: "Previous",
				disabled: true,
				handler: this.showPrevTip,
				scope: this
			}, {
				xtype: 'button',
				ref: 'nextBtn',
				text: 'Next',
				handler: this.showNextTip,
				scope: this
			}]
		}]);
	},
	/**
	 * 
	 * @param {Object} tipDefinitions
	 */
	addTips: function(tipDefinitions){
		var i;
		var existingTipCount = this.tips.length;
		for (i = 0; i < tipDefinitions.length; i++) {
			var index = existingTipCount+i;
			this.targetEls.push(tipDefinitions[i].element);
			// make tips
			var tip = this.initTip(tipDefinitions[i]);
			tip.tipIndex = index;
			this.tips.push(tip);
			// make nav buttons for tips
			this.controlBtns.insert(index+1, {
				xtype: 'button',
				ref: 'progBtn' + index,
				icon: '/Gemma/images/icons/bullet_black.png',
				cls: 'transparent-btn',
				toggleHandler: function(button, state){
					if (state) {
						button.setIcon('/Gemma/images/icons/bullet_blue.png');
					} else {
						button.setIcon('/Gemma/images/icons/bullet_black.png');
					}
				},
				enableToggle: true,
				handler: this.playTips.createDelegate(this, [index]),
				scope: this
			});
		}
		this.doLayout();
	},
	/**
	 * hides all tips and restarts the series from the index param
	 * @param {Object} index index to start playing tips from
	 */
	playTips: function(index){
		this.hideTips();
		this.currIndex = index;
		this.showTip(this.tips[index]);
	},
	updateBtnDisabling: function(){
		if (this.currIndex === 0) {
			this.controlBtns.prevBtn.disable();
		} else {
			this.controlBtns.prevBtn.enable();
		}
		if (this.currIndex === (this.tips.length - 1)) {
			this.controlBtns.nextBtn.disable();
		} else {
			this.controlBtns.nextBtn.enable();
		}
	},
	showNextTip: function(){
		this.hideTip(this.tips[this.currIndex]);
		this.showTip(this.tips[++this.currIndex]);
	},
	showPrevTip: function(){
		if (this.currIndex) this.hideTip(this.tips[this.currIndex]);
		this.showTip(this.tips[--this.currIndex]);
	},
	hideTutorial: function(){
		this.currIndex = -1;
		this.hideTips();
		this.hide();
		this.fireEvent('tutorialHidden');
		//this.destroy();
	},
	/**
	 * 
	 * @param {Object} tipsToHide [optional] if not specified, will hide all tips
	 */
	hideTips: function(tipsToHide){
		if (!tipsToHide) {
			tipsToHide = this.tips;
		}
		var i;
		for (i = 0; i < tipsToHide.length; i++) {
			this.hideTip(tipsToHide[i]);
		}
	},
	hideTip: function(tip){
		if (!tip) return;
		tip.hide();
		this.controlBtns['progBtn' + tip.tipIndex].toggle(false);
		
	},
	showTip: function(tip){
		if (!tip) return;
		tip.show();
		this.controlBtns['progBtn' + tip.tipIndex].toggle(true);
		this.updateBtnDisabling();
	},
	/**
	 * return an array of all the tips for which the param function returns true
	 * 
	 * @param {Object} func takes a tip as param 
	 * @return {Object[]} array of tips for which the param function returns true or an empty array if none do
	 */
	getTipsBy: function(func){
		var i;
		var trueTips = [];
		for (i = 0; i < this.tips.length; i++) {
			if (func(this.tips[i])) {
				trueTips.push(this.tips[i]);
			}
		}
		return trueTips;
	},
	initTip: function(tipDefinition){
		var element, tipTitle, tipBody, tipConfig;
		element = tipDefinition.element;
		tipTitle = tipDefinition.title;
		tipBody = tipDefinition.text;
		tipConfig = tipDefinition.tipConfig;
		var newX = (tipDefinition.position)?tipDefinition.position.x:null;
		var newY = (tipDefinition.position)?tipDefinition.position.y:null;
		var fromLeft = (tipDefinition.position)?tipDefinition.position.fromLeft:null;
		var fromTop = (tipDefinition.position)?tipDefinition.position.fromTop:null;
		var moveDown = (tipDefinition.position && tipDefinition.position.moveDown)?tipDefinition.position.moveDown:0;
		var moveRight = (tipDefinition.position && tipDefinition.position.moveRight)?tipDefinition.position.moveRight:0;
		// need this for override of onShow
		var topScope = this;
		var defaultConfigs = {
			cls: 'x-tip-yellow',
			anchorToTarget: true,
			anchor: 'right',
			trackMouse: false,
			target: element.el, // The overall target element.
			// overwrite the onShow method to control when the tool tip is shown
			// we don't want its show/hide state to be effected by hovering on the target element	
			onShow: function(){
				// if the tip is supposed to be shown according to the tutorial's controls, then show it
				var onId = (topScope.tips[topScope.currIndex])? topScope.tips[topScope.currIndex].id : -1;
				if ( onId === this.id) {
					element.addClass('highlightToggleBorderOn');
					if (tipDefinition.onShow) {
						tipDefinition.onShow();
					}
					if (this.floating) {
						return this.el.show();
					}
					Ext.Panel.superclass.onShow.call(this);
				}
				// in any other case, don't show it
				return false;
			},
			hidden: true,
			padding: 10,
			shadow: 'frame',
			renderTo: Ext.getBody(), // Render immediately so that tip.body can be referenced prior to the element show.
			html: tipBody,
			title: tipTitle,
			autoHide: false,
			draggable: true, // need this to be true so clicking outside the tip doesn't close it
			closable: true,
			listeners: {
				'hide': function(){
					element.removeClass('highlightToggleBorderOn');
				},
				'afterlayout': function(){
					if (fromLeft && fromLeft !== null && fromTop && fromTop !== null) {
						this.setPosition(fromLeft,fromTop);
					}
					if (newX && newX !== null && newY && newY !== null) {
						this.setPagePosition(newX,newY);
					}
					if ( moveDown !== 0 || moveRight !== 0) {
						var arr = this.getPosition(true);
						this.setPosition(arr[0]+moveRight, arr[1]+moveDown);
					}
				}
			}
		};
		// need to apply configs before creating tooltip to control anchor config
		Ext.apply(defaultConfigs, tipConfig);
		
		var tip = new Ext.ToolTip(defaultConfigs);
		return tip;
	}
});

Ext.reg('Tutorial.ControlPanel', Gemma.Tutorial.ControlPanel);
