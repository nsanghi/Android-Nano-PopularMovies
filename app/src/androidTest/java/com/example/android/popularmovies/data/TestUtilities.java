package com.example.android.popularmovies.data;

import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

import com.example.android.popularmovies.utils.PollingCheck;

import java.util.Map;
import java.util.Set;

/**
 * Created by admin on 21-07-2015.
 */
public class TestUtilities extends AndroidTestCase {


    static void validateCursor(String error, Cursor valueCursor, ContentValues expectedValues) {
        assertTrue("Empty cursor returned. " + error, valueCursor.moveToFirst());
        validateCurrentRecord(error, valueCursor, expectedValues);
        valueCursor.close();
    }

    static void validateCurrentRecord(String error, Cursor valueCursor, ContentValues expectedValues) {
        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse("Column '" + columnName + "' not found. " + error, idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals("Value '" + entry.getValue().toString() +
                    "' did not match the expected value '" +
                    expectedValue + "'. " + error, expectedValue, valueCursor.getString(idx));
        }
    }


    /*
    Students: The functions we provide inside of TestProvider use this utility class to test
    the ContentObserver callbacks using the PollingCheck class that we grabbed from the Android
    CTS tests.

    Note that this only tests that the onChange function is called; it does not test that the
    correct Uri is returned.
 */
    static class TestContentObserver extends ContentObserver {
        final HandlerThread mHT;
        boolean mContentChanged;

        static TestContentObserver getTestContentObserver() {
            HandlerThread ht = new HandlerThread("ContentObserverThread");
            ht.start();
            return new TestContentObserver(ht);
        }

        private TestContentObserver(HandlerThread ht) {
            super(new Handler(ht.getLooper()));
            mHT = ht;
        }

        // On earlier versions of Android, this onChange method is called
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mContentChanged = true;
        }

        public void waitForNotificationOrFail() {
            // Note: The PollingCheck class is taken from the Android CTS (Compatibility Test Suite).
            // It's useful to look at the Android CTS source for ideas on how to test your Android
            // applications.  The reason that PollingCheck works is that, by default, the JUnit
            // testing framework is not running on the main Android application thread.
            new PollingCheck(5000) {
                @Override
                protected boolean check() {
                    return mContentChanged;
                }
            }.run();
            mHT.quit();
        }
    }

    static TestContentObserver getTestContentObserver() {
        return TestContentObserver.getTestContentObserver();
    }


    static ContentValues createMovieValues(String code) {
        ContentValues movieValues = new ContentValues();
        movieValues.put(MovieContract.MovieEntry.COLUMN_MOVIE_CODE, code);
        movieValues.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, "overview");
        movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, "http://somepath");
        movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, "2001-01-01");
        movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, "Title");
        movieValues.put(MovieContract.MovieEntry.COLUMN_RATING, "4.5");
        movieValues.put(MovieContract.MovieEntry.COLUMN_RUNTIME, "110");
        return movieValues;
    }

    static  ContentValues createTrailerValues(long _id, String url) {
        ContentValues trailerValues = new ContentValues();
        trailerValues.put(MovieContract.TrailerEntry.COLUMN_YOUTUBE_URL, url);
        trailerValues.put(MovieContract.TrailerEntry.COLUMN_MOVIE_ID, _id);
        return trailerValues;
    }

    static  ContentValues createReviewValues(long _id, int index) {
        ContentValues reviewValues = new ContentValues();
        reviewValues.put(MovieContract.ReviewEntry.COLUMN_REVIEWER_NAME, "name"+index);
        reviewValues.put(MovieContract.ReviewEntry.COLUMN_REVIEWER_COMMENT, "comment"+index);
        reviewValues.put(MovieContract.ReviewEntry.COLUMN_MOVIE_ID, _id);
        return reviewValues;
    }


}
