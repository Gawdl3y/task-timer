package com.gawdl3y.android.tasktimer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.gawdl3y.android.tasktimer.classes.Group;
import com.gawdl3y.android.tasktimer.classes.Task;
import com.gawdl3y.android.tasktimer.classes.Utilities;

public class TaskService extends Service {
	public static String TAG = "TaskService";
	
	public static final int MSG_GET_TASKS = 1;
	public static final int MSG_ADD_TASK = 2;
	public static final int MSG_DELETE_TASK = 3;
	public static final int MSG_UPDATE_TASK = 4;
	public static final int MSG_GET_GROUPS = 5;
	public static final int MSG_ADD_GROUP = 6;
	public static final int MSG_DELETE_GROUP = 7;
	public static final int MSG_UPDATE_GROUP = 8;
	public static final int MSG_GET_ALL = 9;
	public static final int MSG_EXIT = 10;
	
	public final Messenger messenger = new Messenger(new IncomingHandler());
	private Messenger activityMessenger;
	
	private Notification notification;
	private ArrayList<Task> tasks = new ArrayList<Task>();
	private ArrayList<Group> groups = new ArrayList<Group>();
	
	@Override
	public void onCreate() {
		// Create the intent to launch the app
		Intent intent = new Intent(this, MainActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(MainActivity.class).addNextIntent(intent);
		
		// Create the notification
		notification = new NotificationCompat.Builder(this)
				.setContentTitle(getString(R.string.app_name))
				.setContentText("Hey! Listen!")
				//.setTicker("Hey! Listen!")
				.setSmallIcon(R.drawable.ic_launcher)
				.setLargeIcon(Utilities.drawableToBitmap(getResources().getDrawable(R.drawable.ic_launcher)))
				.setWhen(System.currentTimeMillis())
				.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT))
				.build();
		
		// Start the service in the foreground
		startForeground(1, notification);
		
		// Use these until database is implemented
		ArrayList<Task> tasks1 = new ArrayList<Task>(), tasks2 = new ArrayList<Task>(), tasks3 = new ArrayList<Task>();
		tasks1.add(new Task("Bob Malooga", "", new Task.Time(1, 2, 3), new Task.Time(), true, false, false, false, 22, 1, 42));
		tasks1.add(new Task("Ermahgerd a tersk", "", new Task.Time(1, 0, 42), new Task.Time(2, 0, 0), false, false, false, false, 0, 5, 42));
		tasks2.add(new Task("It's a task!", "", new Task.Time(), new Task.Time(), false, false, false, true, 0, 1, 43));
		
		Collections.sort(tasks1, new Task.PositionComparator());
		Collections.sort(tasks2, new Task.PositionComparator());
		Collections.sort(tasks3, new Task.PositionComparator());
		
		tasks.addAll(tasks1);
		tasks.addAll(tasks2);
		tasks.addAll(tasks3);
		
		groups.add(new Group("It's a group!", tasks1, 0, 42));
		groups.add(new Group("Grouplol", tasks2, 2, 43));
		groups.add(new Group("zomg", tasks3, 1, 2));
		
		Collections.sort(groups, new Group.PositionComparator());
		
		Log.v(TAG, "Started");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "Bound to activity");
		return messenger.getBinder();
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "Unbound from activity");
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "Destroyed");
		super.onDestroy();
	}

	public class ServiceBinder extends Binder {
		TaskService getService() {
			return TaskService.this;
		}
	}
	
	private final class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			activityMessenger = msg.replyTo;
			Log.d(TAG, "Received message: " + msg);
			
			Message response;
			Bundle contents;
			
			switch(msg.what) {
			case MSG_GET_TASKS:
				// Send the response message
				sendObjectToActivity(MSG_GET_TASKS, "tasks", tasks);
				break;
			case MSG_ADD_TASK:
				// TODO SQL
				groups.get(msg.arg1).getTasks().add((Task) msg.getData().getSerializable("task"));
				break;
			case MSG_DELETE_TASK:
				// TODO SQL
				groups.get(msg.arg1).getTasks().remove(msg.arg2);
				break;
			case MSG_UPDATE_TASK:
				// TODO SQL
				groups.get(msg.arg1).getTasks().set(msg.arg2, (Task) msg.getData().getSerializable("task"));
				break;
				
			case MSG_GET_GROUPS:
				// Send the response message
				response = Message.obtain(null, MSG_GET_GROUPS);
				contents = new Bundle();
				contents.putSerializable("groups", groups);
				response.setData(contents);
				sendMessageToActivity(response);
				break;
			case MSG_ADD_GROUP:
				// Add the group TODO SQL
				Group group = (Group) msg.getData().getSerializable("group");
				group.setId(10);
				group.setPosition(groups.size());
				groups.add(group);
				sendObjectToActivity(MSG_GET_GROUPS, "groups", groups);
				break;
			case MSG_DELETE_GROUP:
				// TODO SQL
				break;
			case MSG_UPDATE_GROUP:
				// TODO SQL
				break;
				
			case MSG_GET_ALL:
				// Send the response message
				response = Message.obtain(null, MSG_GET_ALL);
				contents = new Bundle();
				contents.putSerializable("groups", groups);
				contents.putSerializable("tasks", tasks);
				response.setData(contents);
				sendMessageToActivity(response);
				break;
				
			case MSG_EXIT:
				TaskService.this.stopSelf();
				break;
				
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	public void sendMessageToActivity(Message msg) {
		// Set who to reply to
		msg.replyTo = messenger;
		
		// Send the message
		try {
			activityMessenger.send(msg);
			Log.d(TAG, "Sent message: " + msg);
		} catch(android.os.RemoteException e) {
			Log.d(TAG, "Failed to send message: " + msg + " (" + e.getLocalizedMessage() + " caused by " + e.getCause() + ")");
		}
		
		// Return the message to the global pool
		msg.recycle();
	}
	
	public void sendObjectToActivity(int msgType, String key, Object o) {
		Message msg = Message.obtain(null, msgType);
		Bundle contents = new Bundle();
		contents.putSerializable(key, (Serializable) o);
		msg.setData(contents);
		sendMessageToActivity(msg);
	}
	
	public ArrayList<Group> getGroups() {
		return groups;
	}
	
	public ArrayList<Task> getTasks() {
		return tasks;
	}
}
