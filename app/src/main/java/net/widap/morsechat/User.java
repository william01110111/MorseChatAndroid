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

	public static void getUniqueUsername(User userIn, FirebaseHelper.GetUserListener listener) {

		String name = "";

		for (char c: userIn.username.toLowerCase().toCharArray()) {

			if ((c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9') || c=='_' || c=='.' || c=='-') {
				name+=c;
			}
		}

		if (name.length() < 3) {
			name = "user";
		}

		userIn.username=name;

		checkNextUsername(0, userIn, listener);
	}

	private static void checkNextUsername(final int iter, final User userIn, final FirebaseHelper.GetUserListener listener) {

		final String attempt=userIn.username+(iter>0?iter:"");

		FirebaseHelper.checkIfUsernameAvailable(attempt, false, new FirebaseHelper.BoolListener() {
			@Override
			public void func(boolean available) {
				if (available)
				{
					userIn.username=attempt;
					listener.userReady(userIn);
				}
				else
				{
					checkNextUsername(iter+1, userIn, listener);
				}
			}
		});
	}

	//returns nil if there is no error, otherwise returns error message
	static String checkUsername(String name) {

		if (name.isEmpty()) {
			return "Username required";
		}
		if (name.length()<3) {
			return "Username too short";
		}

		for (char c: name.toCharArray()) {

			if (c==' ') {
				return "user name may not contain spaces";
			}

			if (!((c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9') || c=='_' || c=='.' || c=='-')) {
				return "username may only contain letters, numbers and these characters: .-_";
			}
		}

		return null;
	}
}
