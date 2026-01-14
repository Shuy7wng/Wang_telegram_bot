import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;

import java.io.File;
import java.util.List;
import java.util.Random;

public class CocktailBot implements LongPollingUpdateConsumer {

    private final OkHttpTelegramClient telegramClient;
    private final BotFunctions botFunctions = new BotFunctions();

    public CocktailBot(String botToken) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            new Thread(() -> handleUpdate(update)).start();
        }
    }

    private void handleStart(long chatId, String username) {
        try {
            File folder = new File("src/main/resources/gifs");
            File[] gifs = folder.listFiles((dir, name) -> name.endsWith(".gif"));
            if (gifs == null || gifs.length == 0) return;

            File gifFile = gifs[new Random().nextInt(gifs.length)];
            InputFile inputFile = new InputFile(gifFile);

            SendAnimation animation = SendAnimation.builder()
                    .chatId(chatId)
                    .animation(inputFile)
                    .caption("Benvenuto " + username + " negli alcolisti anonimi! 🍹")
                    .build();

            telegramClient.execute(animation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleUpdate(Update update) {
        try {
            DataService db = DataService.getInstance();

            if (update.hasMessage() && update.getMessage().hasText()) {
                Message msg = update.getMessage();
                long chatId = msg.getChatId();
                String username = msg.getFrom() != null ? msg.getFrom().getUserName() : "Unknown";
                String text = msg.getText().trim();
                String lowerText = text.toLowerCase();

                db.saveUser(chatId, username);

                switch (lowerText) {
                    case "/start" -> handleStart(chatId, username);

                    case "/help" -> telegramClient.execute(
                            SendMessage.builder()
                                    .chatId(chatId)
                                    .text("""
                                            Comandi disponibili:
                                            /start - Benvenuto 😍
                                            /random - Cocktail casuale 🎈
                                            /cocktail <nome> - Info cocktail 🍸
                                            /ingredient <ingrediente> - Lista Cocktail 🍋
                                            /adoro <nome> - Aggiungi preferito ❤
                                            /elimina <nome> - Rimuovi preferito 💔
                                            /profilo - Profilo e preferiti 🙋🏻‍♂️
                                            """)
                                    .build()
                    );

                    case "/random" -> {
                        BotFunctions.CocktailInfo cocktail = botFunctions.getRandomCocktail();
                        sendCocktail(chatId, cocktail);
                    }

                    default -> {
                        if (lowerText.startsWith("/cocktail ")) {
                            String name = text.substring(10).trim();
                            BotFunctions.CocktailInfo cocktail = botFunctions.getCocktailInfo(name);
                            if (cocktail.getDescription().contains("non trovato")) {
                                telegramClient.execute(
                                        SendMessage.builder()
                                                .chatId(chatId)
                                                .text("❌ Cocktail \"" + name + "\" non trovato.")
                                                .build()
                                );
                            } else {
                                sendCocktail(chatId, cocktail);
                            }
                            return;

                        } else if (lowerText.startsWith("/ingredient")) {
                            String[] parts = text.split(" ", 2);
                            if (parts.length < 2 || parts[1].isBlank()) {
                                telegramClient.execute(SendMessage.builder()
                                        .chatId(chatId)
                                        .text("⚠️ Devi specificare un ingrediente. Esempio: /ingredient limone")
                                        .build());
                            } else {
                                String ingredient = parts[1].trim();
                                String result = botFunctions.getCocktailsByIngredient(ingredient);
                                telegramClient.execute(SendMessage.builder()
                                        .chatId(chatId)
                                        .text(result)
                                        .build());
                            }
                            return;
                        } else if (lowerText.startsWith("/adoro ")) {
                            String alc = text.substring(7).trim();
                            db.addFavorite(chatId, alc);
                            telegramClient.execute(SendMessage.builder()
                                    .chatId(chatId)
                                    .text("❤️ Aggiunto ai preferiti: " + alc)
                                    .build());
                        }else if (lowerText.startsWith("/elimina ")) {
                            String alc = text.substring(9).trim();
                            List<String> favorites = db.getFavorites(chatId);

                            String found = null;
                            for (String f : favorites) {
                                if (f.equalsIgnoreCase(alc)) {
                                    found = f;
                                    break;
                                }
                            }

                            if (found != null) {
                                db.removeFavorite(chatId, found);
                                telegramClient.execute(SendMessage.builder()
                                        .chatId(chatId)
                                        .text("🗑️ Rimosso dai preferiti: " + found)
                                        .build());
                            } else {
                                telegramClient.execute(SendMessage.builder()
                                        .chatId(chatId)
                                        .text("⚠️ \"" + alc + "\" non è nei tuoi preferiti!")
                                        .build());
                            }
                        } else if (lowerText.equals("/profilo")) {
                            String profile = db.getProfile(chatId);
                            telegramClient.execute(SendMessage.builder()
                                    .chatId(chatId)
                                    .text(profile)
                                    .build());
                        } else {
                            telegramClient.execute(SendMessage.builder()
                                    .chatId(chatId)
                                    .text("Comando non riconosciuto. Scrivi /help.")
                                    .build());
                        }
                    }
                }
            }

            if (update.hasCallbackQuery()) {
                String data = update.getCallbackQuery().getData();
                long chatId = update.getCallbackQuery().getMessage().getChatId();
                String callbackId = update.getCallbackQuery().getId(); // id della callback

                if (data.startsWith("adoro:")) {
                    String cocktailName = data.substring(6);
                    db.addFavorite(chatId, cocktailName);

                    // Risposta al callback (serve per far sparire il loading)
                    telegramClient.execute(AnswerCallbackQuery.builder()
                            .callbackQueryId(callbackId)
                            .text("❤️ \"" + cocktailName + "\" aggiunto ai preferiti!")
                            .showAlert(false)
                            .build()
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendCocktail(long chatId, BotFunctions.CocktailInfo cocktail) throws Exception {
        if (cocktail.getImageUrl() != null) {
            telegramClient.execute(SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(cocktail.getImageUrl()))
                    .build());
        }
        //bottone adoro
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text("❤️ Adoro")
                .callbackData("adoro:" + extractCocktailName(cocktail.getDescription()))
                .build();

        InlineKeyboardRow row = new InlineKeyboardRow();
        row.add(button);

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .build();

        telegramClient.execute(SendMessage.builder()
                .chatId(chatId)
                .text(cocktail.getDescription())
                .replyMarkup(markup)
                .build());
    }

    private String extractCocktailName(String description) {
        for (String line : description.split("\n")) {
            if (line.startsWith("Nome:")) {
                return line.substring(6).trim();
            }
        }
        return "Cocktail";
    }
}
