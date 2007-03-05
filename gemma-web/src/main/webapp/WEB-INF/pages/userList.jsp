<%@ include file="/common/taglibs.jsp"%>

<title><fmt:message key="userList.title" /></title>
<content tag="heading">
<fmt:message key="userList.heading" />
</content>

<c:set var="buttons">
	<button type="button" style="margin-right: 5px"
		onclick="location.href='<c:url value="/editUser.html"/>?method=Add&from=list'">
		<fmt:message key="button.add" />
	</button>

	<button type="button"
		onclick="location.href='<c:url value="/mainMenu.html" />'">
		<fmt:message key="button.cancel" />
	</button>
</c:set>

<%-- <display:table name="${userList}" cellspacing="0" cellpadding="0" --%>
<display:table name="userList" cellspacing="0" cellpadding="0"
	requestURI="" defaultsort="1" id="users" pagesize="25">

	<%-- Table columns --%>
	<display:column property="userName" sortable="true"
		url="/editUser.html?from=list" paramId="userName"
		paramProperty="userName" titleKey="user.userName" />
	<display:column property="name" sortable="true" titleKey="user.name" />
	<display:column property="lastName" sortable="true"
		titleKey="user.lastName" />
	<display:column property="email" sortable="true" autolink="true"
		titleKey="user.email" />

	<display:setProperty name="paging.banner.item_name" value="user" />
	<display:setProperty name="paging.banner.items_name" value="users" />

</display:table>

<c:out value="${buttons}" escapeXml="false" />

