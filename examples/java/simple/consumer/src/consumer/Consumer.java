package consumer;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.pivotal.labs.cfenv.CloudFoundryEnvironment;
import io.pivotal.labs.cfenv.CloudFoundryEnvironmentException;
import io.pivotal.labs.cfenv.CloudFoundryService;

@WebServlet("/products")
public class Consumer extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private String username;
	private String password;
	private String productsServiceUrl;

	private static String getProductsServiceName() throws Exception {
		String productsServiceName = System.getenv("PRODUCTS_SERVICE_NAME");
		if (productsServiceName == null) {
			throw new Exception("Could not find 'PRODUCTS_SERVICE_NAME' in environment");
		}
		return productsServiceName;
	}

	private static CloudFoundryService getProductsService(String productsServiceName)
			throws CloudFoundryEnvironmentException {
		CloudFoundryEnvironment environment = new CloudFoundryEnvironment(System::getenv);
		return environment.getService(productsServiceName);
	}

	private HttpResponse<String> requestService() throws UnirestException {
		return Unirest.get(this.productsServiceUrl).basicAuth(this.username, this.password).asString();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpResponse<String> serviceResponse;
		try {
			serviceResponse = requestService();
		} catch (Exception e) {
			response.getWriter().append(e.getMessage());
			return;
		}
		response.setStatus(serviceResponse.getStatus());
		response.getWriter().append(serviceResponse.getBody().toString());
	}

	@Override
	public void init() throws ServletException {
		CloudFoundryService productsService;
		String productsServiceName;
		try {
			productsServiceName = getProductsServiceName();
			productsService = getProductsService(productsServiceName);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		Map<String, Object> credentials = productsService.getCredentials();
		Object usernameObject = credentials.get("username");
		Object passwordObject = credentials.get("password");
		Object urlObject = credentials.get("url");
		if (usernameObject == null || passwordObject == null || urlObject == null) {
			String message = "Expected '" + productsServiceName
					+ "' service to have 'url', 'username' and 'password'";
			throw new ServletException(message);
		}
		this.username = usernameObject.toString();
		this.password = passwordObject.toString();
		this.productsServiceUrl = urlObject.toString();
	}
}