import java.sql.*;

public class TestMySQLConnection {
    public static void main(String[] args) {
        System.out.println("=== MySQL Connection Test ===\n");
        
        String url = "jdbc:mysql://localhost:3306/camfu_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "Shivam@9797";
        
        System.out.println("Connection Details:");
        System.out.println("  URL: " + url);
        System.out.println("  User: " + user);
        System.out.println("  Password: " + (password.isEmpty() ? "(empty)" : "***"));
        System.out.println("");
        
        try {
            // Load driver
            System.out.println("[1] Loading MySQL JDBC Driver...");
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("    ✓ Driver loaded");
            System.out.println("");
            
            // Try to connect
            System.out.println("[2] Connecting to MySQL...");
            long startTime = System.currentTimeMillis();
            Connection conn = DriverManager.getConnection(url, user, password);
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("    ✓ Connected successfully (" + elapsed + "ms)!");
            System.out.println("");
            
            // Get database info
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("[3] Database Information:");
            System.out.println("    Driver: " + meta.getDriverName() + " " + meta.getDriverVersion());
            System.out.println("    Database: " + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());
            System.out.println("");
            
            // List databases
            System.out.println("[4] Available Databases:");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW DATABASES");
            while (rs.next()) {
                System.out.println("    - " + rs.getString(1));
            }
            System.out.println("");
            
            // Check cameras table
            System.out.println("[5] Checking camfu_db.cameras table...");
            rs = stmt.executeQuery("SELECT COUNT(*) as count FROM camfu_db.cameras");
            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("    ✓ Found " + count + " camera(s) in database");
            }
            
            // List cameras
            System.out.println("");
            System.out.println("[6] Camera Details:");
            rs = stmt.executeQuery("SELECT camera_id, camera_name, camera_url, status FROM camfu_db.cameras");
            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                System.out.println("    ID: " + rs.getInt("camera_id") + 
                                 " | Name: " + rs.getString("camera_name") + 
                                 " | URL: " + rs.getString("camera_url") +
                                 " | Status: " + rs.getString("status"));
            }
            if (!hasRows) {
                System.out.println("    (No cameras found)");
            }
            
            stmt.close();
            conn.close();
            
            System.out.println("");
            System.out.println("=== All Tests PASSED ✓ ===");
            System.out.println("MySQL is working correctly. The issue is in the application code.");
            
        } catch (ClassNotFoundException e) {
            System.out.println("✗ ERROR: MySQL JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("✗ ERROR: SQL Exception");
            System.out.println("    Error Code: " + e.getErrorCode());
            System.out.println("    SQL State: " + e.getSQLState());
            System.out.println("    Message: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("✗ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
