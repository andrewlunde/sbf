package service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.xs2.security.container.SecurityContext;
import com.sap.xs2.security.container.UserInfo;
import com.sap.xs2.security.container.UserInfoException;

@WebServlet("/products")
public class Products extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static void pringUserInfo() throws UserInfoException {
		UserInfo userInfo = SecurityContext.getUserInfo();
		System.out.println("Service instance id: " + userInfo.getCloneServiceInstanceId());
		System.out.println("Caller tenant id: " + userInfo.getIdentityZone());
		System.out.println("Token grant type: " + userInfo.getGrantType());
		System.out.println("Calling app has name " + userInfo.getAdditionalAuthAttribute("application_name")
				+ " and id " + userInfo.getAdditionalAuthAttribute("application_id") + ".");
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json");
		try {
			pringUserInfo();
		} catch (UserInfoException e) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().append("{\"error\": \"").append(e.getMessage()).append("\"}");
			e.printStackTrace();
			return;
		}
		// Some random JSON response
		response.getWriter().append("{ \"name\": \"Beer\" }");
	}

}
