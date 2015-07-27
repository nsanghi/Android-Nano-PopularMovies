package com.example.android.popularmovies;


import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.android.popularmovies.data.Movie;
import com.example.android.popularmovies.data.MovieContract;
import com.example.android.popularmovies.data.Review;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class MovieDetailFragment extends Fragment implements LoaderManager
        .LoaderCallbacks<Cursor> {
    public static final String LOG_TAG = MovieDetailFragment.class.getSimpleName();
    public static final String EXTRA_MOVIE_RUNLENGTH = "EXTRA_MOVIE_RUNLENGTH";
    public static final String EXTRA_TRAILERS = "EXTRA_TRAILERS";
    public static final String EXTRA_REVIEWS = "EXTRA_REVIEW";

    public static final int MOVIE_LOADER = 0;
    public static final int TRAILER_LOADER = 1;
    public static final int REVIEW_LOADER = 2;
    public static final int MOVIE_LOADER_CHECK_EXIST = 3;

    static final String DETAIL_URI = "URI";

    private TextView mTitle;
    private ImageView mMovieImage;
    private TextView mReleaseDate;
    private TextView mRating;
    private TextView mOverview;
    private TextView mRuntime;
    private View mRootView;
    private ImageButton mButtom;
    private ArrayList<String> mTrailers;
    private ArrayList<Review> mReviews;
    private long movieRowId;
    private String mMovieCode;
    private String mMovieImagePath;
    private Activity mActivity;


    private ShareActionProvider mShareActionProvider;


    public MovieDetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        mTrailers = new ArrayList<String>();
        mReviews = new ArrayList<Review>();
        mTitle = (TextView) mRootView.findViewById(R.id.orignal_title);
        mMovieImage = (ImageView) mRootView.findViewById(R.id.movie_image);
        mReleaseDate = (TextView) mRootView.findViewById(R.id.release_date);
        mRating = (TextView) mRootView.findViewById(R.id.user_rating);
        mOverview = (TextView) mRootView.findViewById(R.id.overview);
        mRuntime = (TextView) mRootView.findViewById(R.id.runtime);
        mButtom = (ImageButton) mRootView.findViewById(R.id.favorite_button);


        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        //when movie data needs to come from db. Use cursorloader to load
        //all three table data - movie, trailer, review, for a given
        //movie row Id
        mActivity = getActivity();

        Bundle args = getArguments();
        if (args != null) {
            Uri uri = args.getParcelable(MovieDetailFragment.DETAIL_URI);
            Movie movie = args.getParcelable(MoviesFragment.EXTRA_MOVIE);

            if (uri != null) {
                //load from cursor using the cursor row id stored in movieRowId. Loading will be
                // done in onActivityCreated() method
                movieRowId = ContentUris.parseId(uri);
                Log.d(LOG_TAG, "movieRowId form Uri: " + movieRowId);
            } else if (movie != null) {
                //the movie to be loaded is not persisted in DB and needs to be loaded by api call
                mTitle.setText(movie.getOriginalTitle());
                mReleaseDate.setText(movie.getReleaseDate().substring(0, 4));
                mRating.setText(movie.getRating() + "/10");
                mOverview.setText(movie.getOverview());
                Picasso.with(getActivity()).load(movie.getPosterPath()).into(mMovieImage);
                mMovieImagePath = movie.getPosterPath();
                mMovieCode = movie.getId();
                if (savedInstanceState == null) {
                    FetchMovieDetailsTask movieTask = new FetchMovieDetailsTask();
                    movieTask.execute(movie.getId());
                    FetchTrailersTask trailersTask = new FetchTrailersTask();
                    trailersTask.execute(movie.getId());
                    FetchReviewsTask reviewsTask = new FetchReviewsTask();
                    reviewsTask.execute(movie.getId());
                } else {
                    mRuntime.setText(savedInstanceState.getString(EXTRA_MOVIE_RUNLENGTH));
                    mTrailers = savedInstanceState.getStringArrayList(EXTRA_TRAILERS);
                    mReviews = savedInstanceState.getParcelableArrayList(EXTRA_REVIEWS);
                    populateTrailers();
                    populateReviews();
                }

                mButtom.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //persist the data in database when button is clicked.
                        //this button listener will be attached only when sort option is not
                        // "Favorite"
                        //Process is first start a loader to retrieve a matching existing movie row
                        // if present. Then pass this movierow to an AsyncTask which will delete
                        // existing rows, followed by insert of new Rows
                        mButtom.setClickable(false);
                        Log.d(LOG_TAG, "Favorite button clicked");
                        Log.d(LOG_TAG, "Calling Loader for Movie to check Existing");
                        getLoaderManager().initLoader(MOVIE_LOADER_CHECK_EXIST, null,
                                MovieDetailFragment.this);
                    }
                });
            } else {
                //shotcut method to hide the static text in detials fragment when there is no movie
                //to be loaded
                mRootView.setVisibility(View.INVISIBLE);
                Log.d(LOG_TAG, "Detail activity started without a reference to a movie object or " +
                        "an URI");
            }

        } else {
            //shotcut method to hide the static text in detials fragment when there is no movie
            //to be loaded
            mRootView.setVisibility(View.INVISIBLE);
            Log.d(LOG_TAG, "Started with null Arguments");
        }

        if (Utility.isFavoriteOption(getActivity())) {
            Log.d(LOG_TAG, "Calling Loader for Movie");
            getLoaderManager().initLoader(MOVIE_LOADER, null, this);
            Log.d(LOG_TAG, "Calling Loader for Trailer");
            getLoaderManager().initLoader(TRAILER_LOADER, null, this);
            Log.d(LOG_TAG, "Calling Loader for Review");
            getLoaderManager().initLoader(REVIEW_LOADER, null, this);
        }
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (!Utility.isFavoriteOption(getActivity())) {
            //to keep footprint small, movie data is not persisted as it will be available
            //in the Intent that started this activity. This is true for situation when sort option
            //is mostPopular or highest
            //run length of movie is stored as movie length is not part of api result from search
            //to get movie length, an api call is made with specific movie id (imdb movie id)
            // for sort option of Favorite, loaded data is not persisted at all. Instead, movie and
            //trailer/review are loaded from cursor using the uri that was passed on start.

            outState.putString(EXTRA_MOVIE_RUNLENGTH, mRuntime.getText().toString());
            outState.putStringArrayList(EXTRA_TRAILERS, mTrailers);
            outState.putParcelableArrayList(EXTRA_REVIEWS, mReviews);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void onSortOptionChanged() {
        //whenever sort setting is changed, new data is loaded from api or cursor. Accordingly,
        //Details fragment's rootview is hidden to avoid seeing static text
        mRootView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_movie_detail, menu);
        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
    }

    private Intent createShareForecastIntent() {
        if (mTrailers != null && mTrailers.size() > 0) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "http://www.youtube.com/watch?v=" + mTrailers
                    .get(0));

            return shareIntent;
        }
        return null;
    }


    //convenience method to take a collection of trailer URLs (as stored in member variable) and
    // paint the UI with associated play action
    private void populateTrailers() {
        LinearLayout trailersLayout = (LinearLayout) mRootView.findViewById(R.id.trailer_list);
        trailersLayout.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(mActivity);
        for (int i = 0; i < mTrailers.size(); i++) {
            final String id = mTrailers.get(i);
            View view = inflater.inflate(R.layout.trailer_item, null);
            trailersLayout.addView(view);
            TextView textView = (TextView) view.findViewById(R.id.trailer_link);
            textView.setText("Trailer " + (i + 1));

            textView.setOnClickListener(new View.OnClickListener() {
                @Override

                //idea borrowed from http://stackoverflow
                // .com/questions/574195/android-youtube-app-play-video-intent
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd" +
                                ".youtube:" +
                                id));
                        startActivity(intent);
                    } catch (ActivityNotFoundException ex) {
                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://www.youtube.com/watch?v=" + id));
                        startActivity(intent);
                    }
                }
            });
        }
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareForecastIntent());
        }
    }

    //convenience method to take a collection of reviews (stored as member variable) and paint the
    // UI with associated play action
    private void populateReviews() {
        LinearLayout reviewsLayout = (LinearLayout) mRootView.findViewById(R.id.review_list);
        reviewsLayout.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(mActivity);
        for (Review r : mReviews) {
            View view = inflater.inflate(R.layout.review_item, null);
            reviewsLayout.addView(view);
            TextView author = (TextView) view.findViewById(R.id.reviewer_name);
            TextView content = (TextView) view.findViewById(R.id.review_content);
            author.setText("Review by " + r.getAutoher());
            content.setText(r.getComment());
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "called onCreateLoader for loader id: " + id);
        switch (id) {
            case MOVIE_LOADER: {
                Uri uri = MovieContract.MovieEntry.buildMovieUri(movieRowId);
                return new CursorLoader(
                        getActivity(),
                        uri,
                        null,
                        null,
                        null,
                        null
                );
            }

            case MOVIE_LOADER_CHECK_EXIST: {
                Uri uri = MovieContract.MovieEntry.buildMovieUriFromMovieCode(mMovieCode);
                return new CursorLoader(
                        getActivity(),
                        uri,
                        null,
                        null,
                        null,
                        null
                );
            }

            case REVIEW_LOADER: {
                Uri uri = MovieContract.ReviewEntry.buildReviewMovie(movieRowId);
                return new CursorLoader(
                        getActivity(),
                        uri,
                        null,
                        null,
                        null,
                        null
                );
            }
            case TRAILER_LOADER: {
                Uri uri = MovieContract.TrailerEntry.buildTrailerMovie(movieRowId);
                return new CursorLoader(
                        getActivity(),
                        uri,
                        null,
                        null,
                        null,
                        null
                );
            }
        }
        return null;
    }

    // to populate UI once Favorite movie data has been loaded through Cursor
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "called onLoadFinished for loader id: " + loader.getId());
        switch (loader.getId()) {
            case MOVIE_LOADER: {
                if (data != null && data.moveToFirst()) {
                    mTitle.setText(data.getString(data.getColumnIndex(MovieContract.MovieEntry
                            .COLUMN_TITLE)));
                    mReleaseDate.setText(data.getString(data.getColumnIndex(MovieContract
                            .MovieEntry.COLUMN_RELEASE_DATE)));
                    mRating.setText(data.getString(data.getColumnIndex(MovieContract.MovieEntry
                            .COLUMN_RATING)));
                    mOverview.setText(data.getString(data.getColumnIndex(MovieContract.MovieEntry
                            .COLUMN_OVERVIEW)));
                    Picasso.with(getActivity())
                            .load(data.getString(data.getColumnIndex(MovieContract.MovieEntry
                                    .COLUMN_POSTER_PATH)))
                            .into(mMovieImage);
                    mRuntime.setText(data.getString(data.getColumnIndex(MovieContract.MovieEntry
                            .COLUMN_RUNTIME)));
                    mMovieCode = data.getString(data.getColumnIndex(MovieContract.MovieEntry
                            .COLUMN_MOVIE_CODE));
                    mMovieImagePath = data.getString(data.getColumnIndex(MovieContract.MovieEntry
                            .COLUMN_POSTER_PATH));
                    mButtom.setImageResource(R.drawable.ic_star_black_48dp);
                    mButtom.setClickable(false);
                }
                break;
            }

            //once the status of current matching movie in database is selected, start AsyncProcess
            // to delete and insert new data
            case MOVIE_LOADER_CHECK_EXIST: {
                long existingMovieRowId = -1L;
                if (data != null && data.getCount() > 0) {
                    data.moveToFirst();
                    existingMovieRowId = data.getLong(data.getColumnIndex(MovieContract
                            .MovieEntry._ID));
                }

                Log.d(LOG_TAG, "Calling AsyncTask to save data");
                SaveFavoriteTask task = new SaveFavoriteTask(
                        getActivity(),
                        existingMovieRowId,
                        mMovieCode,
                        mOverview.getText().toString(),
                        mMovieImagePath, mReleaseDate.getText().toString(),
                        mTitle.getText().toString(),
                        mRating.getText().toString(),
                        mRuntime.getText().toString(),
                        new ArrayList<String>(mTrailers),
                        new ArrayList<Review>(mReviews), mButtom
                );
                task.execute();
            }
            case TRAILER_LOADER: {
                if (data != null) {
                    Log.d(LOG_TAG, "Trailer rows retured: " + data.getCount());
                    while (data.moveToNext()) {
                        mTrailers.add(data.getString(data.getColumnIndex(MovieContract.TrailerEntry
                                .COLUMN_YOUTUBE_URL)));
                    }
                }
                populateTrailers();
                break;
            }
            case REVIEW_LOADER: {
                if (data != null) {
                    Log.d(LOG_TAG, "Review rows retured: " + data.getCount());
                    String author;
                    String comment;
                    while (data.moveToNext()) {
                        author = data.getString(data.getColumnIndex(MovieContract.ReviewEntry
                                .COLUMN_REVIEWER_NAME));
                        comment = data.getString(data.getColumnIndex(MovieContract
                                .ReviewEntry.COLUMN_REVIEWER_COMMENT));
                        mReviews.add(new Review(author, comment));
                    }
                }
                populateReviews();
                break;
            }
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    // Async Task to get Movie details. Strictly speaking, this is required only to get
    //Movie run time. Rest of the data is already available through cursor or movie object in the
    //intent
    public class FetchMovieDetailsTask extends AsyncTask<String, Void, String> {

        private final String LOG_TAG = FetchMovieDetailsTask.class.getSimpleName();
        private final String BASE_URL = "http://api.themoviedb.org/3/movie?";
        private final String MBD_RUNTIME = "runtime";

        @Override
        protected String doInBackground(String... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String movieJsonStr = null;

            try {
                Uri buildUri = Uri.parse(BASE_URL).buildUpon()
                        .appendPath(params[0])
                        .appendQueryParameter(Constants.API_KEY_CODE, Constants.API_KEY_VALUE)
                        .build();

                URL url = new URL(buildUri.toString());
                Log.d(LOG_TAG, "URL : " + url);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    Log.d(LOG_TAG, "No data returned by call to : " + url);
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n"); // to help format in printing
                }

                if (buffer.length() == 0) {
                    Log.d(LOG_TAG, "No data returned by call to : " + url);
                    return null;
                }

                movieJsonStr = buffer.toString();
                Log.d(LOG_TAG, "Data fetched by api: " + movieJsonStr);

                JSONObject movieJson = new JSONObject(movieJsonStr);
                String userRating = movieJson.getString(MBD_RUNTIME);
                return userRating;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error occured :" + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            mRuntime.setText(s == null ? "" : s + "min");
        }
    }

    //to lead Trailer data for a given move id (imdb movie id)
    public class FetchTrailersTask extends AsyncTask<String, Void, ArrayList<String>> {

        private final String LOG_TAG = FetchTrailersTask.class.getSimpleName();
        private final String BASE_URL = "http://api.themoviedb.org/3/movie?";
        private final String MDB_TRAILER_PATH = "trailers";
        private final String MDB_TRAILERS_SOURCE = "youtube";
        private final String MDB_TRAILER_ID = "source";

        @Override
        protected ArrayList<String> doInBackground(String... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            ArrayList<String> trailers = new ArrayList<String>();
            String trailerJsonStr;


            try {
                Uri buildUri = Uri.parse(BASE_URL).buildUpon()
                        .appendPath(params[0])
                        .appendPath(MDB_TRAILER_PATH)
                        .appendQueryParameter(Constants.API_KEY_CODE, Constants.API_KEY_VALUE)
                        .build();

                URL url = new URL(buildUri.toString());
                Log.d(LOG_TAG, "URL : " + url);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    Log.d(LOG_TAG, "No data returned by call to : " + url);
                    return trailers;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n"); // to help format in printing
                }

                if (buffer.length() == 0) {
                    Log.d(LOG_TAG, "No data returned by call to : " + url);
                    return trailers;
                }

                trailerJsonStr = buffer.toString();
                Log.d(LOG_TAG, "Data fetched by api: " + trailerJsonStr);

                JSONObject trailerJson = new JSONObject(trailerJsonStr);
                JSONArray trailerArray = trailerJson.getJSONArray(MDB_TRAILERS_SOURCE);
                for (int i = 0; i < trailerArray.length(); i++) {
                    JSONObject trailer = trailerArray.getJSONObject(i);
                    String trailerSource = trailer.getString(MDB_TRAILER_ID);
                    trailers.add(trailerSource);
                }
                return trailers;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error occured :" + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<String> trailers) {
            if (mTrailers != null && mTrailers.size() > 0) {
                mTrailers.clear();
            }

            if (trailers != null && trailers.size() > 0) {
                mTrailers.addAll(trailers);
            }
            populateTrailers();
        }
    }


    //Async Task to make API call to get Review details for a given movie id
    public class FetchReviewsTask extends AsyncTask<String, Void, ArrayList<Review>> {

        private final String LOG_TAG = FetchReviewsTask.class.getSimpleName();
        private final String BASE_URL = "http://api.themoviedb.org/3/movie?";
        private final String MDB_REVIEW_PATH = "reviews";
        private final String MDB_REVIEWS_LIST = "results";
        private final String MDB_REVIEW_AUTHOR = "author";
        private final String MDB_REVIEW_CONTENT = "content";

        @Override
        protected ArrayList<Review> doInBackground(String... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            ArrayList<Review> reviews = new ArrayList<Review>();
            String reviewJsonStr;


            try {
                Uri buildUri = Uri.parse(BASE_URL).buildUpon()
                        .appendPath(params[0])
                        .appendPath(MDB_REVIEW_PATH)
                        .appendQueryParameter(Constants.API_KEY_CODE, Constants.API_KEY_VALUE)
                        .build();

                URL url = new URL(buildUri.toString());
                Log.d(LOG_TAG, "URL : " + url);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    Log.d(LOG_TAG, "No data returned by call to : " + url);
                    return reviews;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n"); // to help format in printing
                }

                if (buffer.length() == 0) {
                    Log.d(LOG_TAG, "No data returned by call to : " + url);
                    return reviews;
                }

                reviewJsonStr = buffer.toString();
                Log.d(LOG_TAG, "Data fetched by api: " + reviewJsonStr);

                JSONObject reviewJson = new JSONObject(reviewJsonStr);
                JSONArray reviewArray = reviewJson.getJSONArray(MDB_REVIEWS_LIST);
                for (int i = 0; i < reviewArray.length(); i++) {
                    JSONObject review = reviewArray.getJSONObject(i);
                    String author = review.getString(MDB_REVIEW_AUTHOR);
                    String content = review.getString(MDB_REVIEW_CONTENT);
                    reviews.add(new Review(author, content));
                }
                return reviews;
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error occured :" + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Review> reviews) {
            if (mReviews != null && mReviews.size() > 0) {
                mReviews.clear();
            }

            if (reviews != null && reviews.size() > 0) {
                mReviews.addAll(reviews);
            }
            populateReviews();
        }

    }

}
