package gg.xp.xivsupport.events.triggers.duties;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.AutoChildEventHandler;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.duties.*;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.TetherEvent;
import gg.xp.xivsupport.events.state.XivState;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.events.triggers.support.NpcCastCallout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ScanMe
@CalloutRepo(name = "Baldesion Arsenal", duty = KnownDuty.Eureka_Hydatos)
public class BaldesionArsenal extends AutoChildEventHandler implements FilteredEventHandler {

	private static final Logger log = LoggerFactory.getLogger(BaldesionArsenal.class);

	private final XivState state;
	private final StatusEffectRepository buffs;
	private StatusEffectRepository getBuffs() {
		return buffs;
	}
	private XivState getState() {
		return state;
	}

	@Override
	public boolean enabled(EventContext context) {
		return state.dutyIs(KnownDuty.Eureka_Hydatos);
	}

	public BaldesionArsenal(XivState state, StatusEffectRepository buffs) {
		this.state = state;
		this.buffs = buffs;
	}

	// --------------------------------------------------------
	// Ovni (Testing)
	@NpcCastCallout({0x39C5}) // TODO: Buff applied instead
	private final ModifiableCallout<AbilityCastStart> fluorescence = ModifiableCallout.durationBasedCall("Fluorescence", "Dispell");
	@NpcCastCallout({0x39C0})
	private final ModifiableCallout<AbilityCastStart> megastorm = ModifiableCallout.durationBasedCall("Megastorm", "In");
	@NpcCastCallout({0x39BF})
	private final ModifiableCallout<AbilityCastStart> concussive = ModifiableCallout.durationBasedCall("Concussive Oscillation", "Out");
	private final ModifiableCallout<AbilityCastStart> ionshower = new ModifiableCallout<>("Ion Shower", "Ion Shower");

	// --------------------------------------------------------
	// Art (West)
	private final ModifiableCallout<HeadMarkerEvent> graviball = new ModifiableCallout<>("Graviball Prey", "Run Away");

	// --------------------------------------------------------
	// Owain (East)
	private final ModifiableCallout<HeadMarkerEvent> dorito_stack = new ModifiableCallout<>("Spiritcull Stack", "Stack Mid");
	private final ModifiableCallout<HeadMarkerEvent> dorito_spread = new ModifiableCallout<>("Spiritcull Spread", "Edge of Arena");

	// --------------------------------------------------------
	// Raiden
	@NpcCastCallout({0x3868, 0x3869})
	private final ModifiableCallout<AbilityCastStart> ameno = ModifiableCallout.durationBasedCall("Ame-no-Sakahoko", "Out");
	@NpcCastCallout({0x386A})
	private final ModifiableCallout<AbilityCastStart> whirling = ModifiableCallout.durationBasedCall("Whirling Zantetsuken", "In Hitbox");

	// TODO: Check if this really is N/S
	@NpcCastCallout({0x386A})
	private final ModifiableCallout<AbilityCastStart> lateral_n = ModifiableCallout.durationBasedCall("Lateral Zantetsuken (N)", "South");
	@NpcCastCallout({0x386B})
	private final ModifiableCallout<AbilityCastStart> lateral_s = ModifiableCallout.durationBasedCall("Lateral Zantetsuken (S)", "North");

	private final ModifiableCallout<HeadMarkerEvent> lancing_bolt = new ModifiableCallout<>("Lancing Bolt", "Arena Edge");

	@NpcCastCallout({0x387D})
	private final ModifiableCallout<AbilityCastStart> booming = ModifiableCallout.durationBasedCall("Booming Lament", "Dodge AoE");
	@NpcCastCallout({0x3870,0x3871,0x3872})
	private final ModifiableCallout<AbilityCastStart> cloud2ground = ModifiableCallout.durationBasedCall("Cloud to Ground", "Exaflare, get behind first");

	private final ModifiableCallout<TetherEvent> bitter_barbs = new ModifiableCallout<>("Bitter Barbs", "Break Chain");

	@NpcCastCallout({0x386F,0x3870})
	private final ModifiableCallout<AbilityCastStart> levinwhorl = ModifiableCallout.durationBasedCall("Levinwhorl", "Out of Hitbox");

	@NpcCastCallout({0x387C})
	private final ModifiableCallout<AbilityCastStart> honor = ModifiableCallout.durationBasedCall("For Honor", "Away from Boss");

	// --------------------------------------------------------
	// Absolute Virtue
	// Turbulent Aether - if tether -- check/bait/resolve (light/dark), no tether -- (javelin)
	private final ModifiableCallout<TetherEvent> turb_aether_dark = new ModifiableCallout<>("Turbulent Aether", "Go Dark");
	private final ModifiableCallout<TetherEvent> turb_aether_light = new ModifiableCallout<>("Turbulent Aether", "Go Light");
	private final ModifiableCallout<AbilityCastStart> medusa = ModifiableCallout.durationBasedCall("Medusa's Javelin", "Dodge Cone");
	// TODO: AV Element given away by buff right?
	// TODO: Room splits, puddles, proximity?

