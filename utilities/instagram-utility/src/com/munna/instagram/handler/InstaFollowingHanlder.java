package com.munna.instagram.handler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.http.client.ClientProtocolException;
import org.brunocvcunha.instagram4j.requests.InstagramTagFeedRequest;
import org.brunocvcunha.instagram4j.requests.payload.InstagramFeedItem;
import org.brunocvcunha.instagram4j.requests.payload.InstagramFeedResult;
import org.brunocvcunha.instagram4j.requests.payload.InstagramGetUserFollowersResult;
import org.brunocvcunha.instagram4j.requests.payload.InstagramSearchUsernameResult;
import org.brunocvcunha.instagram4j.requests.payload.InstagramUserSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.munna.instagram.constants.InstaConstants;
import com.munna.instagram.factory.InstagramConnectionFactory;
import com.munna.instagram.factory.InstagramManager;

/**
 * @author Mohammed Fathauddin
 * @since 2018
 */

public class InstaFollowingHanlder extends InstagramHandler{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(InstaFollowingHanlder.class);
	
	String IgUsername = InstaConstants.AuthenticationConstant.IG_USERNAME;
	Boolean isFollowingEmpty = InstaConstants.FollowingConstants.IS_STOP_UNFOLLOWING_USER;
	Integer dayOftheWeek = InstaConstants.FollowingConstants.DAY_OF_THE_WEEK;
	
	@Override
	public void init() {
		if(!InstaConstants.AuthenticationConstant.IG_STOP_PROCESS)
			do {
				Date now = new Date();
				SimpleDateFormat simpleDateformat = new SimpleDateFormat("EEEE");
		        LOGGER.info("Its "+ simpleDateformat.format(now));
		        Calendar calendar = Calendar.getInstance();
		        calendar.setTime(now);
		        if(calendar.get(Calendar.DAY_OF_WEEK) == dayOftheWeek && !isFollowingEmpty) {
		        	unFollowTheFollowingUsers();
		        } else {
		        	searchForNewUsersAndFollowThem();
		        }
			}while(!InstagramManager.stopProcess());		
	}

	private void searchForNewUsersAndFollowThem(){
		LOGGER.info("Searching for new Users based on feeds.");
		long fixedCount = InstaConstants.FollowingConstants.FIXED_COUNT;
		long cc = 0L;
		String[] hashTag_arr = InstaConstants.FollowingConstants.HASH_TAGS.split(",");
		String randomTag = hashTag_arr[new Random().nextInt(hashTag_arr.length)];
		InstagramFeedResult tagFeed = null;
		LOGGER.info("Searching feeds under : #"+randomTag);
		tagFeed = InstagramManager.getTagFeeds(randomTag);
		LOGGER.info("Number of following count  on " + new Date() + " :"
				+ InstagramManager.getUserDetails(IgUsername).getUser().getFollowing_count());
		if(tagFeed != null) {
			for (InstagramFeedItem feedResult : tagFeed.getItems()) {
				try {
					if (cc != fixedCount) {
						InstagramManager.likePost(feedResult.getPk());
						InstagramManager.commentPost(feedResult.getPk());
						LOGGER.info("Post ID : " + feedResult.getPk() + "posted by : " + feedResult.getUser().getUsername());
						InstagramManager.followUser(feedResult.getUser().getPk());
						LOGGER.info("Following user : " + feedResult.getUser().getUsername());
						cc++;
						LOGGER.info("User Count in loop: " + cc);
					} else {
						break;
					}
					if (cc % 10 == 0) {
						sleep();
					}
				} catch (ClientProtocolException e) {
					LOGGER.error("ClientProtocolException while trigerring unfollow user command ("
							+ feedResult.getUser().getUsername() + ")", e);
				} catch (IOException e) {
					LOGGER.error("IOException while trigerring unfollow user command (" + feedResult.getUser().getUsername()
							+ ")", e);
				}
			}
		}
	}
	

	private void unFollowTheFollowingUsers() {
		InstagramSearchUsernameResult user = InstagramManager.getUserDetails(IgUsername);
		LOGGER.info("Number of followers for("+IgUsername+"): " + user.getUser().getFollower_count());
		LOGGER.info("Number of following for("+IgUsername+"): " + user.getUser().getFollowing_count());
		long followingPeopleCount = user.getUser().getFollowing_count();
		long i = 1L;
		if(followingPeopleCount != 0) {
			while(followingPeopleCount != 0) {
				InstagramGetUserFollowersResult userFollowingList = InstagramManager.getFollowingUser(user.getUser().getPk());
				List<InstagramUserSummary> followings = userFollowingList.getUsers();
				if(followings != null && followings.size() !=0) {
					for(InstagramUserSummary followingUser : followings) {
						try {
							LOGGER.info("Unfollowing USER :"+ followingUser.getUsername());
							InstagramManager.unFollowUser(followingUser.getPk());
						} catch (ClientProtocolException e) {
							LOGGER.error("ClientProtocolException while trigerring unfollow user command ("+followingUser.getUsername()+")",e);
						} catch (IOException e) {
							LOGGER.error("IOException while trigerring unfollow user command ("+followingUser.getUsername()+")",e);
						}
						i++;
						if(i%100 ==0)
							sleep();
					}
					user = InstagramManager.getUserDetails(IgUsername);
					followingPeopleCount = user.getUser().getFollowing_count();
				}else
					followingPeopleCount =0L;
								
			}
		}else {
			LOGGER.info("Following list is empty");
			isFollowingEmpty = true;
		}
	}

	
}
