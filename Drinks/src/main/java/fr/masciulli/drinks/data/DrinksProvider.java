package fr.masciulli.drinks.data;

import java.util.List;

import fr.masciulli.drinks.model.Drink;
import fr.masciulli.drinks.model.Liquor;
import retrofit.Callback;
import retrofit.RestAdapter;

public class DrinksProvider {

    private static RestAdapter sRestAdapter = new RestAdapter.Builder()
            .setEndpoint("http://drinks-api.appspot.com/api")
            .build();
    private static DrinksService sService = sRestAdapter.create(DrinksService.class);

    public static void getAllDrinks(Callback<List<Drink>> callback) {
        sService.getAllDrinks(callback);
    }

    public static void getAllLiquors(Callback<List<Liquor>> callback) {
        sService.getAllLiquors(callback);
    }

    public static void updateServer(String server) {
        sRestAdapter = new RestAdapter.Builder()
                .setServer(server)
                .build();
        sService = sRestAdapter.create(DrinksService.class);
    }

}
