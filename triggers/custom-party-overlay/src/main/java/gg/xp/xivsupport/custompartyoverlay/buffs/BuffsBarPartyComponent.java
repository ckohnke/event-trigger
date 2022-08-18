package gg.xp.xivsupport.custompartyoverlay.buffs;

import gg.xp.xivsupport.custompartyoverlay.AlwaysRepaintingPartyListComponent;
import gg.xp.xivsupport.custompartyoverlay.BasePartyListComponent;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class BuffsBarPartyComponent extends AlwaysRepaintingPartyListComponent {

	private final BuffsBar bar = new BuffsBar();
	private final StatusEffectRepository buffRepo;
	private final BuffsBarConfig config;

	public BuffsBarPartyComponent(StatusEffectRepository buffRepo, BuffsBarConfig config) {
		this.buffRepo = buffRepo;
		this.config = config;
		config.addAndRunListener(this::applySettings);
	}

	private void applySettings() {
		bar.setNormalBuffColor(config.getNormalTextColor().get());
		bar.setMyBuffColor(config.getMyBuffTextColor().get());
		bar.setRemovableBuffColor(config.getRemoveableBuffColor().get());
		bar.setXPadding(config.getxPadding().get());
		bar.setEnableShadows(config.getShadows().get());
		bar.setEnableTimers(config.getTimers().get());
		bar.reformat();
		bar.repaint();
	}

	@Override
	protected Component makeComponent() {
		return bar;
	}

	@Override
	protected void reformatComponent(@NotNull XivPlayerCharacter xpc) {
		bar.setBuffs(buffRepo.sortedStatusesOnTarget(xpc));
	}
}
