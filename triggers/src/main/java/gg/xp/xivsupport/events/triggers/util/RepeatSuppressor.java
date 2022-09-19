package gg.xp.xivsupport.events.triggers.util;

import gg.xp.reevent.events.BaseEvent;

import java.time.Duration;

public class RepeatSuppressor {

	private final Duration suppressionDuration;
	private BaseEvent last;

	public RepeatSuppressor(Duration suppressionDuration) {
		this.suppressionDuration = suppressionDuration;
	}

	public boolean check(BaseEvent event) {
		final boolean result;
		if (last == null) {
			result = true;
		}
		else {
			result = last.getEffectiveTimeSince().compareTo(suppressionDuration) > 0;
		}
		last = event;
		return result;

	}

}
