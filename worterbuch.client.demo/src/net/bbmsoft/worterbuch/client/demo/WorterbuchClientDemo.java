package net.bbmsoft.worterbuch.client.demo;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component
public class WorterbuchClientDemo {

	@Activate
	public void activate() {
		System.err.println("Starting...");
	}
	
	@Deactivate
	public void deactivate() {
		System.err.println("Stopping...");
	}
}
