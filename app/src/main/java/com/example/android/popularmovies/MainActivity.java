package com.example.android.popularmovies;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.popularmovies.data.Movie;


public class MainActivity extends AppCompatActivity implements MoviesFragment.Callback {


    public static final String DETAILFRAGMENT_TAG = "DFTAG";
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private boolean mTwoPane;
    private String mSortBy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mSortBy = Utility.getPreferredSortBy(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG, "calling onCreate of MainActivity");
        if (findViewById(R.id.movie_detail_container) != null) {
            Log.d(LOG_TAG, "found 2 pane");
            mTwoPane = true;
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction().replace(R.id
                        .movie_detail_container, new MovieDetailFragment(), DETAILFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //Overall design of app got messy. As each Activity (mail and detail) need to have two ways
    //to load the data. One from Curosor for movies that are persisted in DB and another using
    //AsyncTask to load data thru api calls. The Fragment codes got complicated to hndle the
    // lifecycle of loading data in two different ways. And also the way data needs to be passed
    // from Main to Detail acvtivity. Will be great to get some pointers to handle situations like
    //this for future
    @Override
    public void onItemSelected(boolean isFavorite, Uri cursorUri, Movie movie) {
        if (mTwoPane) {
            Bundle args = new Bundle();
            if (isFavorite) {
                args.putParcelable(MovieDetailFragment.DETAIL_URI, cursorUri);
            } else {
                args.putParcelable(MoviesFragment.EXTRA_MOVIE, movie);
            }
            MovieDetailFragment fragment = new MovieDetailFragment();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction().replace(R.id.movie_detail_container,
                    fragment, DETAILFRAGMENT_TAG).commit();
        } else {
            Intent intent = new Intent(this, MovieDetail.class);
            if (isFavorite) {
                intent.setData(cursorUri);
            } else {
                intent.putExtra(MoviesFragment.EXTRA_MOVIE, movie);
            }
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String sortBy = Utility.getPreferredSortBy(this);
        if (sortBy != null && !sortBy.equals(mSortBy)) {
            MoviesFragment mf = (MoviesFragment) getSupportFragmentManager().findFragmentById(R
                    .id.fragment_movie);
            if (null != mf) {
                mf.onSortOptionChanged();
            }

            MovieDetailFragment mdf = (MovieDetailFragment) getSupportFragmentManager()
                    .findFragmentByTag(DETAILFRAGMENT_TAG);
            if (null != mdf) {
                mdf.onSortOptionChanged();
            }
            mSortBy = sortBy;
        }
    }

}

