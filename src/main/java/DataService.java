import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataService {
    private static DataService instance;
    private Connection connection;

    private DataService() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:src/main/database/cocktailbot.db");
            createTables();
            System.out.println("Godo");
        } catch (SQLException e) {
            System.err.println("C'è da piangere qui");
            e.printStackTrace();
        }
    }

    public static DataService getInstance() throws SQLException {
        if (instance == null) {
            instance = new DataService();
        }
        return instance;
    }

    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                chat_id INTEGER UNIQUE NOT NULL,
                username TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS favorites (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                alcohol_name TEXT NOT NULL,
                added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
        """);
    }

    public void saveUser(long chatId, String username) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT OR IGNORE INTO users(chat_id, username) VALUES (?, ?)"
            );
            stmt.setLong(1, chatId);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getUserId(long chatId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT id FROM users WHERE chat_id = ?"
        );
        stmt.setLong(1, chatId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) return rs.getInt("id");
        return -1;
    }

    public void  addFavorite(long chatId, String alcoholName) throws SQLException {
        int userId = getUserId(chatId);

        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO favorites(user_id, alcohol_name) VALUES (?, ?)"
        );
        stmt.setInt(1, userId);
        stmt.setString(2, alcoholName);
        stmt.executeUpdate();
    }
    public boolean removeFavorite(long chatId, String alcoholName) {
        try {
            int userId = getUserId(chatId);
            if (userId == -1) return false;

            PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM favorites WHERE user_id = ? AND alcohol_name = ?"
            );
            stmt.setInt(1, userId);
            stmt.setString(2, alcoholName);

            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    public List<String> getFavorites(long chatId) throws SQLException {
        List<String> favorites = new ArrayList<>();
        int userId = getUserId(chatId);

        PreparedStatement stmt = connection.prepareStatement(
                "SELECT alcohol_name FROM favorites WHERE user_id = ?"
        );
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            favorites.add(rs.getString("alcohol_name"));
        }
        return favorites;
    }

    // INFO PROFILO
    public String getProfile(long chatId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT username, created_at FROM users WHERE chat_id = ?"
        );
        stmt.setLong(1, chatId);
        ResultSet rs = stmt.executeQuery();

        if (!rs.next()) return "Abbiamo un problema.";

        String username = rs.getString("username");
        String createdAt = rs.getString("created_at");
        List<String> favs = getFavorites(chatId);

        StringBuilder sb = new StringBuilder();
        sb.append("Profilo utente\n");
        sb.append("Username: ").append(username).append("\n");
        sb.append("Alcolizzato dal : ").append(createdAt).append("\n\n");
        sb.append("Alcolici preferiti:\n");

        if (favs.isEmpty()) {
            sb.append("Aggiungi qualcosa ai preferiti!\n");
        } else {
            for (String f : favs) {
                sb.append("- ").append(f).append("\n");
            }
        }
        return sb.toString();
    }
}
