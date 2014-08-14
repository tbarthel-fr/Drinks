package fr.masciulli.drinks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.widget.TextView;

public class WearDrinkActivity extends Activity {

    private TextView mNameView;
    private WearDrink mDrink;
    private TextView mIngredientsView;
    private TextView mInstructionsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_drink);
        mNameView = (TextView)findViewById(R.id.title);
        mIngredientsView = (TextView)findViewById(R.id.ingredients);
        mInstructionsView = (TextView)findViewById(R.id.instructions);
        Intent intent = getIntent();
        if (intent.hasExtra("drink")) {
            mDrink = intent.getParcelableExtra("drink");
            updateViews();
        }
    }

    private void updateViews() {
        if (mDrink != null) {
            mNameView.setText(mDrink.name);
            mInstructionsView.setText(mDrink.instructions);

            String htmlString = "";
            int i = 0;
            for (String ingredient : mDrink.ingredients) {
                if (++i == mDrink.ingredients.size()) {
                    htmlString += "&#8226; " + ingredient;
                } else {
                    htmlString += "&#8226; " + ingredient + "<br>";
                }
            }
            mIngredientsView.setText(Html.fromHtml(htmlString));
        }
    }
}
