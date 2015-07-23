package com.example.android.popularmovies.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by admin on 21-07-2015.
 */
public class MovieProvider extends ContentProvider {

    static final int MOVIE = 100;
    static final int MOVIE_WITH_ID = 200;
    static final int TRAILER = 300;
    static final int TRAILER_WITH_MOVIE_ID = 400;
    static final int REVIEW = 500;
    static final int REVIEW_WITH_MOVIE_ID = 600;
    public static final String LOG_TAG = MovieProvider.class.getSimpleName();

    public static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final String sMovieSelection =
            MovieContract.MovieEntry.TABLE_NAME + "." +
                    MovieContract.MovieEntry._ID + " = ?";

    private static final String sTrailerSelection =
            MovieContract.TrailerEntry.TABLE_NAME + "." +
                    MovieContract.TrailerEntry.COLUMN_MOVIE_ID + " = ?";

    private static final String sReviewSelection =
            MovieContract.ReviewEntry.TABLE_NAME + "." +
                    MovieContract.ReviewEntry.COLUMN_MOVIE_ID + " = ?";

    private MovieDbHelper mOpenHelper;

    static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MovieContract.CONTENT_AUTHORITY;
        // "movie"
        matcher.addURI(authority, MovieContract.PATH_MOVIE, MOVIE);
        // "movie/#"
        matcher.addURI(authority, MovieContract.PATH_MOVIE + "/#/", MOVIE_WITH_ID);
        // "trailer"
        matcher.addURI(authority, MovieContract.PATH_TRAILER, TRAILER);
        // "movie/#/trailer"
        matcher.addURI(authority, MovieContract.PATH_MOVIE + "/#/" + MovieContract.PATH_TRAILER,
                TRAILER_WITH_MOVIE_ID);
        // "review"
        matcher.addURI(authority, MovieContract.PATH_REVIEW, REVIEW);
        // "movie/#/review"
        matcher.addURI(authority, MovieContract.PATH_MOVIE + "/#/" + MovieContract.PATH_REVIEW,
                REVIEW_WITH_MOVIE_ID);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new MovieDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case MOVIE:
                return MovieContract.MovieEntry.CONTENT_TYPE;
            case MOVIE_WITH_ID:
                return MovieContract.MovieEntry.CONTENT_ITEM_TYPE;
            case TRAILER:
                return MovieContract.TrailerEntry.CONTENT_TYPE;
            case TRAILER_WITH_MOVIE_ID:
                return MovieContract.TrailerEntry.CONTENT_TYPE;
            case REVIEW:
                return MovieContract.ReviewEntry.CONTENT_TYPE;
            case REVIEW_WITH_MOVIE_ID:
                return MovieContract.ReviewEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        Cursor retCursor;
        Log.d(LOG_TAG, "query uri called: " + uri);

        switch (sUriMatcher.match(uri)) {
            // "movie"
            case MOVIE: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            // "movie/#"
            case MOVIE_WITH_ID: {
                selectionArgs = new String[]{Long.toString(ContentUris.parseId(uri))};
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        sMovieSelection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            // "trailer"
            case TRAILER: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.TrailerEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            // "movie/#/trailer"
            case TRAILER_WITH_MOVIE_ID: {
                selectionArgs =
                        new String[]{Long.toString(MovieContract.TrailerEntry.getMovieIdFromUri
                                (uri))};
                Log.d(LOG_TAG, "Selection Args : " + Arrays.toString(selectionArgs));
                Log.d(LOG_TAG, "sTrailerSelection: " + sTrailerSelection);

                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.TrailerEntry.TABLE_NAME,
                        projection,
                        sTrailerSelection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            // "review"
            case REVIEW: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.ReviewEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            // "movie/#/review"
            case REVIEW_WITH_MOVIE_ID: {
                selectionArgs =
                        new String[]{Long.toString(MovieContract.ReviewEntry.getMovieIdFromUri
                                (uri))};
                Log.d(LOG_TAG, "Selection Args : " + Arrays.toString(selectionArgs));
                Log.d(LOG_TAG, "sReviewSelection: " + sReviewSelection);
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.ReviewEntry.TABLE_NAME,
                        projection,
                        sReviewSelection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;
        Log.d(LOG_TAG, "insert uri called: " + uri);
        switch (match) {
            case MOVIE : {
                long _id = db.insert(MovieContract.MovieEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = MovieContract.MovieEntry.buildMovieUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case TRAILER : {
                long _id = db.insert(MovieContract.TrailerEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = MovieContract.TrailerEntry.buildTrailerUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case REVIEW : {
                long _id = db.insert(MovieContract.ReviewEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = MovieContract.ReviewEntry.buildReviewUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int rowsDeleted;

        if (null == selection) selection="1";

        switch (match) {
            case MOVIE :
                rowsDeleted =
                        db.delete(MovieContract.MovieEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case TRAILER :
                rowsDeleted =
                        db.delete(MovieContract.TrailerEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case REVIEW :
                rowsDeleted =
                        db.delete(MovieContract.ReviewEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return  rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        //NO plan to support this
        return 0;
    }
}
