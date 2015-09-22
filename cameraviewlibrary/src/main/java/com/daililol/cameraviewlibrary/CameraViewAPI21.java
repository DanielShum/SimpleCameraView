package com.daililol.cameraviewlibrary;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewParent;
import android.widget.RelativeLayout;


@SuppressLint("NewApi")
public class CameraViewAPI21 extends TextureView implements TextureView.SurfaceTextureListener {

	private ViewParent parentView;
	private CameraManager cameraManager;
	private Semaphore cameraOpenCloseLock = new Semaphore(1);
	private Size previewSize;
	private Size captureSize;
	private Surface previewSurface;
	private CameraDevice cameraDevice;
	private CameraCaptureSession.StateCallback previewStateCallback;
	private CaptureRequest.Builder previewBuilder;
	private CameraCaptureSession captureSession;
	private CaptureSessionState captureSessionState;
	private Handler backgroundHandler;
	private HandlerThread backgroundThread;
	private ImageReader imageReader;

	private Point viewSize;
	private int viewHeight = 0;
	private PhotoCaptureCallback photoCaptureCallback;
	private String currentCamera;
	private String frontCamera;
	private String backCamera;
	
	private static enum CaptureSessionState{
		PREVIEW,
		CAPTURE
	}

	
	public static interface PhotoCaptureCallback{
		public void onPhotoCaptured(boolean success, Bitmap bitmap);
	}
	
