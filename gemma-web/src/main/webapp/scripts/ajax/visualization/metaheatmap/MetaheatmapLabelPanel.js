// TODO: Should also listen for events from mainarea to highlight label when cell is highlighted			


Ext.namespace('Gemma.Metaheatmap');
/**
 * Class to display labels for genes/conditions. It uses provided tree and orientation to draw the labels.
 * Currently, the group labels reflect tree structure and are drawn as coloured bars. The gene/condtion labels are drawn at the right angle to the group labels.  
 * We may support dendrograms in the future.
 * 
 * Methods:
 *  + draw()
 *  + getLabelItem (x,y) -> returns label object
 *  	+ highlight()
 *  	+ unhighlight()
 *  	+ select()
 *  	+ deselect()
 *  	+ data -- like mini pie chart, gene info, etc
 * 
 * Properties:
 *  + orientation : 'horizontal' or 'vertical'
 *  + tree : condition/gene tree instance. It points to tree inside the application and therefore can change in response to sorting/filtering/zoom/etc.
 *
 * Events:
 *  + label_mouse_in
 *  + label_mouse_out
 * 
 * Mouse:
 *	mousemove : 
 * 	mouseout  : 
 * 
 */
Gemma.Metaheatmap.LabelBox = Ext.extend (Ext.Panel, {
	colors : {
		groupLabelA : 'rgb(179,226,205)', 
		groupLabelB : 'rgb(253,205,172)',
		itemLabelA : 'rgba(203,213,232, 0.5)',
		itemLabelB : 'rgba(203,213,232, 0.9)'
	},
	
	initComponent : function() {
		Ext.apply (this, {
			tree   		: this.tree,
			orientation : this.orientation,
			items: [{
				autoEl : 'canvas',
				ref    : 'boxCanvas',
				xtype  : 'box'
			}]
			
		});
		Gemma.Metaheatmap.LabelBox.superclass.initComponent.apply (this, arguments);
	},
		
	resizeAndPosition : function () {
		
		// when we say 'owner container' here, we mean the visualisation panel, so travel up the container stack until we get to it
		// (this is alternative to this.ownerCt which is dangerous to use because it will break if there are any layout changes made)
		var ownerCt = this.findParentByType('Metaheatmap.VisualizationPanel');
		
		if (!this.amIinitialized) {
			this.initializeMe();		
		}

		var headerHeight = ownerCt.variableWidthCol.boxTopLabels.tree.display.size.height;
		var sideWidth    = ownerCt.fixedWidthCol.boxSideLabels.tree.display.size.height;
		var extraRoom = 20;
		
		if (this.orientation === 'vertical') {
	   		//this.setPosition (0, ownerCt.variableWidthCol.boxTopLabels.tree.display.size.height);	
			this.setWidth (this.tree.display.size.height);					
			this.setHeight (ownerCt.getHeight() - headerHeight - extraRoom);		
			this.boxCanvas.setSize (this.tree.display.size.height, this.tree.display.size.width + 15);
		} else { // horizontal
			//this.setPosition (ownerCt.fixedWidthCol.boxSideLabels.tree.display.size.height, 0);
			this.setHeight (this.tree.display.size.height);					
			this.setWidth (ownerCt.getWidth() - sideWidth - extraRoom);
			this.boxCanvas.setSize (this.tree.display.size.width + 15, this.tree.display.size.height);       		        	   			
		}
	},

	initializeMe : function () {
		this.addEvents('label_mouse_in','label_mouse_out', 'label_click');
		
		// Initialize canvas context.
		this.ctx = Gemma.Metaheatmap.Utils.getCanvasContext (this.boxCanvas.el.dom);
		CanvasTextFunctions.enable (this.ctx);
		
		this.boxCanvas.el.on('mousemove', function(e, t) {
			if (this.lastLabelMouseIn !== null) {
				this.lastLabelMouseIn.item.display.label.drawFn (false);				
				document.body.style.cursor = 'default'; // makes it really slow
				this.fireEvent('label_mouse_out',this.lastLabelMouseIn.label,e,t);
			}
			var x = e.getPageX() - Ext.get(t).getX();
			var y = e.getPageY() - Ext.get(t).getY();
			var label = this.getLabelByXY (x,y);			
			if (label !== null) {
				label.item.display.label.drawFn (true);
				document.body.style.cursor = 'pointer'; // makes it really slow
				this.fireEvent('label_mouse_in',label,e,t);
			}
			this.lastLabelMouseIn = label;			
		}, this);
				
		this.boxCanvas.el.on('mouseout', function(e, t) {
			if (this.lastLabelMouseIn !== null) {
				this.lastLabelMouseIn.item.display.label.drawFn(false);
				this.fireEvent('label_mouse_out',this.lastLabelMouseIn.label,e,t);
			}
			this.lastLabelMouseIn = null;
			document.body.style.cursor = 'default'; // makes it really slow		
		}, this);				

		this.boxCanvas.el.on('click', function(e, t) {
			var x = e.getPageX() - Ext.get(t).getX();
			var y = e.getPageY() - Ext.get(t).getY();
			var label = this.getLabelByXY (x,y);
			if (label !== null) {
				this.onClick(label, e);
			}
		}, this);				

		this.amIinitialized = true;
	},
	
	onClick : function (label, e) {
		// defined at configuration time
	},
	
	draw : function() {
		this.lastLabelMouseIn = null;
		
		if (!this.amIinitialized) {
			this.initializeMe();
		}
		this.clearCanvas_();
		
		this.alternateColorCounter = [];
		for (var i = 0; i < this.tree.root.level; i++) {
			this.alternateColorCounter.push(0);
		}
		
		this.drawColorBars_ (this.tree.root, this.tree);
	},
		
	getLabelByXY : function (x,y) {
		var item;
		if (this.orientation == 'vertical') {
			item = this.tree.findItemByCoordinate (y);
		} else {
			item = this.tree.findItemByCoordinate (x);
		}
		if (item === null) {return null;}
		
		var labelItemObj = {
			'item' : item // gene or condition			
		};
		return labelItemObj;
	},

	// Color bars style.
	drawColorBars_ : function (node, tree) {
		var x, y, size, text, textOrientation, labelColor;
					
		if (node instanceof Gemma.Metaheatmap.TreeLeafNode) {
			if (this.orientation == 'horizontal') {
				textOrientation = 'vertical';
			} else {
				textOrientation = 'horizontal';
			}
			size = 120;
			for (var i = 0; i < node.items.length; i++) {
				if ((this.alternateColorCounter[0] % 2) === 0) {
					labelColor = this.colors.itemLabelA;
				} else {
					labelColor = this.colors.itemLabelB;					
				}
				this.drawItemLabel_ (node.items[i], size, textOrientation, labelColor);
				this.alternateColorCounter[0]++;
			}								
		} else {
			for (var j = 0; j < node.children.length; j++) {
				var child = node.children[j];
				text = child.groupName;
				if (text === null) { text ='null';}

				if ((this.alternateColorCounter[child.level] % 2) === 0) {
					labelColor = this.colors.groupLabelA;
				} else {
					labelColor = this.colors.groupLabelB; 					
				}
				if (this.orientation == 'horizontal') {
					x  	   = child.display.pxlStart;
					y	   = tree.display.size.height - tree.root.display.levelToY[child.level];
					height = tree.root.display.levelToY[child.level] - tree.root.display.levelToY[child.level-1] - 1;
					width  = child.display.pxlSize;
					if (child.display.textOrientation == 'side') {
						textOrientation = 'vertical';
					} else {
						textOrientation = 'horizontal';						
					}
					this.drawGroupLabel_ (x, y, width, height, text, textOrientation, labelColor);				
				} else {
					x	   = tree.display.size.height - tree.root.display.levelToY[child.level];
					y	   = child.display.pxlStart;				
					height = child.display.pxlSize;
					width  = tree.root.display.levelToY[child.level] - tree.root.display.levelToY[child.level-1] - 1;
					if (child.display.textOrientation == 'side') {
						textOrientation = 'horizontal';
					} else {
						textOrientation = 'vertical';						
					}
					this.drawGroupLabel_ (x, y, width, height, text, textOrientation, labelColor);				
				}
				
				this.alternateColorCounter[child.level]++;				
				this.drawColorBars_ (node.children[j], tree);									
			}														
		}		
	},
				
	drawGroupLabel_ : function (x, y, xSize, ySize, text, textOrientation, color) {		
		this.ctx.fillStyle = color;
		this.ctx.fillRect (x, y, xSize, ySize - 1);		

		this.ctx.save();
		this.ctx.beginPath();
		this.ctx.rect (x, y, xSize-1, ySize - 1);				
		this.ctx.clip();			

		var textSize = CanvasTextFunctions.measure (null, 9, text);			
		
		if (textOrientation === 'vertical') {
			if (textSize < ySize) {
				y = y + ySize/2 + textSize/2;
			} else {
				y = y + ySize;
			}
			this.ctx.drawRotatedText (x + xSize/2 + 4, y, 270, 9, 'black', text); 
		} else {
			if (textSize < xSize) {
				x = x + xSize/2 - textSize/2;
			}
			this.ctx.strokeStyle = 'black';
			this.ctx.drawText ('', 9, x, y + ySize/2 + 4, text); 
		}
		
		this.ctx.restore();
	},			
	
	clearCanvas_ : function() {
		this.ctx.canvas.width  = this.boxCanvas.getWidth();
		this.ctx.canvas.height = this.boxCanvas.getHeight();			
		this.ctx.clearRect (0, 0, this.ctx.canvas.width, this.ctx.canvas.height);
	},
	
	onRender : function() {
		Gemma.Metaheatmap.LabelBox.superclass.onRender.apply (this, arguments);		
		this.amIinitialized = false;				
	}	
});

