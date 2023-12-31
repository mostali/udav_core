package mpf.multitask;

import java.util.concurrent.ThreadFactory;

public class CustomizableThreadFactory extends CustomizableThreadCreator implements ThreadFactory {
	public CustomizableThreadFactory() {
	}

	public CustomizableThreadFactory(String threadNamePrefix) {
		super(threadNamePrefix);
	}

	public Thread newThread(Runnable runnable) {
		return this.createThread(runnable);
	}
}
