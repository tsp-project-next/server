package server;

import java.io.Console;
import java.sql.*;
import java.util.Scanner;

public class Database {

    private Connection conn = null;
    private String username = null;
    private String password = null;
    private boolean mask = false;
    private char[] maskedPassword = null;

    private static boolean debugBuild = false;

    public boolean connectDB(String u, String p) {
        try {

            username = u;
            password = p;

            if (username == null || password == null)
                return false;

            conn = DriverManager.getConnection("jdbc:mysql://" + "78.46.43.55" + ":3306/pnexttest", username, password);
            System.out.println("Database Connected...");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            //e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * connectDB
     * attempts to connect to the lobby
     * prompts to mask input or not to mask input
     *
     * @return true if successful, else false
     */
    public boolean connectDB() {
        try {
            Scanner scanner = new Scanner(System.in);
            if (scanner == null) {
                System.out.println("Scanner is null.");
                return false;
            }
            System.out.print("Do you want to mask input (y/n): ");
            String response = scanner.nextLine();
            if (response.charAt(0) == 'y' || response.charAt(0) == 'Y') {
                mask = true;
            }

            if (mask == true) {
                Console console = System.console();
                if (console == null) {
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
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        try {
            if (mask == true) {
                if (debugBuild) {
                    conn = DriverManager.getConnection("jdbc:mysql://" + "78.46.43.55" + ":3306/pnexttest", username, new String(maskedPassword));
                    System.out.println("Database Connected Test Build...");
                } else {
                    conn = DriverManager.getConnection("jdbc:mysql://" + "78.46.43.55" + ":3306/pnext", username, new String(maskedPassword));
                    System.out.println("Database Connected...");
                }
            } else {
                if (debugBuild) {
                    conn = DriverManager.getConnection("jdbc:mysql://" + "78.46.43.55" + ":3306/pnexttest", username, password);
                    System.out.println("Database Connected Test Build...");
                } else {
                    conn = DriverManager.getConnection("jdbc:mysql://" + "78.46.43.55" + ":3306/pnext", username, password);
                    System.out.println("Database Connected...");
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            //e.printStackTrace();
            return false;
        }
        return true;
    }

    public void disconnectDB() {
        try {
            conn.close();
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("Vendor: " + e.getErrorCode());

        }
    }

    public Connection getConn() {
        return conn;
    }

    /**
     * addLobby
     * calls the add lobby procedure from the database
     *
     * @param code the lobby code
     * @param uri  the playlist uri
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
     *
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
     *
     * @param user_id the user id
     * @param code    the lobby code
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
     *
     * @param user_id the user id
     * @return true if successful, else false
     */
    public boolean removeUser(String user_id) {
        CallableStatement stmt = null;
        String query = "{ call removeUser(?) }";
        try {
            stmt = conn.prepareCall(query);
            stmt.setString(1, user_id);
            stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * userHelp
     * deals with the statements prepared by remove and addUser
     *
     * @param stmt    the sql statement
     * @param query   the query to be executed
     * @param user_id user id
     * @param code    lobby code
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
     * editUser
     * calls the editUser procedure from the database
     *
     * @param user_id the user to be edited
     * @param code    the code of the destination lobby
     * @param is_host whether or not the user will become the host
     * @return true if successful, else false
     */
    public boolean editUser(String user_id, String code, boolean is_host) {
        CallableStatement stmt;
        String query = "{ call editUser(?, ?, ?) }";
        try {
            stmt = conn.prepareCall(query);
            stmt.setString(1, user_id);
            stmt.setString(2, code);
            if (is_host)
                stmt.setInt(3, 1);
            else
                stmt.setInt(3, 0);
            stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * getLobbySize
     * returns the size of a lobby
     *
     * @param code the lobby code
     * @return the size of the lobby
     */
    public int getLobbySize(String code) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String query = "select currentsize from lobby where code = ?";
        int ret;
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, code);
            rs = stmt.executeQuery();
            ret = rs.getInt("currensize");
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return ret;
    }

    /**
     * getURI
     * retrieves the playlist uri of a lobby
     *
     * @param code the lobby code
     * @return the playlist uri
     */
    public String getURI(String code) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String query = "select playlist_uri from lobby where code = ?";
        String ret;
        try {
            stmt = conn.prepareStatement(query);
            stmt.setString(1, code);
            rs = stmt.executeQuery();
            rs.next();
            ret = rs.getString("playlist_uri");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return ret;
    }

    /**
     * deleteTableRows
     * deletes all rows in a table
     *
     * @param table the table name to have its rows deleted
     * @return the number of rows deleted
     */
    public int deleteTableRows(String table) {
        PreparedStatement stmt = null;
        int rows;
        String query = "delete from " + table;
        try {
            stmt = conn.prepareStatement(query);
            rows = stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return rows;
    }

    /**
     * printTableRows
     * prints the rows of a given table
     *
     * @param table table to be printed
     * @return the number of rows in the table
     */
    public int printTableRows(String table) {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String query = "select * from " + table;
        int rowCount = 0;

        try {
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                if (table.toLowerCase().equals("lobby")) {
                    System.out.println(rs.getString(1) + " " + rs.getString(2)
                            + " " + rs.getInt(3) + " " + rs.getInt(4));
                    rowCount++;
                } else if (table.toLowerCase().equals("users")) {
                    System.out.println(rs.getString(1) + " " + rs.getString(2)
                            + " " + rs.getInt(3));
                    rowCount++;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return rowCount;
    }

    /**
     * getBlacklist
     * retrieves the uris of all blacklisted items in the database
     *
     * @param code the code of the lobby
     * @return an array of uris
     */
    public String[] getBlacklist(String code) {
        PreparedStatement stmt;
        ResultSet rs;
        String query = "select uri from blacklist where code = ?";
        String sizeQuery = "select count(*) from blacklist";
        String[] data;
        try {
            stmt = conn.prepareStatement(sizeQuery);
            rs = stmt.executeQuery();
            rs.next();
            data = new String[rs.getInt("count(*)")];

            stmt = conn.prepareStatement(query);
            stmt.setString(1, code);
            rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                data[count] = rs.getString("uri");
                count++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return data;
    }


    /**
     * addBlacklist
     * calls the addBlacklist procedure in the database
     *
     * @param uri  the playlist uri
     * @param code the lobby code
     * @return true if successful, else false
     */
    public boolean addBlacklist(String uri, String code) {
        CallableStatement stmt;
        String query = "{ call addBlacklist(?, ?) }";
        try {
            stmt = conn.prepareCall(query);
            stmt.setString(1, uri);
            stmt.setString(2, code);
            stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * removeBlacklist
     * calls the removeBlacklist procedure in the database
     *
     * @param uri  the playlist uri
     * @param code the lobby code
     * @return true if successful, else false
     */
    public boolean removeBlacklist(String uri, String code) {
        CallableStatement stmt;
        String query = "{ call removeBlacklist(?, ?) }";
        try {
            stmt = conn.prepareCall(query);
            stmt.setString(1, uri);
            stmt.setString(2, code);
            stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /**
     * isBlacklisted
     * tests to see if a uri is in a lobby's blacklist
     *
     * @param uri  the uri to be tested
     * @param code the lobby code
     * @return true if the uri is in the blacklist, else false
     */
    public boolean isBlacklisted(String uri, String code) {
        String[] blacklist = getBlacklist(code);
        if (blacklist == null)
            return false;
        for (int i = 0; i < blacklist.length; i++) {
            System.out.println(blacklist[i]);
            if (blacklist[i] == null)
                continue;
            if (blacklist[i].equals(uri)) {
                return true;
            }
        }
        return false;
    }
}
