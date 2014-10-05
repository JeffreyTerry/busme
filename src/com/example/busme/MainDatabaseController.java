package com.example.busme;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MainDatabaseController {
	public static final String NULL_QUERY = "sdf9832e";

	private static final String KEY_ROWID = "_id";
	private static final String KEY_SEARCH_START_QUERY = "start_query";
	private static final String KEY_SEARCH_END_QUERY = "end_query";
	private static final String DATABASE_NAME = "main_busme_db";
	private static final String DATABASE_TABLE = "searches_table";
	private static final int DATABASE_VERSION = 2;

	private DBHelper helper;
	private final Context context;
	private SQLiteDatabase localDatabase;

	private static class DBHelper extends SQLiteOpenHelper {
		public DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
				db.execSQL("CREATE TABLE " + DATABASE_TABLE + " ( " + KEY_ROWID
						+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
						+ KEY_SEARCH_START_QUERY + " TEXT, "
						+ KEY_SEARCH_END_QUERY + " TEXT )");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}

	}

	public MainDatabaseController(Context c) {
		context = c;
	}

	public MainDatabaseController open() throws SQLException {
		helper = new DBHelper(context);
		localDatabase = helper.getWritableDatabase();
		return this;
	}

	public void close() {
		helper.close();
	}

	public long addStartSearch(String start) {
		return addStartEndSearch(start, NULL_QUERY);
	}

	public long addEndSearch(String end) {
		return addStartEndSearch(NULL_QUERY, end);
	}

	public long addStartEndSearch(String start, String end) {
		// first check to make sure we haven't already added this query
		String[] columns = new String[] { KEY_ROWID, KEY_SEARCH_START_QUERY,
				KEY_SEARCH_END_QUERY };
		Cursor cursor = localDatabase.query(DATABASE_TABLE, columns,
				KEY_SEARCH_START_QUERY + "='" + start + "' AND "
						+ KEY_SEARCH_END_QUERY + "='" + end + "'", null,
				null, null, null);
		if (cursor.getCount() > 0) {
			return -1L;
		}
		cursor.close();

		// then add the new query
		ContentValues cv = new ContentValues();
		cv.put(KEY_SEARCH_START_QUERY, start);
		cv.put(KEY_SEARCH_END_QUERY, end);
		return localDatabase.insert(DATABASE_TABLE, null, cv);
	}

	public void deleteStartSearch(String start) throws SQLException {
		localDatabase.delete(DATABASE_TABLE, KEY_SEARCH_START_QUERY + "="
				+ start, null);
	}

	public void deleteEndSearch(String end) throws SQLException {
		localDatabase.delete(DATABASE_TABLE, KEY_SEARCH_END_QUERY + "=" + end,
				null);
	}

	public void deleteStartEndSearch(String start, String end)
			throws SQLException {
		localDatabase.delete(DATABASE_TABLE, KEY_SEARCH_START_QUERY + "='"
				+ start + "' AND " + KEY_SEARCH_END_QUERY + "='" + end + "'", null);
	}

	/**
	 * Gets relevant search history data
	 * 
	 * @return An ArrayList<String[]> with each start query data at index 0 and
	 *         each end query data at index 1
	 */
	public ArrayList<String[]> getRelevantSearchData() {
		String[] columns = new String[] { KEY_ROWID, KEY_SEARCH_START_QUERY,
				KEY_SEARCH_END_QUERY };
		Cursor cursor = localDatabase.query(DATABASE_TABLE, columns, null,
				null, null, null, null);

		int indexStart = cursor.getColumnIndex(KEY_SEARCH_START_QUERY);
		int indexEnd = cursor.getColumnIndex(KEY_SEARCH_END_QUERY);

		ArrayList<String[]> results = new ArrayList<String[]>();
		String[] nextResult;
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
			nextResult = new String[2];
			nextResult[0] = cursor.getString(indexStart);
			nextResult[1] = cursor.getString(indexEnd);
			results.add(nextResult);
		}

		cursor.close();
		return results;
	}
}
