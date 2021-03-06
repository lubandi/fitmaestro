package com.leonty.fitmaestro;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.leonty.fitmaestro.domain.Exercise;
import com.leonty.fitmaestro.domain.FitmaestroDb;
import com.leonty.fitmaestro.domain.LogEntry;
import com.leonty.fitmaestro.domain.Session;

public class SessionRepsList extends ListActivity {

	private Long mSessionConnectorId;
	private Long mSessionId;
	private Long mExerciseId;
	private Long mExType;
	private Long mListPosition;
	private Dialog mEditRepsDialog;
	private Dialog mCounterDialog;
	private Long mSessionRepsId;
	private Long mLastChronometerBase;
	private Chronometer mCounter = null;
	private TextView sumTxt;
	private TextView maxTxt;
	private SharedPreferences mPrefs;
	private String mUnits;

	ArrayList<HashMap<String, String>> mSessionRepsList = new ArrayList<HashMap<String, String>>();

	private static final int ACTIVITY_EDIT = 1;

	private static final int DIALOG_EDIT_REPS = 2;
	private static final int DIALOG_CHRONOMETER = 3;
	private static final int INSERT_ID = Menu.FIRST;
	private static final int DELETE_ID = Menu.FIRST + 1;
	private static final int VIEW_STATS_ID = Menu.FIRST + 2;

