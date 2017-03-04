package com.dreambig.app4it.util;

public class Settings {

    private static final String HOME_BASE_ENDPOINT_URL = "https://1-dot-universal-helix-789.appspot.com/_ah/api/"; //prod

    public static String getHomeBaseEndpointUrl() {
        return HOME_BASE_ENDPOINT_URL;
    }

	public static String getFirebaseUrl() {
		return "https://shining-fire-7962-d.firebaseio.com/"; //prod
		//return "https://shining-fire-7962.firebaseio.com/"; //dev
	}

}
