<%@ include file="/common/taglibs.jsp"%>


<security:authorize ifAnyGranted="GROUP_ADMIN">

	<p>
		Maintenance mode simply puts a notice on every page that
		things might be broken.
	</p>

	<input type="button" style="color:BB3333" name="start"
		onclick="location.href='maintenanceMode.html?start=1'"
		value="Enter Maintenance Mode">

	<input type="button" style="color:BB3333" name="start"
		onclick="location.href='maintenanceMode.html?stop=1'"
		value="Exit Maintenance Mode">
		
</security:authorize>
<security:authorize ifNotGranted="GROUP_ADMIN">
	<p>
		Permission denied.
	</p>
</security:authorize>
