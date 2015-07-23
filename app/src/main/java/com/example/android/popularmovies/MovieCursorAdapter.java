package com.example.android.popularmovies;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;

import com.example.android.popularmovies.data.MovieContract;
import com.squareup.picasso.Picasso;

/**
 * Created by admin on 12-07-2015.
 */
public class MovieCursorAdapter extends CursorAdapter {

    private final String LOG_CAT = MovieCursorAdapter.class.getSimpleName();

    public MovieCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_movie, null);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        String moviePath = cursor.getString(cursor.getColumnIndex(MovieContract.MovieEntry
                .COLUMN_POSTER_PATH));
        ImageView movieImage = (ImageView)view.findViewById(R.id.movie_image);
        Picasso.with(context).load(moviePath).into(movieImage);
    }

}
