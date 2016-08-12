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

	static void authStateChanged()
	{
		firebaseUser = auth.getCurrentUser();
		stateChangedListener.loginStateChanged();
	}

	static void getUserFromKey(String key, GetUserListener listener)
	{
		Query query=root.child("users").child(key);

		query.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				User out=new User
						(
								dataSnapshot.child("username").getValue(),
								dataSnapshot.child("displayName").getValue(),
								dataSnapshot.child(key).getValue()
						);

				listener.userReady(out);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				listener.userReady(null);
			}
		});
	}

	static void forAllUsersInSnapshot(DataSnapshot data, GetUserListener listenerIn, final VoidListener whenDoneIn)
	{
		new ForAllUsersInSnapshotClass(data, listenerIn, whenDoneIn);
	}

	static class ForAllUsersInSnapshotClass
	{
		int elemLeft;
		GetUserListener listener;
		VoidListener whenDone;

		ForAllUsersInSnapshotClass(DataSnapshot data, GetUserListener listenerIn, final VoidListener whenDoneIn)
		{
			elemLeft = (int) data.getChildrenCount();
			listener=listenerIn;
			whenDone=whenDoneIn;

			if (elemLeft == 0) {
				whenDone.func();
			} else {
				for (DataSnapshot ref : data.getChildren()) {
					getUserFromKey(ref.getKey(), new GetUserListener() {
						@Override
						public void userReady(User user) {
							if (user != null)
								listener.userReady(user);

							elemLeft--;

							if (elemLeft<=0)
							{
								whenDone.func();
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
				forAllUsersInSnapshot(data, new GetUserListener() {
					@Override
					public void userReady(User user) {

					}
				});
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {}
		};
	}

	public interface StateChangedListener
	{
		void loginStateChanged();
		void userDataChanged();
	}

	public interface GetUserListener
	{
		void userReady(User user);
	}

	public interface VoidListener
	{
		void func();
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