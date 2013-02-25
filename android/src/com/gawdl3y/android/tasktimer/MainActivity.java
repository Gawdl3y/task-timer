package com.gawdl3y.android.tasktimer;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.gawdl3y.android.tasktimer.classes.Group;
import com.gawdl3y.android.tasktimer.classes.Task;
import com.gawdl3y.android.tasktimer.layout.GroupEditDialogFragment;
import com.gawdl3y.android.tasktimer.layout.GroupEditDialogFragment.GroupEditDialogListener;
import com.gawdl3y.android.tasktimer.layout.MainFragment;
import com.gawdl3y.android.tasktimer.layout.TaskEditDialogFragment;
import com.gawdl3y.android.tasktimer.layout.TaskEditDialogFragment.TaskEditDialogListener;
import com.gawdl3y.android.tasktimer.layout.TaskListFragment;

public class MainActivity extends SherlockFragmentActivity implements GroupEditDialogListener, TaskEditDialogListener {
	private static final String TAG = "MainActivity";
	
	private TaskTimerApplication app;
	private MainFragment mainFragment;
	
	private Messenger messenger = new Messenger(new IncomingHandler()), serviceMessenger;
	private boolean connected = false;
	
	/**
	 * The service connection
	 */
	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.v(TAG, "Service connected: " + name);
			
			// Set the service messenger and connected status
			MainActivity.this.serviceMessenger = new Messenger(service);
			MainActivity.this.connected = true;
			
			// Retrieve ALL THE THINGS
			Message msg = Message.obtain(null, TaskService.MSG_GET_ALL);
			sendMessageToService(msg);
		}

		public void onServiceDisconnected(ComponentName name) {
			if(app.debug) Log.v(TAG, "Service disconnected: " + name);
			
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
		// Set app and switch theme (we do this before calling the super method so that the theme properly applies)
		app = (TaskTimerApplication) getApplication();
		setTheme(app.theme);
		
		// Call the superclass' method
		super.onCreate(savedInstanceState);
		
		// Request window features
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		// Display
		setContentView(R.layout.activity_main);
		
		// Initialize
		if(app.groups == null) app.groups = new ArrayList<Group>();
		if(app.tasks == null) app.tasks = new ArrayList<Task>();
		
		// Start and bind the service
		Intent intent = new Intent(this, TaskService.class);
		startService(intent);
		if(bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT | Context.BIND_ADJUST_WITH_ACTIVITY)) {
			if(app.debug) Log.v(TAG, "Service bound");
		} else {
			Log.w(TAG, "Service couldn't be bound");
		}
	}
	
	/* (non-Javadoc)
	 * The activity is being started
	 * @see android.support.v4.app.FragmentActivity#onStart()
	 */
	protected void onStart() {
		super.onStart();
		
		// Show the loading indicator if we don't have the groups or tasks yet
		if(app.groups.size() == 0 && app.tasks.size() == 0) setSupportProgressBarIndeterminateVisibility(true);
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
		if(app.debug) Log.v(TAG, "Service unbound");
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
				TaskEditDialogFragment taskEditDialog = TaskEditDialogFragment.newInstance(null, mainFragment.pager.getCurrentItem());
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
	
	/* (non-Javadoc)
	 * The add group dialog is finished
	 * @see com.gawdl3y.android.tasktimer.fragments.GroupEditDialogFragment.GroupEditDialogListener#onFinishEditDialog(com.gawdl3y.android.tasktimer.classes.Group, int)
	 */
	@Override
	public void onFinishEditDialog(Group group) {
		Message msg = Message.obtain(null, TaskService.MSG_ADD_GROUP);
		Bundle contents = new Bundle();
		contents.putParcelable("group", group);
		msg.setData(contents);
		msg.arg1 = group.getPosition();
		sendMessageToService(msg);
	}
	
	/* (non-Javadoc)
	 * The add task dialog is finished
	 * @see com.gawdl3y.android.tasktimer.fragments.TaskEditDialogFragment.TaskEditDialogListener#onFinishEditDialog(com.gawdl3y.android.tasktimer.classes.Task)
	 */
	@Override
	public void onFinishEditDialog(Task task, int group) {
		Message msg = Message.obtain(null, TaskService.MSG_ADD_TASK);
		Bundle contents = new Bundle();
		contents.putParcelable("task", task);
		msg.setData(contents);
		msg.arg1 = group;
		sendMessageToService(msg);
	}
	
	/**
	 * A task button is clicked
	 * @param view The view of the button that was clicked
	 */
	public void onTaskButtonClick(View view) {
		View parent = (View) view.getParent();
		Task task = app.groups.get((Integer) parent.getTag(R.id.tag_group)).getTasks().get((Integer) parent.getTag(R.id.tag_task));
		Message msg;
		Bundle contents = new Bundle();
				
		if(view.getId() == R.id.task_toggle) {
			task.toggle();
			Task.updateView(task, parent);
			
			msg = Message.obtain(null, TaskService.MSG_TOGGLE_TASK);
			msg.arg1 = (Integer) parent.getTag(R.id.tag_group);
			msg.arg2 = (Integer) parent.getTag(R.id.tag_task);
			sendMessageToService(msg);
		}
	}
	
	/**
	 * The handler for the activity to receive messages from the service
	 * @author Schuyler Cebulskie
	 */
	private final class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if(app.debug) Log.v(TAG, "Received message: " + msg);
			
			Bundle data = msg.getData();
			data.setClassLoader(getClassLoader());
			Task task;
			
			switch(msg.what) {
			case TaskService.MSG_GET_TASKS:
				app.tasks = data.getParcelableArrayList("tasks");
				buildList();
				break;
			case TaskService.MSG_ADD_TASK:
				task = (Task) data.getParcelable("task");
				Group group = app.groups.get(msg.arg1);
				
				// Update the task list fragment for the group
				TaskListFragment fragment = (TaskListFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":" + msg.arg1);
				if(fragment != null) {
					fragment.group = group;
					fragment.adapter.notifyDataSetChanged();
				}
				
				// Update the main adapter
				mainFragment.adapter.groups.set(msg.arg1, group);
				mainFragment.adapter.notifyDataSetChanged();
				
				// Scroll to the group that the task was added to
				mainFragment.pager.setCurrentItem(msg.arg1);
				break;
			case TaskService.MSG_UPDATE_TASK:
				task = (Task) data.getParcelable("task");
				
				try {
					fragment = (TaskListFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.pager + ":" + msg.arg1);
					View view = fragment.getView().findViewWithTag(Integer.valueOf(task.getPosition()));
					Task.updateView(task, view);
				} catch(NullPointerException e) {
					
				}
				break;
			
			case TaskService.MSG_GET_GROUPS:
				if(msg.arg1 != -1) buildList(msg.arg1); else buildList();
				break;
			
			case TaskService.MSG_GET_ALL:
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
	 * Builds the list of groups and tasks
	 */
	private void buildList() {
		mainFragment.adapter.groups = app.groups;
		mainFragment.adapter.notifyDataSetChanged();
		mainFragment.pager.invalidate();
	}
	
	/**
	 * Builds the list of groups and tasks, then switches to a group
	 * @param position The position of the group to switch to
	 */
	private void buildList(int position) {
		buildList();
		mainFragment.pager.setCurrentItem(position);
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
			if(app.debug) Log.v(TAG, "Sent message: " + msg);
		} catch(android.os.RemoteException e) {
			if(app.debug) Log.w(TAG, "Failed to send message: " + msg + " (" + e.getLocalizedMessage() + " caused by " + e.getCause() + ")");
		}
		
		// Return the message to the global pool
		msg.recycle();
	}
}
