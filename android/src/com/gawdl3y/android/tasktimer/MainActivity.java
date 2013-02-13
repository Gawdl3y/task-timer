package com.gawdl3y.android.tasktimer;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.gawdl3y.android.tasktimer.classes.Group;
import com.gawdl3y.android.tasktimer.classes.Task;
import com.gawdl3y.android.tasktimer.fragments.GroupEditDialogFragment;
import com.gawdl3y.android.tasktimer.fragments.GroupEditDialogFragment.GroupEditDialogListener;
import com.gawdl3y.android.tasktimer.fragments.MainFragment;
import com.gawdl3y.android.tasktimer.fragments.TaskEditDialogFragment;
import com.gawdl3y.android.tasktimer.fragments.TaskEditDialogFragment.TaskEditDialogListener;

public class MainActivity extends SherlockFragmentActivity implements GroupEditDialogListener, TaskEditDialogListener {
	public static String PACKAGE = null;
	public static String TAG = "MainActivity";
	public static int THEME = R.style.Theme_Dark;
	public static Resources RES = null;
	public static SharedPreferences PREFS = null;
	
	public static ArrayList<Group> groups = new ArrayList<Group>();
	public static ArrayList<Task> tasks = new ArrayList<Task>();
	
	private MainFragment mainFragment;
	
	private Messenger messenger = new Messenger(new IncomingHandler()), serviceMessenger;
	private boolean connected = false;
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.v(TAG, "Service connected: " + name);
			
			// Set the service messenger and connected status
			MainActivity.this.serviceMessenger = new Messenger(service);
			MainActivity.this.connected = true;
			
