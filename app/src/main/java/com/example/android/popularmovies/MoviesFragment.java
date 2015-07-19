package com.example.android.popularmovies;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.example.android.popularmovies.data.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MoviesFragment extends Fragment {
    private static final String LOG_TAG = MoviesFragment.class.getSimpleName();
    private static final String EXTRA_MOVIELIST = "com.example.android.popularmovies.movies";
    static final String EXTRA_MOVIE = "movie";

    private MovieArrayAdapter mMovieArrayAdapter;
    private GridView mGridView;
    private String mCurrentSortPref;

    public MoviesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        List<Movie> adapterData;
        mCurrentSortPref = Utility.getPreferredSortBy(getActivity());

        if (savedInstanceState == null || !savedInstanceState.containsKey(EXTRA_MOVIELIST)) {
            adapterData = new ArrayList<Movie>();
            updateMovieList();
        } else {

            adapterData = savedInstanceState.getParcelableArrayList(EXTRA_MOVIELIST);
        }
        mMovieArrayAdapter = new MovieArrayAdapter(getActivity(),R.layout.list_item_movie,adapterData);
        mGridView = (GridView) rootView.findViewById(R.id.listview_movie);
        mGridView.setAdapter(mMovieArrayAdapter);


        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Movie movie = mMovieArrayAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), MovieDetail.class);
                intent.putExtra(EXTRA_MOVIE, movie);
                startActivity(intent);
            }
        });

        return rootView;
    }

    void updateMovieList(){
        FetchMoviesTask task = new FetchMoviesTask();
        task.execute(mCurrentSortPref);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(EXTRA_MOVIELIST,(ArrayList)mMovieArrayAdapter.getMovies());
    }

    @Override
    public void onResume() {
        super.onResume();
        String newSortPref = Utility.getPreferredSortBy(getActivity());
        Log.v(LOG_TAG, "Old Pref:"+ mCurrentSortPref);
        Log.v(LOG_TAG, "New Pref:" + newSortPref);
        if (!mCurrentSortPref.equals(newSortPref)) {
            mCurrentSortPref = newSortPref;
            updateMovieList();
        }
    }

    public class FetchMoviesTask extends AsyncTask<String, Void, List<Movie>> {

        private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

        private final String BASE_URL = "http://api.themoviedb.org/3/discover/movie?";
        private final String SORT_CODE = "sort_by";
        private final String SORT_TOP_VALUE = "vote_average.desc";
        private final String SORT_POPULAR_VALUE = "popularity.desc";


        @Override
        protected List<Movie> doInBackground(String... params) {
            String sortBy = "";
            Log.v(LOG_TAG, "param[0]:"+params[0]);
            if (params == null || params.length == 0) {
                sortBy = SORT_POPULAR_VALUE;
            } else if (params[0].equals(getString(R.string.pref_sort_highest_rated))) {
                sortBy = SORT_TOP_VALUE;
            } else if (params[0].equals(getString(R.string.pref_sort_most_popular))) {
                sortBy = SORT_POPULAR_VALUE;
            }

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String movieJsonStr = null;
            List<Movie> movies = null;

            try {
                Uri buildUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(SORT_CODE, sortBy)
                        .appendQueryParameter(Utility.API_KEY_CODE, Utility.API_KEY_VALUE)
                        .build();

                //create the request to movieDB api and open the connection
                URL url = new URL(buildUri.toString());

                Log.d(LOG_TAG, "URL : " + url);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();


                //read the input stream into a String
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
                    return null;
                }

                movieJsonStr = buffer.toString();
                Log.v(LOG_TAG,"Data fetched by api: " + movieJsonStr);
                movies = getMovieDataFromJson(movieJsonStr);
                Log.v(LOG_TAG, "Movies : " + movies);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                e.printStackTrace();
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
                return movies;
        }


        private List<Movie> getMovieDataFromJson(String movieJsonStr) throws JSONException {

            final String MDB_LIST = "results";
            final String MDB_MOVIE_ID = "id";
            final String MDB_PATH_PREFIX_W185 = "http://image.tmdb.org/t/p/w185/";
            final String MDB_POSTER_PATH = "poster_path";
            final String MDB_ORIGINAL_TITLE = "original_title";
            final String MDB_OVERVIEW = "overview";
            final String MDB_USER_RATING = "vote_average";
            final String MDB_RELEASE_DATE = "release_date";

            List<Movie> movies = new ArrayList<Movie>();

            try {
                JSONObject movieJson = new JSONObject(movieJsonStr);
                JSONArray movieArray = movieJson.getJSONArray(MDB_LIST);
                for (int i = 0; i < movieArray.length(); i++) {
                    JSONObject movie = movieArray.getJSONObject(i);
                    String movieId = movie.getString(MDB_MOVIE_ID);
                    String posterPath = MDB_PATH_PREFIX_W185 + movie.getString(MDB_POSTER_PATH);
                    String originalTitle = movie.getString(MDB_ORIGINAL_TITLE);
                    String overview = movie.getString(MDB_OVERVIEW);
                    String userRating = movie.getString(MDB_USER_RATING);
                    String releaseDate = movie.getString(MDB_RELEASE_DATE);
                    movies.add(i, new Movie(movieId, posterPath, originalTitle, overview, userRating, releaseDate));
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            return movies;
        }

        @Override
        protected void onPostExecute(List<Movie> movies) {
            mMovieArrayAdapter.clear();
            mMovieArrayAdapter.addAll(movies);
        }
    }
}