	public CameraViewAPI21(Context context) {
		this(context, null);
	}

	
	public CameraViewAPI21(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setSurfaceTextureListener(this);
		cameraManager = (CameraManager) getContext().getSystemService(Activity.CAMERA_SERVICE);
		assignCameraIds();
		
	}
	
	
	
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		startPreview();
		invalidate();
	}


	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		releaseCamera();
		return false;
	}


	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		
		
	}


	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		
		
	}
	
	public void startPreview(){
		if (currentCamera == null) return;
		setupPreviewSize(currentCamera);
		if (previewSize == null) return;
		setupImageReader();
		
		try{
			if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
	            throw new RuntimeException("Time out waiting to lock camera opening.");
	        }

			cameraManager.openCamera(currentCamera, cameraStateCallback, null);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void assignCameraIds(){
		try {
			String[] cameraIds = cameraManager.getCameraIdList();
			if (cameraIds == null) return;
			if (cameraIds.length > 0) backCamera = cameraIds[0];
			if (cameraIds.length > 1) frontCamera = cameraIds[1];
			currentCamera = backCamera;
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}
	
	private void setupPreviewSize(String cameraId){
		previewSize = null;
		try{
			CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
			StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			captureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());
			previewSize = getOptimalPreviewSize(map.getOutputSizes(SurfaceTexture.class), 600, 600, captureSize);

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void setupImageReader(){
		 imageReader = ImageReader.newInstance(captureSize.getWidth(), captureSize.getHeight(), ImageFormat.JPEG, 1 /*maxImages*/);
		 imageReader.setOnImageAvailableListener(imageAvailableListener, getBackgroundHandler());
	}
	
	private Surface getPreviewSurface(){

		if (previewSurface != null) return previewSurface;
		
		SurfaceTexture texture = getSurfaceTexture();
		if (texture == null) return null;
		texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
		previewSurface = new Surface(texture);
		
		return previewSurface;
	}
	
	
	private Handler getBackgroundHandler(){
		
		if (backgroundHandler != null) return backgroundHandler;
		backgroundThread = new HandlerThread("CameraPreview");
		backgroundThread.start();
		backgroundHandler = new Handler(backgroundThread.getLooper());
		return backgroundHandler;
	}
	
	private void stopBackgroundThread(){
		if (backgroundThread == null) return;
		backgroundThread.quitSafely();
		try{
			backgroundThread.join();
			backgroundThread = null;
			backgroundHandler = null;
		}catch(Exception e){
			
		}
	}
	
	public String getFrontCamera(){
		return frontCamera;
	}
	
	public String getBackCamera(){
		return backCamera;
	}
	
	public String getCurrentCamera(){
		return currentCamera;
	}
	
	
	public void setPhotoCaptureCallback(PhotoCaptureCallback callback){
		photoCaptureCallback = callback;
	}

	public void useFrontCamera(){
		if (getCurrentCamera().equals(frontCamera)) return;
		if (frontCamera == null) return;

		currentCamera = frontCamera;
		cameraDevice.close();
		startPreview();
	}

	public void useBackCamera(){
		if (getCurrentCamera().equals(backCamera)) return;
		if (backCamera == null) return;

		currentCamera = backCamera;
		cameraDevice.close();
		startPreview();
	}

	public void swapCamera(){
		if (getCurrentCamera().equals(backCamera)){
			useFrontCamera();
		}else{
			useBackCamera();
		}
	}
	
	public void capture(){
		if (captureSession == null) return;
		captureSessionState = CaptureSessionState.CAPTURE;
		
		try {
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            //captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            captureSession.stopRepeating();
            captureSession.capture(captureBuilder.build(), captureCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

	}
	

	
    private ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {

        	processPhoto(reader.acquireLatestImage());

        }
    };
	
	private CameraCaptureSession.CaptureCallback captureCallback = new  CameraCaptureSession.CaptureCallback(){
		
		synchronized void process(CaptureResult partialResult){
			if (captureSessionState == null) return;
			switch (captureSessionState){
			case PREVIEW:{
				break;
			}case CAPTURE:{
				break;
			}
			
			}
			
		}
		
		@Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
			process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
        	process(result);
        }
	};
	
	private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback(){

		@Override
		public void onOpened(CameraDevice camera) {
			cameraOpenCloseLock.release();
			cameraDevice = camera;
			captureSessionState = CaptureSessionState.PREVIEW;
			
			try {
				previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
				previewBuilder.addTarget(getPreviewSurface());
				cameraDevice.createCaptureSession(Arrays.asList(getPreviewSurface(), imageReader.getSurface()), createPreviewStateCallback(), null);
			} catch (CameraAccessException e){
				e.printStackTrace();
			}
			
		}
		
		@Override
		public void onDisconnected(CameraDevice arg0) {
			cameraOpenCloseLock.release();
		}

		@Override
		public void onError(CameraDevice arg0, int arg1) {
			cameraOpenCloseLock.release();
		}

	};


	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (previewSize == null) return;

		parentView = getParent();
		if (parentView == null || !(parentView instanceof RelativeLayout)){
			throw new RuntimeException("The CameraView's parent view must be a RelativeLayout.");
		}

		RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)getLayoutParams();
		if (params == null) params = new RelativeLayout.LayoutParams
				(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);

		viewSize = new Point(((RelativeLayout) parentView).getWidth(), ((RelativeLayout) parentView).getHeight());
		float sizePercentage = (float)viewSize.x / (float)previewSize.getHeight();
		viewHeight = (int)((float)previewSize.getWidth() * sizePercentage);

		this.setMeasuredDimension(viewSize.x, viewHeight);
	}
	
	private CameraCaptureSession.StateCallback createPreviewStateCallback(){
		
		previewStateCallback = new CameraCaptureSession.StateCallback(){

			@Override
			public void onConfigureFailed(CameraCaptureSession session) {
				
				
			}

			@Override
			public void onConfigured(CameraCaptureSession session) {

				captureSession = session;
				previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
				
				try {
					captureSession.setRepeatingRequest(previewBuilder.build(), null, getBackgroundHandler());
				} catch (CameraAccessException e) {
				}
				
			}
			
		};
		
		return previewStateCallback;
	}
	
	
	public void releaseCamera(){
		stopBackgroundThread();

		if (cameraDevice == null) return;
		
		try{
			previewSurface.release();
			cameraOpenCloseLock.release();
			captureSession.close();
			cameraDevice.close();
			imageReader.close();
			cameraDevice = null;
			previewStateCallback = null;
			previewSurface = null;
		}catch(Exception e){
			
		}
	}

	private void processPhoto(Image image){

		if (image == null){
			return;
		}

		if (image == null){
			if (photoCaptureCallback != null) photoCaptureCallback.onPhotoCaptured(false, null);
			return;
		}

		ByteBuffer buffer = image.getPlanes()[0].getBuffer();
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data);
		image.close();

		AsyncTask<Object, Void, Object> task = new AsyncTask<Object, Void, Object>(){

			@Override
			protected Object doInBackground(Object... params) {

				byte[] data = (byte[])params[0];

				Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
				if (bitmap == null) return null;

				float ratio = (float)bitmap.getHeight() / (float)viewSize.x;
				int heightDifference = bitmap.getWidth() - (int)((float)viewSize.y * ratio);

				if (heightDifference > 2) {
					float offset = (float)heightDifference / 2.0f;
					bitmap = Bitmap.createBitmap(bitmap, (int)offset, 0, (int)((float)viewSize.y * ratio), bitmap.getHeight());
					bitmap = createRotatedBitmap(bitmap, (getCurrentCamera() == getFrontCamera()) ? -90 : 90);
				}

				return bitmap;

			}

			@Override
			protected void onPostExecute(Object bitmap){
				super.onPostExecute(bitmap);

				if (photoCaptureCallback != null) photoCaptureCallback.onPhotoCaptured
						(bitmap == null ? false : true, (Bitmap) bitmap);

				try {
					captureSession.setRepeatingRequest(previewBuilder.build(), null, getBackgroundHandler());
				} catch (CameraAccessException e) {
					e.printStackTrace();
				}
			}

		};

		if (Build.VERSION.SDK_INT >= 11){
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);
		}else{
			task.execute(data);
		}
	}
	
	private Size getOptimalPreviewSize(Size[] choices, int width, int height, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            return choices[0];
        }
    }
	
	private class CompareSizesByArea implements Comparator<Size> {

	    @Override
	    public int compare(Size lhs, Size rhs) {
	        // We cast here to ensure the multiplications won't overflow
	        return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
	                (long) rhs.getWidth() * rhs.getHeight());
	    }

	}

	private Bitmap createRotatedBitmap(Bitmap bitmap, int rotation){

		if (bitmap == null) return null;

		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		Matrix mtx = new Matrix();
		mtx.preRotate(rotation);
		bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);

		return bitmap;
	}


	
	



}
