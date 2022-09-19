package gg.xp.xivsupport.events.triggers.easytriggers.conditions;

import com.fasterxml.jackson.annotation.JacksonInject;
import gg.xp.xivsupport.events.actlines.events.HasSourceEntity;
import gg.xp.xivsupport.events.actlines.events.HasTargetEntity;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.triggers.easytriggers.model.SimpleCondition;
import gg.xp.xivsupport.models.XivCombatant;

public class TargetPartyMemberFilter implements SimpleCondition<HasTargetEntity> {

	@Description("Invert (not in party)")
	public boolean invert;
	@EditorIgnore
	private final XivState state;

	public TargetPartyMemberFilter(@JacksonInject XivState state) {
		this.state = state;
	}

	@Override
	public String fixedLabel() {
		return "Target is party member";
	}

	@Override
	public String dynamicLabel() {
		return String.format("Target %s in your party", invert ? "is not" : "is");
	}

	@Override
	public boolean test(HasTargetEntity hasTargetEntity) {
		XivCombatant cbt = hasTargetEntity.getTarget().walkParentChain();
		//noinspection SuspiciousMethodCalls
		boolean isInParty = state.getPartyList().contains(cbt);
		return isInParty != invert;
	}
}