			// Send a new message to retrieve ALL THE THINGS
			Message msg = Message.obtain(null, TaskService.MSG_GET_ALL);
			sendMessageToService(msg);
		}

		public void onServiceDisconnected(ComponentName name) {
			Log.v(TAG, "Service disconnected: " + name);
			
			// Reset the service messenger and connection status
			MainActivity.this.serviceMessenger = null;
			MainActivity.this.connected = false;
		}
	};
	
	/* (non-Javadoc)
	 * The activity is being created
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Set some global values
		PACKAGE = getApplicationContext().getPackageName();
		RES = getResources();
		
		// Set the default preferences, and load the preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		PREFS = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Request window features
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		// Switch theme
		String theme = PREFS.getString("pref_theme", "0");
		THEME = theme.equals("2") ? R.style.Theme_Light_DarkActionBar : (theme.equals("1") ? R.style.Theme_Light : R.style.Theme_Dark);
		setTheme(THEME);
		
		// Call the superclass' method (we do this after setting the theme so that the theme properly applies to pre-honeycomb devices)
		super.onCreate(savedInstanceState);
		
		// Display
		setSupportProgressBarIndeterminateVisibility(true);
		setContentView(R.layout.activity_main);
		
		// Start and bind the service
		Intent intent = new Intent(this, TaskService.class);
		startService(intent);
		if(bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT | Context.BIND_ADJUST_WITH_ACTIVITY)) {
			Log.v(TAG, "Service bound");
		} else {
			Toast.makeText(this, "Task Service couldn't be bound", Toast.LENGTH_SHORT).show();
			finish();
		}
	}
	
	/* (non-Javadoc)
	 * The activity is being destroyed
	 * @see com.actionbarsherlock.app.SherlockFragmentActivity#onStop()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		// Unbind service
		unbindService(serviceConnection);
		Log.v(TAG, "Service unbound");
	}
	
	/* (non-Javadoc)
	 * The action bar was created
	 * @see com.actionbarsherlock.app.SherlockActivity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	/* (non-Javadoc)
	 * An action bar menu button was pressed
	 * @see com.actionbarsherlock.app.SherlockActivity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		FragmentManager fm;
		
	    switch(item.getItemId()) {
	        case R.id.menu_new_task:
				fm = getSupportFragmentManager();
				TaskEditDialogFragment taskEditDialog = TaskEditDialogFragment.newInstance(null);
				taskEditDialog.show(fm, "fragment_task_edit");
	        	return true;
	        case R.id.menu_new_group:
	        	fm = getSupportFragmentManager();
				GroupEditDialogFragment groupEditDialog = GroupEditDialogFragment.newInstance(null, 0);
				groupEditDialog.show(fm, "fragment_group_edit");
	        	return true;
	        case R.id.menu_settings:
	        	intent = new Intent(this, SettingsActivity.class);
	        	startActivity(intent);
	        	return true;
	        case R.id.menu_exit:
	        	Message msg = Message.obtain(null, TaskService.MSG_EXIT);
	        	sendMessageToService(msg);
	        	finish();
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onFinishEditDialog(Group group, int position) {
		Message msg = Message.obtain(null, TaskService.MSG_ADD_GROUP);
		Bundle contents = new Bundle();
		contents.putParcelable("group", group);
		msg.setData(contents);
		msg.arg1 = position;
		sendMessageToService(msg);
	}
	
	@Override
	public void onFinishEditDialog(Task task) {
		Message msg = Message.obtain(null, TaskService.MSG_ADD_TASK);
		Bundle contents = new Bundle();
		contents.putParcelable("task", task);
		msg.setData(contents);
		sendMessageToService(msg);
	}
	
	public void onTaskButtonClick(View view) {
		View parent = (View) view.getParent();
		Task task = groups.get((Integer) parent.getTag(R.id.tag_group)).getTasks().get((Integer) parent.getTag(R.id.tag_task));
		Message msg;
		Bundle contents = new Bundle();
				
		if(view.getId() == R.id.task_toggle) {
			task.toggle();
			Task.updateView(task, parent);
			
			msg = Message.obtain(null, TaskService.MSG_UPDATE_TASK);
			msg.arg1 = (Integer) parent.getTag(R.id.tag_group);
			msg.arg2 = (Integer) parent.getTag(R.id.tag_task);
			contents.putParcelable("task", task);
			msg.setData(contents);
			sendMessageToService(msg);
		}
	}
	
	/**
	 * @author Schuyler Cebulskie
	 * The handler for the activity to receive messages from the service
	 */
	private final class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "Received message: " + msg);
			
			Bundle data = msg.getData();
			data.setClassLoader(getClassLoader());
			
			switch(msg.what) {
			case TaskService.MSG_GET_TASKS:
				tasks = data.getParcelableArrayList("tasks");
				buildList();
				break;
			case TaskService.MSG_GET_GROUPS:
				groups = data.getParcelableArrayList("groups");
				if(msg.arg1 != -1) buildList(msg.arg1); else buildList();
				break;
			case TaskService.MSG_GET_ALL:
				// Set the objects
				groups = data.getParcelableArrayList("groups");
				tasks = data.getParcelableArrayList("tasks");
				
				// Add the main fragment to the activity
				mainFragment = new MainFragment();
				FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
				transaction.add(R.id.activity_main, mainFragment);
				transaction.commit();
				
				// Hide the loading indicator
				setSupportProgressBarIndeterminateVisibility(false);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	/**
	 * Sends a message to the service
	 * @param msg The message to send
	 */
	private void sendMessageToService(Message msg) {
		// Set who to reply to
		msg.replyTo = messenger;
		
		// Send the message
		try {
			serviceMessenger.send(msg);
			Log.d(TAG, "Sent message: " + msg);
		} catch(android.os.RemoteException e) {
			Log.d(TAG, "Failed to send message: " + msg + " (" + e.getLocalizedMessage() + " caused by " + e.getCause() + ")");
		}
		
		// Return the message to the global pool
		msg.recycle();
	}
	
	private void buildList() {
		mainFragment.adapter.groups = groups;
		mainFragment.adapter.notifyDataSetChanged();
	}
	
	private void buildList(int position) {
		buildList();
		mainFragment.pager.setCurrentItem(position);
	}
}
