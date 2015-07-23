package com.example.android.popularmovies.data;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import com.example.android.popularmovies.data.MovieContract.MovieEntry;
import com.example.android.popularmovies.data.MovieContract.ReviewEntry;
import com.example.android.popularmovies.data.MovieContract.TrailerEntry;


/**
 * Created by admin on 21-07-2015.
 */
public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    public void deleteAllRecords() {
        mContext.getContentResolver().delete(
                TrailerEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                ReviewEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                MovieEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                TrailerEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Trailer table during delete",
                0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                ReviewEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Trailer table during delete",
                0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                MovieEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Trailer table during delete",
                0, cursor.getCount());
        cursor.close();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteAllRecords();
    }

    /*
        This test checks to make sure that the content provider is registered correctly.
     */
    public void testProviderRegistry() {
        PackageManager pm = mContext.getPackageManager();

        // We define the component name based on the package name from the context and the
        // WeatherProvider class.
        ComponentName componentName = new ComponentName(mContext.getPackageName(),
                MovieProvider.class.getName());
        try {
            // Fetch the provider info using the component name from the PackageManager
            // This throws an exception if the provider isn't registered.
            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);

            // Make sure that the registered authority matches the authority from the Contract.
            assertEquals("Error: MovieProvider registered with authority: " + providerInfo
                            .authority +
                            " instead of authority: " + MovieContract.CONTENT_AUTHORITY,
                    providerInfo.authority, MovieContract.CONTENT_AUTHORITY);
        } catch (PackageManager.NameNotFoundException e) {
            // I guess the provider isn't registered correctly.
            assertTrue("Error: MovieProvider not registered at " + mContext.getPackageName(),
                    false);
        }
    }

    /*
            This test doesn't touch the database.  It verifies that the ContentProvider returns
            the correct type for each type of URI that it can handle.
         */
    public void testGetType() {
        // content://com.example.android.popularmovies/movie/
        String type = mContext.getContentResolver().getType(MovieEntry.CONTENT_URI);

        // vnd.android.cursor.dir/com.example.android.popularmovies/movie
        assertEquals("Error: the MovieEntry CONTENT_URI should return WeatherEntry.CONTENT_TYPE",
                MovieEntry.CONTENT_TYPE, type);

        long testid = 94074L;
        // content://com.example.android.popularmovies/movie/94074/trailer
        type = mContext.getContentResolver().getType(
                MovieEntry.buildMovieUri(testid));
        // vnd.android.cursor.dir/com.example.android.popularmovies/movie
        assertEquals("Error: the MovieEntry CONTENT_URI with movieId should return MovieEntry" +
                        ".CONTENT_TYPE",
                MovieEntry.CONTENT_ITEM_TYPE, type);

        // content://com.example.android.popularmovies/movie/94074/trailer
        assertTrue("content://com.example.android.popularmovies/movie/94074/trailer".equals
                (TrailerEntry.buildTrailerMovie(testid).toString()));


        type = mContext.getContentResolver().getType(
                TrailerEntry.buildTrailerMovie(testid));
        // vnd.android.cursor.dir/com.example.android.popularmovies/trailer
        assertEquals("Error: the TrailerEntry CONTENT_URI with movieId should return TrailerEntry" +
                        ".CONTENT_TYPE",
                TrailerEntry.CONTENT_TYPE, type);

    }

    public void testInsertReadProvider() {
        ContentValues testMovieValues1 = TestUtilities.createMovieValues("code1");
        ContentValues testMovieValues2 = TestUtilities.createMovieValues("code2");

        // Register a content observer for our insert.  This time, directly with the content
        // resolver
        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(MovieEntry.CONTENT_URI, true, tco);
        Uri movieUri = mContext.getContentResolver().insert(MovieEntry.CONTENT_URI,
                testMovieValues1);

        // Did our content observer get called?  Students:  If this fails, your insert location
        // isn't calling getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        long movieRowId = ContentUris.parseId(movieUri);

        // Verify we got a row back.
        assertTrue(movieRowId != -1);

        // Data's inserted.  IN THEORY.  Now pull some out to stare at it and verify it made
        // the round trip.

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                MovieEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating MovieEntry.",
                cursor, testMovieValues1);


        //Now add one row of Trailer data
        ContentValues trailerValues1 = TestUtilities.createTrailerValues(movieRowId, "trailer1");
        tco = TestUtilities.getTestContentObserver();

        mContext.getContentResolver().registerContentObserver(TrailerEntry.CONTENT_URI, true, tco);

        Uri trailerInsertUri = mContext.getContentResolver()
                .insert(TrailerEntry.CONTENT_URI, trailerValues1);
        assertTrue(trailerInsertUri != null);

        // Did our content observer get called?  If this fails, your insert trailer
        // in your ContentProvider isn't calling
        // getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        // A cursor is your primary interface to the query results.
        Cursor trailerCursor = mContext.getContentResolver().query(
                TrailerEntry.buildTrailerMovie(movieRowId),  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null // columns to group by
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating TrailerEntry " +
                        "insert.",
                trailerCursor, trailerValues1);

        //Now add one row of Review data
        ContentValues reviewValues1 = TestUtilities.createReviewValues(movieRowId, 1);
        tco = TestUtilities.getTestContentObserver();

        mContext.getContentResolver().registerContentObserver(ReviewEntry.CONTENT_URI, true, tco);

        Uri reviewInsertUri = mContext.getContentResolver()
                .insert(ReviewEntry.CONTENT_URI, reviewValues1);
        assertTrue(reviewInsertUri != null);

        // Did our content observer get called?  If this fails, your insert trailer
        // in your ContentProvider isn't calling
        // getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        // A cursor is your primary interface to the query results.
        Cursor reviewCursor = mContext.getContentResolver().query(
                ReviewEntry.buildReviewMovie(movieRowId),  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null // columns to group by
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating reviewEntry " +
                        "insert.",
                reviewCursor, reviewValues1);

        //insert onr more trailer and one more review
        //so that given movie has 2 reviews and 2 trailer
        //and then check if count is 2 for each type
        ContentValues trailerValues2 = TestUtilities.createTrailerValues(movieRowId, "trailer2");
        trailerInsertUri = mContext.getContentResolver()
                .insert(TrailerEntry.CONTENT_URI, trailerValues2);


        trailerCursor = mContext.getContentResolver().query(
                TrailerEntry.buildTrailerMovie(movieRowId),  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null // columns to group by
        );

        assertEquals("Error: 2 rows not inserted into Trailer", 2, trailerCursor.getCount());

        ContentValues reviewValues2 = TestUtilities.createReviewValues(movieRowId, 2);
        reviewInsertUri = mContext.getContentResolver().insert(ReviewEntry.CONTENT_URI,
                reviewValues2);

        reviewCursor = mContext.getContentResolver().query(
                ReviewEntry.buildReviewMovie(movieRowId),  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null // columns to group by
        );

        assertEquals("Error: 2 rows not inserted into Review", 2, reviewCursor.getCount());


        //now insert one more movie and see if a query returns back 2 rows of movies
        movieUri = mContext.getContentResolver().insert(MovieEntry.CONTENT_URI,
                testMovieValues2);

        cursor = mContext.getContentResolver().query(
                MovieEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        assertEquals("Error: 2 rows not inserted into Movie", 2, cursor.getCount());


    }
}
