package server;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.*;
import java.util.Scanner;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.*;

import static org.junit.Assert.*;

public class ServerTest {

    String code = "abcd";
    String userID = "test";
    String uri = "this.is.a.test";
    static String users = "users";
    static String lobby = "lobby";
    boolean host = false;
    private static Database db = new Database();
    private static Connection conn;
    int lobbySize = 0;
    int usersSize = 0;

    private int getTableSize(String code) {
        PreparedStatement stmt;
        ResultSet rs;
        String query = "select count(*) from " + code;
        int size;
        try {
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            rs.next();
            size = rs.getInt("count(*)");
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
        return size;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        File file = new File("src/test/java/server/creds.test");
        String username = null;
        String password = null;
        try {
            Scanner reader = new Scanner(file);
            username = reader.nextLine();
            password = reader.nextLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        db.connectDB(username, password);
        conn = db.getConn();
    }

    @Before
    public void init() {
        db.deleteTableRows(users);
        db.deleteTableRows(lobby);
        db.deleteTableRows("blacklist");
    }

    @After
    public void finalilze() {
        db.deleteTableRows(users);
        db.deleteTableRows(lobby);
        db.deleteTableRows("blacklist");
    }

    @Test
    public void addLobbyTest() {
        boolean tester = db.addLobby(code, uri);
        assertEquals(true, tester);
        assertEquals(1, getTableSize(lobby));
        if (tester)
            lobbySize++;
    }

    @Test
    public void removeLobbyTest() {
        db.addLobby("RLT1", "uri");
        int size = getTableSize("lobby");
        assertEquals(true, db.removeLobby("RLT1"));
        assertEquals(size - 1, getTableSize(lobby));
    }

    @Test
    public void addUserTest() {
        db.addLobby(code, uri);
        boolean tester = db.addUser(userID, code, host);
        assertEquals(true, tester);
        assertEquals(usersSize + 1, getTableSize(users));
        if (tester)
            usersSize++;
    }

    @Test
    public void editUserTest() {
        boolean tester = db.addUser("test", null, false);
        assertEquals(true, tester);
        tester = db.addLobby("test", "test.io");
        assertEquals(true, tester);
        tester = db.editUser("test", "test", true);
        assertEquals(true, tester);
    }

    @Test
    public void getURITest() {
        db.addLobby("GURI", "GURI");
        assertEquals("GURI", db.getURI("GURI"));
    }

    @Test
    public void printTableRowsTest() {
        db.addLobby("1234", "1234");
        db.addLobby("2345", "2345");
        db.addUser("zxcv", "1234", false);
        db.addUser("qwer", "2345", true);
        usersSize = getTableSize(users);
        lobbySize = getTableSize(lobby);
        assertEquals(usersSize, db.printTableRows(users));
        assertEquals(lobbySize, db.printTableRows(lobby));
    }

    @Test
    public void deleteTableRowsTest() {
        db.addLobby("asdf", "asdf");
        db.addUser("asdf", "asdf", true);
        usersSize = getTableSize(users);
        lobbySize = getTableSize(lobby);
        assertEquals(usersSize, db.deleteTableRows(users));
        assertEquals(lobbySize, db.deleteTableRows(lobby));
        lobbySize = 0;
        usersSize = 0;
    }

    @Test
    public void getBlacklistTest() {
        db.addLobby("asdf", "asdf");
        db.addBlacklist("test.io", "asdf");
        db.addBlacklist("io.test", "asdf");
        String [] actual = db.getBlacklist("asdf");
        assertEquals(true, contains(actual, "test.io"));
        assertEquals(true, contains(actual, "io.test"));
    }

    private boolean contains(String [] array, String tester) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(tester))
                return true;
        }
        return false;
    }

    @Test
    public void addBlacklistTest() {
        int blSize;
        int expectedSize = 5;
        db.addLobby("asdf", "asdf");
        assertEquals(true, db.addBlacklist("test.io", "asdf"));
        assertEquals(true, db.addBlacklist("tester.io", "asdf"));
        assertEquals(true, db.addBlacklist("asdf.io", "asdf"));
        assertEquals(true, db.addBlacklist("fdsa.io", "asdf"));
        assertEquals(true, db.addBlacklist("qwer.io", "asdf"));
        blSize = getTableSize("blacklist");
        assertEquals(expectedSize, blSize);
    }

    @Test
    public void removeBlacklistTest() {
        db.addLobby("asdf", "asdf");

        db.addBlacklist("test.io", "asdf");
        db.addBlacklist("tester.io", "asdf");
        db.addBlacklist("asdf.io", "asdf");
        db.addBlacklist("fdsa.io", "asdf");
        db.addBlacklist("qwer.io", "asdf");

        assertEquals(true, db.removeBlacklist("test.io", "asdf"));
        assertEquals(true, db.removeBlacklist("tester.io", "asdf"));
        assertEquals(true, db.removeBlacklist("asdf.io", "asdf"));
        assertEquals(true, db.removeBlacklist("fdsa.io", "asdf"));
        assertEquals(true, db.removeBlacklist("qwer.io", "asdf"));
        assertEquals(0, getTableSize("blacklist"));
    }

    @Test
    public void isBlacklistedTest() {
        db.addLobby("asdf", "asdf");

        db.addBlacklist("test.io", "asdf");
        db.addBlacklist("io.test", "asdf");

        assertEquals(true, db.isBlacklisted("test.io", "asdf"));
        assertEquals(true, db.isBlacklisted("io.test", "asdf"));
        assertEquals(false, db.isBlacklisted("asdf.io", "asdf"));
        assertEquals(false, db.isBlacklisted("asdf.io", "qwer"));
    }
}