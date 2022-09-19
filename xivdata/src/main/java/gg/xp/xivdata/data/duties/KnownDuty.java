package gg.xp.xivdata.data.duties;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public enum KnownDuty implements Duty {
	None("General", Expansion.GENERAL, DutyType.OTHER),
	Odin("Urth's Fount", 394, Expansion.ARR, DutyType.TRIAL),
	UWU("Weapon's Refrain", 0x309L, Expansion.SB, DutyType.ULTIMATE),
	P1S("P1S", 0x3EB, Expansion.EW, DutyType.SAVAGE_RAID),
	P2S("P2S", 0x3ED, Expansion.EW, DutyType.SAVAGE_RAID),
	P3S("P3S", 0x3EF, Expansion.EW, DutyType.SAVAGE_RAID),
	P4S("P4S", 0x3F1, Expansion.EW, DutyType.SAVAGE_RAID),
	P5N("P5N", 0x439, Expansion.EW, DutyType.RAID),
	P6N("P6N", 0x43B, Expansion.EW, DutyType.RAID),
	P7N("P7N", 0x43D, Expansion.EW, DutyType.RAID),
	P8N("P8N", 0x43F, Expansion.EW, DutyType.RAID),
	P5S("P5S", 0x43A, Expansion.EW, DutyType.SAVAGE_RAID),
	P6S("P6S", 0x43C, Expansion.EW, DutyType.SAVAGE_RAID),
	P7S("P7S", 0x43E, Expansion.EW, DutyType.SAVAGE_RAID),
	P8S("P8S", 0x440, Expansion.EW, DutyType.SAVAGE_RAID),
	EndsingerEx("EX3", 0x3e6, Expansion.EW, DutyType.TRIAL_EX),
	BarbarEx("EX4", 1072, Expansion.EW, DutyType.TRIAL_EX),
	Dragonsong("Dragonsong", 0x3C8, Expansion.EW, DutyType.ULTIMATE);

	private final String name;
	private final Expansion expac;
	private final DutyType type;
	private final @Nullable Long zoneId;

	KnownDuty(String name, long zoneId, Expansion expac, DutyType type) {
		this.name = name;
		this.expac = expac;
		this.type = type;
		this.zoneId = zoneId;
	}

	KnownDuty(String name, Expansion expac, DutyType type) {
		this.name = name;
		this.expac = expac;
		this.type = type;
		this.zoneId = null;
	}

	public static KnownDuty forZone(long zone) {
		return Arrays.stream(values())
				.filter(kd -> kd.zoneId != null && kd.zoneId == zone)
				.findFirst()
				.orElse(null);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Expansion getExpac() {
		return expac;
	}

	@Override
	public DutyType getType() {
		return type;
	}

	@Override
	public @Nullable Long getZoneId() {
		return zoneId;
	}
}