Ext.reg('Metaheatmap.LabelBox', Gemma.Metaheatmap.LabelBox);


Gemma.Metaheatmap.ConditionLabel = {};
Gemma.Metaheatmap.ConditionLabel.constructDrawLabelFunction = function (ctx, item, text, orientation, size, maxSize, backgroundColor) {		
	var x, y, width, height, highlightBox;
	var fns;

	if (item.isSelected) {
		backgroundColor = 'red';
	}

	if (orientation === 'horizontal') {
		x	   = maxSize.width - size; 
		y	   = item.display.pxlStart ;
		width  = size;
		height = item.display.pxlSize;
		highlightBox = {
				width : maxSize.width,
				height : 14,
				x : 0,
				y : Math.max (0, y + height/2 - 7),
				color : item.isSelected ? 'red': 'white',
				fontSize : 13
		};
		fns = Gemma.Metaheatmap.ConditionLabel.makeHorizontalLabelDrawFunction (ctx, item, text, x, y, width, height, highlightBox, backgroundColor);			
	} else {
		x 	   = item.display.pxlStart ;
		y	   = maxSize.height - size; 
		width  = item.display.pxlSize; 
		height = size;
		highlightBox = {
				width : 14,
				height : maxSize.height,
				x : Math.max (0, x + width/2 - 7),
				y : 0,
				color : item.isSelected ? 'red': 'white',
				fontSize : 13
		};
		fns = Gemma.Metaheatmap.ConditionLabel.makeVerticalLabelDrawFunction (ctx, item, text, x, y, width, height, highlightBox, backgroundColor);
	}
	
	return function (isHighlighted) {
		if (isHighlighted) {
			fns.highlight();
		} else {
			fns.draw();
		}
	};
	
};

