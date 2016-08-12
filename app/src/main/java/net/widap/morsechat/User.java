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
}
