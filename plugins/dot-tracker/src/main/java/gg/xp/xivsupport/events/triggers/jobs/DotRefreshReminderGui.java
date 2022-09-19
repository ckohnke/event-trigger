package gg.xp.xivsupport.events.triggers.jobs;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivdata.data.DotBuff;
import gg.xp.xivdata.data.Job;
import gg.xp.xivsupport.events.triggers.jobs.gui.DotTrackerOverlay;
import gg.xp.xivsupport.gui.TitleBorderFullsizePanel;
import gg.xp.xivsupport.gui.WrapLayout;
import gg.xp.xivsupport.gui.extra.PluginTab;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;
import gg.xp.xivsupport.persistence.gui.ColorSettingGui;
import gg.xp.xivsupport.persistence.gui.IntSettingSpinner;
import gg.xp.xivsupport.persistence.gui.LongSettingGui;
import gg.xp.xivsupport.persistence.settings.BooleanSetting;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ScanMe
public class DotRefreshReminderGui implements PluginTab {

	private final DotRefreshReminders backend;
	private final DotTrackerOverlay overlay;

	public DotRefreshReminderGui(DotRefreshReminders backend, DotTrackerOverlay overlay) {
		this.backend = backend;
		this.overlay = overlay;
	}

	@Override
	public String getTabName() {
		return "DoT/Buff Tracker";
	}


	@Override
	public int getSortOrder() {
		return 3;
	}
	@Override
	public Component getTabContents() {
		TitleBorderFullsizePanel outerPanel = new TitleBorderFullsizePanel("Dots and Buffs");
//		outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.PAGE_AXIS));
		outerPanel.setLayout(new BorderLayout());
		BooleanSetting enableTtsSetting = backend.getEnableTts();
		BooleanSetting enableOverlaySetting = overlay.getEnabled();
		BooleanSetting showTicksSetting = overlay.showTicks();

		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new WrapLayout());

		{
			JPanel preTimeBox = new LongSettingGui(backend.getDotRefreshAdvance(), "Time before expiry to call out (milliseconds)").getComponent();
			settingsPanel.add(preTimeBox);
		}
		{
			enableTtsSetting.addListener(outerPanel::repaint);
			JCheckBox enableTts = new BooleanSettingGui(enableTtsSetting, "Enable TTS").getComponent();
			settingsPanel.add(enableTts);
		}
		{
			JCheckBox enableOverlay = new BooleanSettingGui(enableOverlaySetting, "Enable Overlay").getComponent();
			settingsPanel.add(enableOverlay);
			enableOverlaySetting.addListener(outerPanel::repaint);
		}
		{
			JPanel numSetting = new IntSettingSpinner(backend.getNumberToDisplay(), "Max in Overlay", enableOverlaySetting::get).getComponent();
			settingsPanel.add(numSetting);
		}
		{
			JCheckBox showTicks = new BooleanSettingGui(showTicksSetting, "Show Ticks", enableOverlaySetting::get).getComponent();
			settingsPanel.add(showTicks);
			showTicksSetting.addListener(outerPanel::repaint);
		}
		{
			// Line break
			settingsPanel.add(Box.createHorizontalStrut(32000));
		}
		{
			settingsPanel.add(new ColorSettingGui(backend.getNormalColor(), "Bar Color", enableOverlaySetting::get).getComponent());
			settingsPanel.add(new ColorSettingGui(backend.getExpiringColor(), "Expiring Color", enableOverlaySetting::get).getComponent());
			settingsPanel.add(new ColorSettingGui(backend.getExpiredColor(), "Expired Color", enableOverlaySetting::get).getComponent());
			settingsPanel.add(new ColorSettingGui(backend.getFontColor(), "Font Color", enableOverlaySetting::get).getComponent());
		}

		outerPanel.add(settingsPanel, BorderLayout.PAGE_START);

		JPanel innerPanel = new JPanel();
		innerPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Map<DotBuff, BooleanSetting> dots = backend.getEnabledDots();
		Map<Job, List<DotBuff>> byJob = dots.keySet().stream().collect(Collectors.groupingBy(DotBuff::getJob));
		List<Job> jobKeys = byJob.keySet().stream().sorted(Comparator.comparing(Job::getFriendlyName)).toList();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.ipadx = 50;
		c.gridy = 0;
		jobKeys.forEach((job) -> {
			List<DotBuff> dotsForJob = byJob.get(job);
			c.gridwidth = 1;
			c.gridx = 0;
			c.weightx = 0;
			// left filler
			innerPanel.add(new JPanel());
			c.gridx ++;
			JLabel label = new JLabel(job.getFriendlyName());
			innerPanel.add(label, c);
			dotsForJob.forEach(dot -> {
				c.gridx++;
				BooleanSetting setting = dots.get(dot);
				JCheckBox checkbox = new BooleanSettingGui(setting, dot.getLabel(), () -> enableTtsSetting.get() || enableOverlaySetting.get()).getComponent();
				innerPanel.add(checkbox, c);
			});
			c.gridx++;
			c.weightx = 1;
			c.gridwidth = GridBagConstraints.REMAINDER;
			// Add dummy to pad out the right side
			JPanel dummyPanel = new JPanel();
			innerPanel.add(dummyPanel, c);
			c.gridy++;
		});
		c.weighty = 1;
		innerPanel.add(new JPanel(), c);
		innerPanel.setPreferredSize(innerPanel.getMinimumSize());
		JScrollPane scroll = new JScrollPane(innerPanel);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		outerPanel.add(scroll);
		return outerPanel;
	}
}
