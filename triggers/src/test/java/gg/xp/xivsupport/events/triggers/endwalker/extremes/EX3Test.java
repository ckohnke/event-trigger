package gg.xp.xivsupport.events.triggers.endwalker.extremes;

import gg.xp.xivsupport.events.triggers.util.CalloutInitialValues;
import gg.xp.xivsupport.events.triggers.util.CalloutVerificationTest;

import java.util.List;

public class EX3Test extends CalloutVerificationTest {
	@Override
	protected String getFileName() {
		return "/ew_ex3.log";
	}

	@Override
	protected List<CalloutInitialValues> getExpectedCalls() {
		return List.of(
				call(6906, "Raidwide", "Raidwide (4.7)"),
				call(18270, "Northeast safe", "Northeast safe (6.7)"),
				call(25079, "Raidwide", "Raidwide (4.7)"),
				call(36446, "Knockback from Northwest", "Knockback from Northwest (6.7)"),
				call(55377, "Break Tether (with BRD Player)", "Break Tether (with BRD Player)"),
				call(59565, "Middle", "Middle (5.7)"),
				call(69679, "Big Raidwide", "Big Raidwide (4.7)"),
				call(79838, "Tankbuster", "Tankbuster (4.7)"),
				call(92041, "Raidwide", "Raidwide (4.7)"),
				call(103401, "Knockback from Southeast", "Knockback from Southeast (6.7)"),
				call(109194, "Healer Stacks", "Healer Stacks (4.7)"),
				call(127461, "Northwest then Southeast", "Northwest, then Southeast (6.5)"),
				call(139543, "Middle", "Middle (5.7)"),
				call(160876, "Southeast, Southwest, Northwest"),
				call(189251, "Southeast"),
				call(199440, "Sides", "Sides (5.7)"),
				call(209592, "Tankbuster", "Tankbuster (4.7)"),
				call(226220, "Spread", "Spread (6.0)"),
				call(239337, "Flare", "Flare (6.0)"),
				call(244125, "Middle", "Middle (5.7)"),
				call(259721, "Donut", "Donut (6.0)"),
				call(269398, "Flare", "Flare (12.0)"),
				call(330605, "Southwest and Northwest safe"),
				call(339716, "Northeast and Southeast safe"),
				call(342675, "West and Northeast safe"),
				call(348191, "Middle", "Middle (5.7)"),
				call(358297, "Big Raidwide", "Big Raidwide (4.7)"),
				call(368417, "Tankbuster", "Tankbuster (4.7)"),
				call(382774, "Raidwide", "Raidwide (4.7)"),
				call(394137, "Southwest safe", "Southwest safe (6.7)"),
				call(399942, "Healer Stacks", "Healer Stacks (4.7)"),
				call(416234, "Southwest then Northeast", "Southwest, then Northeast (6.5)"),
				call(426358, "Northwest then Southeast", "Northwest, then Southeast (6.5)"),
				call(441529, "Sides", "Sides (5.7)"),
				call(487061, "Northwest, Northeast, Southeast"),
				call(515479, "Northwest"),
				call(525701, "Middle", "Middle (5.7)"),
				call(535799, "Tankbuster", "Tankbuster (4.7)")
		);
	}
}
