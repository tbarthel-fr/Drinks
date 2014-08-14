package fr.masciulli.drinks;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class WearDrink implements Parcelable {
    public String name;
    public String imageUrl;
    public String history;
    public String instructions;
    public List<String> ingredients = new ArrayList<String>();
    public String wikipedia;

    public WearDrink() {
    }

    private WearDrink(Parcel parcel) {
        name = parcel.readString();
        imageUrl = parcel.readString();
        history = parcel.readString();
        instructions = parcel.readString();
        parcel.readStringList(ingredients);
        wikipedia = parcel.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(imageUrl);
        parcel.writeString(history);
        parcel.writeString(instructions);
        parcel.writeStringList(ingredients);
        parcel.writeString(wikipedia);
    }

    public static final Creator<WearDrink> CREATOR = new Creator<WearDrink>() {
        public WearDrink createFromParcel(Parcel in) {
            return new WearDrink(in);
        }

        public WearDrink[] newArray(int size) {
            return new WearDrink[size];
        }
    };
}