Gemma.Metaheatmap.ConditionLabel.makeHorizontalLabelDrawFunction = function (ctx, item, text, x, y, width, height, highlightBox, backgroundColor) {				
	//var text 	     = condition.contrastFactorValue;
	var isSelected   = item.isSelected;
	var miniPieValue = item.miniPieValue;
	
	var yCenter = y + height/2;	
	var fontSize = 9;
	var miniPieSize = 8;

	var tinyScale = (height < fontSize);	
					
	var savedLabelImage = null;
	
	return {
		draw : function () {
			if (savedLabelImage === null) {
				ctx.save();
				if (!tinyScale || isSelected) {
					ctx.fillStyle = backgroundColor;
					ctx.fillRect (x, y, width, height);
				}
				if (tinyScale) {
					// We stop drawing text since small text is not distinguishable.
					// Draw barchart instead of pie. 
					ctx.fillStyle = 'gray';
					if (miniPieValue !== null) {
						ctx.moveTo(x+width - 1,y);
						ctx.lineTo(x+width - 1,y);
						ctx.stroke();
						ctx.fillRect ( x + width - 1 - 15 * miniPieValue / 360.0, yCenter, 15 * miniPieValue / 360.0, height);
					}
				} else {
					ctx.beginPath();
					ctx.rect (x, y+0.5, width, height-1);				
					ctx.clip();			
					ctx.strokeStyle = 'black';
					ctx.drawTextRight ('', fontSize, x + width - miniPieSize, yCenter + fontSize/2, text);

					ctx.fillStyle = 'gray';
					if (miniPieValue !== null) {
						ctx.moveTo(x+width - 1,y);
						ctx.lineTo(x+width - 1,y);
						ctx.stroke();
						ctx.fillRect ( x + width - 1 - 15 * miniPieValue / 360.0, yCenter, 15 * miniPieValue / 360.0, height);
					}
					
					
//					if (miniPieValue !== null) {
//						MiniPieLib.drawMiniPie (ctx, x + width + 5, yCenter, miniPieSize, 'gray', miniPieValue);
//					}
				}
				ctx.restore();											
			} else {
				// Restore non-highlighted label if it was previously drawn.
				ctx.putImageData (savedLabelImage, highlightBox.x, highlightBox.y);
			}			
		
		},
	
		highlight : function () {
			if (savedLabelImage === null) {
				savedLabelImage = ctx.getImageData (highlightBox.x, highlightBox.y, highlightBox.width, highlightBox.height);
			}
			ctx.save();
			ctx.beginPath();
			ctx.rect (highlightBox.x, highlightBox.y+0.5, highlightBox.width, highlightBox.height-1);				
			ctx.clip();			
			ctx.fillStyle = highlightBox.color; 
			ctx.fillRect (highlightBox.x, highlightBox.y, highlightBox.width, highlightBox.height);
			ctx.strokeStyle = 'black';		
			ctx.drawTextRight ('', highlightBox.fontSize, x+width-miniPieSize-1, yCenter+highlightBox.fontSize/2, text);
			if (miniPieValue !== null) {
//				MiniPieLib.drawMiniPie (ctx, x+width+5, yCenter, miniPieSize, 'gray', miniPieValue);

				ctx.moveTo(x+width - 1,y);
				ctx.lineTo(x+width - 1,y);
				ctx.stroke();
				ctx.fillRect ( x + width - 1 - 15 * miniPieValue / 360.0, yCenter, 15 * miniPieValue / 360.0, height);

			
			}
			ctx.restore();
		}
	};   		        						
};	

