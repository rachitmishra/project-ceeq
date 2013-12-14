/**
 * 
 * @author Rachit Mishra
 * @licence The MIT License (MIT) Copyright (c) <2013> <Rachit Mishra> 
 *
 */

package in.ceeq.activities;

import in.ceeq.R;
import in.ceeq.actions.Choose;
import in.ceeq.actions.Reset;
import in.ceeq.helpers.PhoneHelper;
import in.ceeq.helpers.PhoneHelper.Phone;
import in.ceeq.helpers.PreferencesHelper;
import in.ceeq.receivers.DeviceAdmin;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.plus.PlusClient;

public class Firstrun extends Activity implements ConnectionCallbacks,
		OnConnectionFailedListener {

	private static final int DEVICE_ADMIN_ACTIVATION_REQUEST = 9011;

	private boolean exit = false, setPin, setContact;
	private EditText pinNumber;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_firstrun);

		setupHelpers();
		setupGoogleConnect();
		setupHelplist();

		Reset.getInstance(this).defaults();
		setupDevice();
		pinNumber = (EditText) findViewById(R.id.pinNumber);
		pinNumber.addTextChangedListener(new PinValidator());
	}

	public void setupDevice() {
		preferencesHelper.setString(PreferencesHelper.SIM_NUMBER,
				phoneHelper.getData(Phone.SIM_ID));
		preferencesHelper.setString(PreferencesHelper.IEMI_NUMBER,
				phoneHelper.getData(Phone.IEMI));
	}

	private PreferencesHelper preferencesHelper;
	private PhoneHelper phoneHelper;

	public void setupHelpers() {
		phoneHelper = PhoneHelper.getInstance(this);
		preferencesHelper = PreferencesHelper.getInstance(this);
	}

	private PlusClient plus;

	public void setupGoogleConnect() {
		plus = new PlusClient.Builder(this, this, this).setActions(
				"http://schemas.google.com/AddActivity").build();
	}

	private ExpandableListView helpList;

	public void setupHelplist() {
		helpList = (ExpandableListView) findViewById(R.id.help_list);
		ArrayList<String> help_list = new ArrayList<String>();
		help_list.add(getString(R.string.help_note_23));
		help_list.add(getString(R.string.help_note_24));
		ExpandableListAdapter helplistAdapter = new ListAdapter(this, help_list);
		helpList.setAdapter(helplistAdapter);
	}

	private ToggleButton toggle_admin;

	public void onButtonPressed(View v) {
		switch (v.getId()) {
		case R.id.chooseContact:
			Choose.getInstance(this).contact();
			break;
		case R.id.toggle_admin:
			toggle_admin = (ToggleButton) v.findViewById(R.id.toggle_admin);
			preferencesHelper.setBoolean(PreferencesHelper.DEVICE_ADMIN_STATUS,
					toggle_admin.isChecked());
			if (toggle_admin.isChecked()) {
				Intent intent = new Intent(
						DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
				ComponentName deviceAdminComponentName = new ComponentName(
						this, DeviceAdmin.class);
				intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
						deviceAdminComponentName);
				intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
						getString(R.string.help_note_25));
				startActivityForResult(intent, DEVICE_ADMIN_ACTIVATION_REQUEST);
			} else {
				preferencesHelper.setBoolean(
						PreferencesHelper.DEVICE_ADMIN_STATUS, false);
				Toast.makeText(Firstrun.this, R.string.toast_string_5,
						Toast.LENGTH_LONG).show();
			}
			break;

		case R.id.done:
			if (setPin & setContact) {
				preferencesHelper.setBoolean(
						PreferencesHelper.APP_INITIALIZATION_STATUS, true);
				Intent launchHome = new Intent(Firstrun.this, Home.class);
				launchHome.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				launchHome.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
				launchHome.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivity(launchHome);
				overridePendingTransition(R.drawable.fadeout, R.drawable.fadein);
			} else if (!setPin)
				Toast.makeText(Firstrun.this, R.string.toast_string_10,
						Toast.LENGTH_SHORT).show();
			else if (!setContact)
				Toast.makeText(Firstrun.this, R.string.toast_string_11,
						Toast.LENGTH_SHORT).show();
			break;
		}
	}

	public class PinValidator implements TextWatcher {

		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() < 6) {
				pinNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0,
						R.drawable.ic_no, 0);
				setPin = false;
			} else {
				pinNumber.setCompoundDrawablesWithIntrinsicBounds(0, 0,
						R.drawable.ic_yes, 0);
				setPin = true;
			}
		}

		@Override
		public void beforeTextChanged(CharSequence text, int arg1, int arg2,
				int arg3) {
			setPin = false;
		}

		@Override
		public void onTextChanged(CharSequence text, int arg1, int arg2,
				int arg3) {
			if (text.length() >= 6) {
				preferencesHelper.setString(PreferencesHelper.PIN_NUMBER,
						pinNumber.getText().toString());

				Toast.makeText(Firstrun.this, R.string.toast_string_2,
						Toast.LENGTH_LONG).show();
				setPin = true;
			}

		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		plus.connect();
	}

	@Override
	protected void onStop() {
		super.onStop();
		plus.disconnect();
	}

	@Override
	public void onBackPressed() {
		if (exit)
			Firstrun.this.finish();
		else {
			Toast.makeText(getApplicationContext(), R.string.toast_string_3,
					Toast.LENGTH_SHORT).show();
			exit = true;
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					exit = false;
				}
			}, 3 * 1000);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case DEVICE_ADMIN_ACTIVATION_REQUEST:
			if (resultCode == Activity.RESULT_OK) {
				preferencesHelper.setBoolean(
						PreferencesHelper.DEVICE_ADMIN_STATUS, true);
				Toast.makeText(this, R.string.toast_string_6, Toast.LENGTH_LONG)
						.show();
			} else {
				toggle_admin.setChecked(false);
				preferencesHelper.setBoolean(
						PreferencesHelper.DEVICE_ADMIN_STATUS, false);
				Toast.makeText(this, R.string.toast_string_7, Toast.LENGTH_LONG)
						.show();
			}
			break;

		case Choose.CONTACT_ACTIVATION_REQUEST:
			if (resultCode == Activity.RESULT_OK) {
				Uri contactData = data.getData();
				Cursor c = getContentResolver().query(contactData, null, null,
						null, null);
				if (c.moveToFirst()) {
					preferencesHelper
							.setString(
									"emergencyName",
									c.getString(c
											.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
					preferencesHelper
							.setString(
									"emergencyNumber",
									c.getString(c
											.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));

					Toast.makeText(Firstrun.this, R.string.toast_string_8,
							Toast.LENGTH_SHORT).show();
					setContact = true;
				}
			} else
				Toast.makeText(Firstrun.this, R.string.toast_string_9,
						Toast.LENGTH_SHORT).show();
		}

	}

	public class ListAdapter extends BaseExpandableListAdapter {

		public ArrayList<String> helpList = new ArrayList<String>();
		public int notificationCounter;
		public LayoutInflater inflater;
		public Context context;
		public boolean status;

		public ListAdapter(Context context, ArrayList<String> h_list) {
			this.context = context;
			this.helpList = h_list;
			this.inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return null;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return 0;
		}

		@Override
		public View getChildView(int groupPosition, final int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			TextView text = null;
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_help_inner, null);
			}

			text = (TextView) convertView.findViewById(R.id.n_text);
			text.setText(helpList.get(childPosition));
			convertView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Toast.makeText(context, helpList.get(childPosition),
							Toast.LENGTH_SHORT).show();
				}
			});
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return helpList.size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return null;
		}

		@Override
		public int getGroupCount() {
			return 1;
		}

		@Override
		public void onGroupCollapsed(int groupPosition) {
			super.onGroupCollapsed(groupPosition);
		}

		@Override
		public void onGroupExpanded(int groupPosition) {
			super.onGroupExpanded(groupPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return 0;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_help_outer, null);
			}
			TextView header = (TextView) convertView
					.findViewById(R.id.n_header);
			header.setText(R.string.help);
			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return false;
		}

	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {

	}

	private TextView userName;
	private ImageView userImage;

	@Override
	public void onConnected(Bundle arg0) {
		userName = (TextView) findViewById(R.id.user_name);
		userImage = (ImageView) findViewById(R.id.user_image);
		try {
			userName.setText(plus.getCurrentPerson().getDisplayName());
			new AsyncTask<String, String, Bitmap>() {

				@Override
				protected Bitmap doInBackground(String... url) {
					Bitmap bm = null;
					try {
						URL aURL = new URL(url[0]);
						URLConnection conn = aURL.openConnection();
						conn.connect();
						InputStream is = conn.getInputStream();
						BufferedInputStream bis = new BufferedInputStream(is);
						bm = BitmapFactory.decodeStream(bis);
						bis.close();
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					return bm;
				}

				@Override
				protected void onPostExecute(Bitmap bitmap) {
					userImage.setImageBitmap(bitmap);
				}

			}.execute(plus.getCurrentPerson().getImage().getUrl());
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onDisconnected() {

	}
}