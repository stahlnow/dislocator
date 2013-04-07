package com.stahlnow.dislocator;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.mapsforge.android.maps.PausableThread;


import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;

class ScreenshotCapturer extends PausableThread {
	private static final String SCREENSHOT_FILE_NAME = "Map screenshot";
	private static final int SCREENSHOT_QUALITY = 90;
	private static final String THREAD_NAME = "ScreenshotCapturer";

	private final DislocatorActivity dislocator;
	private CompressFormat compressFormat;

	ScreenshotCapturer(DislocatorActivity dislocator) {
		this.dislocator = dislocator;
	}

	private File assembleFilePath(File directory) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(SCREENSHOT_FILE_NAME);
		stringBuilder.append('.');
		stringBuilder.append(this.compressFormat.name().toLowerCase(Locale.ENGLISH));
		return new File(directory, stringBuilder.toString());
	}

	@Override
	protected synchronized void doWork() {
		
		
		try {
			File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			if (!directory.exists() && !directory.mkdirs()) {
				this.dislocator.showToastOnUiThread("Could not create screenshot directory");
				return;
			}

			File outputFile = assembleFilePath(directory);
			if (this.dislocator.localMap.takeScreenshot(this.compressFormat, SCREENSHOT_QUALITY, outputFile)) {
				this.dislocator.showToastOnUiThread(outputFile.getAbsolutePath());
			} else {
				this.dislocator.showToastOnUiThread("Screenshot could not be saved");
			}
		} catch (IOException e) {
			this.dislocator.showToastOnUiThread(e.getMessage());
		}

		this.compressFormat = null;
		
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected ThreadPriority getThreadPriority() {
		return ThreadPriority.BELOW_NORMAL;
	}

	@Override
	protected synchronized boolean hasWork() {
		return this.compressFormat != null;
	}

	synchronized void captureScreenshot(CompressFormat screenhotFormat) {
		this.compressFormat = screenhotFormat;
		notify();
	}
}

