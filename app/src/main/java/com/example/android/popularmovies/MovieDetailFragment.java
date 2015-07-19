package com.example.android.popularmovies;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.android.popularmovies.data.Movie;
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
public class MovieDetailFragment extends Fragment {
    public static final String LOG_TAG = MovieDetailFragment.class.getSimpleName();
    public static final String EXTRA_MOVIE_RUNLENGTH = "EXTRA_MOVIE_RUNLENGTH";
    public static final String EXTRA_TRAILERS = "EXTRA_TRAILERS";
    public static final String EXTRA_REVIEWS = "EXTRA_REVIEW";

    private TextView mTitle;
    private ImageView mMovieImage;
    private TextView mReleaseDate;
    private TextView mRating;
    private TextView mOverview;
    private TextView mRuntime;
    private View mRootView;
    private ArrayList<String> mTrailers;
    private ArrayList<Review> mReviews;


    public MovieDetailFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        mTrailers = new ArrayList<String>();
        mReviews = new ArrayList<Review>();

        Intent intent = getActivity().getIntent();

        if (intent != null && intent.hasExtra(MoviesFragment.EXTRA_MOVIE)) {
            Movie movie = intent.getParcelableExtra(MoviesFragment.EXTRA_MOVIE);
            mTitle = (TextView) mRootView.findViewById(R.id.orignal_title);
            mMovieImage = (ImageView) mRootView.findViewById(R.id.movie_image);
            mReleaseDate = (TextView) mRootView.findViewById(R.id.release_date);
            mRating = (TextView) mRootView.findViewById(R.id.user_rating);
            mOverview = (TextView) mRootView.findViewById(R.id.overview);
            mRuntime = (TextView) mRootView.findViewById(R.id.runtime);

            mTitle.setText(movie.getOriginalTitle());
            mReleaseDate.setText(movie.getReleaseDate().substring(0, 4));
            mRating.setText(movie.getRating() + "/10");
            mOverview.setText(movie.getOverview());
            Picasso.with(getActivity()).load(movie.getPosterPath()).into(mMovieImage);
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

        } else {
            Log.d(LOG_TAG, "Detail activity started without a reference to a movie object");
        }


        return mRootView;
    }

    private void populateTrailers() {
        LinearLayout trailersLayout = (LinearLayout) mRootView.findViewById(R.id.trailer_list);
        trailersLayout.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        for (int i = 0; i < mTrailers.size(); i++) {
            final String id = mTrailers.get(i);
            View view = inflater.inflate(R.layout.trailer_item, null);
            trailersLayout.addView(view);
            TextView textView = (TextView) view.findViewById(R.id.trailer_link);
            textView.setText("Trailer " + (i + 1));
            textView.setOnClickListener(new View.OnClickListener() {
                @Override

                //idea borrowed from http://stackoverflow.com/questions/574195/android-youtube-app-play-video-intent
                public void onClick(View v) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" +
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
    }

    private void populateReviews() {
        LinearLayout reviewsLayout = (LinearLayout) mRootView.findViewById(R.id.review_list);
        reviewsLayout.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getActivity());
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
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(EXTRA_MOVIE_RUNLENGTH, mRuntime.getText().toString());
        outState.putStringArrayList(EXTRA_TRAILERS, mTrailers);
        outState.putParcelableArrayList(EXTRA_REVIEWS, mReviews);
        super.onSaveInstanceState(outState);
    }

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
                        .appendQueryParameter(Utility.API_KEY_CODE, Utility.API_KEY_VALUE)
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
                        .appendQueryParameter(Utility.API_KEY_CODE, Utility.API_KEY_VALUE)
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


    public class FetchReviewsTask extends AsyncTask<String, Void, ArrayList<Review>> {

        private final String LOG_TAG = FetchReviewsTask.class.getSimpleName();
        private final String BASE_URL = "http://api.themoviedb.org/3/movie?";
        private final String MDB_REVIEW_PATH = "reviews";
        private final String MDB_REVIEWS_LIST = "results";
        private final String MDB_REVIEW_AUTHOR = "author";
        private final String MDB_REVIEW_CONTENT  = "content";

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
                        .appendQueryParameter(Utility.API_KEY_CODE, Utility.API_KEY_VALUE)
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
