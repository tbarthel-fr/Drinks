package fr.masciulli.drinks.fragment;

import android.animation.TimeInterpolator;
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import fr.masciulli.android_quantizer.lib.ColorQuantizer;
import fr.masciulli.drinks.R;
import fr.masciulli.drinks.model.Drink;
import fr.masciulli.drinks.util.AnimUtils;
import fr.masciulli.drinks.view.BlurTransformation;
import fr.masciulli.drinks.view.ObservableScrollView;
import fr.masciulli.drinks.view.ScrollViewListener;

public class DrinkDetailFragment extends Fragment implements ScrollViewListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {

    private static final String STATE_DRINK = "drink";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final long ANIM_IMAGE_ENTER_DURATION = 500;
    private static final long ANIM_TEXT_ENTER_DURATION = 500;
    private static final long ANIM_IMAGE_ENTER_STARTDELAY = 300;
    private static final long ANIM_COLORBOX_ENTER_DURATION = 200;

    /** Request code for launching the Intent to resolve Google Play services errors. */
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError;
    private Gson mGson = new Gson();

    private static final TimeInterpolator sDecelerator = new DecelerateInterpolator();

    @InjectView(R.id.image)
    ImageView mImageView;
    @InjectView(R.id.image_blurred)
    ImageView mBlurredImageView;
    @InjectView(R.id.history)
    TextView mHistoryView;
    @InjectView(R.id.scroll)
    ObservableScrollView mScrollView;
    @InjectView(R.id.ingredients)
    TextView mIngredientsView;
    @InjectView(R.id.instructions)
    TextView mInstructionsView;
    @InjectView(R.id.wikipedia)
    Button mWikipediaButton;
    @InjectView(R.id.colorbox)
    View mColorBox;
    @InjectView(R.id.color1)
    View mColorView1;
    @InjectView(R.id.color2)
    View mColorView2;
    @InjectView(R.id.color3)
    View mColorView3;
    @InjectView(R.id.color4)
    View mColorView4;

    private int mImageViewHeight;

    private Transformation mTransformation;

