import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class BotFunctions {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public String getRandomCocktail() {
        String url = "https://www.thecocktaildb.com/api/json/v1/1/random.php";
        return fetchCocktail(url);
    }

    public String getCocktailInfo(String name) {
        String url = "https://www.thecocktaildb.com/api/json/v1/1/search.php?s=" + name.replace(" ", "_");
        return fetchCocktail(url);
    }

    public String getCocktailsByIngredient(String ingredient) {
        String url = "https://www.thecocktaildb.com/api/json/v1/1/filter.php?i=" + ingredient.replace(" ", "_");
        try {
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) return "Nessun cocktail trovato.";

            String body = response.body().string();
            JsonObject json = gson.fromJson(body, JsonObject.class);
            JsonArray drinks = json.getAsJsonArray("drinks");

            if (drinks == null) return "Nessun cocktail trovato con questo ingrediente.";

            StringBuilder sb = new StringBuilder("Cocktail con " + ingredient + ":\n");
            for (int i = 0; i < drinks.size(); i++) {
                sb.append("- ").append(drinks.get(i).getAsJsonObject().get("strDrink").getAsString()).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            return "Errore nella richiesta: " + e.getMessage();
        }
    }

    private String fetchCocktail(String url) {
        try {
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) return "Errore nella richiesta.";

            String body = response.body().string();
            JsonObject json = gson.fromJson(body, JsonObject.class);
            JsonArray drinks = json.getAsJsonArray("drinks");

            if (drinks == null) return "Cocktail non trovato.";

            JsonObject drink = drinks.get(0).getAsJsonObject();
            StringBuilder sb = new StringBuilder();
            sb.append("Nome: ").append(drink.get("strDrink").getAsString()).append("\n");
            sb.append("Categoria: ").append(drink.get("strCategory").getAsString()).append("\n");
            sb.append("Alcolico: ").append(drink.get("strAlcoholic").getAsString()).append("\n");
            sb.append("Ingredienti:\n");
            for (int i = 1; i <= 15; i++) {
                String ing = drink.get("strIngredient" + i).isJsonNull() ? null : drink.get("strIngredient" + i).getAsString();
                String measure = drink.get("strMeasure" + i).isJsonNull() ? null : drink.get("strMeasure" + i).getAsString();
                if (ing != null && !ing.isEmpty()) {
                    sb.append("- ").append(ing);
                    if (measure != null) sb.append(" (").append(measure).append(")");
                    sb.append("\n");
                }
            }
            sb.append("Istruzioni: ").append(drink.get("strInstructionsIT").getAsString()).append("\n");
            return sb.toString();

        } catch (IOException e) {
            return "Errore nella richiesta: " + e.getMessage();
        }
    }
    public String getHelp() {
        return "Comandi disponibili:\n" +
                "/random - Ottieni un cocktail casuale\n" +
                "/cocktail <nome> - Ottieni informazioni su un cocktail\n" +
                "/ingredient <ingrediente> - Lista cocktail con l'ingrediente";
    }
}
