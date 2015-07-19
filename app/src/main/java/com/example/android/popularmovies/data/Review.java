package com.example.android.popularmovies.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by admin on 19-07-2015.
 */
public class Review implements Parcelable {
    private String autoher;
    private String comment;

    public Review(String autoher, String comment) {
        this.autoher = autoher;
        this.comment = comment;
    }

    public String getAutoher() {
        return autoher;
    }

    public void setAutoher(String autoher) {
        this.autoher = autoher;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(autoher);
        dest.writeString(comment);

    }

    private Review (Parcel in) {
        this.autoher = in.readString();
        this.comment = in.readString();
    }

    @Override
    public String toString() {
        return "Review{" +
                "autoher='" + autoher + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }

    public static final Parcelable.Creator<Review> CREATOR = new Parcelable.Creator<Review>() {
        @Override
        public Review createFromParcel(Parcel source) {
            return new Review(source);
        }

        @Override
        public Review[] newArray(int size) {
            return new Review[size];
        }
    };
}
