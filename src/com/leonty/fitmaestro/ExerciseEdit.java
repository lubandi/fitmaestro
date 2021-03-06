package com.leonty.fitmaestro;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.leonty.fitmaestro.domain.Exercise;
import com.leonty.fitmaestro.domain.ExerciseGroup;
import com.leonty.fitmaestro.domain.FitmaestroDb;

public class ExerciseEdit extends Activity {

	private EditText mTitleText;
	private EditText mDescText;
	private EditText mMaxReps;
	private EditText mMaxWeight;
	private ToggleButton mType;
	private Spinner mGroup;
	private Long mRowId;
	private Long mGroupId;
	private int mTypeVal;
	
	private FitmaestroDb db;
	private Exercise exercise;
	private ExerciseGroup exerciseGroup;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		db = new FitmaestroDb(this).open();
		exercise = new Exercise(db);
		exerciseGroup = new ExerciseGroup(db);
		
		setContentView(R.layout.edit_exercise);

		mTitleText = (EditText) findViewById(R.id.edit_name);
		mDescText = (EditText) findViewById(R.id.edit_description);
		mMaxReps = (EditText) findViewById(R.id.edit_max_reps);
		mMaxWeight = (EditText) findViewById(R.id.edit_max_weight);
		mType = (ToggleButton) findViewById(R.id.toggle_type);
		mGroup = (Spinner) findViewById(R.id.spinner_group);

		mType.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {

				Log.i("TOGGLE CHANGE", "Changed!");

				mTypeVal = mType.isChecked() ? 1 : 0;

				if (mTypeVal == 1) {
					findViewById(R.id.text_max_reps).setVisibility(View.GONE);
					mMaxReps.setVisibility(View.GONE);

					findViewById(R.id.text_max_weight).setVisibility(
							View.VISIBLE);
					mMaxWeight.setVisibility(View.VISIBLE);
				} else {
					findViewById(R.id.text_max_weight).setVisibility(View.GONE);
					mMaxWeight.setVisibility(View.GONE);

					findViewById(R.id.text_max_reps)
							.setVisibility(View.VISIBLE);
					mMaxReps.setVisibility(View.VISIBLE);
				}
				
			}
		});

		Button saveButton = (Button) findViewById(R.id.button_save);

		mRowId = savedInstanceState != null ? savedInstanceState
				.getLong(FitmaestroDb.KEY_ROWID) : null;
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras
					.getLong(FitmaestroDb.KEY_ROWID) : null;
		}

		populateFields();

		saveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
				setResult(RESULT_OK);
				finish();
			}
		});

	}
	
	@Override
	protected void onDestroy() {
		db.close();
		super.onDestroy();
	}

	private void populateFields() {

		if (mRowId != null) {
			Cursor excercise = exercise.fetchExercise(mRowId);
			startManagingCursor(excercise);
			mTitleText.setText(excercise.getString(excercise
					.getColumnIndexOrThrow(FitmaestroDb.KEY_TITLE)));
			mDescText.setText(excercise.getString(excercise
					.getColumnIndexOrThrow(FitmaestroDb.KEY_DESC)));
			mType
					.setChecked(excercise
							.getInt(excercise
									.getColumnIndexOrThrow(FitmaestroDb.KEY_TYPE)) == 1 ? true
							: false);
			mMaxReps.setText(excercise.getString(excercise
					.getColumnIndexOrThrow(FitmaestroDb.KEY_MAX_REPS)));
			mMaxWeight
					.setText(excercise
							.getString(excercise
									.getColumnIndexOrThrow(FitmaestroDb.KEY_MAX_WEIGHT)));
			mGroupId = excercise.getLong(excercise
					.getColumnIndexOrThrow(FitmaestroDb.KEY_GROUPID));
		}
		fillSpinner();
	}

	private void fillSpinner() {
		// populating spinner in any case
		Cursor GroupsCursor = exerciseGroup.fetchAllGroups();
		startManagingCursor(GroupsCursor);
		String[] from = new String[] { FitmaestroDb.KEY_TITLE };
		int[] to = new int[] { android.R.id.text1 };
		SimpleCursorAdapter groups = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, GroupsCursor, from, to);
		groups
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mGroup.setAdapter(groups);

		// if we are editing - set spinner selection to the right item
		if (mRowId != null) {
			if (GroupsCursor != null) {
				int id_index = GroupsCursor
						.getColumnIndexOrThrow(FitmaestroDb.KEY_ROWID);
				if (GroupsCursor.moveToFirst()) {
					int i = 0;
					do {
						if (GroupsCursor.getLong(id_index) == mGroupId) {
							mGroup.setSelection(i);
							break;
						}
						i++;
					} while (GroupsCursor.moveToNext());
				}

			}
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(FitmaestroDb.KEY_ROWID, mRowId);
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveState();
	}

	@Override
	protected void onResume() {
		super.onResume();
		populateFields();
	}

	private void saveState() {
		String title = mTitleText.getText().toString();
		String desc = mDescText.getText().toString();
		int type = mType.isChecked() ? 1 : 0;
		long groupId = mGroup.getSelectedItemId();

		Long maxReps = mMaxReps.getText().length() > 0 ? Long.valueOf(mMaxReps
				.getText().toString()) : 0;

		Float maxWeight = mMaxWeight.getText().length() > 0 ? Float
				.valueOf(mMaxWeight.getText().toString()) : 0;

		if (mRowId == null) {

			// create exercise only if title is not empty
			if (title.length() > 0) {
				long id = exercise.createExercise(title, desc, groupId, type,
						maxReps, maxWeight);
				if (id > 0) {
					mRowId = id;
				}
			}
		} else {
			exercise.updateExercise(mRowId, title, desc, groupId, type,
					maxReps, maxWeight);
		}
	}
}
