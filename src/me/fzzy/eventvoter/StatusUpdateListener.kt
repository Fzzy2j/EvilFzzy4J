package me.fzzy.eventvoter

import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.util.RequestBuffer
import twitter4j.*

class StatusUpdateListener : UserStreamListener {

    override fun onStatus(status: Status) {
        if (!status.isRetweet()) {
            if (status.getUser().getScreenName().equals("OverwatchEU")) {
                val url = "https://twitter.com/" + status.getUser().getScreenName() + "/status/" + status.getId()
                for (guild in guilds) {
                    val channel: MutableList<IChannel> = cli.getGuildByID(guild.leaderboardGuildId).getChannelsByName("overwatch-news")
                    if (channel.size > 0) {
                        RequestBuffer.request { channel[0].sendMessage(url) }
                    }
                }
            }
        }
    }

    override fun onDeletionNotice(l: Long, l1: Long) {

    }

    override fun onFriendList(longs: LongArray) {

    }

    override fun onFavorite(user: User, user1: User, status: Status) {

    }

    override fun onUnfavorite(user: User, user1: User, status: Status) {

    }

    override fun onFollow(user: User, user1: User) {

    }

    override fun onUnfollow(user: User, user1: User) {

    }

    override fun onDirectMessage(directMessage: DirectMessage) {

    }

    override fun onUserListMemberAddition(user: User, user1: User, userList: UserList) {

    }

    override fun onUserListMemberDeletion(user: User, user1: User, userList: UserList) {

    }

    override fun onUserListSubscription(user: User, user1: User, userList: UserList) {

    }

    override fun onUserListUnsubscription(user: User, user1: User, userList: UserList) {

    }

    override fun onUserListCreation(user: User, userList: UserList) {

    }

    override fun onUserListUpdate(user: User, userList: UserList) {

    }

    override fun onUserListDeletion(user: User, userList: UserList) {

    }

    override fun onUserProfileUpdate(user: User) {

    }

    override fun onUserSuspension(l: Long) {

    }

    override fun onUserDeletion(l: Long) {

    }

    override fun onBlock(user: User, user1: User) {

    }

    override fun onUnblock(user: User, user1: User) {

    }

    override fun onRetweetedRetweet(user: User, user1: User, status: Status) {

    }

    override fun onFavoritedRetweet(user: User, user1: User, status: Status) {

    }

    override fun onQuotedTweet(user: User, user1: User, status: Status) {

    }

    override fun onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {

    }

    override fun onTrackLimitationNotice(i: Int) {

    }

    override fun onScrubGeo(l: Long, l1: Long) {

    }

    override fun onStallWarning(stallWarning: StallWarning) {

    }

    override fun onException(e: Exception) {

    }
}