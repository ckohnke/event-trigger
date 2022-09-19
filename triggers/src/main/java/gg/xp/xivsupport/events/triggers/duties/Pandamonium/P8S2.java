package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.BaseEvent;
import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.AutoFeed;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.ActiveCastRepository;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.seq.SequentialTrigger;
import gg.xp.xivsupport.events.triggers.seq.SequentialTriggerController;
import gg.xp.xivsupport.events.triggers.seq.SqtTemplates;
import gg.xp.xivsupport.models.ArenaPos;
import gg.xp.xivsupport.models.ArenaSector;
import gg.xp.xivsupport.models.XivCombatant;
import gg.xp.xivsupport.models.XivPlayerCharacter;
import org.apache.commons.lang3.time.DurationUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// TODO: this is causing there to be a second arena positions tab
@CalloutRepo(name = "P8S Final Boss", duty = KnownDuty.P8S)
public class P8S2 extends AutoChildEventHandler implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P8S2.class);
	private final ArenaPos arenaPos = new ArenaPos(100, 100, 8, 8);

	private final ModifiableCallout<AbilityCastStart> tyrantsUnholyDarkness = ModifiableCallout.durationBasedCall("Tyrant's Unholy Darkness", "Split Buster");
	private final ModifiableCallout<AbilityCastStart> aioniopyr = ModifiableCallout.durationBasedCall("Aioniopyr", "Raidwide with Bleed");
	private final ModifiableCallout<AbilityCastStart> aionagonia = ModifiableCallout.durationBasedCall("Aionagonia", "Raidwide with Bleed");
//				if (alchNow) {
//		s.updateCall(hc1doSecondAlch.getModified());
//		s.waitMs(2_000);
//		s.updateCall(hc1doSecondTowers.getModified());
//	}
//				else {
//		s.updateCall(hc1dontDoSecondAlch.getModified());
//		s.waitMs(2_000);
//		s.updateCall(hc1dontDoSecondTowers.getModified());
//	}

	public P8S2(XivState state, ActiveCastRepository acr, StatusEffectRepository buffs) {
		this.state = state;
		this.acr = acr;
		this.buffs = buffs;
	}

	private final XivState state;

	private XivState getState() {
		return this.state;
	}

	private final ActiveCastRepository acr;

	private ActiveCastRepository getAcr() {
		return acr;
	}

	private final StatusEffectRepository buffs;

	private StatusEffectRepository getBuffs() {
		return buffs;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.P8S);
	}

	@HandleEvents
	public void startsCasting(EventContext context, AbilityCastStart event) {
		int id = (int) event.getAbility().getId();
		ModifiableCallout<AbilityCastStart> call;
		switch (id) {
			// TODO: figure out correct IDs
			case 31197, 31198 -> call = tyrantsUnholyDarkness; // split buster, no bleed
			case 31199 -> call = aioniopyr; // raidwide+bleed
			case 31266 -> call = aionagonia;
			default -> {
				return;
			}
		}
		context.accept(call.getModified(event));
	}

	@HandleEvents
	public void playerBuff(EventContext context, BuffApplied buff) {
		if (buff.getTarget().isThePlayer()) {
			if (buff.buffIdMatches(0xD14)) {
				context.accept(hc2prey.getModified(buff));
			}
		}
	}

	private enum TowerColor {
		Blue,
		Purple,
		Green
	}

	// red
	private static final int impAlpha = 0xD02;
	// yellow
	private static final int impBeta = 0xD03;
	// orange
	private static final int impGamma = 0xD04;
	// green on red
	private static final int perfAlpha = 0xD05;
	// yellow on yellow
	private static final int perfBeta = 0xD06;
	// purple on orange
	private static final int perfGamma = 0xD07;
	// 'no' sign
	private static final int inconceivable = 0xD08;
	// red DNA
	private static final int solosplice = 0xD11;
	// yellow DNA
	private static final int multisplice = 0xD12;
	// blue DNA
	private static final int supersplice = 0xD13;

	private enum NaturalAlignmentRole {
		ON_YOU,
		SAME_ROLE,
		OTHER_ROLE
	}

	private final ModifiableCallout<BuffApplied> naturalAlignmentOnYou = ModifiableCallout.<BuffApplied>durationBasedCall("Natural Alignment on You", "Natural Alignment on {naPlayers}").autoIcon();
	private final ModifiableCallout<BuffApplied> naturalAlignmentSameRole = new ModifiableCallout<>("Natural Alignment on Your Role", "Natural Alignment Role - {naPlayers}");
	private final ModifiableCallout<BuffApplied> naturalAlignmentOtherRole = new ModifiableCallout<>("Natural Alignment on Other Role", "Other Role has Natural Alignment");
	private final ModifiableCallout<?> naStackFirst = new ModifiableCallout<>("NA1: Stack First", "Stack First");
	private final ModifiableCallout<?> naSpreadFirst = new ModifiableCallout<>("NA1: Spread First", "Spread First");
	private final ModifiableCallout<?> naStackFirstWithNA = new ModifiableCallout<>("NA1: Stack First", "Avoid Stack");
	private final ModifiableCallout<?> naSpreadFirstWithNA = new ModifiableCallout<>("NA1: Spread First", "Avoid Spread");
	private final ModifiableCallout<AbilityCastStart> baitThenStack = ModifiableCallout.durationBasedCall("Bait then Stack");
	private final ModifiableCallout<AbilityCastStart> baitThenSpread = ModifiableCallout.durationBasedCall("Bait then Spread");
	private final ModifiableCallout<AbilityCastStart> naAvoidPlayers = ModifiableCallout.durationBasedCall("Bait then Avoid");
	private final ModifiableCallout<AbilityCastStart> spreadSecond = ModifiableCallout.durationBasedCall("Spread {safe}");
	private final ModifiableCallout<AbilityCastStart> stackSecond = ModifiableCallout.durationBasedCall("Stack {safe}");
	private final ModifiableCallout<AbilityCastStart> avoidPlayersCleave = ModifiableCallout.durationBasedCall("{safe}, Avoid Players");
	private final ModifiableCallout<?> iceFireNothing = new ModifiableCallout<>("Ice/Fire - With Natural Alignment", "Avoid Stacks - {frontSafe ? \"Front\" : \"Second\"} row");
	private final ModifiableCallout<?> firePairsSameRole = new ModifiableCallout<>("Fire Pairs, NA Role", "Fire Pairs - {frontSafe ? \"Front\" : \"Second\"} row");
	private final ModifiableCallout<?> iceLightPartiesSameRole = new ModifiableCallout<>("Ice Parties, NA Role", "Ice Parties - {frontSafe ? \"Front\" : \"Second\"} row");
	private final ModifiableCallout<?> firePairsOtherRole = new ModifiableCallout<>("Fire Pairs, Other Role", "Fire Pairs - {frontSafe ? \"Front\" : \"Second\"} row");
	private final ModifiableCallout<?> iceLightPartiesOtherRole = new ModifiableCallout<>("Ice Parties, Other Role", "Ice Parties - {frontSafe ? \"Front\" : \"Second\"} row");
	private final ModifiableCallout<?> na2StackFirst = new ModifiableCallout<>("NA2: Stack First", "Stack First");
	private final ModifiableCallout<?> na2SpreadFirst = new ModifiableCallout<>("NA2: Spread First", "Spread First");
	private final ModifiableCallout<?> na2StackFirstWithNA = new ModifiableCallout<>("NA2: Avoid Stack First", "Avoid Stack First");
	private final ModifiableCallout<?> na2SpreadFirstWithNA = new ModifiableCallout<>("NA2: Avoid Stack First", "Avoid Spread First");
	private final ModifiableCallout<?> na2avoidPlayers = new ModifiableCallout<>("NA2: Avoid", "Avoid Players - {frontSafe ? \"Front\" : \"Second\"} row");
	private final ModifiableCallout<?> na2stack = new ModifiableCallout<>("NA2: Stack", "Stack - {frontSafe ? \"Front\" : \"Second\"} row");
	private final ModifiableCallout<?> na2spread = new ModifiableCallout<>("NA2: Spread", "Spread - {frontSafe ? \"Front\" : \"Second\"} row");
	private final ModifiableCallout<?> na2cleave = new ModifiableCallout<>("NA2: Cleave", "{safe} safe");
