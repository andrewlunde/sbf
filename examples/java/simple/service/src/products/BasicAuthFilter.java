package products;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.pivotal.labs.cfenv.CloudFoundryEnvironment;
import io.pivotal.labs.cfenv.CloudFoundryEnvironmentException;
import io.pivotal.labs.cfenv.CloudFoundryService;

@WebFilter("/*")
public class BasicAuthFilter implements Filter {

    private SBSSClient sbssClient;

    @Override
    public void destroy() {
        // no cleanup to do
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        try {
            if (isValid(getRequestCredentials(req))) {
                chain.doFilter(request, response);
                return;
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        HttpServletResponse res = (HttpServletResponse) response;
        res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Override
    public void init(FilterConfig fConfig) throws ServletException {
        CloudFoundryService sbssService;
        String sbssServiceName;
        try {
            sbssServiceName = getSBSSServiceName();
            sbssService = getSBSSService(sbssServiceName);
            this.sbssClient = new SBSSClient(sbssService);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private static String getSBSSServiceName() throws Exception {
        String productsServiceName = System.getenv("SBSS_SERVICE_NAME");
        if (productsServiceName == null) {
            throw new Exception("Could not find 'SBSS_SERVICE_NAME' in environment");
        }
        return productsServiceName;
    }
    
    private static CloudFoundryService getSBSSService(String sbssServiceName)
            throws CloudFoundryEnvironmentException {
        CloudFoundryEnvironment environment = new CloudFoundryEnvironment(System::getenv);
        return environment.getService(sbssServiceName);
    }
    
    private static String[] getRequestCredentials(HttpServletRequest request) throws Exception {
        String authorization = request.getHeader("Authorization");
        if (authorization == null) {
            throw new Exception("No authorization header");
        }
        if (!authorization.toUpperCase().startsWith(HttpServletRequest.BASIC_AUTH)) {
            throw new Exception("Invalid authorization header");
        }
        String credentialsBase64 = authorization.substring(HttpServletRequest.BASIC_AUTH.length() + 1);
        String credentialsString = new String(Base64.getDecoder().decode(credentialsBase64),
                StandardCharsets.ISO_8859_1);
        String[] credentials = credentialsString.split(":", 2);
        if (credentials.length != 2) {
            throw new Exception("Invalid authorization token");
        }
        return credentials;
    }

    private boolean isValid(String[] credentials) {
        String username = credentials[0];
        String password = credentials[1];
        return this.sbssClient.validateCredentials(username, password);
    }
    
}
