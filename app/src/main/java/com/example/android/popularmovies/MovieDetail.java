package com.example.android.popularmovies;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.android.popularmovies.data.Movie;


public class MovieDetail extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            Uri uri = getIntent().getData();
            Movie movie = getIntent().getParcelableExtra(MoviesFragment.EXTRA_MOVIE);
            if (uri != null) {
                arguments.putParcelable(MovieDetailFragment.DETAIL_URI, uri);
            } else if (movie != null) {
                arguments.putParcelable(MoviesFragment.EXTRA_MOVIE, movie);
            }
            MovieDetailFragment fragment = new MovieDetailFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction().add(R.id.movie_detail_container,
                    fragment).commit();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getSupportParentActivityIntent() {
        return super.getSupportParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}