//	private final ModifiableCallout<?> na2StackFirst = new ModifiableCallout<>("NA2: Stack First", "Stack - {frontSafe ? \"Front\" : \"Second\"} row");
//	private final ModifiableCallout<?> na2SpreadFirst = new ModifiableCallout<>("NA2: Spread First", "Spread - {frontSafe ? \"Front\" : \"Second\"} row");

	@AutoFeed
	private final SequentialTrigger<BaseEvent> naturalAlignment1 = SqtTemplates.multiInvocation(80_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(31163),
			this::na1,
			this::na2);

	private void na1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
		log.info("NA1: Start");
		List<BuffApplied> naturalAlignmentBuffs = s.waitEventsQuickSuccession(2,
				BuffApplied.class, ba -> ba.buffIdMatches(3412), Duration.ofMillis(200));
		List<XivCombatant> naBuffs = naturalAlignmentBuffs.stream().map(BuffApplied::getTarget).toList();
		Map<String, Object> params = Map.of("naPlayers", naBuffs);
		final NaturalAlignmentRole roleStatus;
		if (naBuffs.stream().anyMatch(XivCombatant::isThePlayer)) {
			roleStatus = NaturalAlignmentRole.ON_YOU;
		}
		else {
			boolean sameRole = getState().getPlayerJob().isDps() == ((XivPlayerCharacter) naBuffs.get(0)).getJob().isDps();
			if (sameRole) {
				roleStatus = NaturalAlignmentRole.SAME_ROLE;
			}
			else {
				roleStatus = NaturalAlignmentRole.OTHER_ROLE;
			}
		}
		log.info("NA1: Role {}", roleStatus);
		ModifiableCallout<BuffApplied> call = switch (roleStatus) {
			case ON_YOU -> naturalAlignmentOnYou;
			case SAME_ROLE -> naturalAlignmentSameRole;
			case OTHER_ROLE -> naturalAlignmentOtherRole;
		};
		s.accept(call.getModified(naturalAlignmentBuffs.get(0), params));
		{
			// 1e3 = spread then stack
			// 1e1 = stack then spread
			BuffApplied headBuff = s.waitEvent(BuffApplied.class,
					ba -> ba.getBuff().getId() == 0x9F8
							&& (ba.getRawStacks() == 0x1e1
							|| ba.getRawStacks() == 0x1e3));
			boolean stackFirst = headBuff.getRawStacks() == 0x1e1;
			log.info("NA1: Head Buff 1: {}", headBuff);
			log.info("NA1: Stack First? {}", stackFirst);
			if (roleStatus != NaturalAlignmentRole.ON_YOU) {
				if (stackFirst) {
					s.updateCall(naStackFirst.getModified());
				}
				else {
					s.updateCall(naSpreadFirst.getModified());
				}
			}
			else {
				if (stackFirst) {
					s.updateCall(naStackFirstWithNA.getModified());
				}
				else {
					s.updateCall(naSpreadFirstWithNA.getModified());
				}
			}
			AbilityCastStart flare = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7a89));
			log.info("NA1: Got Flare");
			if (roleStatus == NaturalAlignmentRole.ON_YOU) {
				s.updateCall(naAvoidPlayers.getModified(flare));
			}
			else {
				s.updateCall((stackFirst ? baitThenStack : baitThenSpread).getModified(flare));
			}
			AbilityCastStart cleave = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x79D7, 0x79D8));
			ArenaSector safe = cleave.abilityIdMatches(0x79D7) ? ArenaSector.EAST : ArenaSector.WEST;
			log.info("NA1: Cleave Safe Spot {}", safe);
			Map<String, Object> cleaveParams = Map.of("safe", safe);
			if (roleStatus == NaturalAlignmentRole.ON_YOU) {
				s.updateCall(avoidPlayersCleave.getModified(cleave, cleaveParams));
			}
			else {
				s.updateCall((stackFirst ? spreadSecond : stackSecond).getModified(cleave, cleaveParams));
			}
		}

		{
			BuffApplied headBuff = s.waitEvent(BuffApplied.class,
					ba -> ba.getBuff().getId() == 0x9F8
							&& (ba.getRawStacks() == 0x1dd
							|| ba.getRawStacks() == 0x1df));
			boolean partnersFirst = headBuff.getRawStacks() == 0x1dd;
			log.info("NA1: Head Buff 2: {}", headBuff);
			log.info("NA1: Partners First {}", partnersFirst);
			{
				List<AbilityCastStart> endOfDays = s.waitEvents(3, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7A8B));
				boolean frontSafe = endOfDays.stream().noneMatch(acs -> acs.getSource().getPos().y() < 90);
				log.info("NA1: Front Safe? {}", frontSafe);
				Map<String, Object> safeSpotParams = Map.of("frontSafe", frontSafe);
				switch (roleStatus) {
					case ON_YOU -> {
						s.updateCall(iceFireNothing.getModified(safeSpotParams));
					}
					case SAME_ROLE -> {
						if (partnersFirst) {
							s.updateCall(firePairsSameRole.getModified(safeSpotParams));
						}
						else {
							s.updateCall(iceLightPartiesSameRole.getModified(safeSpotParams));
						}
					}
					case OTHER_ROLE -> {
						if (partnersFirst) {
							s.updateCall(firePairsOtherRole.getModified(safeSpotParams));
						}
						else {
							s.updateCall(iceLightPartiesOtherRole.getModified(safeSpotParams));
						}
					}
				}
			}
			{
				List<AbilityCastStart> endOfDays = s.waitEvents(3, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7A8B));
				boolean frontSafe = endOfDays.stream().noneMatch(acs -> acs.getSource().getPos().y() < 90);
				log.info("NA1: Front Safe? {}", frontSafe);
				Map<String, Object> safeSpotParams = Map.of("frontSafe", frontSafe);
				switch (roleStatus) {
					case ON_YOU -> {
						s.updateCall(iceFireNothing.getModified(safeSpotParams));
					}
					case SAME_ROLE -> {
						if (!partnersFirst) {
							s.updateCall(firePairsSameRole.getModified(safeSpotParams));
						}
						else {
							s.updateCall(iceLightPartiesSameRole.getModified(safeSpotParams));
						}
					}
					case OTHER_ROLE -> {
						if (!partnersFirst) {
							s.updateCall(firePairsOtherRole.getModified(safeSpotParams));
						}
						else {
							s.updateCall(iceLightPartiesOtherRole.getModified(safeSpotParams));
						}
					}
				}
			}
		}

	}

	private void na2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
		log.info("NA2: Start");
		List<BuffApplied> naturalAlignmentBuffs = s.waitEventsQuickSuccession(2,
				BuffApplied.class, ba -> ba.buffIdMatches(3412), Duration.ofMillis(200));
		List<XivCombatant> naBuffs = naturalAlignmentBuffs.stream().map(BuffApplied::getTarget).toList();
		Map<String, Object> params = Map.of("naPlayers", naBuffs);
		final NaturalAlignmentRole roleStatus;
		if (naBuffs.stream().anyMatch(XivCombatant::isThePlayer)) {
			roleStatus = NaturalAlignmentRole.ON_YOU;
		}
		else {
			boolean sameRole = getState().getPlayerJob().isDps() == ((XivPlayerCharacter) naBuffs.get(0)).getJob().isDps();
			if (sameRole) {
				roleStatus = NaturalAlignmentRole.SAME_ROLE;
			}
			else {
				roleStatus = NaturalAlignmentRole.OTHER_ROLE;
			}
		}
		log.info("NA2: Role {}", roleStatus);
		ModifiableCallout<BuffApplied> call = switch (roleStatus) {
			case ON_YOU -> naturalAlignmentOnYou;
			case SAME_ROLE -> naturalAlignmentSameRole;
			case OTHER_ROLE -> naturalAlignmentOtherRole;
		};
		s.accept(call.getModified(naturalAlignmentBuffs.get(0), params));
		{
			// 1e3 = spread then stack
			// 1e1 = stack then spread
			BuffApplied headBuff = s.waitEvent(BuffApplied.class,
					ba -> ba.getBuff().getId() == 0x9F8
							&& (ba.getRawStacks() == 0x1e1
							|| ba.getRawStacks() == 0x1e3));
			boolean stackFirst = headBuff.getRawStacks() == 0x1e1;
			log.info("NA2: Head Buff 1: {}", headBuff);
			log.info("NA2: Stack First? {}", stackFirst);
			// Inverse
			if (getBuffs().statusesOnTarget(headBuff.getTarget()).stream().anyMatch(ba -> ba.buffIdMatches(0xD15))) {
				log.info("NA2: Inverting!");
				stackFirst = !stackFirst;
			}
			if (roleStatus != NaturalAlignmentRole.ON_YOU) {
				if (stackFirst) {
					s.updateCall(na2StackFirst.getModified());
				}
				else {
					s.updateCall(na2SpreadFirst.getModified());
				}
			}
			else {
				if (stackFirst) {
					s.updateCall(na2StackFirstWithNA.getModified());
				}
				else {
					s.updateCall(na2SpreadFirstWithNA.getModified());
				}
			}
			{
				List<AbilityCastStart> endOfDays = s.waitEvents(3, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7A8B));
				boolean frontSafe = endOfDays.stream().noneMatch(acs -> acs.getSource().getPos().y() < 90);
				log.info("NA2: Front Safe? {}", frontSafe);
				Map<String, Object> safeSpotParams = Map.of("frontSafe", frontSafe);
				if (roleStatus == NaturalAlignmentRole.ON_YOU) {
					s.updateCall(na2avoidPlayers.getModified(safeSpotParams));
				}
				else {
					s.updateCall((stackFirst ? na2stack : na2spread).getModified(safeSpotParams));
				}
			}
			{
				List<AbilityCastStart> endOfDays = s.waitEvents(3, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7A8B));
				boolean frontSafe = endOfDays.stream().noneMatch(acs -> acs.getSource().getPos().y() < 90);
				log.info("NA2: Front Safe? {}", frontSafe);
				Map<String, Object> safeSpotParams = Map.of("frontSafe", frontSafe);
				if (roleStatus == NaturalAlignmentRole.ON_YOU) {
					s.updateCall(na2avoidPlayers.getModified(safeSpotParams));
				}
				else {
					s.updateCall((stackFirst ? na2spread : na2stack).getModified(safeSpotParams));
				}
			}
		}

		{
			BuffApplied headBuff = s.waitEvent(BuffApplied.class,
					ba -> ba.getBuff().getId() == 0x9F8
							&& (ba.getRawStacks() == 0x1dd
							|| ba.getRawStacks() == 0x1df));
			boolean partnersFirst = headBuff.getRawStacks() == 0x1dd;
			log.info("NA2: Head Buff 2: {}", headBuff);
			log.info("NA2: Partners First {}", partnersFirst);
			// Inverse
			if (getBuffs().statusesOnTarget(headBuff.getTarget()).stream().anyMatch(ba -> ba.buffIdMatches(0xD15))) {
				log.info("NA2: Inverting!");
				partnersFirst = !partnersFirst;
			}
			log.info("NA2: Partners First {}", partnersFirst);
			{
				List<AbilityCastStart> endOfDays = s.waitEvents(3, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7A8B));
				boolean frontSafe = endOfDays.stream().noneMatch(acs -> acs.getSource().getPos().y() < 90);
				log.info("NA2: Front Safe? {}", frontSafe);
				Map<String, Object> safeSpotParams = Map.of("frontSafe", frontSafe);
				switch (roleStatus) {
					case ON_YOU -> {
						s.updateCall(iceFireNothing.getModified(safeSpotParams));
					}
					case SAME_ROLE -> {
						if (partnersFirst) {
							s.updateCall(firePairsSameRole.getModified(safeSpotParams));
						}
						else {
							s.updateCall(iceLightPartiesSameRole.getModified(safeSpotParams));
						}
					}
					case OTHER_ROLE -> {
						if (partnersFirst) {
							s.updateCall(firePairsOtherRole.getModified(safeSpotParams));
						}
						else {
							s.updateCall(iceLightPartiesOtherRole.getModified(safeSpotParams));
						}
					}
				}
			}
			{
				List<AbilityCastStart> endOfDays = s.waitEvents(3, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7A8B));
				boolean frontSafe = endOfDays.stream().noneMatch(acs -> acs.getSource().getPos().y() < 90);
				log.info("NA2: Front Safe? {}", frontSafe);
				Map<String, Object> safeSpotParams = Map.of("frontSafe", frontSafe);
				switch (roleStatus) {
					case ON_YOU -> {
						s.updateCall(iceFireNothing.getModified(safeSpotParams));
					}
					case SAME_ROLE -> {
						if (!partnersFirst) {
							s.updateCall(firePairsSameRole.getModified(safeSpotParams));
						}
						else {
							s.updateCall(iceLightPartiesSameRole.getModified(safeSpotParams));
						}
					}
					case OTHER_ROLE -> {
						if (!partnersFirst) {
							s.updateCall(firePairsOtherRole.getModified(safeSpotParams));
						}
						else {
							s.updateCall(iceLightPartiesOtherRole.getModified(safeSpotParams));
						}
					}
				}
			}
			{
				AbilityCastStart cleave = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x79D7, 0x79D8));
				ArenaSector safe = cleave.abilityIdMatches(0x79D7) ? ArenaSector.EAST : ArenaSector.WEST;
				log.info("NA1: Cleave Safe Spot {}", safe);
				Map<String, Object> cleaveParams = Map.of("safe", safe);
				s.updateCall(na2cleave.getModified(cleaveParams));

			}
		}

	}


	@AutoFeed
	private final SequentialTrigger<BaseEvent> highConcept1 = SqtTemplates.multiInvocation(60_000,
			AbilityCastStart.class, acs -> acs.abilityIdMatches(31148),
			this::hc1,
			this::hc2);

	private final ModifiableCallout<AbilityCastStart> hc1start = ModifiableCallout.durationBasedCall("HC1: Start", "Heavy Raidwide");
	private final ModifiableCallout<BuffApplied> hc1shortAlpha = new ModifiableCallout<BuffApplied>("HC1: Short Alpha", "Alpha Defamation").autoIcon();
	private final ModifiableCallout<BuffApplied> hc1shortBeta = new ModifiableCallout<BuffApplied>("HC1: Short Beta", "Beta Defamation").autoIcon();
	private final ModifiableCallout<BuffApplied> hc1shortGamma = new ModifiableCallout<BuffApplied>("HC1: Short Gamma", "Gamma Defamation").autoIcon();
	private final ModifiableCallout<BuffApplied> hc1longAlpha = new ModifiableCallout<BuffApplied>("HC1: Long Alpha", "Long Alpha - Avoid Defamation").autoIcon();
	private final ModifiableCallout<BuffApplied> hc1longBeta = new ModifiableCallout<BuffApplied>("HC1: Long Beta", "Long Beta - Avoid Defamation").autoIcon();
	private final ModifiableCallout<BuffApplied> hc1longGamma = new ModifiableCallout<BuffApplied>("HC1: Long Gamma", "Long Gamma - Avoid Defamation").autoIcon();
	private final ModifiableCallout<BuffApplied> hc1multiSplice = new ModifiableCallout<BuffApplied>("HC1: Multisplice", "2-Stack").autoIcon();
	private final ModifiableCallout<BuffApplied> hc1superSplice = new ModifiableCallout<BuffApplied>("HC1: Supersplice", "3-Stack").autoIcon();
	private final ModifiableCallout<?> hc1doAlchemy = new ModifiableCallout<>("HC1: Do Alchemy", "Alch if {color1} or {color2} tower");
	private final ModifiableCallout<?> hc1dontDoAlchemy = new ModifiableCallout<>("HC1: Don't Do Alchemy", "Avoid Alchemy");
	private final ModifiableCallout<?> hc1doTower = new ModifiableCallout<>("HC1: Do Tower", "{safe} safe, Soak Tower if {color1} or {color2}");
	private final ModifiableCallout<?> hc1dontDoTower = new ModifiableCallout<>("HC1: Don't Do Tower", "{safe} safe, Avoid Tower");
	private final ModifiableCallout<?> hc1shortAlphaFup = new ModifiableCallout<>("HC1: Short Alpha Followup", "Avoid Defamations");
	private final ModifiableCallout<?> hc1shortBetaFup = new ModifiableCallout<>("HC1: Short Beta Followup", "Avoid Defamations");
	private final ModifiableCallout<?> hc1shortGammaFup = new ModifiableCallout<>("HC1: Short Gamma Followup", "Avoid Defamations");
	private final ModifiableCallout<?> hc1shortAlphaFupNoAlch = new ModifiableCallout<>("HC1: Short Alpha, Not First", "Out or {emptyDefa}");
	private final ModifiableCallout<?> hc1shortBetaFupNoAlch = new ModifiableCallout<>("HC1: Short Beta, Not First", "Out or {emptyDefa}");
	private final ModifiableCallout<?> hc1shortGammaFupNoAlch = new ModifiableCallout<>("HC1: Short Gamma Not First", "Out or {emptyDefa}");
	private final ModifiableCallout<?> hc1longAlphaFup = new ModifiableCallout<>("HC1: Long Alpha Followup", "Alpha Defamation");
	private final ModifiableCallout<?> hc1longBetaFup = new ModifiableCallout<>("HC1: Long Beta Followup", "Beta Defamation");
	private final ModifiableCallout<?> hc1longGammaFup = new ModifiableCallout<>("HC1: Long Gamma Followup", "Gamma Defamation");
	private final ModifiableCallout<?> hc1multiSpliceFup = new ModifiableCallout<>("HC1: Multisplice Followup", "Avoid {emptyDefa}");
	private final ModifiableCallout<?> hc1superSpliceFup = new ModifiableCallout<>("HC1: Supersplice Followup", "Avoid {emptyDefa}");
	private final ModifiableCallout<?> hc1doSecondAlch = new ModifiableCallout<>("HC1: Do Second Alchemy", "Alch if {color1} or {color2} tower");
	private final ModifiableCallout<?> hc1dontDoSecondAlch = new ModifiableCallout<>("HC1: Don't Do Second Alchemy", "Avoid Alchemy");
	private final ModifiableCallout<?> hc1doSecondTower = new ModifiableCallout<>("HC1: Do Tower", "{safe} safe, Soak Tower if {color1} or {color2}");
	private final ModifiableCallout<?> hc1dontDoSecondTower = new ModifiableCallout<>("HC1: Don't Do Tower", "{safe} safe, Avoid Tower");

	private void hc1(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
		log.info("HC1: Begin");
		s.updateCall(hc1start.getModified(e1));
		BuffApplied playerBuff = s.waitEvent(BuffApplied.class, ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(impAlpha, impBeta, impGamma, supersplice, multisplice));
		log.info("HC1: Player Buff: {}", playerBuff.getBuff().getId());
		long seconds = playerBuff.getInitialDuration().toSeconds();
		ModifiableCallout<BuffApplied> initialCall;
		ModifiableCallout<?> followupCall;
		boolean maybeDoingTower = false;
		@Nullable TowerColor neededTowerColor1 = null;
		@Nullable TowerColor neededTowerColor2 = null;
		// Set up first couple callouts since they're static based on debuff
		switch ((int) playerBuff.getBuff().getId()) {
			case impAlpha -> {
				if (seconds < 15) {
					initialCall = hc1shortAlpha;
					followupCall = hc1shortAlphaFup;
					maybeDoingTower = true;
					neededTowerColor1 = TowerColor.Blue;
					neededTowerColor2 = TowerColor.Green;
				}
				else {
					initialCall = hc1longAlpha;
					followupCall = hc1longAlphaFup;
				}
			}
			case impBeta -> {
				if (seconds < 15) {
					initialCall = hc1shortBeta;
					followupCall = hc1shortBetaFup;
					maybeDoingTower = true;
					neededTowerColor1 = TowerColor.Purple;
					neededTowerColor2 = TowerColor.Green;
				}
				else {
					initialCall = hc1longBeta;
					followupCall = hc1longBetaFup;
				}
			}
			case impGamma -> {
				if (seconds < 15) {
					initialCall = hc1shortGamma;
					followupCall = hc1shortGammaFup;
					maybeDoingTower = true;
					neededTowerColor1 = TowerColor.Purple;
					neededTowerColor2 = TowerColor.Blue;
				}
				else {
					initialCall = hc1longGamma;
					followupCall = hc1longGammaFup;
				}
			}
			case multisplice -> {
				initialCall = hc1multiSplice;
				followupCall = hc1multiSpliceFup;
			}
			case supersplice -> {
				initialCall = hc1superSplice;
				followupCall = hc1superSpliceFup;
			}
			default -> {
				log.warn("HC1: Unknown debuff {}", playerBuff);
				return;
			}
		}
		log.info("HC1: First Calc: {} {} {} {} {} {} {}", playerBuff.getBuff().getId(), seconds, initialCall, followupCall, maybeDoingTower, neededTowerColor1, neededTowerColor2);
		s.updateCall(initialCall.getModified(playerBuff));
		// This debuff means first defamations have gone off
		s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(inconceivable));
		log.info("HC1: Checkpoint 1");
		if (maybeDoingTower) {
			s.updateCall(hc1doAlchemy.getModified(Map.of("color1", neededTowerColor1, "color2", neededTowerColor2)));
		}
		else {
			s.updateCall(hc1dontDoAlchemy.getModified());
		}
		{
			// Cleave and towers
			AbilityCastStart cleave = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x79D7, 0x79D8));
			ArenaSector safe = cleave.abilityIdMatches(0x79D7) ? ArenaSector.EAST : ArenaSector.WEST;
			log.info("HC1: Cleave Safe Spot {}", safe);
			log.info("HC1: Checkpoint 2");
			if (maybeDoingTower) {
				s.updateCall(hc1doTower.getModified(Map.of("safe", safe, "color1", neededTowerColor1, "color2", neededTowerColor2)));
			}
			else {
				s.updateCall(hc1dontDoTower.getModified(Map.of("safe", safe)));
			}
		}
		s.waitMs(5_000);
		log.info("HC1: Checkpoint 3");
		String emptyDefa;
		List<BuffApplied> perfectBuffs = getBuffs().getBuffs().stream()
				.filter(ba -> ba.buffIdMatches(perfAlpha, perfBeta, perfGamma))
				.toList();

		if (perfectBuffs.size() != 1) {
			emptyDefa = "Error";
		}
		else {
			// These are for the previously-unused defamation
			switch ((int) perfectBuffs.get(0).getBuff().getId()) {
				case perfAlpha -> {
					emptyDefa = "Alpha";
					if (followupCall == hc1shortAlphaFup) {
						followupCall = hc1shortAlphaFupNoAlch;
					}
				}
				case perfBeta -> {
					emptyDefa = "Beta";
					if (followupCall == hc1shortBetaFup) {
						followupCall = hc1shortBetaFupNoAlch;
					}
				}
				case perfGamma -> {
					emptyDefa = "Gamma";
					if (followupCall == hc1shortGammaFup) {
						followupCall = hc1shortGammaFupNoAlch;
					}
				}
				default -> emptyDefa = "Error";
			}
		}
		log.info("HC1: Avoid {}", emptyDefa);
		s.updateCall(followupCall.getModified(Map.of("emptyDefa", emptyDefa)));
		s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(inconceivable));
		log.info("HC1: Checkpoint 4");

		s.waitMs(500);
		BuffApplied secondAlchBuff = getBuffs().statusesOnTarget(getState().getPlayer())
				.stream().filter(ba -> ba.buffIdMatches(perfAlpha, perfBeta, perfGamma))
				.findAny().orElse(null);
		boolean secondAlchMaybe = false;
		// Second towers and cleaves
		if (secondAlchBuff != null) {

			switch ((int) secondAlchBuff.getBuff().getId()) {
				case perfAlpha -> {
					neededTowerColor1 = TowerColor.Blue;
					neededTowerColor2 = TowerColor.Green;
					secondAlchMaybe = true;
				}
				case perfBeta -> {
					neededTowerColor1 = TowerColor.Purple;
					neededTowerColor2 = TowerColor.Green;
					secondAlchMaybe = true;
				}
				case perfGamma -> {
					neededTowerColor1 = TowerColor.Blue;
					neededTowerColor2 = TowerColor.Purple;
					secondAlchMaybe = true;
				}
			}
		}
		log.info("HC1: Second alch buff: {} {} {} {}", secondAlchBuff, secondAlchMaybe, neededTowerColor1, neededTowerColor2);
		if (secondAlchMaybe) {
			s.updateCall(hc1doSecondAlch.getModified(Map.of("color1", neededTowerColor1, "color2", neededTowerColor2)));
		}
		else {
			s.updateCall(hc1dontDoSecondAlch.getModified());
		}
		log.info("HC1: Checkpoint 5");
		{
			AbilityCastStart cleave2 = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x79D7, 0x79D8));
			ArenaSector safe2 = cleave2.abilityIdMatches(0x79D7) ? ArenaSector.EAST : ArenaSector.WEST;
			log.info("NA1: Cleave Safe Spot {}", safe2);
			log.info("HC1: Checkpoint 6");
			if (secondAlchMaybe) {
				s.updateCall(hc1doSecondTower.getModified(Map.of("safe", safe2, "color1", neededTowerColor1, "color2", neededTowerColor2)));
			}
			else {
				s.updateCall(hc1dontDoSecondTower.getModified(Map.of("safe", safe2)));
			}
		}
	}

	private final ModifiableCallout<AbilityCastStart> hc2start = ModifiableCallout.durationBasedCall("HC2: Start", "Heavy Raidwide");
	private final ModifiableCallout<?> hc2nothingInitial = new ModifiableCallout<>("HC2: Nothing Initial", "Nothing");
	private final ModifiableCallout<?> hc2nothingSecond = new ModifiableCallout<>("HC2: Nothing Post-Defa", "");
	private final ModifiableCallout<?> hc2ShortAlphaInitial = new ModifiableCallout<>("HC2: Short Alpha Initial", "Short Alpha").statusIcon(3330);
	private final ModifiableCallout<?> hc2ShortAlphaSecond = new ModifiableCallout<>("HC2: Short Alpha Post-Defa", "Alch if Blue Tower").statusIcon(3333);
	private final ModifiableCallout<?> hc2ShortBetaInitial = new ModifiableCallout<>("HC2: Short Beta Initial", "Short Beta").statusIcon(3331);
	private final ModifiableCallout<?> hc2ShortBetaSecond = new ModifiableCallout<>("HC2: Short Beta Post-Defa", "Alch if Purple Tower").statusIcon(3334);
	private final ModifiableCallout<?> hc2ShortGammaInitial = new ModifiableCallout<>("HC2: Short Gamma Initial", "Short Gamma").statusIcon(3332);
	private final ModifiableCallout<?> hc2ShortGammaSecond = new ModifiableCallout<>("HC2: Short Gamma Post-Defa", "Alch if Blue or Purple Tower").statusIcon(3335);
	private final ModifiableCallout<?> hc2LongAlphaInitial = new ModifiableCallout<>("HC2: Long Alpha Initial (No Splice)", "Long Alpha").statusIcon(3330);
	private final ModifiableCallout<?> hc2LongAlphaSecond = new ModifiableCallout<>("HC2: Long Alpha Post-Defa", "Preposition to Alpha").statusIcon(3330);
	private final ModifiableCallout<?> hc2LongBetaInitial = new ModifiableCallout<>("HC2: Long Beta Initial (No Splice)", "Long Beta").statusIcon(3331);
	private final ModifiableCallout<?> hc2LongBetaSecond = new ModifiableCallout<>("HC2: Long Beta Post-Defa", "Preposition to Beta").statusIcon(3331);
	private final ModifiableCallout<?> hc2LongGammaInitial = new ModifiableCallout<>("HC2: Long Gamma Initial (No Splice)", "Long Gamma").statusIcon(3332);
	private final ModifiableCallout<?> hc2LongGammaSecond = new ModifiableCallout<>("HC2: Long Gamma Post-Defa", "Preposition to Gamma").statusIcon(3332);
	private final ModifiableCallout<?> hc2soloInitial = new ModifiableCallout<>("HC2: Solo Splice Initial", "Solo Splice").statusIcon(3345);
	private final ModifiableCallout<?> hc2multiInitial = new ModifiableCallout<>("HC2: Multi Splice Initial", "Multi Splice").statusIcon(3346);
	private final ModifiableCallout<AbilityCastStart> hc2cleave = ModifiableCallout.durationBasedCall("HC2: Cleave", "{safe} safe");

	// Post towers
	private final ModifiableCallout<?> hc2LongAlphaDefa = new ModifiableCallout<>("HC2: Second Alpha Defa", "Alpha").statusIcon(3330);
	private final ModifiableCallout<?> hc2LongBetaDefa = new ModifiableCallout<>("HC2: Second Beta Defa", "Beta").statusIcon(3331);
	private final ModifiableCallout<?> hc2LongGammaDefa = new ModifiableCallout<>("HC2: Second Gamma Defa", "Gamma").statusIcon(3332);
	private final ModifiableCallout<?> hc2withAlphaDefa = new ModifiableCallout<>("HC2: Stack with Second Alpha", "Stack with Alpha");
	private final ModifiableCallout<?> hc2withBetaDefa = new ModifiableCallout<>("HC2: Stack with Second Beta", "Stack with Beta");;
	private final ModifiableCallout<?> hc2withGammaDefa = new ModifiableCallout<>("HC2: Stack with Second Gamma", "Stack with Gamma");;
	private final ModifiableCallout<?> hc2avoidSecondDefa = new ModifiableCallout<>("HC2: Avoid Second Defa", "Avoid Defa");
	private final ModifiableCallout<?> hc2finalBaitAsNothing = new ModifiableCallout<>("HC2: Bait Charge (Original Nothing)", "Bait Charge");
	private final ModifiableCallout<?> hc2finalBaitFirstAlch = new ModifiableCallout<>("HC2: Bait Charge (Original Alch)", "Bait Charge");
	private final ModifiableCallout<?> hc2finalAlchemy = new ModifiableCallout<>("HC2: Do Second Alchemy", "Do Alchemy and Towers");
	private final ModifiableCallout<?> hc2finalAlchemyUnused = new ModifiableCallout<>("HC2: Do Second Alchemy (Did Not Alch 1st)", "Do Alchemy and Towers");
	private final ModifiableCallout<BuffApplied> hc2prey = ModifiableCallout.<BuffApplied>durationBasedCall("HC2: Prey", "Move").autoIcon();
	private final ModifiableCallout<?> hc2heal = new ModifiableCallout<>("HC2: Heal", "Heal to Full");
	private final ModifiableCallout<?> hc2makePhoenix = new ModifiableCallout<>("HC2: Make Phoenix", "Make Phoenix Then Heal To Full").abilityIcon(3343);

	private void hc2(AbilityCastStart e1, SequentialTriggerController<BaseEvent> s) {
		log.info("HC2: Begin");
		s.updateCall(hc2start.getModified(e1));
		List<BuffApplied> allBuffs = s.waitEventsQuickSuccession(8, BuffApplied.class, ba -> ba.buffIdMatches(impAlpha, impBeta, impGamma, solosplice, multisplice), Duration.ofMillis(200));
		@Nullable BuffApplied letterBuff = allBuffs.stream().filter(ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(impAlpha, impBeta, impGamma)).findAny().orElse(null);
		@Nullable BuffApplied spliceBuff = allBuffs.stream().filter(ba -> ba.getTarget().isThePlayer() && ba.buffIdMatches(solosplice, multisplice)).findAny().orElse(null);
		log.info("HC2: Player Letter: {}; Splice: {}", letterBuff, spliceBuff);
		// initialCall is the initial positions
		ModifiableCallout<?> initialCall;
		// secondCall is the command immediately after the first defamations go off
		ModifiableCallout<?> secondCall;
		if (letterBuff == null) {
			initialCall = hc2nothingInitial;
			secondCall = hc2nothingSecond;
		}
		else {
			boolean longLetter = letterBuff.getInitialDuration().toSeconds() > 10;
			if (longLetter) {
				boolean hasSolo = spliceBuff != null && spliceBuff.buffIdMatches(solosplice);
				boolean hasMulti = spliceBuff != null && spliceBuff.buffIdMatches(multisplice);
				if (letterBuff.buffIdMatches(impAlpha)) {
					initialCall = hc2LongAlphaInitial;
					secondCall = hc2LongAlphaSecond;
				}
				else if (letterBuff.buffIdMatches(impBeta)) {
					initialCall = hc2LongBetaInitial;
					secondCall = hc2LongBetaSecond;
				}
				else if (letterBuff.buffIdMatches(impGamma)) {
					initialCall = hc2LongGammaInitial;
					secondCall = hc2LongGammaSecond;
				}
				else {
					log.error("HC2: Error!");
					return;
				}
				if (hasSolo) {
					initialCall = hc2soloInitial;
				}
				else if (hasMulti) {
					initialCall = hc2multiInitial;
				}
			}
			else {
				if (letterBuff.buffIdMatches(impAlpha)) {
					initialCall = hc2ShortAlphaInitial;
					secondCall = hc2ShortAlphaSecond;
				}
				else if (letterBuff.buffIdMatches(impBeta)) {
					initialCall = hc2ShortBetaInitial;
					secondCall = hc2ShortBetaSecond;
				}
				else if (letterBuff.buffIdMatches(impGamma)) {
					initialCall = hc2ShortGammaInitial;
					secondCall = hc2ShortGammaSecond;
				}
				else {
					log.error("HC2: Error!");
					return;
				}
			}
		}
		s.updateCall(initialCall.getModified());
		s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(inconceivable));
		s.updateCall(secondCall.getModified());
		log.info("HC2: Checkpoint 1");
		{
			AbilityCastStart cleave = s.waitEvent(AbilityCastStart.class, acs -> acs.abilityIdMatches(0x79D7, 0x79D8));
			ArenaSector safe = cleave.abilityIdMatches(0x79D7) ? ArenaSector.EAST : ArenaSector.WEST;
			log.info("HC2: Checkpoint 2");
			s.updateCall(hc2cleave.getModified(cleave, Map.of("safe", safe)));
		}
		s.waitEvent(AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x79D7, 0x79D8));
		s.waitMs(200);
		List<BuffApplied> playerBuffs = getBuffs().statusesOnTarget(getState().getPlayer());
		boolean playerHasAnimalAfterFirstSet = playerBuffs.stream().anyMatch(ba -> ba.buffIdMatches(3337, 3338, 3339, 3340, 3341, 3342));
		boolean playerUnused = playerBuffs.stream().anyMatch(ba -> ba.buffIdMatches(3333, 3334, 3335));
		log.info("HC2: player has animal after first set? {}", playerHasAnimalAfterFirstSet);
		// This set of calls is for the second defamation positions
		ModifiableCallout<?> secondDefaCall;
		if (playerHasAnimalAfterFirstSet) {
			secondDefaCall = (hc2avoidSecondDefa);
		}
		else {
			if (letterBuff != null) {
				if (letterBuff.buffIdMatches(impAlpha)) {
					secondDefaCall = hc2LongAlphaDefa;
				}
				else if (letterBuff.buffIdMatches(impBeta)) {
					secondDefaCall = hc2LongBetaDefa;
				}
				else if (letterBuff.buffIdMatches(impGamma)) {
					secondDefaCall = hc2LongGammaDefa;
				}
				else {
					log.error("HC2 error!");
					return;
				}
			}
			else {
				if (playerBuffs.stream().anyMatch(ba -> ba.buffIdMatches(impAlpha))) {
					secondDefaCall = hc2withAlphaDefa;
				}
				else if (playerBuffs.stream().anyMatch(ba -> ba.buffIdMatches(impBeta))) {
					secondDefaCall = hc2withBetaDefa;
				}
				else if (playerBuffs.stream().anyMatch(ba -> ba.buffIdMatches(impGamma))) {
					secondDefaCall = hc2withGammaDefa;
				}
				else {
					// This is a tricky case. The players making Ifrit (or whatever) don't *technically* need to do it
					// before this point, so assume they're going to.
					secondDefaCall = hc2avoidSecondDefa;
				}
			}
		}
		s.updateCall(secondDefaCall.getModified());
		s.waitMs(1000);
		s.waitEvent(BuffApplied.class, ba -> ba.buffIdMatches(inconceivable));
		// Final calls
		log.info("HC2: Checkpoint 3");
		// Ifrit
