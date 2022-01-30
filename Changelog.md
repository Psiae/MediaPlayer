# 2022/01/30
- added MusicSource class for converting fetched song metadata to MediaMetadataCompat
- setting up NotificationManager class for handling Notification by MusicService Class
- added NotificationListener class to Listen for NotificationManager changes / callbacks

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