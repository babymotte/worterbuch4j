package net.bbmsoft.worterbuch.speedtester.impl;

import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

public class Utils {

	public static void shutDown() {

		final var bundle = FrameworkUtil.getBundle(Utils.class);
		if (bundle == null) {
			throw new IllegalStateException("Not running in an OSGi container.");
		}
		final var ctx = bundle.getBundleContext();
		if (ctx == null) {
			throw new IllegalStateException("Bundle hast not been activated.");
		}
		final var systemBundle = ctx.getBundle(0);
		if (systemBundle == null) {
			throw new IllegalStateException("System bundle does not exist.");
		}
		try {
			systemBundle.stop();
		} catch (final BundleException e) {
			throw new IllegalStateException("System bundle can't be stopped.", e);
		}

	}
}
