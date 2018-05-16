package me.fzzy.eventvoter;

import twitter4j.StatusListener;
import twitter4j.TwitterStream;

public class T4JCompat {

    public static void addStatusListener(TwitterStream stream, StatusListener listener) {
        stream.addListener(listener);
    }

    public static void removeStatusListener(TwitterStream stream, StatusListener listener) {
        stream.removeListener(listener);
    }

}
