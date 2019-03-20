package server;

import com.mysql.cj.ServerPreparedQueryTestcaseGenerator;

import java.io.Console;
import java.sql.*;
import java.util.Scanner;

public class Database {

    private Connection conn = null;
    private String username = null;
    private String password = null;
    private boolean mask = false;
    private char[] maskedPassword = null;

    public boolean connectDB(){
        try{
            Scanner scanner = new Scanner(System.in);
            if(scanner == null){
                System.out.println("Scanner is null.");
                return false;
            }
            System.out.print("Do you want to mask input (y/n): ");
            String response = scanner.nextLine();
            if(response.charAt(0) == 'y' || response.charAt(0) == 'Y') {
                mask = true;
            }

            if(mask == true) {
                Console console = System.console();
                if(console == null){
                    System.out.println("Console is null. Run the program in terminal.");
                    return false;
                }
                username = console.readLine("Please enter your name:");
                maskedPassword = console.readPassword("Please enter your password:");
            } else {
                System.out.print("Please enter your username: ");
                username = scanner.nextLine();
                System.out.print("Please enter your password: ");
                password = scanner.nextLine();
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            return false;
        }

        try {
            if(mask == true) {
                conn = DriverManager.getConnection("jdbc:mysql://" + "78.46.43.55" + ":3306/", username, new String(maskedPassword));
                System.out.println("Database Connected...");
            } else {
                conn = DriverManager.getConnection("jdbc:mysql://" + "78.46.43.55" + ":3306/", username, password);
                System.out.println("Database Connected...");
            }
        } catch (SQLException e){
            System.out.println(e.getMessage());
            //e.printStackTrace();
            return false;
        }
        return true;
    }

    public void disconnectDB(){
        try{
            conn.close();
        } catch(SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("Vendor: " + e.getErrorCode());

        }
    }

    /**
     * addLobby
     * calls the add lobby procedure from the database
     * @param code the lobby code
     * @param uri the playlist uri
     * @return true if succesful, else false
     */
    public boolean addLobby(String code, String uri) {
        CallableStatement stmt = null;
        ResultSet rs = null;
        String query = "{ call addLobby(?, ?) }";
        try {
            stmt = conn.prepareCall(query);
            stmt.setString(1, code);
            stmt.setString(2, uri);
            rs = stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * removeLobby
     * calls the remove lobby procedure from the database
     * @param code the lobby code
     * @return true if succesful, else false
     */
    public boolean removeLobby(String code) {
        CallableStatement stmt = null;
        ResultSet rs = null;
        String query = "{ call removeLobby(?) }";
        try {
            stmt = conn.prepareCall(query);
            stmt.setString(1, code);
            rs = stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * addUser
     * calls the addUser procedure from the database
     * @param user_id the user id
     * @param code the lobby code
     * @param is_host user host status
     * @return true if successful, else false
     */
    public boolean addUser(String user_id, String code, boolean is_host) {
        CallableStatement stmt = null;
        String query = "{ call addUser(?, ?, ?) }";
        return userHelp(stmt, query, user_id, code, is_host);
    }

    /**
     * removeUser
     * calls the removeUser procedure from the database
     * @param user_id the user id
     * @param code the lobby code
     * @param is_host user host status
     * @return true if successful, else false
     */
    public boolean removeUser(String user_id, String code, boolean is_host) {
        CallableStatement stmt = null;
        String query = "{ call removeUser(?, ?, ?) }";
        return userHelp(stmt, query, user_id, code, is_host);
    }

    /**
     * userHelp
     * deals with the statements prepared by remove and addUser
     * @param stmt the sql statement
     * @param query the query to be executed
     * @param user_id user id
     * @param code lobby code
     * @param is_host user host status
     * @return true if successful, else false
     */
    private boolean userHelp(CallableStatement stmt, String query, String user_id, String code, boolean is_host) {
        ResultSet rs;
        try {
            stmt = conn.prepareCall(query);
            stmt.setString(1, user_id);
            stmt.setString(2, code);
            if (is_host)
                stmt.setInt(3, 1);
            else
                stmt.setInt(3, 0);
            rs = stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * getLobbySize
     * returns the size of a lobby
     * @param code the lobby code
     * @return the size of the lobby
     */
    public int getLobbySize(String code) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String query = "select currentsize from lobby where code = ?";
        int ret;
        try {
            conn.setAutoCommit(false);
            stmt = conn.prepareStatement(query);
            stmt.setString(1, code);
            rs = stmt.executeQuery();
            conn.commit();
            ret = rs.getInt("currensize");
        }
        catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return ret;
    }

    /**
     * getURI
     * retrieves the playlist uri of a lobby
     * @param code the lobby code
     * @return the playlist uri
     */
    public String getURI(String code) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String query = "select playlist_uri from lobby where code = ?";
        String ret;
        try {
            conn.setAutoCommit(false);
            stmt = conn.prepareStatement(query);
            stmt.setString(1, code);
            rs = stmt.executeQuery();
            conn.commit();
            ret = rs.getString("playlist_uri");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }
}