    private Drink mDrink;
    private Target mTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            mImageView.setImageBitmap(bitmap);
            new QuantizeBitmapTask().execute(bitmap);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_drink_detail, container, false);
        ButterKnife.inject(this, root);

        setHasOptionsMenu(true);

        Intent intent = getActivity().getIntent();
        mDrink = intent.getParcelableExtra("drink");

        getActivity().setTitle(mDrink.name);
        Picasso.with(getActivity()).load(mDrink.imageUrl).into(mTarget);

        mTransformation = new BlurTransformation(getActivity(), getResources().getInteger(R.integer.blur_radius));
        Picasso.with(getActivity()).load(mDrink.imageUrl).transform(mTransformation).into(mBlurredImageView);

        mImageViewHeight = (int) getResources().getDimension(R.dimen.drink_detail_recipe_margin);
        mScrollView.setScrollViewListener(this);

        if (savedInstanceState != null) {
            mColorBox.setAlpha(1);
            Drink drink = savedInstanceState.getParcelable(STATE_DRINK);
            if (drink != null) {
                refreshUI(drink);
            }
        } else {
            ViewTreeObserver observer = mImageView.getViewTreeObserver();
            if (observer != null) {
                observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                    @Override
                    public boolean onPreDraw() {
                        mImageView.getViewTreeObserver().removeOnPreDrawListener(this);
                        runEnterAnimation();
                        return true;
                    }
                });
            }
        }

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        if (!mResolvingError && null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    private Collection<String> getNodes() {
        Log.d("DrinkDetailFragment","getting nodes");
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        Log.d("DrinkDetailFragment","nodes retrieved");

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    private void pushToWearable() {
        new PushToWearableTask().execute();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mResolvingError = false;
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    return;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        return;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(getActivity(), REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            mResolvingError = false;
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        }
    }

    private class PushToWearableTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            final Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }

    private void sendStartActivityMessage(String node) {
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, START_ACTIVITY_PATH, mGson.toJson(mDrink).getBytes()).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e("DrinkDetailFragment", "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        } else {
                            Log.d("DrinkDetailFragment", "SUCCESS");
                        }
                    }
                }
        );
    }

    @OnClick(R.id.wikipedia)
    void goToWikipedia() {
        if (mDrink == null) return;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mDrink.wikipedia)));
    }

    private void runEnterAnimation() {

        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshUI(mDrink);
            }
        };
        mImageView.setTranslationY(-mImageView.getHeight());

        ViewPropertyAnimator animator = mImageView.animate().setDuration(ANIM_IMAGE_ENTER_DURATION).
                setStartDelay(ANIM_IMAGE_ENTER_STARTDELAY).
                translationY(0).
                setInterpolator(sDecelerator);

        Runnable animateColorBoxRunnable = new Runnable() {
            @Override
            public void run() {
                mColorBox.animate()
                        .alpha(1)
                        .setDuration(ANIM_COLORBOX_ENTER_DURATION)
                        .setInterpolator(sDecelerator);
            }
        };

        AnimUtils.scheduleStartAction(animator, refreshRunnable, ANIM_IMAGE_ENTER_STARTDELAY);
        AnimUtils.scheduleEndAction(animator, animateColorBoxRunnable, ANIM_IMAGE_ENTER_STARTDELAY, ANIM_IMAGE_ENTER_DURATION);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mDrink != null) {
            outState.putParcelable(STATE_DRINK, mDrink);
        }
    }

    @Override
    public void onScrollChanged(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
        float alpha = 2 * (float) y / (float) mImageViewHeight;
        if (alpha > 1) {
            alpha = 1;
        }
        mBlurredImageView.setAlpha(alpha);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mImageView.getLayoutParams();
        params.setMargins(params.leftMargin, -y / 2, params.rightMargin, params.bottomMargin);
        mImageView.setLayoutParams(params);

        params = (RelativeLayout.LayoutParams) mBlurredImageView.getLayoutParams();
        params.setMargins(params.leftMargin, -y / 2, params.rightMargin, params.bottomMargin);
        mBlurredImageView.setLayoutParams(params);
    }

    public void refreshUI(Drink drink) {
        mDrink = drink;

        if (getActivity() == null) return;

        mHistoryView.setText(drink.history);

        String htmlString = "";
        int i = 0;
        for (String ingredient : drink.ingredients) {
            if (++i == drink.ingredients.size()) {
                htmlString += "&#8226; " + ingredient;
            } else {
                htmlString += "&#8226; " + ingredient + "<br>";
            }
        }
        mIngredientsView.setText(Html.fromHtml(htmlString));

        mInstructionsView.setText(drink.instructions);
        mWikipediaButton.setText(String.format(getString(R.string.drink_detail_wikipedia), drink.name));

        ViewTreeObserver observer = mScrollView.getViewTreeObserver();
        if (observer != null) {
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                @Override
                public boolean onPreDraw() {
                    mScrollView.getViewTreeObserver().removeOnPreDrawListener(this);
                    mScrollView.setAlpha(0);
                    mScrollView.animate().setDuration(ANIM_TEXT_ENTER_DURATION).
                            alpha(1).
                            setInterpolator(sDecelerator);
                    // Fake a onScrollChangedCall to apply changes to mBlurredImageView and mImageView.
                    fakeOnScrollChanged();
                    return true;
                }
            });
        }
        mScrollView.setVisibility(View.VISIBLE);
    }

    private void fakeOnScrollChanged() {
        onScrollChanged(mScrollView, mScrollView.getScrollX(),
                mScrollView.getScrollY(), mScrollView.getScrollX(), mScrollView.getScrollY());
    }

    private class QuantizeBitmapTask extends AsyncTask<Bitmap, Void, ArrayList<Integer>> {

        @Override
        protected ArrayList<Integer> doInBackground(Bitmap... bitmaps) {

            Bitmap originalBitmap = bitmaps[0];

            int originalWidth = originalBitmap.getWidth();
            int originalHeight = originalBitmap.getHeight();

            Bitmap bitmap = Bitmap.createScaledBitmap(originalBitmap, originalWidth / 16, originalHeight / 16, true);

            ArrayList<Integer> quantizedColors = new ColorQuantizer().load(bitmap).quantize().getQuantizedColors();

            return quantizedColors;
        }

        @Override
        protected void onPostExecute(ArrayList<Integer> colors) {
            View[] colorViews = new View[]{mColorView1, mColorView2, mColorView3, mColorView4};
            for (int i = 0; i < colors.size() && i < 4; i++) {
                colorViews[i].setBackgroundColor(colors.get(i));
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.drink_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.push_to_wearable:
                pushToWearable();
                return true;
            case android.R.id.home:
                getActivity().finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