	private FitmaestroDb db;
	private Exercise exercise;	
	private Session session;
	private LogEntry logEntries;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.session_reps_list);

		db = new FitmaestroDb(this).open();
		exercise = new Exercise(db);	
		session = new Session(db);
		logEntries = new LogEntry(db);
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mUnits = mPrefs.getString("units", getText(R.string.default_unit).toString());
		
		mSessionConnectorId = savedInstanceState != null ? savedInstanceState
				.getLong(FitmaestroDb.KEY_ROWID) : null;
		if (mSessionConnectorId == null) {
			Bundle extras = getIntent().getExtras();
			mSessionConnectorId = extras != null ? extras
					.getLong(FitmaestroDb.KEY_ROWID) : null;
		}

		fillData();
		registerForContextMenu(getListView());

		// restoring chronometer counter
		mLastChronometerBase = savedInstanceState != null ? savedInstanceState
				.getLong("last_chronometer_base") : null;

	}
	
	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(FitmaestroDb.KEY_ROWID, mSessionConnectorId);

		// saving chronometer state
		if (mCounter != null) {
			long currentChronometerBase = mCounter.getBase();
			outState.putLong("last_chronometer_base", currentChronometerBase);
			Log.i("CHRONOMETER VALUE:", String.valueOf(currentChronometerBase));
		}

	}

	private void fillData() {

		Cursor sessionConnectorCursor = session
				.fetchSessionConnector(mSessionConnectorId);
		startManagingCursor(sessionConnectorCursor);
		mSessionId = sessionConnectorCursor.getLong(sessionConnectorCursor
				.getColumnIndexOrThrow(FitmaestroDb.KEY_SESSIONID));
		mExerciseId = sessionConnectorCursor.getLong(sessionConnectorCursor
				.getColumnIndexOrThrow(FitmaestroDb.KEY_EXERCISEID));

		Cursor exerciseCursor = (Cursor) exercise.fetchExercise(mExerciseId);
		startManagingCursor(exerciseCursor);
		mExType = exerciseCursor.getLong(exerciseCursor
				.getColumnIndexOrThrow(FitmaestroDb.KEY_TYPE));
		
		if(mExType == 0){
			findViewById(R.id.x_col).setVisibility(View.GONE);
			findViewById(R.id.planned_weight_col).setVisibility(
					View.GONE);
			findViewById(R.id.x_done_col).setVisibility(View.GONE);
			findViewById(R.id.weight_col).setVisibility(View.GONE);
		}
		
		// getting max and total values for session
		// R.id.sum_txt, R.id.max_txt
		String maxValue = "0";
		String sumValue = "0";
		Cursor totalsCursor = logEntries.getTotalsForExercise(mExerciseId, mSessionId, mExType.intValue());
		startManagingCursor(totalsCursor);
		if(totalsCursor.getCount() > 0){
			if(mExType == 0){
				maxValue = String.valueOf(totalsCursor.getLong(totalsCursor.getColumnIndex("max")));
				sumValue = String.valueOf(totalsCursor.getLong(totalsCursor.getColumnIndex("sum")));				
			}else{
				maxValue = String.valueOf(totalsCursor.getDouble(totalsCursor.getColumnIndex("max")));
				sumValue = String.valueOf(totalsCursor.getDouble(totalsCursor.getColumnIndex("sum")));
				maxValue += " " + mUnits;
				sumValue += " " + getString(R.string.reps_small) + "*"+ mUnits;
			}
		}
		maxTxt = (TextView) findViewById(R.id.max_txt);
		sumTxt = (TextView) findViewById(R.id.sum_txt);
		maxTxt.setText(maxValue);
		sumTxt.setText(sumValue);
		
		SessionRepsArray repsArray = new SessionRepsArray(this, mSessionId,
				mExerciseId, mSessionConnectorId);
		mSessionRepsList = repsArray.getRepsArray();

		String[] from = new String[] { "reps", "weight", "planned_reps",
				"planned_weight" };
		int[] to = new int[] { R.id.reps_value, R.id.weight_value,
				R.id.planned_reps_value, R.id.planned_weight_value };

		SpecialAdapter SessionRepsAdapter = new SpecialAdapter(this,
				mSessionRepsList, R.layout.session_reps_list_row, from, to);

		setListAdapter(SessionRepsAdapter);

		Log.i("EX TYPE: ", String.valueOf(mExType));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem insert = menu.add(0, INSERT_ID, 0, R.string.add_entry);
		insert.setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, VIEW_STATS_ID, 0, R.string.view_stats);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case INSERT_ID:
			createEntry();
			return true;
		case VIEW_STATS_ID:
			viewStats();
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	private void createEntry() {
		mListPosition = null;
		showDialog(DIALOG_EDIT_REPS);
		populateRepsDialog();
	}
	
	private void viewStats(){
		Intent i = new Intent(this,
				Statistics.class);
		i.putExtra(FitmaestroDb.KEY_ROWID, mExerciseId);
		startActivity(i);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		fillData();

		switch (requestCode) {
		case ACTIVITY_EDIT:
			Toast.makeText(this, R.string.session_reps_entry_edited,
					Toast.LENGTH_SHORT).show();
			break;
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info;
		info = (AdapterView.AdapterContextMenuInfo) menuInfo;

		// String title = ((TextView) info.targetView).getText().toString();

		if (mSessionRepsList.get(info.position).get("id") != null) {

			String reps = mSessionRepsList.get(info.position).get("reps");
			String weight = mSessionRepsList.get(info.position).get("weight");

			if (mExType == 0) {
				menu.setHeaderTitle(reps);
			} else {
				menu.setHeaderTitle(reps + " x " + weight);
			}

			menu.add(0, DELETE_ID, 1, R.string.delete_session_reps_entry);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();

		Long id = Long.valueOf(mSessionRepsList.get(info.position).get("id"));
		switch (item.getItemId()) {
		case DELETE_ID:
			Log.i("DELETE SESSION REPS, ID: ", String.valueOf(id));
			session.deleteSessionRepsEntry(id);
			fillData();
			return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		mListPosition = Long.valueOf(position);
		showDialog(DIALOG_EDIT_REPS);
		populateRepsDialog();
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case DIALOG_EDIT_REPS:
			LayoutInflater factory = LayoutInflater.from(this);
			final View repsEditView = factory.inflate(
					R.layout.edit_session_reps_entry, null);

			final EditText repsText = (EditText) repsEditView
					.findViewById(R.id.editText_reps);
			final EditText weightText = (EditText) repsEditView
					.findViewById(R.id.editText_weight);

			if (mExType == Long.valueOf(0)) {

				repsEditView.findViewById(R.id.text_weight).setVisibility(
						View.GONE);
				weightText.setText("0");
				weightText.setVisibility(View.GONE);
				repsEditView.findViewById(R.id.text_x).setVisibility(View.GONE);
			}

			mEditRepsDialog = new AlertDialog.Builder(this).setTitle(
					R.string.edit_session_reps_entry).setView(repsEditView)
					.setPositiveButton(R.string.done,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked OK so do some stuff */

									boolean showCounter = false;
									String reps = repsText.getText().toString();
									String weight = weightText.getText()
											.toString();

									if (reps.length() > 0
											&& weight.length() > 0) {

										// entry is new, so we add it
										if (mSessionRepsId == null) {

											// if it has an entry than it's
											// planned, so we get corresponding
											// sessionDetailId, if not - it's 0
											Long sessionDetailId = mListPosition != null ? Long
													.valueOf(mSessionRepsList
															.get(
																	mListPosition
																			.intValue())
															.get(
																	"session_detail_id"))
													: 0;

											session.createSessionRepsEntry(
													mSessionId, mExerciseId,
													sessionDetailId, Integer
															.parseInt(reps
																	.trim()),
													Float
															.valueOf(weight
																	.trim()));

											// setting counter flag to true
											showCounter = true;

										} else {

											// entry is old so we update it
											session.updateSessionRepsEntry(
													mSessionRepsId, Integer
															.parseInt(reps
																	.trim()),
													Float
															.valueOf(weight
																	.trim()));
										}

										// setting focus back on reps
										repsText.requestFocus();

										fillData();
										registerForContextMenu(getListView());
									}

									if (showCounter) {
										showDialog(DIALOG_CHRONOMETER);
										mCounter = (Chronometer) mCounterDialog
												.findViewById(R.id.counter);
										startCounter(SystemClock
												.elapsedRealtime());
									}
								}
							}).setNegativeButton(R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked cancel so do some stuff */
								}
							}).create();

			return mEditRepsDialog;

		case DIALOG_CHRONOMETER:
			LayoutInflater counterInflater = LayoutInflater.from(this);
			final View counterView = counterInflater.inflate(R.layout.counter,
					null);
			final Chronometer counter = (Chronometer) counterView
					.findViewById(R.id.counter);

			mCounterDialog = new AlertDialog.Builder(this).setTitle(
					R.string.counter_title).setView(counterView)
					.setPositiveButton(R.string.stop,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

									/* User clicked OK so do some stuff */
									counter.stop();
								}
							}).create();

			if (mLastChronometerBase != null) {
				mCounter = counter;
				startCounter(mLastChronometerBase);
				mLastChronometerBase = null;
			}

			return mCounterDialog;
		}

		return null;
	}

	public void populateRepsDialog() {

		EditText repsText = (EditText) mEditRepsDialog
				.findViewById(R.id.editText_reps);
		EditText weightText = (EditText) mEditRepsDialog
				.findViewById(R.id.editText_weight);

		// we have an entry in the list
		if (mListPosition != null) {

			mSessionRepsId = mSessionRepsList.get(mListPosition.intValue())
					.get("id") != null ? Long.valueOf(mSessionRepsList.get(
					mListPosition.intValue()).get("id")) : null;

			// entry has corresponding entry in session - so prepopulate it
			if (mSessionRepsId != null) {
				repsText.setText(mSessionRepsList.get(mListPosition.intValue())
						.get("reps"));
				weightText.setText(mSessionRepsList.get(
						mListPosition.intValue()).get("weight"));
			} else {

				// it's not done yet - prepopulate it with planned values for
				// easier entry
				// empty values for now

				repsText.setText("");

				if (mExType == Long.valueOf(0)) {

					weightText.setText("0");
				} else {
					weightText.setText("");
				}

			}

			// empty the values for new extra entry
		} else {
			mSessionRepsId = null;
			repsText.setText("");

			if (mExType == Long.valueOf(0)) {

				weightText.setText("0");
			} else {
				weightText.setText("");
			}
		}
	}

	public void startCounter(long base) {
		mCounter.setBase(base);
		mCounter.start();
	}

	public class SpecialAdapter extends SimpleAdapter {
		private int[] colors = new int[] { 0x30FF0000, 0x300000FF };

		public SpecialAdapter(Context context,
				ArrayList<HashMap<String, String>> items, int resource,
				String[] from, int[] to) {
			super(context, items, resource, from, to);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			int colorPos = position % colors.length;
			view.setBackgroundColor(colors[colorPos]);

			 TextView plannedWeight = (TextView)
			 view.findViewById(R.id.planned_weight_value);
			 
			 // adding weight units if exercise is planned
			 if(Boolean.parseBoolean(mSessionRepsList.get(position).get("planned")) == true){
				 String plannedWeightValue = mSessionRepsList.get(position).get("planned_weight") + " " + mUnits;
				 plannedWeight.setText(plannedWeightValue);
			 }

			 TextView weight = (TextView)
			 view.findViewById(R.id.weight_value);
			 
			 // adding weight value if exercise is done
			 if(mSessionRepsList.get(position).get("id") != null){
				 String weightValue = mSessionRepsList.get(position).get("weight") + " " + mUnits;
				 weight.setText(weightValue);
			 }
			 //plannedReps.setText("Dyg: " + String.valueOf(position) );

			if (mExType == 0) {

				view.findViewById(R.id.planned_weight_value).setVisibility(
						View.GONE);
				view.findViewById(R.id.weight_value).setVisibility(View.GONE);
				view.findViewById(R.id.x_col_value).setVisibility(View.GONE);
				view.findViewById(R.id.x_col_value2).setVisibility(View.GONE);
			}

			return view;
		}
	}

}
