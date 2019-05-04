package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.widget.Toast;

import com.example.android.pets.R;

import static com.example.android.pets.data.PetContract.*;

public class PetProvider extends ContentProvider {

    // Create log tag name
    private static final String LOGTAG = PetProvider.class.getSimpleName();
    private PetDbHelper mDbHelper;
    /**
     * URI matcher code for the content URI for the pets table
     */
    public static final int PETS = 100;

    /**
     * URI matcher code for the content URI for a single pet in the pets table
     */
    public static final int PET_ID = 101;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.android.pets/pets" will map to the
        // integer code {@link #PETS}. This URI is used to provide access to MULTIPLE rows
        // of the pets table.
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);

        // The content URI of the form "content://com.example.android.pets/pets/#" will map to the
        // integer code {@link #PET_ID}. This URI is used to provide access to ONE single row
        // of the pets table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.example.android.pets/pets/3" matches, but
        // "content://com.example.android.pets/pets" (without a number at the end) doesn't match.
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);
    }


    public PetProvider() {
    }

    @Override
    public boolean onCreate() {
        // TODO: Create and initialize a PetDbHelper object to gain access to the pets database.
        mDbHelper = new PetDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // TODO: Implement this to handle query requests from clients.

        // Get access to the db
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Declare cursor
        Cursor cursor;

        // Match the Uri to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                cursor = db.query(PetEntry.TABLE_NAME, projection, null, null, null, null, sortOrder);
                break;
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                cursor = db.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, null);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set a notification URI on the cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;

    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        // Connect to the db in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Match the Uri to the specific code
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PETS:
                return db.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return db.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data at the given URI.
        // Match the Uri to the specific code
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PETS:
                return PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
// TODO: Implement this to handle requests to insert a new row.

// Match the Uri to the specific code
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PETS:
                return insertPet(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertPet(Uri uri, ContentValues values) {
        // Sanity Checks
        String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Pet requires a name");
        }

        Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
        if (gender == null || !PetEntry.isValidGender(gender)) {
            throw new IllegalArgumentException("Pet requires valid gender");
        }

        Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
        if (weight != null & weight < 0) {
            throw new IllegalArgumentException("Pet requires valid weight");
        }


        // Insert new pet into database
        // Get access to the db
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        long newId = db.insert(PetEntry.TABLE_NAME, null, values);

        // Show a toast message depending on whether or not the insertion was successful
        if (newId == -1) {
            // If the row ID is -1, then there was an error with insertion.
            Toast.makeText(getContext(), R.string.editor_insert_pet_failed, Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the insertion was successful and we can display a toast with the row ID.
            Toast.makeText(getContext(), R.string.editor_insert_pet_successful, Toast.LENGTH_SHORT).show();
        }
        // Notify all listeners that the data has changed
        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, newId);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
// TODO: Implement this to handle requests to update one or more rows.

// match the Uri to detect which route to follow
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return updatePet(PetEntry.CONTENT_URI, values, null, null);
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, values, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Sanity Checks
        if (values.containsKey(PetEntry.COLUMN_PET_NAME)) {
            String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }

        if (values.containsKey(PetEntry.COLUMN_PET_GENDER)) {
            Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
            if (gender == null || !PetEntry.isValidGender(gender)) {
                throw new IllegalArgumentException("Pet requires valid gender");
            }
        }

        if (values.containsKey(PetEntry.COLUMN_PET_WEIGHT)) {
            // Check that the weight is greater than or equal to 0 kg
            Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
            if (weight != null && weight < 0) {
                throw new IllegalArgumentException("Pet requires valid weight");
            }
        }

        // Check if there are new items to be updated
        if (values.size() == 0) {
            return 0;
        }

        // Otherwise, get writeable database to update the data
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Returns the number of database rows affected by the update statement
        return db.update(PetEntry.TABLE_NAME, values, selection, selectionArgs);
    }
}
