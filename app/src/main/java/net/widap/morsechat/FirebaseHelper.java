package net.widap.morsechat;

import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.util.ArrayList;

/**
 *  Created by william on 8/3/16.
 */
public class FirebaseHelper {

	private static FirebaseUser firebaseUser;
	private static FirebaseAuth auth;
	private static DatabaseReference root;
	private static boolean initialLoginAttemptDone = false;
	private static boolean initialAccountSetupDone = true;
	public static StateChangedListener stateChangedListener;
	private static ArrayList<DatabaseListener> databaseListeners = new ArrayList<>();
	//var loginChangedCallback: (() -> Void)?
	//var userDataChangedCallback: (() -> Void)?
	//var firebaseErrorCallback: ((msg: String) -> Void)



	static void init()
	{
		//callback is used so user is not requested while internal state is changing or some BS like that

		auth = FirebaseAuth.getInstance();
		root = FirebaseDatabase.getInstance().getReference();
		auth.addAuthStateListener(new FirebaseAuth.AuthStateListener() {
			@Override
			public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
				authStateChanged();
			}
		});
	}

	static void error(String msg)
	{
		System.out.println("Firebase error: " + msg);
	}

	static void checkIfUsernameAvailable(String name, boolean ignoreMe, final BoolListener listener) {

		if (ignoreMe && name.toLowerCase() == User.me.username.toLowerCase()) {
			listener.func(true);
			return;
		}

		Query query = root.child("users").orderByChild("lowercase").equalTo(name.toLowerCase());

		query.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot data) {
				listener.func(data.exists());
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {}
		});
	}


	//auth

	static void authStateChanged()
	{
		initialLoginAttemptDone=true;

		firebaseUser = auth.getCurrentUser();

		if (stateChangedListener!=null)
			stateChangedListener.loginStateChanged();

		if (firebaseUser!=null)
		{
			getUserFromKey(firebaseUser.getUid(), new GetUserListener() {
				@Override
				public void userReady(User user) {
					if (user!=null)
					{
						addDatabaseListeners();
					}
					else
					{
						initialAccountSetupDone=false;
						createUser();
					}
				}
			});
		}
		else
		{
			User.clearData();
		}
	}

	boolean getIfSignedIn()
	{
		return firebaseUser!=null;
	}

	static void signOut()
	{
		removeDatabaseListeners();
		User.clearData();
		auth.signOut();
	}


	//upload data

	static void createUser() {

		User.getUniqueUsername(new User(User.me.displayName, User.me.displayName, firebaseUser.getUid()), new GetUserListener() {
					@Override
					public void userReady(User user) {
						uploadMe(user, new SucceedFailListener() {
							@Override
							public void success() {
								addDatabaseListeners();
							}

							@Override
							public void fail(String err) {
								User.clearData();
								error(err);
							}
						});
					}
				});
	}

	static void uploadMe(final User newMe, final SucceedFailListener listener) {

		String error = User.checkUsername(newMe.username);

		if (error != null) {
			listener.fail(error);
			return;
		}

		if (newMe.displayName.isEmpty()) {
			listener.fail("Display name required");
			return;
		}

		checkIfUsernameAvailable(newMe.username, true, new BoolListener() {
			@Override
			public void func(boolean available) {
				if (available) {
					DatabaseReference ref=root.child("users").child(newMe.key);
					ref.child("displayName").setValue(newMe.displayName);
					ref.child("username").setValue(newMe.username);
					ref.child("lowercase").setValue(newMe.username.toLowerCase());
					User.me = newMe;
					stateChangedListener.userDataChanged();
					listener.success();
				} else {
					listener.fail("Username already taken");
				}
			}
		});
	}


	//download data

	static void getUserFromKey(final String key, final GetUserListener listener)
	{
		Query query=root.child("users").child(key);

		query.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				User out=new User
						(
								(String)dataSnapshot.child("username").getValue(),
								(String)dataSnapshot.child("displayName").getValue(),
								(String)dataSnapshot.child(key).getValue()
						);

				listener.userReady(out);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				listener.userReady(null);
			}
		});
	}

	static void forAllUsersInSnapshot(DataSnapshot data, GetUserArrayListener listener)
	{
		new ForAllUsersInSnapshotClass(data, listener);
	}

	private static class ForAllUsersInSnapshotClass
	{
		int elemLeft;
		GetUserArrayListener listener;
		User[] users;

		ForAllUsersInSnapshotClass(DataSnapshot data, GetUserArrayListener listenerIn)
		{
			elemLeft = (int) data.getChildrenCount();
			users=new User[elemLeft];
			listener=listenerIn;

			if (elemLeft == 0) {
				listener.userArrayReady(new User[0]);
			} else {
				for (DataSnapshot ref : data.getChildren()) {
					getUserFromKey(ref.getKey(), new GetUserListener() {
						@Override
						public void userReady(User user) {
							if (user != null)
								users[users.length-elemLeft]=user;

							elemLeft--;

							if (elemLeft<=0)
							{
								listener.userArrayReady(users);
							}
						}
					});
				}
			}
		}
	}

	static void removeDatabaseListeners()
	{
		for (DatabaseListener listener : databaseListeners)
		{
			listener.stop();
		}

		databaseListeners.clear();
	}

	static void addDatabaseListeners()
	{
		removeDatabaseListeners();

		if (firebaseUser==null)
		{
			error("callled addDatabaseListeners without active firebase user");
			return;
		}

		Query query;
		ValueEventListener valueListener;


		//me

		query=root.child("users").child(firebaseUser.getUid());

		valueListener=new ValueEventListener() {

			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				getUserFromKey(firebaseUser.getUid(), new GetUserListener() {
					@Override
					public void userReady(User user) {
						User.me=user;
						stateChangedListener.userDataChanged();
					}
				});
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {}
		};

		databaseListeners.add(new DatabaseListener(query, valueListener));


		//friends

		query=root.child("friendsByUser").child(firebaseUser.getUid());

		valueListener=new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot data) {
				forAllUsersInSnapshot(data, new GetUserArrayListener() {
					@Override
					public void userArrayReady(User[] users) {
						User.friends=users;
						stateChangedListener.userDataChanged();
					}
				});
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {}
		};

		databaseListeners.add(new DatabaseListener(query, valueListener));

		//requests in

		query=root.child("requestsByReceiver").child(firebaseUser.getUid());

		valueListener=new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot data) {
				forAllUsersInSnapshot(data, new GetUserArrayListener() {
					@Override
					public void userArrayReady(User[] users) {
						User.requestsIn=users;
						stateChangedListener.userDataChanged();
					}
				});
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {}
		};

		databaseListeners.add(new DatabaseListener(query, valueListener));
	}

	static void getFriendStatusOfUser(String other, GetFriendStatusListener listenerIn)
	{
		new GetFriendStatusOfUserClass(other, listenerIn);
	}

	private static class GetFriendStatusOfUserClass
	{
		User.FriendStatus status;
		GetFriendStatusListener listener;
		boolean isFriendDone = false, requestOutDone = false, requestInDone = false;

		GetFriendStatusOfUserClass(String other, GetFriendStatusListener listenerIn)
		{
			status = new User.FriendStatus();
			listener=listenerIn;

			root.child("friendsByUser").child(User.me.key).child(other).addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot data) {
					status.isFriend=data.exists();
					isFriendDone=true;
					downloadDone();
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {}
			});

			root.child("requestsBySender").child(User.me.key).child(other).addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot data) {
					status.requestOut=data.exists();
					requestOutDone=true;
					downloadDone();
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {}
			});

			root.child("requestsByReceiver").child(User.me.key).child(other).addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot data) {
					status.requestIn=data.exists();
					requestInDone=true;
					downloadDone();
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {}
			});
		}

		private void downloadDone() {

			if (isFriendDone && requestOutDone && requestInDone) {
				listener.friendStatusReady(status);
			}
		}
	}


	//listener interfaces

	public interface StateChangedListener
	{
		void loginStateChanged();
		void userDataChanged();
	}

	public interface GetUserListener
	{
		void userReady(User user);
	}

	public interface GetUserArrayListener
	{
		void userArrayReady(User[] users);
	}

	public interface GetFriendStatusListener
	{
		void friendStatusReady(User.FriendStatus status);
	}

	public interface VoidListener
	{
		void func();
	}

	public interface BoolListener
	{
		void func(boolean var);
	}

	public interface SucceedFailListener
	{
		void success();
		void fail(String err);
	}

	public static class DatabaseListener
	{
		private ValueEventListener listener;
		private Query query;

		DatabaseListener(Query queryIn, ValueEventListener listenerIn)
		{
			listener=listenerIn;
			query=queryIn;

			query.addValueEventListener(listener);
		}

		void stop()
		{
			query.removeEventListener(listener);
		}
	}
}