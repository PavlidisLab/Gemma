<%@ include file="/common/taglibs.jsp"%>

<menu:useMenuDisplayer name="Velocity" config="WEB-INF/classes/cssHorizontalMenu.vm" permissions="rolesAdapter">
	<ul id="primary-nav" class="menuList">
		<li class="pad">
			&nbsp;
		</li>
		<menu:displayMenu name="MainMenu" />
		<menu:displayMenu name="ResourcesMenu" />
		<menu:displayMenu name="UserMenu" />
		<menu:displayMenu name="AdminMenu" />
	</ul>
</menu:useMenuDisplayer>

<script type="text/javascript">
/*<![CDATA[*/
var navItems = document.getElementById("primary-nav").getElementsByTagName("li");

for (var i=0; i<navItems.length; i++) {
    if(navItems[i].className == "menubar") {
        navItems[i].onmouseover=function() { this.className += " over"; }
        navItems[i].onmouseout=function() { this.className = "menubar"; }
    }
}
/*]]>*/
</script>