Gemma.Metaheatmap.ConditionLabel.makeVerticalLabelDrawFunction = function (ctx, item, text, x, y, width, height, highlightBox, backgroundColor) {				
	//var text 	   = condition.contrastFactorValue;
	var isSelected = item.isSelected;
	var miniPieValue = item.miniPieValue;
	var miniBarValue = item.miniBarValue;
	
	var xCenter = x + width/2;	
	var fontSize = 9;
	var miniPieSize = 8;

	var tinyScale = (width < fontSize);
	
	var savedLabelImage = null;
	
	return {		
		draw : function () {
			if (savedLabelImage === null) {
				ctx.save();
				if (!tinyScale || isSelected) {
					ctx.fillStyle = backgroundColor;
					ctx.fillRect (x, y, width, height);
				}
				if (tinyScale) {
					// We stop drawing label since small text is not distinguishable.
					ctx.strokeStyle = 'black';		
					if (miniBarValue !== null) {
						ctx.moveTo(x, y + height - 8.5);
						ctx.lineTo(x + width,y + height - 8.5);
						ctx.stroke();
						ctx.fillStyle = 'black';
						ctx.fillRect (x, y + height - 0.5 - miniBarValue, width, miniBarValue);
					}
				} else {
					ctx.beginPath();
					ctx.rect (x+0.5, y, width-1, height);				
					ctx.clip();			
					ctx.strokeStyle = 'black';		
					ctx.drawRotatedText (xCenter + fontSize/2, y + height - miniPieSize - 2, 270, fontSize, 'black', text);
					if (miniBarValue !== null) {
						ctx.moveTo(x, y + height - 8.5);
						ctx.lineTo(x + width,y + height - 8.5);
						ctx.stroke();
						ctx.fillStyle = 'black';
						ctx.fillRect (x, y + height - 0.5 - miniBarValue, width, miniBarValue);
					}
				}
				ctx.restore();											
			} else {
				// Restore non-highlighted label if it was previously drawn.
				ctx.putImageData (savedLabelImage, highlightBox.x, highlightBox.y);
			}			
		},
	
		highlight : function () {
			if (savedLabelImage === null) {
				savedLabelImage = ctx.getImageData (highlightBox.x, highlightBox.y, highlightBox.width, highlightBox.height);
			}
			ctx.save();
			ctx.beginPath();
			ctx.rect (highlightBox.x+0.5, highlightBox.y, highlightBox.width-1, highlightBox.height);				
			ctx.clip();			
			ctx.fillStyle = highlightBox.color; 
			ctx.fillRect (highlightBox.x, highlightBox.y, highlightBox.width, highlightBox.height);
			ctx.strokeStyle = 'black';		
			ctx.drawRotatedText (xCenter + highlightBox.fontSize/2, y + height - miniPieSize - 2, 270, highlightBox.fontSize, 'black', text);
			if (miniPieValue !== null) {
				MiniPieLib.drawMiniPie (ctx, xCenter, y + height - 5, miniPieSize, 'black', miniPieValue);
			}
			ctx.restore();								
		}
	};			
};