//		if (getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(3340, 3341, 3342))) {
		if (playerHasAnimalAfterFirstSet) {
			// TODO: prey marker
			if (secondCall == hc2nothingSecond) {
				s.updateCall(hc2finalBaitAsNothing.getModified());
			}
			else {
				s.updateCall(hc2finalBaitFirstAlch.getModified());
			}
		}
		else {
			// TODO: test this
			if (playerUnused) {
				s.updateCall(hc2finalAlchemyUnused.getModified());
			}
			else {
				s.updateCall(hc2finalAlchemy.getModified());
			}
		}
		// TODO: do something smarter than a wait here
		s.waitMs(10_000);
		if (getBuffs().statusesOnTarget(getState().getPlayer()).stream().anyMatch(ba -> ba.buffIdMatches(3340, 3337))) {
			s.updateCall(hc2makePhoenix.getModified());
		}
		else {
			s.updateCall(hc2heal.getModified());
		}
	}

	@HandleEvents
	public void wipeReset(EventContext context, DutyCommenceEvent dce) {
		limitlessDesoNumber = 0;
	}

	private final ModifiableCallout<AbilityCastStart> limitlessDesolationStart = ModifiableCallout.durationBasedCall("Limitless Desolation: Start", "Spread");
	private final ModifiableCallout<AbilityUsedEvent> limitlessDesoNumberCall = new ModifiableCallout<>("Limitless Desolation: Number", "Number {number}");
	//	private final ModifiableCallout<AbilityCastStart> limitlessDesoBait = ModifiableCallout.durationBasedCall("Limitless Desolation: Bait", "Bait");
	private final ModifiableCallout<?> limitlessDesoSoak = new ModifiableCallout<>("Limitless Desolation: Soak", "Soak");
	private int limitlessDesoNumber;
	@AutoFeed
	private final SequentialTrigger<BaseEvent> limitlessDesolationSq = SqtTemplates.sq(60_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x75ED),
			(e1, s) -> {
				log.info("LD SQ 1: Start");
				s.updateCall(limitlessDesolationStart.getModified(e1));
				for (int i = 1; i <= 4; i++) {
					log.info("LD SQ 1: i == {}, before", i);
					List<AbilityUsedEvent> events = s.waitEventsQuickSuccession(2, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(0x75F0) && aue.isFirstTarget(), Duration.ofMillis(200));
					log.info("LD SQ 1: i == {}, after", i);
					Optional<AbilityUsedEvent> onPlayer = events.stream().filter(aue -> aue.getTarget().isThePlayer()).findAny();
					if (onPlayer.isPresent()) {
						limitlessDesoNumber = i;
						s.updateCall(limitlessDesoNumberCall.getModified(onPlayer.get(), Map.of("number", i)));
						log.info("LD SQ 1: Done, number {}", i);
						return;
					}
				}
			});

	@AutoFeed
	private final SequentialTrigger<BaseEvent> limitlessDesolationBaitsSq = SqtTemplates.sq(60_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(0x75ED),
			(e1, s) -> {
				log.info("LD SQ 2: Start");
				for (int i = 1; i <= 4; i++) {
					log.info("LD SQ 2: i == {}, before", i);
					List<AbilityCastStart> events = s.waitEventsQuickSuccession(2, AbilityCastStart.class, acs -> acs.abilityIdMatches(0x7A88), Duration.ofMillis(200));
					log.info("LD SQ 2: i == {}, after", i);
					if (i == limitlessDesoNumber) {
						AbilityCastStart sampleEvent = events.get(0);
						s.updateCall(limitlessDesoSoak.getModified());
						log.info("LD SQ 2: waiting");
						AbilityUsedEvent followup = s.waitEvent(AbilityUsedEvent.class, aue -> aue.getSource().equals(sampleEvent.getSource()));
						// TODO: see about getting this working
//						s.updateCall(limitlessDesoBait.getModified(sampleEvent));
						log.info("LD SQ 2: done");
						return;
					}
				}
			});

	private final ModifiableCallout<AbilityCastStart> dominionInitial = ModifiableCallout.durationBasedCall("Dominion: Initial", "Spread for Dominion");
	private final ModifiableCallout<?> dominionFirstSet = new ModifiableCallout<>("Dominion: First Towers", "First Set");
	private final ModifiableCallout<?> dominionSecondSet = new ModifiableCallout<>("Dominion: First Towers", "Second Set");
	@AutoFeed
	private final SequentialTrigger<BaseEvent> dominionSq = SqtTemplates.sq(30_000, AbilityCastStart.class,
			acs -> acs.abilityIdMatches(31193),
			(e1, s) -> {
				s.updateCall(dominionInitial.getModified(e1));
				List<AbilityUsedEvent> hits = s.waitEventsQuickSuccession(4, AbilityUsedEvent.class, aue -> aue.abilityIdMatches(31195) && aue.isFirstTarget(), Duration.ofMillis(100));
				if (hits.stream().anyMatch(aue -> aue.getTarget().isThePlayer())) {
					s.updateCall(dominionSecondSet.getModified());
				}
				else {
					s.updateCall(dominionFirstSet.getModified());
				}
			});

	/*
	https://discord.com/channels/551474815727304704/968569969103216651/1015795892688797726

	_rsv_28938_-1_1_0_0_SE2DC5B04_EE2DC5B04|High Concept
	_rsv_30996_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Octaflare
	_rsv_30997_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Tetraflare
	_rsv_30998_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Tetraflare
	_rsv_30999_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Diflare
	_rsv_31000_-1_1_0_0_SE2DC5B04_EE2DC5B04|Emergent Octaflare
	_rsv_31001_-1_1_0_0_SE2DC5B04_EE2DC5B04|Emergent Tetraflare
	_rsv_31003_-1_1_0_0_SE2DC5B04_EE2DC5B04|Emergent Diflare
	_rsv_31005_-1_1_0_0_SE2DC5B04_EE2DC5B04|Octaflare
	_rsv_31006_-1_1_0_0_SE2DC5B04_EE2DC5B04|Tetraflare
	_rsv_31007_-1_1_0_0_SE2DC5B04_EE2DC5B04|Nest of Flamevipers
	_rsv_31008_-1_1_0_0_SE2DC5B04_EE2DC5B04|Nest of Flamevipers
	_rsv_31009_-1_1_0_0_SE2DC5B04_EE2DC5B04|Manifold Flames
	_rsv_31010_-1_1_0_0_SE2DC5B04_EE2DC5B04|Manifold Flames
	_rsv_31021_-1_1_0_0_SE2DC5B04_EE2DC5B04|Eye of the Gorgon
	_rsv_31022_-1_1_0_0_SE2DC5B04_EE2DC5B04|Crown of the Gorgon
	_rsv_31023_-1_1_0_0_SE2DC5B04_EE2DC5B04|Blood of the Gorgon
	_rsv_31024_-1_1_0_0_SE2DC5B04_EE2DC5B04|Breath of the Gorgon
	_rsv_31030_-1_1_0_0_SE2DC5B04_EE2DC5B04|Stomp Dead
	_rsv_31031_-1_1_0_0_SE2DC5B04_EE2DC5B04|Stomp Dead
	_rsv_31032_-1_1_0_0_SE2DC5B04_EE2DC5B04|Blazing Footfalls
	_rsv_31033_-1_1_0_0_SE2DC5B04_EE2DC5B04|Trailblaze
	_rsv_31038_-1_1_0_0_SE2DC5B04_EE2DC5B04|Trailblaze
	_rsv_31040_-1_1_0_0_SE2DC5B04_EE2DC5B04|Rain of Fire
	_rsv_31043_-1_1_0_0_SE2DC5B04_EE2DC5B04|Rain of Fire
	_rsv_31044_-1_1_0_0_SE2DC5B04_EE2DC5B04|Genesis of Flame
	_rsv_31050_-1_1_0_0_SE2DC5B04_EE2DC5B04|Genesis of Flame
	_rsv_31148_-1_1_0_0_SE2DC5B04_EE2DC5B04|High Concept
	_rsv_31149_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Shift
	_rsv_31150_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Shift
	_rsv_31151_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conceptual Shift
	_rsv_31152_-1_1_0_0_SE2DC5B04_EE2DC5B04|Conception
	_rsv_31153_-1_1_0_0_SE2DC5B04_EE2DC5B04|Failure of Imagination
	_rsv_31154_-1_1_0_0_SE2DC5B04_EE2DC5B04|Splicer
	_rsv_31155_-1_1_0_0_SE2DC5B04_EE2DC5B04|Splicer
	_rsv_31156_-1_1_0_0_SE2DC5B04_EE2DC5B04|Splicer
	_rsv_31157_-1_1_0_0_SE2DC5B04_EE2DC5B04|Everburn
	_rsv_31158_-1_1_0_0_SE2DC5B04_EE2DC5B04|Arcane Control
	_rsv_31159_-1_1_0_0_SE2DC5B04_EE2DC5B04|Arcane Channel
	_rsv_31160_-1_1_0_0_SE2DC5B04_EE2DC5B04|Arcane Wave
	_rsv_31162_-1_1_0_0_SE2DC5B04_EE2DC5B04|Ego Death
	_rsv_31163_-1_1_0_0_SE2DC5B04_EE2DC5B04|Natural Alignment
	_rsv_31164_-1_1_0_0_SE2DC5B04_EE2DC5B04|Twist Nature
	_rsv_31165_-1_1_0_0_SE2DC5B04_EE2DC5B04|Forcible Trifire
	_rsv_31166_-1_1_0_0_SE2DC5B04_EE2DC5B04|Forcible Difreeze
	_rsv_31167_-1_1_0_0_SE2DC5B04_EE2DC5B04|Forcible Fire III
	_rsv_31168_-1_1_0_0_SE2DC5B04_EE2DC5B04|Forcible Fire II
	_rsv_31169_-1_1_0_0_SE2DC5B04_EE2DC5B04|Forcible Fire IV
	_rsv_31170_-1_1_0_0_SE2DC5B04_EE2DC5B04|Inverse Magicks
	_rsv_31171_-1_1_0_0_SE2DC5B04_EE2DC5B04|Fates Unending
	_rsv_31172_-1_1_0_0_SE2DC5B04_EE2DC5B04|Unsightly Chaos
	_rsv_31173_-1_1_0_0_SE2DC5B04_EE2DC5B04|Unsightly Chaos
	_rsv_31176_-1_1_0_0_SE2DC5B04_EE2DC5B04|Illusory Soul
	_rsv_31177_-1_1_0_0_SE2DC5B04_EE2DC5B04|Soul Strand
	_rsv_31178_-1_1_0_0_SE2DC5B04_EE2DC5B04|Soul Strand
	_rsv_31179_-1_1_0_0_SE2DC5B04_EE2DC5B04|Soul Strand
	_rsv_31180_-1_1_0_0_SE2DC5B04_EE2DC5B04|End of the End
	_rsv_31181_-1_1_0_0_SE2DC5B04_EE2DC5B04|Dirge of Second Death
	_rsv_31182_-1_1_0_0_SE2DC5B04_EE2DC5B04|Limitless Kindling
	_rsv_31184_-1_1_0_0_SE2DC5B04_EE2DC5B04|Somatic Dirge of Second Death
	_rsv_31185_-1_1_0_0_SE2DC5B04_EE2DC5B04|Somatic Tyrant's Fire III
	_rsv_31186_-1_1_0_0_SE2DC5B04_EE2DC5B04|Somatic Dirge of Second Death
	_rsv_31187_-1_1_0_0_SE2DC5B04_EE2DC5B04|Somatic Tyrant's Fire III
	_rsv_31188_-1_1_0_0_SE2DC5B04_EE2DC5B04|Dead Outside
	_rsv_31193_-1_1_0_0_SE2DC5B04_EE2DC5B04|Dominion
	_rsv_31194_-1_1_0_0_SE2DC5B04_EE2DC5B04|Orogenic Annihilation
	_rsv_31195_-1_1_0_0_SE2DC5B04_EE2DC5B04|Orogenic Deformation
	_rsv_31196_-1_1_0_0_SE2DC5B04_EE2DC5B04|Orogenic Shift
	_rsv_31197_-1_1_0_0_SE2DC5B04_EE2DC5B04|Tyrant's Unholy Darkness
	_rsv_31198_-1_1_0_0_SE2DC5B04_EE2DC5B04|Tyrant's Unholy Darkness

	_rsv_31199_-1_1_0_0_SE2DC5B04_EE2DC5B04|Aioniopyr
	_rsv_31204_-1_1_0_0_SE2DC5B04_EE2DC5B04|Ego Death
	_rsv_31205_-1_1_0_0_SE2DC5B04_EE2DC5B04|Somatic End of the End
	_rsv_31210_-1_1_0_0_SE2DC5B04_EE2DC5B04|Ektothermos
	_rsv_31266_-1_1_0_0_SE2DC5B04_EE2DC5B04|Aionagonia
	_rsv_31366_-1_1_0_0_SE2DC5B04_EE2DC5B04|Ego Death
	_rsv_31367_-1_1_0_0_SE2DC5B04_EE2DC5B04|Ego Death
	_rsv_28938_-1_1_0_1_SE2DC5B04_EE2DC5B04|High Concept
	_rsv_30996_-1_1_0_1_SE2DC5B04_EE2DC5B04|Conceptual Octaflare
	_rsv_30997_-1_1_0_1_SE2DC5B04_EE2DC5B04|Conceptual Tetraflare
	_rsv_30998_-1_1_0_1_SE2DC5B04_EE2DC5B04|Conceptual Tetraflare
	_rsv_30999_-1_1_0_1_SE2DC5B04_EE2DC5B04|Conceptual Diflare
	_rsv_31000_-1_1_0_1_SE2DC5B04_EE2DC5B04|Emergent Octaflare
	_rsv_31001_-1_1_0_1_SE2DC5B04_EE2DC5B04|Emergent Tetraflare
	_rsv_31003_-1_1_0_1_SE2DC5B04_EE2DC5B04|Emergent Diflare
	_rsv_31005_-1_1_0_1_SE2DC5B04_EE2DC5B04|Octaflare
	_rsv_31006_-1_1_0_1_SE2DC5B04_EE2DC5B04|Tetraflare
	_rsv_31007_-1_1_0_1_SE2DC5B04_EE2DC5B04|Nest of Flamevipers
	_rsv_31008_-1_1_0_1_SE2DC5B04_EE2DC5B04|…earby influences when this effect expires. // this one clearly failed

	_rsv_3347_-1_1_0_1_S74CFC3B0_E74CFC3B0|Supersplice
	_rsv_3347_-1_1_1_1_S74CFC3B0_E74CFC3B0|Self-concept is being warped beyond recognition, resulting in an adverse reaction determined by nearby influences when this effect expires.
	_rsv_3349_-1_1_0_1_S74CFC3B0_E74CFC3B0|Inverse Magicks
	_rsv_3349_-1_1_1_1_S74CFC3B0_E74CFC3B0|The order of forcible magicks to be cast is inverted.
	_rsv_3350_-1_1_0_1_S74CFC3B0_E74CFC3B0|Soul Stranded
	_rsv_3350_-1_1_1_1_S74CFC3B0_E74CFC3B0|Physical and spiritual forms have been separated.
	_rsv_3351_-1_1_0_1_S74CFC3B0_E74CFC3B0|Eye of the Gorgon
	_rsv_3351_-1_1_1_1_S74CFC3B0_E74CFC3B0|Cursed to unleash a petrifying attack in the direction of gaze when this effect expires.
	_rsv_3352_-1_1_0_1_S74CFC3B0_E74CFC3B0|Crown of the Gorgon
	_rsv_3352_-1_1_1_1_S74CFC3B0_E74CFC3B0|Cursed to unleash a petrifying light upon those nearby when this effect expires.
	_rsv_3406_-1_1_0_1_S74CFC3B0_E74CFC3B0|Everburn
	_rsv_3406_-1_1_1_1_S74CFC3B0_E74CFC3B0|Calling upon the power of a Phoenix concept. Damage dealt is increased.
	_rsv_3412_-1_1_0_1_S74CFC3B0_E74CFC3B0|Natural Alignment
	_rsv_3412_-1_1_1_1_S74CFC3B0_E74CFC3B0|Graven with a sigil and sustaining damage over time. Taking damage from certain actions caused by Twist Nature will result in a destructive forcible failure.
	 */

}