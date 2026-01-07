import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.sql.SQLException;
import java.util.List;

public class CocktailBot implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final BotFunctions botFunctions = new BotFunctions();

    public CocktailBot(String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message msg = update.getMessage();
        long chatId = msg.getChatId();
        String username = msg.getFrom() != null ? msg.getFrom().getUserName() : "Unknown";
        String text = msg.getText();

        // Gestione database utenti
        try {
            DataService db = DataService.getInstance();
            db.addUser(chatId, username);
            db.updateLastCommand(chatId, text);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Analisi comandi
        String responseText = analyzeText(chatId, text);

        // Invia risposta
        SendMessage.SendMessageBuilder<?, ?> builder = SendMessage.builder()
                .chatId(chatId)
                .text(responseText);

        try {
            telegramClient.execute(builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String analyzeText(long chatId, String text) {
        if (text == null || text.isEmpty()) return "";

        try {
            DataService db = DataService.getInstance();

            if (text.equals("/random")) {
                String cocktail = botFunctions.getRandomCocktail();
                db.saveCocktailQuery(chatId, "Random Cocktail");
                return cocktail;

            } else if (text.startsWith("/cocktail ")) {
                String name = text.substring(10);
                db.saveCocktailQuery(chatId, name);
                return botFunctions.getCocktailInfo(name);

            } else if (text.startsWith("/ingredient ")) {
                String ing = text.substring(12);
                db.saveCocktailQuery(chatId, "Ingredient: " + ing);
                return botFunctions.getCocktailsByIngredient(ing);

            } else if (text.equals("/history")) {
                List<String> history = db.getUserCocktailHistory(chatId);
                if (history.isEmpty()) return "Non hai ancora cercato cocktail.";
                StringBuilder sb = new StringBuilder("Ultime ricerche:\n");
                for (String h : history) sb.append("- ").append(h).append("\n");
                return sb.toString();

            } else if (text.equals("/stats")) {
                int total = db.getUserTotalQueries(chatId);
                return "Hai consultato " + total + " cocktail finora!";

            } else if (text.equals("/help")) {
                return botFunctions.getHelp() +
                        "\n/history - Mostra ultime ricerche\n/stats - Mostra statistiche";

            } else {
                return "Comando non riconosciuto. Scrivi /help per vedere i comandi disponibili.";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Errore database: impossibile processare il comando.";
        }
    }
}
