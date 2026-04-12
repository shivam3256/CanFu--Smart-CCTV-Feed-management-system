import java.sql.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Diagnostic utility to test feed display issues
 */
public class FeedDiagnostic {
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== CamFu Feed Display Diagnostic ===\n");
        
        // 1. Test database connection
        System.out.println("[1] Testing Database Connection...");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String dbUrl = "jdbc:mysql://localhost:3306/camfu_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            Connection conn = DriverManager.getConnection(dbUrl, "root", "Shivam@9797");
            System.out.println("✓ Database connection successful\n");
            
            // 2. Query cameras
            System.out.println("[2] Checking Cameras in Database...");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT camera_id, camera_name, camera_url, status FROM cameras");
            
            int cameraCount = 0;
            while (rs.next()) {
                cameraCount++;
                int id = rs.getInt("camera_id");
                String name = rs.getString("camera_name");
                String url = rs.getString("camera_url");
                String status = rs.getString("status");
                
                System.out.println("Camera #" + id);
                System.out.println("  Name: " + name);
                System.out.println("  URL: " + url);
                System.out.println("  Status: " + status);
                
                // 3. Test URL accessibility
                System.out.println("\n[3] Testing URL Accessibility for " + name + "...");
                testStreamURL(url);
                
                // 4. Test frame endpoint
                System.out.println("\n[4] Testing Frame Endpoint Conversion...");
                String frameEndpoint = convertToFrameEndpoint(url);
                System.out.println("  Original URL: " + url);
                System.out.println("  Frame endpoint: " + frameEndpoint);
                testFrameEndpoint(frameEndpoint);
                
                System.out.println("\n" + "-".repeat(50) + "\n");
            }
            
            if (cameraCount == 0) {
                System.out.println("✗ NO CAMERAS FOUND IN DATABASE!");
                System.out.println("  Please add a camera first using the application.");
            } else {
                System.out.println("✓ Found " + cameraCount + " camera(s)");
            }
            
            conn.close();
            
        } catch (Exception e) {
            System.out.println("✗ Database connection failed!");
            e.printStackTrace();
        }
    }
    
    private static void testStreamURL(String streamUrl) {
        try {
            URL url = new URL(streamUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            
            int responseCode = conn.getResponseCode();
            System.out.println("  Stream URL Response: " + responseCode);
            
            if (responseCode == 200) {
                System.out.println("  ✓ Stream URL is accessible");
            } else {
                System.out.println("  ✗ Stream URL returned error code: " + responseCode);
            }
            
            conn.disconnect();
        } catch (Exception e) {
            System.out.println("  ✗ Failed to access stream URL: " + e.getMessage());
        }
    }
    
    private static void testFrameEndpoint(String frameUrl) {
        try {
            URL url = new URL(frameUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "CamFu/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();
            
            int responseCode = conn.getResponseCode();
            System.out.println("  Frame endpoint response: " + responseCode);
            
            if (responseCode == 200) {
                long contentLength = conn.getContentLength();
                String contentType = conn.getContentType();
                System.out.println("  ✓ Frame endpoint is accessible");
                System.out.println("  Content-Type: " + contentType);
                System.out.println("  Content-Length: " + contentLength + " bytes");
                
                if (contentLength <= 0) {
                    System.out.println("  ⚠ WARNING: No content received!");
                }
            } else {
                System.out.println("  ✗ Frame endpoint returned error code: " + responseCode);
            }
            
            conn.disconnect();
        } catch (Exception e) {
            System.out.println("  ✗ Failed to access frame endpoint: " + e.getMessage());
        }
    }
    
    private static String convertToFrameEndpoint(String streamUrl) {
        if (streamUrl.contains("/video")) {
            return streamUrl.replace("/video", "/shot.jpg");
        } else {
            return (streamUrl.endsWith("/") ? streamUrl : streamUrl + "/") + "shot.jpg";
        }
    }
}
