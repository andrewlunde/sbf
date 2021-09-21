package consumer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.sap.xs2.security.container.SecurityContext;

import io.pivotal.labs.cfenv.CloudFoundryEnvironment;
import io.pivotal.labs.cfenv.CloudFoundryEnvironmentException;
import io.pivotal.labs.cfenv.CloudFoundryService;

@WebServlet("/products")
public class Consumer extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private String productsServiceName;
	private CloudFoundryService productsService;
	private Map<String, String> uaaConfig;

	private static String getProductsServiceName() throws Exception {
		String productsServiceName = System.getenv("PRODUCTS_SERVICE_NAME");
		if (productsServiceName == null) {
			throw new Exception("Could not find 'PRODUCTS_SERVICE_NAME' in environment");
		}
		return productsServiceName;
	}

	private static CloudFoundryService getProductsService(String productsServiceName) throws CloudFoundryEnvironmentException {
		CloudFoundryEnvironment environment = new CloudFoundryEnvironment(System::getenv);
		return environment.getService(productsServiceName);
	}

	private Map<String, String> getUaaConfig() throws CloudFoundryEnvironmentException {
		Map<String, Object> credentials = this.productsService.getCredentials();
		Object uaa = credentials.get("uaa");
		if (uaa == null) {
			throw new CloudFoundryEnvironmentException(
					"Could not find 'credentials.uaa' in '" + this.productsServiceName + "' service");
		}
		@SuppressWarnings("unchecked")
		Map<String, String> uaaMap = (Map<String, String>) uaa;
		return uaaMap;
	}

	private static void checkHttpOk(HttpResponse<JsonNode> uaaResponse, String grantType) throws Exception {
		if (uaaResponse.getStatus() == HttpServletResponse.SC_OK) {
			return;
		}
		String errorMessage = "Expected uaa reponse 200 on grant_type=" + grantType + ", got "
				+ String.valueOf(uaaResponse.getStatus());
		System.out.println(errorMessage);
		System.out.println(uaaResponse.getBody().toString());
		throw new Exception(errorMessage);
	}
	
	// This function is a workaround for missing feature in the security library.
	// It will be replaced in the future.
	private String getToken() throws Exception {
		String clientId = this.uaaConfig.get("clientid");
		String clientSecred = this.uaaConfig.get("clientsecret");
		String uaaUrl = this.uaaConfig.get("url");
		String userTokenUrlPath = "/oauth/token?grant_type=user_token&response_type=token&client_id=" +
				URLEncoder.encode(clientId, "UTF-8");
		String userTokenUrl = new URL(new URL(uaaUrl), userTokenUrlPath).toString();
		// Workaround to get the token as a String
		String token = SecurityContext.getUserInfo().getToken("SYSTEM", "JobScheduler");
		HttpResponse<JsonNode> uaaUserTokenResponse = Unirest
				.post(userTokenUrl)
				.header("Authorization", "Bearer " + token)
				.asJson();
		checkHttpOk(uaaUserTokenResponse, "user_token");
		Object refreshTokenObject = uaaUserTokenResponse.getBody().getObject().get("refresh_token");
		if (refreshTokenObject == null) {
			throw new Exception("No refresh_token in UAA response");
		}
		String refreshToken = refreshTokenObject.toString();
		String refreshTokenPath = "/oauth/token?grant_type=refresh_token&refresh_token=" +
				URLEncoder.encode(refreshToken, "UTF-8");
		String refreshTokenUrl = new URL(new URL(uaaUrl), refreshTokenPath).toString();
		HttpResponse<JsonNode> uaaRefreshTokenResponse = Unirest
				.post(refreshTokenUrl)
				.basicAuth(clientId, clientSecred)
				.asJson();
		checkHttpOk(uaaRefreshTokenResponse, "refresh_token");
		Object accessTokenObject = uaaRefreshTokenResponse.getBody().getObject().get("access_token");
		if (accessTokenObject == null) {
			throw new Exception("No access_token in UAA response");
		}
		return accessTokenObject.toString();
	}

	private HttpResponse<String> requestService(String token) throws UnirestException, MalformedURLException {
		String productServiceRootUrl = this.productsService.getCredentials().get("url").toString();
		String productServiceUrl = new URL(new URL(productServiceRootUrl), "/products").toString(); 
		return Unirest.get(productServiceUrl).header("Authorization", "Bearer " + token).asString();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HttpResponse<String> serviceResponse;
		try {
			String token = getToken();
			serviceResponse = requestService(token);
		} catch (Exception e) {
			response.getWriter().append(e.getMessage());
			return;
		}
		response.setStatus(serviceResponse.getStatus());
		response.getWriter().append(serviceResponse.getBody().toString());
	}

	@Override
	public void init() throws ServletException {
		try {
			this.productsServiceName = getProductsServiceName();
			this.productsService = getProductsService(this.productsServiceName);
			this.uaaConfig = getUaaConfig();
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
}