	// --------------------------------------------------------
	// Ozma
	// TODO: Triple shade callouts...
	private final ModifiableCallout<HeadMarkerEvent> accelbomb = new ModifiableCallout<>("Acceleration Bomb", "Stop Moving Soon");
	private final ModifiableCallout<HeadMarkerEvent> meteor = new ModifiableCallout<>("Meteor", "Waymark 1 or 2");
	@NpcCastCallout({0x37A9})
	private final ModifiableCallout<AbilityCastStart> holykb = ModifiableCallout.durationBasedCall("Holy", "Holy Knockback");

	@NpcCastCallout({0x37AF})
	private final ModifiableCallout<AbilityCastStart> tornado = ModifiableCallout.durationBasedCall("Tornado", "Kill Add");

	@NpcCastCallout({}) // TODO: Find
	private final ModifiableCallout<AbilityCastStart> black_hole = ModifiableCallout.durationBasedCall("Black Hole", "Cover Button");

	// --------------------------------------------------------
	// Trash
	// Centaur (Berserk)
	@NpcCastCallout({0x3BFE,0x3DC0})
	private final ModifiableCallout<AbilityCastStart> berserk = ModifiableCallout.durationBasedCall("Centaur: Berserk", "Sleep Centaur");
	// TODO: If Centaur gains Berserk, "Run, run run"

	// Calca (Terrifying Glance)
	@NpcCastCallout({0x3C60})
	private final ModifiableCallout<AbilityCastStart> terror_glance = ModifiableCallout.durationBasedCall("Calca: Terrifying Glance", "Sleep Doll");

	// Sprite (Banish)
	// @NpcCastCallout({})
	// private final ModifiableCallout<AbilityCastStart> banish = ModifiableCallout.durationBasedCall("Sprite: Banish", "Interject Sprite");

	// Porrogo (Toy Hammer)
	// @NpcCastCallout({})
	// private final ModifiableCallout<AbilityCastStart> toy_hammer = ModifiableCallout.durationBasedCall("Porrogo: Toy Hammer", "Sleep Frog");

	// Bibliotaph (Magic Burst In/Out?)
	// @NpcCastCallout({})
	// private final ModifiableCallout<AbilityCastStart> magic_burst = ModifiableCallout.durationBasedCall("Bibliotaph: Magic Burst", "Sleep Biblio");

	// Stryx (OTPOD)
	@NpcCastCallout({0x341C,0x341D,0x3C0D})
	private final ModifiableCallout<AbilityCastStart> otpod = ModifiableCallout.durationBasedCall("Stryx: On the Properties of Darkness", "Stun Owl");

	// --------------------------------------------------------

	@HandleEvents
	public void ionShowerEvent(EventContext context, AbilityCastStart event) {
		if (event.getAbility().getId() == 0x39C3 && event.getTarget().isThePlayer()) {
			context.accept(ionshower.getModified(event));
		}
	}

	@HandleEvents
	public void doritostackEvent(EventContext context, HeadMarkerEvent event) {
		if (event.getMarkerId() == 0x0037 && event.getTarget().isThePlayer()) { 	// TODO: Check head marker
			context.accept(dorito_stack.getModified(event));
		}
	}

	@HandleEvents
	public void doritospreadEvent(EventContext context, HeadMarkerEvent event) {
		if (event.getMarkerId() == 0x0039 && event.getTarget().isThePlayer()) {	// TODO: Check head marker
			context.accept(dorito_spread.getModified(event));
		}
	}

	@HandleEvents
	public void graviballEvent(EventContext context, HeadMarkerEvent event) {
		if (event.getMarkerId() == 0x005C && event.getTarget().isThePlayer()) { 	// TODO: Check head marker
			context.accept(graviball.getModified(event));
		}
	}

	@HandleEvents
	public void lancingboltEvent(EventContext context, HeadMarkerEvent event) {
		if (event.getMarkerId() == 0x008A && event.getTarget().isThePlayer()) {	// TODO: Check head marker
			context.accept(lancing_bolt.getModified(event));
		}
	}

	@HandleEvents
	public void bitterbarbsEvent(EventContext context, TetherEvent event) {
//		if (event.eitherTargetMatches(.isThePlayer()) {
//			context.accept(bitter_barbs.getModified(event));
//		}
	}

	@HandleEvents
	public void accelbombEvent(EventContext context, HeadMarkerEvent event) {
		if (event.getMarkerId() == 0x4B && event.getTarget().isThePlayer()) {
			context.accept(accelbomb.getModified(event));
		}
	}
	@HandleEvents
	public void meteorEvent(EventContext context, HeadMarkerEvent event) {
		if (event.getMarkerId() == 0x004C && event.getTarget().isThePlayer()) {	// TODO: Check head marker
			context.accept(meteor.getModified(event));
		}
	}

}