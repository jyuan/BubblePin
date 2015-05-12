package bubblepin.com.bubblepin.util;

import android.location.Location;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ParseUtil {

    public static final String TAG = "ParseUtil";

    // Media Type
    public static final String TEXT = "text";
    public static final String IMAGE = "image";
    public static final String VEDIO = "vedio";
    public static final String RECORDING = "recording";
    public static final String ADDRESS_RECOMMEND = "recommend";

    // common
    public static final String OBJECT_ID = "objectId";
    public static final String CREATE_AT = "createdAt";
    public static final String UPDATE_AT = "updatedAt";

    // User
    public static final String USER_EMAIL = "username";
    public static final String USER_NICKNAME = "nickname";
    public static final String USER_PHOTO = "Photo";
    public static final String USER_COUNTRY = "Country";
    public static final String USER_CITY = "City";

    // memory
    public static final String MEMORY = "Memory";
    public static final String MEMORY_CREATE_DATE = "createDate";
    public static final String MEMORY_MEDIA_TYPE = "MediaType";
    public static final String MEMORY_ADDRESS = "address";
    public static final String MEMORY_GEOPOINT = "geoPoint";
    public static final String MEMORY_IMAGE = "imageFile";
    public static final String MEMORY_INTRODUCTION = "introduction";
    public static final String MEMORY_MEMORY_DATE = "memoryDate";
    public static final String MEMORY_PRIVACY = "privacy";
    public static final String MEMORY_TITLE = "title";
    public static final String MEMORY_USRE_OBJECT_ID = "userObjectId";
    public static final String MEMORY_FILE = "imageFile";

    // Friends
    public static final String FRIENDS = "Friends";
    public static final String FRIENDS_FROM = "FriendFrom";
    public static final String FRIENDS_TO = "FriendTo";
    public static final String FRIENDS_COMFIRM = "IsConfirmed";
    public static final String FRIENDS_ISFRIEND = "IsFriend";

    // Filter
    public static final String FILTER = "Filter";
    public static final String FILTER_DETAIL = "FilterDetail";
    public static final String FILTER_IS_SELECTED = "isSelected";
    public static final String FILTER_CATEGORY_NAME = "Category";
    public static final String FILTER_USER_ID = "UserID";
    public static final String FILTER_CATEGORY_ID = "CategoryID";
    public static final String FILTER_OWNER_ID = "OwnerID";

    /**
     * Method used to convert a Location object to ParseGeoPoint object
     *
     * @param location location that want to parse
     * @return ParseGeoPoint Object
     */
    public static ParseGeoPoint getParseGeoPoint(Location location) {
        if (null == location) {
            return null;
        }
        return new ParseGeoPoint(location.getLatitude(), location.getLongitude());
    }

    public static String getDateWithoutTime(Date date) {
        String dateString = date.toString();
        return dateString.substring(0, 10) + dateString.substring(23, 28);
    }

    public static String getUserLocation(ParseUser parseUser) {
        return parseUser.get(USER_CITY) + ", " + parseUser.get(USER_COUNTRY);
    }

    /**
     * get the Friends Query of the current user
     *
     * @param userID the unique ID of the current user
     * @return query for output
     */
    public static ParseQuery<ParseObject> getAllFriendsQueryFromUser(final String userID) {
        // get friends objectID
        ParseQuery<ParseObject> friendFrom = ParseQuery.getQuery(FRIENDS);
        friendFrom.whereEqualTo(FRIENDS_FROM, userID);

        ParseQuery<ParseObject> friendTo = ParseQuery.getQuery(FRIENDS);
        friendTo.whereEqualTo(FRIENDS_TO, userID);

        // OR Statement
        List<ParseQuery<ParseObject>> queries = new ArrayList<>();
        queries.add(friendFrom);
        queries.add(friendTo);
        return ParseQuery.or(queries);
    }

    /**
     * get the memories Query of the specific User
     *
     * @param userID the unique ID of the specific user
     * @return query for output
     */
    public static ParseQuery<ParseObject> getAllMemoriesQueryFromUser(final String userID) {
        ParseQuery<ParseObject> queryTotalMemories = ParseQuery.getQuery(ParseUtil.MEMORY);
        queryTotalMemories.whereEqualTo(MEMORY_USRE_OBJECT_ID, userID);
        return queryTotalMemories;
    }

    /**
     * get the recently memories Query of the specific User
     *
     * @param userID the unique ID of the specific user
     * @return query for output
     */
    public static ParseQuery<ParseObject> getRecentMemoriesQueryFromUser(final String userID) {
        Date date = getDateBeforeOneWeek();
        ParseQuery<ParseObject> queryRencently = ParseQuery.getQuery(ParseUtil.MEMORY);
        queryRencently.whereEqualTo(MEMORY_USRE_OBJECT_ID, userID);
        queryRencently.whereGreaterThanOrEqualTo(MEMORY_CREATE_DATE, date);
        return queryRencently;
    }

    /**
     * get the date one week before current date
     *
     * @return date
     */
    private static Date getDateBeforeOneWeek() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        return calendar.getTime();
    }

    /**
     * get all the memories from a specific user
     *
     * @return
     * @throws ParseException
     */
    public static List<ParseObject> getAllMemoriesFromUser(String userID)
            throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ParseUtil.MEMORY);
        query.whereEqualTo(ParseUtil.MEMORY_USRE_OBJECT_ID, userID);
        return query.find();
    }

    /**
     * get public memories from a specific user (not current login user)
     *
     * @param userID
     * @return
     */
    public static List<ParseObject> getAllPublicMemoriesFromUser(String userID)
            throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ParseUtil.MEMORY);
        query.whereEqualTo(ParseUtil.MEMORY_USRE_OBJECT_ID, userID);
        query.whereNotEqualTo(ParseUtil.MEMORY_PRIVACY, "Only me");
        return query.find();
    }


    /**
     * get the information of user
     *
     * @param userID unique userID
     * @return ParseUser contains all the information of the user
     * @throws ParseException
     */
    public static ParseUser getUserInfo(String userID) throws ParseException {
        ParseQuery<ParseUser> queryUser = ParseUser.getQuery();
        try {
            queryUser.get(userID);
        } catch (ParseException e1) {
            e1.printStackTrace();
        }
        return queryUser.getFirst();
    }

    /**
     * get all memories from current user and the friends of current user
     *
     * @return list of Memory ParseObject
     * @throws ParseException
     */
    public static List<ParseObject> getAllMemories() throws ParseException {
        String currentUserID = ParseUser.getCurrentUser().getObjectId();
        List<String> list = getAllUserIDInGoogleMap(currentUserID);

        List<ParseObject> parseObjects = new LinkedList<>();
        for (String userID : list) {
            if (userID.equals(currentUserID)) {
                parseObjects.addAll(ParseUtil.getAllMemoriesFromUser(userID));
            } else {
                parseObjects.addAll(ParseUtil.getAllPublicMemoriesFromUser(userID));
            }
        }
        return parseObjects;
    }

    public static List<String> getAllUserIDInGoogleMap(String currentUserID) throws ParseException {
        List<String> list = new LinkedList<>();
        list.add(currentUserID);
        list.addAll(ParseUtil.getSelectedUser(currentUserID));
        Log.i(TAG, "all list shows in the GoogleMap: " + list.toString());
        return list;
    }

    public static List<ParseObject> getNonSelectedCategory(String currentUserID) throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(FILTER);
        query.whereEqualTo(FILTER_USER_ID, currentUserID);
        query.whereEqualTo(FILTER_IS_SELECTED, false);
        return query.find();
    }

    /**
     * get the selected filter category userID
     *
     * @param currentUserID current login userID
     * @return a set of UserID
     * @throws ParseException
     */
    public static Set<String> getSelectedUser(String currentUserID) throws ParseException {
        Set<String> allFriends = getAllFriendsListFromUser(currentUserID);
        Set<String> nonSelectedFriends = getNonSelectedUser(currentUserID);
        allFriends.removeAll(nonSelectedFriends);
        Log.i(TAG, "all selected friends list: " + allFriends.toString());
        return allFriends;
    }

    /**
     * get the user list in the non selected filter category
     *
     * @param currentUserID current login user ID
     * @return set of the userID
     * @throws ParseException
     */
    public static Set<String> getNonSelectedUser(String currentUserID) throws ParseException {
        // get non-selected category list
        List<ParseObject> objects = getNonSelectedCategory(currentUserID);

        Set<String> friendsID = new HashSet<>();

        for (ParseObject object : objects) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery(FILTER_DETAIL);
            query.whereEqualTo(FILTER_CATEGORY_ID, object.getObjectId());
            List<ParseObject> parseObjects = query.find();
            for (ParseObject parseObject : parseObjects) {
                friendsID.add((String) parseObject.get(FILTER_USER_ID));
            }
        }
        Log.i(TAG, "all non selected friends list: " + friendsID.toString());
        return friendsID;
    }

    /**
     * get the Friends List of the current user
     *
     * @return a set of ID
     * @throws ParseException
     */
    public static Set<String> getAllFriendsListFromUser(String currentID)
            throws ParseException {
        Set<String> friendsID = new HashSet<>();

        ParseQuery<ParseObject> query = ParseUtil.getAllFriendsQueryFromUser(currentID);
        List<ParseObject> list = query.find();
        for (ParseObject object : list) {
            String userID;
            if (object.get(FRIENDS_FROM).equals(currentID)) {
                userID = String.valueOf(object.get(FRIENDS_TO));
            } else {
                userID = String.valueOf(object.get(FRIENDS_FROM));
            }
            friendsID.add(userID);
        }
        Log.i(TAG, "all friends list: " + friendsID.toString());
        return friendsID;
    }

    /**
     * get all the categories by current login user
     *
     * @return list of object about the category
     * @throws ParseException
     */
    public static List<ParseObject> getCategory() throws ParseException {
        String userID = ParseUser.getCurrentUser().getObjectId();
        ParseQuery<ParseObject> query = ParseQuery.getQuery(FILTER);
        query.whereEqualTo(FILTER_USER_ID, userID);
        return query.find();
    }

    /**
     * get the user list in one category of current login user
     *
     * @param categoryID category unique ID
     * @return list of user in the category
     * @throws ParseException
     */
    public static List<ParseUser> getUserInCategory(String categoryID) throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(FILTER_DETAIL);
        query.whereEqualTo(FILTER_CATEGORY_ID, categoryID);
        List<ParseObject> parseObjects = query.find();

        List<ParseUser> res = new LinkedList<>();
        for (ParseObject parseObject : parseObjects) {
            res.add(getUserInfo((String) parseObject.get(FILTER_USER_ID)));
        }
        return res;
    }

    /**
     * add friends
     *
     * @param userID targe
     * @throws ParseException
     */
    public static void addFriends(String userID) throws ParseException {
        // current user Info
        ParseUser currentUser = ParseUser.getCurrentUser();
        String currentUserId = currentUser.getObjectId();
        Log.i(TAG, "current user iD: " + currentUserId);
        Log.i(TAG, "the target userID: " + userID);
        ParseObject parseObject = new ParseObject(ParseUtil.FRIENDS);
        parseObject.put(ParseUtil.FRIENDS_FROM, currentUserId);
        parseObject.put(ParseUtil.FRIENDS_TO, userID);
        parseObject.put(ParseUtil.FRIENDS_COMFIRM, false);
        parseObject.saveInBackground();
    }

    /**
     * sign up
     *
     * @param email
     * @param username
     * @param password
     * @throws ParseException
     */
    public static void signUp(String email, String username, String password)
            throws ParseException {
        ParseUser parseUser = new ParseUser();
        parseUser.setUsername(email);
        parseUser.put(USER_NICKNAME, username);
        parseUser.setPassword(password);
        parseUser.signUp();
    }

    /**
     * save memory into server
     *
     * @param address
     * @param parseGeoPoint
     * @param title
     * @param description
     * @param date
     * @param privacy
     * @param type
     * @param parseFile
     * @throws ParseException
     */
    public static void saveMemoryIntoParse(String address, ParseGeoPoint parseGeoPoint,
                                           String title, String description, Date date,
                                           String privacy, String type, ParseFile parseFile)
            throws ParseException {
        // current user Info
        ParseUser currentUser = ParseUser.getCurrentUser();
        String currentUserId = currentUser.getObjectId();
        ParseObject parseObject = new ParseObject(ParseUtil.MEMORY);
        parseObject.put(MEMORY_USRE_OBJECT_ID, currentUserId);
        parseObject.put(MEMORY_CREATE_DATE, new Date());
        parseObject.put(MEMORY_ADDRESS, address);
        parseObject.put(MEMORY_GEOPOINT, parseGeoPoint);
        parseObject.put(MEMORY_TITLE, title);
        parseObject.put(MEMORY_INTRODUCTION, description);
        parseObject.put(MEMORY_MEMORY_DATE, date);
        parseObject.put(MEMORY_PRIVACY, privacy);
        parseObject.put(MEMORY_MEDIA_TYPE, type);
        if (type.equals(IMAGE)) {
            parseObject.put(MEMORY_FILE, parseFile);
        }
        parseObject.save();
    }

    /**
     * save the user photo
     *
     * @param parseFile photo file
     * @throws ParseException
     */
    public static void saveUserPhoto(ParseFile parseFile) throws ParseException {
        ParseUser user = ParseUser.getCurrentUser();
        user.put(USER_PHOTO, parseFile);
        user.save();
    }

    /**
     * save the category into Parse
     *
     * @param name category name
     * @return the Parse Object of the new Category
     * @throws ParseException
     */
    public static ParseObject saveCategory(String name) throws ParseException {
        // current user Info
        String userID = ParseUser.getCurrentUser().getObjectId();
        ParseObject parseObject = new ParseObject(ParseUtil.FILTER);
        parseObject.put(FILTER_USER_ID, userID);
        parseObject.put(FILTER_CATEGORY_NAME, name);
        parseObject.put(FILTER_IS_SELECTED, true);
        parseObject.save();
        return parseObject;
    }

    /**
     * Save teh friend into Category
     *
     * @param categoryID category unique ID
     * @param userID     user unique ID
     * @throws ParseException
     */
    public static void saveFriendIntoCategory(String categoryID, String userID) throws ParseException {
        String currentUserID = ParseUser.getCurrentUser().getObjectId();
        ParseObject parseObject = new ParseObject(ParseUtil.FILTER_DETAIL);
        parseObject.put(FILTER_CATEGORY_ID, categoryID);
        parseObject.put(FILTER_USER_ID, userID);
        parseObject.put(FILTER_OWNER_ID, currentUserID);
        parseObject.saveInBackground();
    }

    /**
     * save user info
     *
     * @param city
     * @param country
     * @throws ParseException
     */
    public static void saveUserInfo(String city, String country) throws ParseException {
        ParseUser parseUser = ParseUser.getCurrentUser();
        if (city != null) {
            parseUser.put(USER_CITY, city);
        }
        if (country != null) {
            parseUser.put(USER_COUNTRY, country);
        }
        parseUser.saveInBackground();
    }

    /**
     * update the filter: if flag is true,
     * means it should be selected and update the field in the server as true
     *
     * @param objectID filter category unique object ID
     * @param flag     selected or not
     * @throws ParseException
     */
    public static void updateSelectedFilter(String objectID, final boolean flag) throws ParseException {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ParseUtil.FILTER);
        query.getInBackground(objectID, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if (null == e) {
                    Log.i(TAG, "before update: " + parseObject.get(FILTER_IS_SELECTED));
                    parseObject.put(FILTER_IS_SELECTED, flag);
                    Log.i(TAG, "after update: " + parseObject.get(FILTER_IS_SELECTED));
                    parseObject.saveInBackground();
                }
            }
        });
    }

    /**
     * check whether the email exist in the server
     *
     * @param email user input
     * @return whether the email exist or not
     * @throws ParseException
     */
    public static boolean isEmailExistInServer(String email) throws ParseException {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(email, USER_EMAIL);
        ParseUser parseUser = null;
        try {
            parseUser = query.getFirst();
        } catch (ParseException e) {
            Log.e(TAG, "check unique failed: " + e.getMessage());
        }
        return null == parseUser;
    }

    /**
     * check whether the user is in the category
     *
     * @param categoryID category unique ID
     * @param userID     user unique ID
     * @return whether the user exist in the category
     * @throws ParseException
     */
    public static boolean isFriendExistInCategory(String categoryID, String userID) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(FILTER_DETAIL);
        query.whereEqualTo(FILTER_CATEGORY_ID, categoryID);
        query.whereEqualTo(FILTER_USER_ID, userID);
        ParseObject object = null;
        try {
            object = query.getFirst();
        } catch (ParseException e) {
            Log.e(TAG, "check friend exist in categoyr error: " + e.getMessage());
        }
        return null == object;
    }

    /**
     * check for whether the element is unique when sign up
     *
     * @param info      input username or email
     * @param checkType check types: username or email
     * @return true if unique
     * @throws ParseException
     */
    public static boolean isUniqueWhenSignUp(String info, String checkType) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(checkType, info);
        ParseUser parseUser = null;
        try {
            parseUser = query.getFirst();
        } catch (ParseException e) {
            Log.e(TAG, "check unique failed: " + e.getMessage());
        }
        return null == parseUser;
    }

    /**
     * check whether the two users are friends or not
     *
     * @param currentID userId of current user
     * @param userID    userId
     * @return Parse Query
     * @throws ParseException
     */
    public static ParseQuery<ParseObject> isFriendQuery(String currentID, String userID) {
        // get friends objectID
        ParseQuery<ParseObject> friendFrom = ParseQuery.getQuery(FRIENDS);
        friendFrom.whereEqualTo(FRIENDS_FROM, currentID);
        friendFrom.whereEqualTo(FRIENDS_TO, userID);

        ParseQuery<ParseObject> friendTo = ParseQuery.getQuery(FRIENDS);
        friendTo.whereEqualTo(FRIENDS_FROM, userID);
        friendTo.whereEqualTo(FRIENDS_TO, currentID);

        // OR Statement
        List<ParseQuery<ParseObject>> queries = new ArrayList<>();
        queries.add(friendFrom);
        queries.add(friendTo);
        return ParseQuery.or(queries);
    }

    /**
     * check whether the author of the memory is current login user
     *
     * @param userID
     * @return true if it's the same user
     */
    public static boolean isCurrentLoginUser(String userID) {
        String currentUserId = ParseUser.getCurrentUser().getObjectId();
        return currentUserId.equals(userID);
    }

    /**
     * check whether there exist same category name by the user
     *
     * @param name category name
     * @return true if exist
     */
    public static boolean isDuplicateCategory(String name) {
        // current user Info
        String userID = ParseUser.getCurrentUser().getObjectId();
        ParseQuery query = ParseQuery.getQuery(FILTER);
        query.whereEqualTo(FILTER_USER_ID, userID);
        query.whereEqualTo(FILTER_CATEGORY_NAME, name);
        try {
            query.getFirst();
        } catch (ParseException e) {
            final int statusCode = e.getCode();
            if (statusCode == ParseException.OBJECT_NOT_FOUND) {
                return true;
            }
        }
        return false;
    }

