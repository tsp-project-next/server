package server;
import java.sql.*;
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
        db.connectDB(1);
        conn = db.getConn();
    }

    @Before
    public void init() {
        db.deleteTableRows(users);
        db.deleteTableRows(lobby);
    }

    @After
    public void finalilze() {
        db.deleteTableRows(users);
        db.deleteTableRows(lobby);
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
}