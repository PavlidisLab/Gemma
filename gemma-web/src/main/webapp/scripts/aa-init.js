/* 

Initialization script for AjaxAnywhere 

Import this javascript with aa.js 
*/

ajaxAnywhere.onAfterResponseProcessing = function () {
	replaceLinks();
};
function replaceLinks() {
	var zones = document.getElementsByTagName("aazone");

	for (i = 0; i < zones.length; i++)  {
		var zone = zones.item(i).attributes.getNamedItem("zone").nodeValue;
		var tables = zones.item(i).attributes.getNamedItem("tableId").nodeValue;

		// replace all the links in <thead> with onclick's that call AjaxAnywhere
		var sortLinks = $(tables).getElementsByTagName('thead')[0].getElementsByTagName('a');
                            
		ajaxifyLinks(sortLinks,zone);
		// find all nodes with the class 'pagelinks' in a zone
		// and ajaxify those links only
		var zoneNode = zones.item(i);
		
		var linkNodes = [];
		var linkClass = new RegExp('\\bpagelinks\\b');
		var elem = zoneNode.getElementsByTagName('*');
		for (var i = 0; i < elem.length; i++) {
			var classes = elem[i].className;
			if (linkClass.test(classes)) linkNodes.push(elem[i]);
		}
	
		if (linkNodes.length > 0) {
			var pagelinks = linkNodes[0].getElementsByTagName("a");	
			ajaxifyLinks(pagelinks,zone);
		}
	}
}
function ajaxifyLinks(links,zone) {
	for (j = 0; j < links.length; j++) {
		links[j].onclick = function () {
			ajaxAnywhere.getZonesToReload = function () {
				return zone;
			};
			ajaxAnywhere.getAJAX(this.href);
			return false;
		};
	}
}
replaceLinks();

