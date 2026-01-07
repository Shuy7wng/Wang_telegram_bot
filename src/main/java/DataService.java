import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DataService {
    private static DataService instance;
    private Connection connection;

    private DataService() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:cocktailbot.db");
        createTables();
    }

    // Singleton
    public static DataService getInstance() throws SQLException {
        if (instance == null) {
            instance = new DataService();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    // Creazione tabelle
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Tabella utenti
            String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "chat_id INTEGER UNIQUE NOT NULL," +
                    "username TEXT," +
                    "last_command TEXT," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            stmt.execute(sqlUsers);

            // Tabella query cocktail
            String sqlCocktails = "CREATE TABLE IF NOT EXISTS cocktail_queries (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "user_id INTEGER NOT NULL," +
                    "cocktail_name TEXT," +
                    "queried_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY(user_id) REFERENCES users(id)" +
                    ");";
            stmt.execute(sqlCocktails);
        }
    }

    // Inserisce un nuovo utente se non esiste
    public void addUser(long chatId, String username) {
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

    // Aggiorna l'ultimo comando dell'utente
    public void updateLastCommand(long chatId, String command) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE users SET last_command = ? WHERE chat_id = ?"
            );
            stmt.setString(1, command);
            stmt.setLong(2, chatId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Ottiene l'id utente tramite chat_id
    public int getUserId(long chatId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT id FROM users WHERE chat_id = ?"
            );
            stmt.setLong(1, chatId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Salva la query cocktail dell'utente
    public void saveCocktailQuery(long chatId, String cocktailName) {
        int userId = getUserId(chatId);
        if (userId == -1) return;

        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO cocktail_queries(user_id, cocktail_name) VALUES (?, ?)"
            );
            stmt.setInt(1, userId);
            stmt.setString(2, cocktailName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // cronologia
    public List<String> getUserCocktailHistory(long chatId) {
        List<String> history = new ArrayList<>();
        int userId = getUserId(chatId);
        if (userId == -1) return history;

        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT cocktail_name, queried_at FROM cocktail_queries " +
                            "WHERE user_id = ? ORDER BY queried_at DESC LIMIT 20"
            );
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                history.add(rs.getString("cocktail_name") + " ("
                        + rs.getString("queried_at") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    // Numero cocktail dall'utente
    public int getUserTotalQueries(long chatId) {
        int total = 0;
        int userId = getUserId(chatId);
        if (userId == -1) return total;

        try {
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT COUNT(*) AS total FROM cocktail_queries WHERE user_id = ?"
            );
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                total = rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }
}
