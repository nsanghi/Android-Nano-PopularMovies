package com.example.android.popularmovies;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.example.android.popularmovies.data.Movie;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by admin on 12-07-2015.
 */
public class MovieArrayAdapter extends ArrayAdapter<Movie> {

    private final String LOG_CAT = MovieArrayAdapter.class.getSimpleName();

    private Context context;
    private int resource;
    private List<Movie> movies;

    public MovieArrayAdapter(Context context, int resource, List<Movie> movies) {
        super(context, resource, movies);
        this.context = context;
        this.resource = resource;
        this.movies = movies;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Movie movie = getItem(position);

        ViewHolder holder;
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(resource,null);
            holder = new ViewHolder();
            holder.movieImage = (ImageView) view.findViewById(R.id.movie_image);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        Picasso.with(context).load(movie.getPosterPath()).into(holder.movieImage);
        return view;
    }

    public List<Movie> getMovies() {
        return this.movies;
    }

    static class ViewHolder {
        ImageView movieImage;
    }

}
