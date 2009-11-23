<%@ include file="/common/taglibs.jsp"%>

<title><fmt:message key="activeUsers.title" /></title>
<content tag="heading">
<fmt:message key="activeUsers.heading" />
</content>
<body id="activeUsers" />
	<security:authorize ifAllGranted="GROUP_ADMIN">
		<p>
			<fmt:message key="activeUsers.message" />
			(FIXME, this is all users)
		</p>




		<display:table name="users" id="user" cellspacing="0" cellpadding="0" defaultsort="1" class="table" pagesize="50"
			requestURI="">

			<display:column property="userName" escapeXml="true" style="width: 30%" titleKey="activeUsers.userName"
				sortable="true" />
			<display:column titleKey="activeUsers.fullName" sortable="true">

				<c:if test="${not empty user.email}">
					<a href="mailto:<c:out value="${user.email}"/>"> <img src="<c:url value="/images/iconEmail.gif"/>"
							alt="<fmt:message key="icon.email"/>" styleClass="icon" /> </a>
				</c:if>
			</display:column>

			<display:setProperty name="basic.empty.showtable" value="true" />
			<display:setProperty name="paging.banner.item_name" value="user" />
			<display:setProperty name="paging.banner.items_name" value="users" />

		</display:table>
	</security:authorize>