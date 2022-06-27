# BoofCV_ZCL

### Open Source Links
- [BoofCV](https://boofcv.org/index.php?title=Main_Page)
- [BoofCV Android](http://peterabeles.com/blog/?p=204)
- [BoofCV Fiducial Marker 284](https://boofcv.org/images/5/54/Fiducial_squre_binary.png)
- [BoofCV 9x6 Calibration Chessboard](http://boofcv.org/notwiki/calibration/A4_chessboard.pdf)) 

### Issues / Problems to Solve
- Need to be able to access detected cubes in FiducialSquareActivity in order to combine with touch
- Temp Solution(?) --> changed the label itself of RenderCube3D.java to include a percentage
- Discuss and change the MYSQL querying method --> Right now, it queries the db for everytime a cube is detected (Slows down the UI thread)


### UI Updates you can do (Assuming that touch detection works)
- You can add a [BottomSheet](https://www.section.io/engineering-education/bottom-sheet-dialogs-using-android-studio/) to fiducial_controls.xml (Change linearlayout to relativelayout). (This works, I tested)
