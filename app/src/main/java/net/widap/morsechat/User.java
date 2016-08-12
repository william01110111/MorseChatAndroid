package net.widap.morsechat;

/**
 *  Created by william on 8/10/16.
 */
public class User
{
	public static User me = new User();
	public static User[] friends = new User[0];
	public static User[] requestsIn = new User[0];

	public String displayName;
	public String username;
	public String key;

	User()
	{
		displayName = "noDisplayName";
		username = "noUsernameProvided";
		key = "noKey";
	}

	User(String usernameIn, String displayNameIn, String keyIn)
	{
		displayName = displayNameIn;
		username = usernameIn;
		key = keyIn;
	}

	public static void clearData()
	{
		me=new User();
		friends=new User[0];
		requestsIn=new User[0];
		FirebaseHelper.stateChangedListener.userDataChanged();
	}

	public static class FriendStatus {
		public boolean isFriend = false;
		public boolean requestOut = false;
		public boolean requestIn = false;
	}
}
