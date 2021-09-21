package consumer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import io.pivotal.labs.cfenv.CloudFoundryEnvironment;
import io.pivotal.labs.cfenv.CloudFoundryEnvironmentException;
import io.pivotal.labs.cfenv.CloudFoundryService;

@WebServlet("/products")
public class Consumer extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private String productsServiceName;
	private CloudFoundryService productsService;
	private Map<String, String> uaaConfig;
	private String additionalProperties;
	private String tokenRequestUrl;

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

	private static String getAdditionalPropertiesAsJSON() throws JSONException {
		JSONObject json = new JSONObject();
		JSONObject vcapApplication = new JSONObject(System.getenv("VCAP_APPLICATION"));

		json.put("application_id", vcapApplication.get("application_id").toString());
		json.put("application_name", vcapApplication.get("application_name").toString());

		// Here the application can pass arbitrary data to the service.
		// In this case we add application id and application name.
		JSONObject result = new JSONObject();
		result.put("az_attr", json); // The "az_attr" is defined by UAA.
		return result.toString();
	}

	private String getToken() throws Exception {
		HttpResponse<JsonNode> jsonResponse = Unirest.post(this.tokenRequestUrl)
				.header("accept", "application/json")
				.field("client_id", this.uaaConfig.get("clientid"))
				.field("client_secret", this.uaaConfig.get("clientsecret"))
				.field("grant_type", "client_credentials")
				.field("response_type", "token")
				.field("authorities", this.additionalProperties)
				.asJson();
		if (jsonResponse.getStatus() != HttpStatus.SC_OK) {
			throw new Exception("Invalid response from UAA. Status code: " + String.valueOf(jsonResponse.getStatus()));
		}
		JSONObject response = jsonResponse.getBody().getObject();
		Object accessToken = response.get("access_token");
		if (accessToken == null) {
			throw new Exception("No access token found. Response from UAA: " + response.toString());
		}
		return accessToken.toString();
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
			this.tokenRequestUrl = new URL(new URL(this.uaaConfig.get("url")), "/oauth/token").toString();
			this.additionalProperties = getAdditionalPropertiesAsJSON();
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
}
