import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

public class Main {
    public static void main(String[] args) {
        try {
            DataService.getInstance();
        } catch (Exception e) {
            System.err.println("Errore database: " + e.getMessage());
            System.exit(-1);
        }

        String botToken = ConfigurationSingleton.getInstance().getProperty("BOT_TOKEN");

        try (TelegramBotsLongPollingApplication botsApp = new TelegramBotsLongPollingApplication()) {
            botsApp.registerBot(botToken, new CocktailBot(botToken));
            System.out.println("Cock-tailBot avviato correttamente!");
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

