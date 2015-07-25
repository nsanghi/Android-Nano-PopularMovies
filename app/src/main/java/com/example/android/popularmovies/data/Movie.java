package com.example.android.popularmovies.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by admin on 11-07-2015.
 */
public class Movie implements Parcelable {

    private String id; //not the curosr Id, but the IMDB movieId (or code)
    private String posterPath;
    private String originalTitle;
    private String overview;
    private String rating;
    private String releaseDate;

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public Movie(String id, String posterPath, String originalTitle, String overview, String rating, String releaseDate) {
            this.id = id;
            this.posterPath = posterPath;
            this.originalTitle = originalTitle;
            this.overview = overview;
            this.rating = rating;
            this.releaseDate = releaseDate;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private Movie(Parcel in) {
        this.id = in.readString();
        this.posterPath = in.readString();
        this.originalTitle = in.readString();
        this.overview = in.readString();
        this.rating = in.readString();
        this.releaseDate = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(posterPath);
        dest.writeString(originalTitle);
        dest.writeString(overview);
        dest.writeString(rating);
        dest.writeString(releaseDate);
    }

    public static final Parcelable.Creator<Movie> CREATOR = new Parcelable.Creator<Movie>() {
        @Override
        public Movie createFromParcel(Parcel source) {
            return new Movie(source);
        }

        @Override
        public Movie[] newArray(int size) {
            return new Movie[size];
        }
    };

    @Override
    public String toString() {
        return "Movie{" +
                "id='" + id + '\'' +
                ", posterPath='" + posterPath + '\'' +
                ", originalTitle='" + originalTitle + '\'' +
                ", overview='" + overview + '\'' +
                ", rating='" + rating + '\'' +
                ", releaseDate=" + releaseDate +
                '}';
    }
}
