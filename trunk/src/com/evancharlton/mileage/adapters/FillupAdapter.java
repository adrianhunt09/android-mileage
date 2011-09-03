
package com.evancharlton.mileage.adapters;

import java.text.DecimalFormat;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.evancharlton.mileage.FillupInfoActivity;
import com.evancharlton.mileage.R;
import com.evancharlton.mileage.dao.Fillup;
import com.evancharlton.mileage.dao.Vehicle;
import com.evancharlton.mileage.math.Calculator;
import com.evancharlton.mileage.provider.tables.FillupsTable;
import com.evancharlton.mileage.views.FormattedCurrencyView;
import com.evancharlton.mileage.views.FormattedDateView;
import com.evancharlton.mileage.views.FormattedNumberView;

public class FillupAdapter extends BaseAdapter implements View.OnClickListener {
    private static final String TAG = "FillupAdapter";

    private static final DecimalFormat ECONOMY_FORMAT = new DecimalFormat("0.00");
    private static final DecimalFormat VOLUME_FORMAT = new DecimalFormat("0.00");
    private static final String[] PROJECTION = FillupsTable.PROJECTION;

    private final Context mContext;

    private Cursor mCursor;

    private Vehicle mVehicle;

    private String mVolumeUnits;

    private String mEconomyUnits;

    private double mAvgEconomy;

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            requery();
        }
    };

    public FillupAdapter(Context context, Vehicle vehicle) {
        mContext = context;
        mContext.getContentResolver().registerContentObserver(FillupsTable.BASE_URI, true,
                mObserver);
        setVehicle(vehicle);
    }

    public void setVehicle(Vehicle vehicle) {
        mVehicle = vehicle;
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = mContext.getContentResolver().query(FillupsTable.BASE_URI, PROJECTION,
                Fillup.VEHICLE_ID + " = ?", new String[] {
                String.valueOf(mVehicle.getId())
        }, Fillup.ODOMETER + " DESC");

        mVolumeUnits = " " + Calculator.getVolumeUnitsAbbr(mContext, vehicle);
        mEconomyUnits = " " + Calculator.getEconomyUnitsAbbr(mContext, vehicle);
    }

    public void requery() {
        mCursor.requery();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        if (mCursor != null && mCursor.moveToPosition(position)) {
            return new Fillup(mCursor);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (mCursor != null && mCursor.moveToPosition(position)) {
            return mCursor.getLong(mCursor.getColumnIndex(Fillup._ID));
        }
        return -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        mCursor.moveToPosition(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.fillup_list_item, parent,
                    false);
        }

        Holder holder = (Holder) convertView.getTag();
        if (holder == null) {
            holder = new Holder(convertView);
        }

        String value;

        holder.dateView.setText(mCursor.getString(mCursor.getColumnIndex(Fillup.DATE)));

        value = VOLUME_FORMAT.format(mCursor.getDouble(mCursor.getColumnIndex(Fillup.VOLUME)))
                + mVolumeUnits;
        holder.volume.setText(value);

        String currency = mVehicle.getCurrency();
        holder.price.setCurrencySymbol(currency);
        holder.price.setText(mCursor.getString(mCursor.getColumnIndex(Fillup.UNIT_PRICE)));

        // holder.metaField.setText(mCursor.getString(mCursor.getColumnIndex(FillupField.VALUE)));
        holder.info.setOnClickListener(this);
        holder.info.setTag(getItem(position));

        // Set the economy
        double economy = mCursor.getDouble(mCursor.getColumnIndex(Fillup.ECONOMY));

        holder.economy.setTextColor(holder.metaField.getTextColors().getDefaultColor());
        if (economy == 0) {
            value = mContext.getString(R.string.status_calculating);
        } else if (mCursor.getInt(mCursor.getColumnIndex(Fillup.PARTIAL)) == 1) {
            value = mContext.getString(R.string.status_partial);
        } else {
            value = ECONOMY_FORMAT.format(economy) + mEconomyUnits;
            if (mAvgEconomy > 0) {
                if (Calculator.isBetterEconomy(mVehicle, economy, mAvgEconomy)) {
                    holder.economy.setTextColor(0xFF0AB807);
                } else {
                    holder.economy.setTextColor(0xFFD90000);
                }
            }
        }

        holder.economy.setText(value);

        return convertView;
    }

    @Override
    public void onClick(View v) {
        Fillup fillup = (Fillup) v.getTag();
        if (fillup != null) {
            Intent intent = new Intent(mContext, FillupInfoActivity.class);
            intent.putExtra(Fillup._ID, fillup.getId());
            mContext.startActivity(intent);
        }
    }

    private static class Holder {
        public final ImageView info;
        public final FormattedDateView dateView;
        public final TextView metaField;
        public final FormattedNumberView volume;
        public final FormattedCurrencyView price;
        public final FormattedNumberView economy;

        public Holder(View convertView) {
            info = (ImageView) convertView.findViewById(android.R.id.icon);
            dateView = (FormattedDateView) convertView.findViewById(android.R.id.text1);
            metaField = (TextView) convertView.findViewById(android.R.id.text2);
            volume = (FormattedNumberView) convertView.findViewById(R.id.volume);
            price = (FormattedCurrencyView) convertView.findViewById(R.id.price);
            economy = (FormattedNumberView) convertView.findViewById(R.id.economy);

            convertView.setTag(this);
        }
    }

    public void calculationFinished(double avgEconomy) {
        Log.d(TAG, "Average economy: " + avgEconomy);
        mAvgEconomy = avgEconomy;
        notifyDataSetChanged();
    }

    public Context getContext() {
        return mContext;
    }
}
