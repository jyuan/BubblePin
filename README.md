# BubblePin

## Java

### Library
There are several libraries used in this application, Let me introduce those indiviudally.

- Set up with Parse, you should apply for your own key, you can get access by [Connect your app to Parse](https://www.parse.com/apps/quickstart#parse_data/mobile/android/native/existing)

- Set up with Google Play Service with `com.google.android.gms:play-services:7.0.0` which includes the Google Maps Android API, you can get the help from [Setting Up Google Play Services](https://developer.android.com/google/play-services/setup.html)

	- Notice that if your want to compile the code, you should Obtain an API key, the `Key` in my code is not work for you. You can apply for the Key from [Google Developers Console](https://console.developers.google.com/project?authuser=0)
	- Another thing keep in mind is that you should apply two keys for diffrent purpose, **one for debug, one for release**

- Add the `com.google.maps.android:android-maps-utils:0.3+` into `build.gradle`, it's a [Google Maps Android API Utility Library](https://developers.google.com/maps/documentation/android/utility/), some function like `Cluster` I used in my application list here.

- Add `Metaio` library into your application, same as `Google Map API`, you should applied a unique key for you. You can get steps from  [Creating a new AR application](http://dev.metaio.com/sdk/getting-started/android/creating-a-new-ar-application/index.html)
	- Notice, use the `Alternate method` method list in the `Android Studio`

- Add `com.android.support:appcompat-v7:22.0.0` and `com.android.support:support-v4:22.0.0` in the `build.gradle` under the `app` folder

### Java File
The Overall of the `.java` file, I will intoduce each individually

- Adaper
	- `AddContactAdapter`, `ContactAdapter` and `FilterAdapter` extends from `BaseAdapter`, match with the `ListView`
	- `ViewPagerAdapter` extends from `PagerAdapter` matches with `ViewPager` as the function of **Guide Book** When you initial the application the first time.

- Add Memory Module
	- `AddMemoryActivity`: user can add memory (text or picture), and complete some basic information like date, privacy, title, description etc. If you choose to get the picture media, use `onActivityResult` method to get the media from `ChooseMediaActivity`
	- `ChooseMediaActivity`: Choose for different media type, now, it only support text and picture (from gallery or take a new picture)

- Contact Module
	- `AddContactActivity`: get all user list, add / delete friends. You can also pull from top to bottom to refresh the friends list. For each User, you can click and go to see the profile.
	- `ContactActivity`: get the friends list, for each item, you can scroll from left to right or right or left to delete the friend. For each Friend, you can click and go to see the profile.

- Filter Module
	- `FilterActivity`: you can get the filter list, and mark it as `on/off` to filter the memories in the map or Metaio. You can also Add a new Filter Category, it will pop up a dialog, and save the new category in background thread. for each item, you can scroll from left to right or right or left to delete the cateogory.
	- `FilterDetailActivity`: a list of the users in the Category, for each item, you can scroll from left to right or right or left to delete the user in this category. You can also click the `Add Contact` button to get the list of your friend and select one to add into this category.

- GoogleMap Cluster
	- `BubblePinClusterItem`: Cluster Item Object that contains the Location

- Login Module
	- `LoginActivity`: login page, use the login template provided by the Android Studio to implement error handle friendly
	- `SignUpActivity`: sign up page, check for Unique username, email, also check for correct Email and password format by using `Regular Expression`
	- `ForgetPasswordActivity`: use the parse forget password scheme. First check whether the email exist in server

- MetaioSDK Location Module
	- `MetaioLocationActivity`: show memories in the 3D view by using the Metaio Android SDK. You can click for each Item to get the brief info of that memory
	- `MetaioMemory`: Metaio Memory Object to store the unique object ID and Metaio Location Object

- Profile Module
	- `ProfileActivity`: user's profile page, shows the brief user info (city, username and user photo), summary of the memories and contact number, and also the memories in Google Map
		- If it's the login user profile page, add an edit button and the user can also change the user photo. Also, User can logout and it will exit the application (finish all the previous activities)
	- `ProfileEditActivity`: simple edit info.

- Util
	- `ImageUtil`: encapsulate handle image as a tool class
	- `LocationUpdateUtil`: Receiving Location Updates, the code is reference from [Making Your App Location-Aware](http://developer.android.com/training/location/index.html), I encapsulate it as a separate tool class.
	- `ParseUtil`: encapsulate the parse operation in this tool class and also some public static final variables.
	- `PreferenceUtil`: encapsulate the `SharePeference` as a tool class
	- `RoundImageView`: get the round ImageView rather than original, used to show the personal image, the code is reference from [Round Image View](http://hackeris.me/2014/05/31/androidroundimageview/)
	- `SlideListView`: extend the function of listView so that user can slide from left to right or right to left to do some extra operation. the code is reference from [Slide ListView](http://www.bkjia.com/Androidjc/848089.html)
	- `ValidateUtil`: encapsulate some method here to validate some data. such as email and password format.

- Others' Activity
	- `GoogleMapActivity`: show memories in the Map, if there are more than five memories in the nearest place(based on the zoom in/out level), it will combined as `Cluster`. Click the cluster, you will get the address of those memories, once when you zoom in to an appropriate level, you can seen the `Marker` on the map, click the mark it will pop up a dialog about the brief info of the memory, click for the `Detail` button it will go to the `MemoryDetailActivity`
	- `InitialActivity`: shows logo of the application, login and signup button.
	- `MemoryDetailActivity`: Memory Detail Info, shows all the ifo about this memory, and also some brief info of the author of the memory. If the author of the memory is the current login user. It will shows the delete button at the ActionBar 
	- `MyApplication`: application set up
	- `WelcomeActivity`
		- first time to open the application, show guide page
		- already open but not login or logout, show initial page
		- already login, go directly to `GoogleMapActivity`