//Gemma.Metaheatmap.ConditionLabel.verticalSmall_fns = function (ctx, text, x, y, width, height, maxHeight, backgroundColor, isSelected, miniPieValue) {			
//	var xCenter = x + width/2;	
//
//	var highlightBox = {
//			width : 14,
//			height : maxHeight,
//			x : Math.max (0, xCenter - 7),
//			y : 0
//	};
//
//	if (isSelected) {
//		backgroundColor = 'red';
//	}	
//
//	var savedLabelImage = null;
//		
//	return {
//		
//		draw : function () {
//			if (savedLabelImage !== null) {
//				// Restore non-highlighted label if it was previously drawn.
//				ctx.putImageData (savedLabelImage, highlightBox.x, highlightBox.y);
//			} else {
//				savedLabelImage = ctx.getImageData (highlightBox.x, highlightBox.y, highlightBox.width, highlightBox.height);
//			}
//		},
//				
//		highlight : function () {
//			var fontSize = 12;
//			var miniPieSize = 10;
//
//			ctx.fillStyle = backgroundColor;//'rgb(240,230,140)'; //TODO: 
//			ctx.fillRect (highlightBox.x, highlightBox.y, highlightBox.width, highlightBox.height);
//			ctx.save();
//			ctx.beginPath();
//			ctx.rect (highlightBox.x, highlightBox.y, highlightBox.width, highlightBox.height);				
//			ctx.clip();			
//			ctx.strokeStyle = 'black';		
//			ctx.drawRotatedText (xCenter + fontSize/2, y + height - miniPieSize - 2, 270, fontSize, 'black', text);
//			MiniPieLib.drawMiniPie (ctx, xCenter, y + height - 4, miniPieSize, 'black', miniPieValue);
//			ctx.restore();								
//		}		
//	};	
//};

//if (text === null) { text ='null';}

//Gemma.Metaheatmap.ConditionLabel.constructDrawHorizontalConditionLabelFn_ : function (condition, size, backgroundColor) {
//	var drawLabel, drawHighlightedLabel;
//	var x 		 = this.getWidth();
//	var y 		 = condition.display.pxlEnd; 
//	var width 	 = size; 
//	var height 	 = condition.display.pxlSize; 
//	var availableWidth = this.getWidth();
//	var fontSize = 9;
//	var text = condition.contrastFactorValue;
//
//	var savedImageData = null;
//	
//	if (height < 9) {
//		drawLabel = function () {
//			if (savedImageData !== null) {
//				ctx.putImageData (savedImageData, 0, y - height);
//			} else {
//				savedImageData = ctx.getImageData (0, y - height, availableWidth, height);
//			}				
////			ctx.fillStyle = 'white';
////			ctx.fillRect (x-width, y-6, width, 12);             		        				
//		};
//		drawHighlightedLabel = function () {
//			ctx.strokeStyle = 'black';					
//			ctx.drawTextRight ('', 9, x-10, y+5, text);          		        				
//		};      		        					
//	} else {
//		drawLabel = function () {					
//			if (savedImageData !== null) {
//				ctx.putImageData (savedImageData, 0, y - height);					
//			} else {
//				ctx.fillStyle = backgroundColor;
//				ctx.fillRect (x-width, y-height, width, height);
//				ctx.save();
//				ctx.beginPath();
//				ctx.rect (x-width, y-height, width, height);				
//				ctx.clip();			
//				ctx.strokeStyle = 'black';
//				ctx.drawTextRight ('', fontSize, x-10, y-height/2+4, text);
//				ctx.restore();
//				MiniPieLib.drawMiniPie (ctx, x-9, y-height/2, 8,'black', condition.miniPieValue);
//				savedImageData = ctx.getImageData (0, y - height, availableWidth, height);
//			}
//		};
//		drawHighlightedLabel = function () {
//			ctx.fillStyle = 'rgb(240,230,140)';
//			ctx.fillRect (x-width, y-height, width, height);
//			ctx.save();
//			ctx.beginPath();
//			ctx.rect (x-width, y-height, width, height);				
//			ctx.clip();			
//			ctx.strokeStyle = 'black';
//			ctx.drawTextRight ('', fontSize, x, y, text);
//			ctx.restore();
//		};      		        					
//	}	
//	
//	return function (isHighlighted) {
//		if (isHighlighted) {
//			drawHighlightedLabel();
//		} else {
//			drawLabel();
//		}
//	};
//};




