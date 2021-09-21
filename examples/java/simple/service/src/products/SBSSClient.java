package products;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import io.pivotal.labs.cfenv.CloudFoundryService;

public class SBSSClient {

    private static String MYBINDINGS_VIEW_NAME = "SYS_XS_SBSS.MYBINDINGS";
    private static String HANA_STATEMENT = "SELECT TOP 1 INSTANCE_ID, BINDING_ID FROM " + MYBINDINGS_VIEW_NAME;

    private String dbUrl;

    SBSSClient(CloudFoundryService sbssService) throws Exception {
        Object dbUrlObject = sbssService.getCredentials().get("url");
        if (dbUrlObject == null) {
            throw new Exception("Expected '" + sbssService.getName() + "' service to have 'url'");
        }
        this.dbUrl = dbUrlObject.toString();
        if (!this.dbUrl.startsWith("jdbc:sap")) {
            throw new Exception("Invalid HANA SBSS service credentials");
        }
    }

    public boolean validateCredentials(String username, String password) {
        try (Connection connection = DriverManager.getConnection(this.dbUrl, username, password);
                ResultSet resultSet = connection.prepareStatement(HANA_STATEMENT).executeQuery()) {
            if (!resultSet.next()) {
                throw new SQLException("Could not fetch instance id and binding id");
            }
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("Service info:").append(System.lineSeparator());
            logMessage.append("instance id: ").append(resultSet.getNString(1)).append(System.lineSeparator())
                    .append("binding id: ").append(resultSet.getNString(2));
            System.out.println(logMessage);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
