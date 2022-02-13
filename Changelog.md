#2022/02/13
- Fix Bug where MainActivity Swipe before songList is Updated

#2022/02/12
- Fix Bug when Item of playlist is deleted and will try to recover

#2022/02/11
- PlayingFragment ViewPager Loop
- seekTo Percentage instead of Duration
- Seekbar Object Animator
- Fix Notification Channel Bug on PlayerNotification

#2022/02/08
- PlayingFragment Palette API background
- reload Children on Device Song List changes

#2022/02/07
-  PlayingFragment that shows currently Playing Song
-  PlayerNotification & PlayingFragment Repeat State
-  PlayingFragment seekTo with Seekbar
-  PlayingFragment Skip Next / Prev button
-  PlayingFragment observe Duration & Progress
-  Reduce Transition Lag

# ~~~~~~~~~~~~

# 2022/02/03
- Added CustomActionReceiver on PlayerNotificationManager
- Added Notification Repeat button that Toggle the exoPlayer Repeat state between OFF, ONCE, ALL
- Notification Repeat button will change depending on the Player Repeat state
- Notification Repeat button will now be placed at index 0 instead the default one defined `usually at [3]`
- now can set Playlist with SearchView by taking the shown songList

# 2022/02/02
- App will now send Toast if needed and not simply crash when exception happen
- Added delay between ServiceConnector request, will cancel if its in between Cooldown
- Fixed Navigator inconsistency when sending command too fast to Connector
- Fixed another inconsistency on MediaStore Query & ViewModels Livedata should only be observed
- Fixed Folder not distinct'd

# 2022/02/01
- Added temporary playlist by Artist & Album from Home Page
- Added Notification Controller!!
- fixed another `onLoadChildren Exception` when changing playlist
- fixed ViewPager not Gliding when changing playlist
- fixed playlist cleared when Notification Cancelled
- fixed suggestion empty when Service Stopped
- fixed yet another liveData NPE, liveData reference will be moved to MainActivity observer instead
- fixed musicQueueNavigator Exception
- added command method to interact with MusicService from UI-VM/Connector-Service

# 2022/01/31
- Fixed ViewPager not Gliding when Activity is Recreated
- Moved Media Query to Repo
- Better Playback Controller
- Fix onLoadChildren Exception
- Fix NPE everywhere :)

# 2022/01/30
- added MusicSource class for converting fetched song metadata to MediaMetadataCompat
- setting up NotificationManager class for handling Notification by MusicService Class
- added NotificationListener class to Listen for NotificationManager changes / callbacks
- added MusicService Playback Preparer & Listener
- added MusicServiceConnector as connector between Activity/ViewModel with MusicService

# 2022/01/29
- yet Another fix to Player Inconsistency `will migrate the player to a Service`
- yet Another fix to problem when song is deleted in Storage `will migrate using Room & Datastore`
- the simple controller will now check for player state even after Activity Recreation `temporary before Service Implemented`
- added better transition & ripple onclick effect
- a little cleanup on MainActivity & ViewModels

# 2022/01/28
- Fixed Activity FinishCallback Leak on Android Q
- Fixed Recyclerview Padding when hiding bnv
- Added Simple Player Listener for playback state & seekbar `Temporary`
- Ability to Play from Home & Folder Fragment
- Fixed Play / Pause Inconsistency when Device Theme changed & Activity Recreation

# 2022/01/27
- Fixed Leak on Adapter & Coordinator Layout
- Fixed Shuffled Song on Suggestion Row is Persisting after Song Deletion on Storage  
- Change Fragment Transition to Motion Layout from Material Design
- Tested and expected the App to be working on Android Nougat 7.0 and above
- Change build gradle to 7.1.0 as per Android Studio Bumblebee 2021.1.1 Stable Update