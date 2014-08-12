package fr.masciulli.drinks;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class WearDrinkActivity extends Activity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_drink);
        mTextView = (TextView)findViewById(R.id.text);
    }
}
