package com.evancharlton.mileage;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.evancharlton.mileage.dao.Field;
import com.evancharlton.mileage.dao.Fillup;
import com.evancharlton.mileage.dao.FillupField;
import com.evancharlton.mileage.dao.Dao.InvalidFieldException;
import com.evancharlton.mileage.provider.FillUpsProvider;
import com.evancharlton.mileage.provider.tables.FieldsTable;
import com.evancharlton.mileage.views.FieldView;

public class FillupActivity extends Activity {
	private EditText mOdometer;
	private EditText mVolume;
	private EditText mPrice;
	private Button mDate;
	private Button mSave;
	private CheckBox mPartial;
	private LinearLayout mFieldsContainer;
	private final ArrayList<FieldView> mFields = new ArrayList<FieldView>();
	private final Fillup mFillup = new Fillup(new ContentValues());

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fillup);

		mOdometer = (EditText) findViewById(R.id.odometer);
		mVolume = (EditText) findViewById(R.id.volume);
		mPrice = (EditText) findViewById(R.id.price);
		mDate = (Button) findViewById(R.id.date);
		mSave = (Button) findViewById(R.id.save_btn);
		mPartial = (CheckBox) findViewById(R.id.partial);
		mFieldsContainer = (LinearLayout) findViewById(R.id.container);
		Cursor fields = managedQuery(Uri.withAppendedPath(FillUpsProvider.BASE_URI, FieldsTable.FIELDS_URI), FieldsTable.getFullProjectionArray(),
				null, null, null);
		LayoutInflater inflater = LayoutInflater.from(this);
		while (fields.moveToNext()) {
			String hint = fields.getString(fields.getColumnIndex(Field.TITLE));
			long id = fields.getLong(fields.getColumnIndex(Field._ID));
			mFillup.setId(id);
			View container = inflater.inflate(R.layout.fillup_field, null);
			FieldView field = (FieldView) container.findViewById(R.id.field);
			field.setFieldId(id);
			field.setId((int) id);
			field.setHint(hint);
			mFieldsContainer.addView(container);
			mFields.add(field);

			if (savedInstanceState != null) {
				String value = savedInstanceState.getString(field.getKey());
				if (value != null) {
					field.setText(value);
				}
			}
		}
		if (fields.getCount() == 0) {
			mFieldsContainer.setVisibility(View.GONE);
		}

		mSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				save();
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		for (FieldView fieldView : mFields) {
			outState.putString(fieldView.getKey(), fieldView.getText().toString());
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	private void save() {
		// TODO: add error catching
		mFillup.setVolume(Double.parseDouble(mVolume.getText().toString()));
		mFillup.setPrice(Double.parseDouble(mPrice.getText().toString()));
		mFillup.setOdometer(Double.parseDouble(mOdometer.getText().toString()));
		mFillup.setPartial(mPartial.isChecked());

		try {
			if (mFillup.save(this)) {
				// save the meta data
				for (FieldView fieldView : mFields) {
					FillupField field = new FillupField(new ContentValues());
					field.setFillupId(mFillup.getId());
					field.setTemplateId(fieldView.getFieldId());
					field.setValue(fieldView.getText().toString());
					field.save(this);
				}
			}
		} catch (InvalidFieldException exception) {
			Toast.makeText(this, getString(exception.getErrorMessage()), Toast.LENGTH_LONG).show();
		}
	}
}
