SimpleCameraView
==========================
SimpleCameraView is an android open source library project which is used to customised camera views .

**Features**
* Swap back camera and front camera.
* Any size of square camera or rectangle camera.
* Capture photos.

**Adding the library to your project**
* Download this project and unzip to your root Android Studio workplace.
* Make edition to your project's settings.gradle file:
```gradle
	include ':app'
	include '..:SimpleCameraView:cameraviewlibrary' //add this line
```
* Add gradle compile code to your app level build.gradle file:
```gradle
	dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
	compile 'com.android.support:appcompat-v7:23.0.0'  //add support v7 library
    compile project(':..:SimpleCameraView:cameraviewlibrary')   //add this library
    //and your other dependencies.
}
```

**Implement codes**

* CameraView for Android API20 or below 

CameraView uses [camera api](http://developer.android.com/reference/android/hardware/Camera.html) which is deprecated on android API21.
```xml
	<RelativeLayout android:id="@+id/cameraViewHolder"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
        
        <com.daililol.cameraviewlibrary.CameraView
            android:id="@+id/cameraView"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"/>
        
    </RelativeLayout>
```

* CameraView for Android API21 or above

CameraViewAPI21 uses [camera2 api](https://developer.android.com/reference/android/hardware/camera2/package-summary.html) which is only available on android API21 or above.
```xml
	<RelativeLayout android:id="@+id/cameraViewHolder"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">
        
        <com.daililol.cameraviewlibrary.CameraViewAPI21
            android:id="@+id/cameraView"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"/>
        
    </RelativeLayout>
```

* Creating a square camera view

```java
	private void setupSquareCameraView(){
        cameraViewHolder = (RelativeLayout) findViewById(R.id.cameraViewHolder);
		
        int size = this.getResources().getDisplayMetrics().widthPixels;
		cameraViewHolder.getLayoutParams().width = size;
		cameraViewHolder.getLayoutParams().height = size;
        cameraViewHolder.invalidate();
	}
```

* Creating a rectangle camera view

```java
	private void setupRetangleCameraView(){
        cameraViewHolder = (RelativeLayout) findViewById(R.id.cameraViewHolder);
		
        int size = this.getResources().getDisplayMetrics().widthPixels;
		cameraViewHolder.getLayoutParams().width = size;
		cameraViewHolder.getLayoutParams().height = (int)(size * 0.5f);
        cameraViewHolder.invalidate();
	}
```

![](https://github.com/DanielShum/SimpleCameraView/blob/master/screenshot/rectangle-camera-view.png)
![](https://github.com/DanielShum/SimpleCameraView/blob/master/screenshot/rectangle-camera-view2.png)
![](https://github.com/DanielShum/SimpleCameraView/blob/master/screenshot/square-camera-view.png)