//    Delete Module
    /**
     * Delete the category
     *
     * @param objectID category unique ID
     * @throws ParseException
     */
    public static void deleteCategory(String objectID) throws ParseException {
        ParseQuery<ParseObject> queryDetail = ParseQuery.getQuery(FILTER_DETAIL);
        queryDetail.whereEqualTo(FILTER_CATEGORY_ID, objectID);
        queryDetail.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                for (ParseObject parseObject : list) {
                    parseObject.deleteInBackground();
                }
            }
        });
        ParseQuery query = ParseQuery.getQuery(FILTER);
        query.whereEqualTo(OBJECT_ID, objectID);
        ParseObject object = query.getFirst();
        object.deleteInBackground();
    }

    public static void deleteFriendInCategory(String categoryID, String userID) throws ParseException {
        ParseQuery query = ParseQuery.getQuery(FILTER_DETAIL);
        query.whereEqualTo(ParseUtil.FILTER_USER_ID, userID);
        query.whereEqualTo(ParseUtil.FILTER_CATEGORY_ID, categoryID);
        ParseObject object = query.getFirst();
        object.deleteInBackground();
    }

    /**
     * Delete friend
     *
     * @param currentID current login user ID
     * @param userID    the user ID of the target user
     * @throws ParseException
     */
    public static void deleteFriend(String currentID, String userID) throws ParseException {
        // delete in filter
        ParseQuery<ParseObject> queryFilter = ParseQuery.getQuery(FILTER_DETAIL);
        queryFilter.whereEqualTo(FILTER_OWNER_ID, currentID);
        queryFilter.whereEqualTo(FILTER_USER_ID, userID);
        queryFilter.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                for (ParseObject parseObject : list) {
                    parseObject.deleteInBackground();
                }
            }
        });

        ParseQuery<ParseObject> query = ParseUtil.isFriendQuery(currentID, userID);
        ParseObject parseObject = query.getFirst();
        parseObject.deleteInBackground();
    }

}
