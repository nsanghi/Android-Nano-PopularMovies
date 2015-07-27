package com.example.android.popularmovies;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageButton;

import com.example.android.popularmovies.data.MovieContract;
import com.example.android.popularmovies.data.Review;

import java.util.ArrayList;

// to save favorite movie data alongwith details into the database on non UI thread
// saving prcoess will frist erase any pre exisitng data for that movie and then save fresh data
public class SaveFavoriteTask extends AsyncTask<Void, Void, Void> {

    public final String LOG_TAG = SaveFavoriteTask.class.getSimpleName();

    Context mContext;
    long mExistingMovieRowId;
    private String mMovieCode;
    private String mOverviewText;
    private String mMovieImagePathText;
    private String mReleaseDateText;
    private String mTitleText;
    private String mRatingText;
    private String mRuntimeText;
    private ArrayList<String> mTrailers;
    private ArrayList<Review> mReviews;
    private ImageButton mButton;

    public SaveFavoriteTask
            (Context mContext, long mExistingMovieRowId, String mMovieCode,
             String mOverviewText, String mMovieImagePathText, String mReleaseDateText,
             String mTitleText, String mRatingText, String mRuntimeText,
             ArrayList<String> mTrailers, ArrayList<Review> mReviews, ImageButton mButton) {
        this.mContext = mContext;
        this.mExistingMovieRowId = mExistingMovieRowId;
        this.mMovieCode = mMovieCode;
        this.mOverviewText = mOverviewText;
        this.mMovieImagePathText = mMovieImagePathText;
        this.mReleaseDateText = mReleaseDateText;
        this.mTitleText = mTitleText;
        this.mRatingText = mRatingText;
        this.mRuntimeText = mRuntimeText;
        this.mTrailers = mTrailers;
        this.mReviews = mReviews;
        this.mButton = mButton;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (mExistingMovieRowId > 0) {
            deleteFavoriteMovieData();
        }
        saveFavoriteMovieData();
        return null;
    }

    private void deleteFavoriteMovieData() {
        int count;
        count = mContext.getContentResolver().delete(MovieContract.TrailerEntry
                .buildTrailerMovie
                        (mExistingMovieRowId),null,null);
        Log.d(LOG_TAG, "No. of trailer rows deleted: " + count);

        count = mContext.getContentResolver().delete(MovieContract.ReviewEntry
                .buildReviewMovie
                        (mExistingMovieRowId),null,null);
        Log.d(LOG_TAG, "No. of review rows deleted: " + count);
        count = mContext.getContentResolver().delete(MovieContract.MovieEntry
                .buildMovieUriFromMovieCode(mMovieCode),null,null);
        Log.d(LOG_TAG, "No. of movie rows deleted: " + count);
    }

    private void saveFavoriteMovieData() {

        Uri returnUri;

        ContentValues movieValues = new ContentValues();
        movieValues.put(MovieContract.MovieEntry.COLUMN_MOVIE_CODE, mMovieCode);
        movieValues.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, mOverviewText);
        movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, mMovieImagePathText);
        movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, mReleaseDateText);
        movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, mTitleText);
        movieValues.put(MovieContract.MovieEntry.COLUMN_RATING, mRatingText);
        movieValues.put(MovieContract.MovieEntry.COLUMN_RUNTIME, mRuntimeText);
        returnUri = mContext.getContentResolver().insert(MovieContract.MovieEntry
                        .CONTENT_URI,
                movieValues);
        Log.d(LOG_TAG, "insert into Movie returned with Uri: " + returnUri);
        long movieRowId = ContentUris.parseId(returnUri);

        ContentValues values;
        for (String youtubeUrl : this.mTrailers) {
            values = new ContentValues();
            values.put(MovieContract.TrailerEntry.COLUMN_YOUTUBE_URL, youtubeUrl);
            values.put(MovieContract.TrailerEntry.COLUMN_MOVIE_ID, movieRowId);
            returnUri = mContext.getContentResolver().insert(MovieContract.TrailerEntry
                            .CONTENT_URI,
                    values);
            Log.d(LOG_TAG, "insert into Trailer returned with Uri: " + returnUri);

        }

        for (Review review : this.mReviews) {
            values = new ContentValues();
            values.put(MovieContract.ReviewEntry.COLUMN_REVIEWER_NAME, review.getAutoher());
            values.put(MovieContract.ReviewEntry.COLUMN_REVIEWER_COMMENT, review.getComment());
            values.put(MovieContract.ReviewEntry.COLUMN_MOVIE_ID, movieRowId);
            returnUri = mContext.getContentResolver().insert(MovieContract.ReviewEntry
                    .CONTENT_URI, values);
            Log.d(LOG_TAG, "insert into Review returned with Uri: " + returnUri);

        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        //update the button to make sure that same data is not saved again and visual clue to
        // the
        //user to tell that the request to save data has been processed
        //though technically till now only the request to persist the data has just been handed
        //over to ContentResolver
        mButton.setImageResource(R.drawable.ic_star_black_48dp);
    }
}
