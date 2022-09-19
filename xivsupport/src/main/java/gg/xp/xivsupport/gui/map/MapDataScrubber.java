package gg.xp.xivsupport.gui.map;

import gg.xp.xivsupport.gui.tables.renderers.FractionDisplayHelper;
import gg.xp.xivsupport.gui.tables.renderers.ResourceBar;
import gg.xp.xivsupport.gui.util.GuiUtil;
import gg.xp.xivsupport.models.CurrentMaxPairImpl;
import gg.xp.xivsupport.persistence.gui.BooleanSettingGui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

@SuppressWarnings({"SerializableNonStaticInnerClassWithoutSerialVersionUID", "SerializableStoresNonSerializable"})
public class MapDataScrubber extends JPanel {

	private final MapDataController controller;

	public MapDataScrubber(MapDataController controller, Runnable settingsCallback) {
		this.controller = controller;
		setLayout(new GridBagLayout());
		GridBagConstraints c = GuiUtil.defaultGbc();

		JCheckBox liveCb = new JCheckBox("Live");
		liveCb.setModel(new JToggleButton.ToggleButtonModel() {
			@Override
			public boolean isSelected() {
				return controller.isLive();
			}

			@Override
			public void setSelected(boolean b) {
				super.setSelected(b);
				controller.setLive(b);
				repaint();
			}
		});
		JCheckBox recordCb = new BooleanSettingGui(controller.getEnableCapture(), "Record", true).getComponent();
		c.weightx = 0;
		add(liveCb, c);
		c.gridy++;
		add(recordCb, c);
		c.gridy = 0;
		c.gridheight = 2;
		c.weightx = 1;
		c.gridx++;
		c.insets = new Insets(4, 8, 2, 8);
		add(new IndicatorAndScrubber(), c);
		c.insets = new Insets(0, 0, 0, 0);
		c.gridheight = 1;
		c.gridx++;
		c.gridy = 0;
		c.weightx = 0;

//		liveCb.addActionListener(l -> {
//			controller.setLive(liveCb.isSelected());
//		});
//		liveCb.setSelected(controller.isLive());
		List<Integer> deltas = List.of(1, 10, 100, 1000);
		for (Integer delta : deltas) {
			String negArrow = "<<";
			JButton negButton = new JButton(String.format("%s  %s  %s", negArrow, Math.abs(delta), negArrow));
			negButton.addActionListener(l -> {
				controller.setRelativeIndex(-1 * delta);
				repaint();
			});
			add(negButton, c);
			c.gridy = 1;
			String posArrow = ">>";
			JButton posButton = new JButton(String.format("%s  %s  %s", posArrow, Math.abs(delta), posArrow));
			posButton.addActionListener(l -> {
				controller.setRelativeIndex(delta);
				repaint();
			});
			add(posButton, c);
			c.gridy = 0;
			c.gridx++;

		}

		JButton clearButton = new JButton("Clear Data");
		clearButton.addActionListener(l -> controller.clearAll());
		add(clearButton, c);
		c.gridy++;
		JButton settingsButton = new JButton("Open/Close Settings");
		settingsButton.addActionListener(l -> settingsCallback.run());
		add(settingsButton, c);
	}

	private class IndicatorAndScrubber extends ResourceBar implements MouseListener, MouseMotionListener {

		private final FractionDisplayHelper fdh = new FractionDisplayHelper();
		private final JLabel leftLabel = new JLabel("Drag anywhere in this area to scrub");
		private final JLabel rightLabel = new JLabel("Shift-Drag for slower scrub, Ctrl-Drag for faster scrub");
		private Point dragPoint;
		private static final Color green = new Color(0, 170, 0);

		{
			leftLabel.setHorizontalAlignment(SwingConstants.LEFT);
			rightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
			add(leftLabel);
			add(rightLabel);
			addMouseListener(this);
			addMouseMotionListener(this);
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}

		@Override
		public void setBounds(int x, int y, int width, int height) {
			super.setBounds(x, y, width, height);
			fdh.setBounds(0, height / 2, width, height / 2);
			leftLabel.setBounds(0, height / 2, width, height / 2);
			rightLabel.setBounds(0, height / 2, width, height / 2);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			int maxFrame = controller.getSize() - 1;
			int curFrame;
			if (maxFrame == 0) {
				maxFrame = 1;
				curFrame = 1;
			}
			else {
				curFrame = controller.getIndex();
			}

			// TODO this shouldn't be here
			fdh.setValue(new CurrentMaxPairImpl(controller.getIndex(), maxFrame));
			Dimension dim = getSize();
			int vBarWidth = 3;
			int hBarHeight = 2;
			graphics.fillRect(0, 0, vBarWidth, dim.height / 2);
			graphics.fillRect(dim.width - vBarWidth, 0, vBarWidth, dim.height / 2);
			graphics.fillRect(vBarWidth, 5, dim.width - 2 * vBarWidth, hBarHeight);
			if (controller.isLive()) {
				graphics.setColor(green);
			}
			else {
				graphics.setColor(Color.RED);
			}

			int barX = vBarWidth + (int) (((double) dim.width - 3 * vBarWidth) * curFrame / maxFrame);
			graphics.fillRect(barX, 0, vBarWidth, dim.height / 2);
		}

		@Override
		public void mouseClicked(MouseEvent e) {

		}

		@Override
		public void mousePressed(MouseEvent e) {
			dragPoint = MouseInfo.getPointerInfo().getLocation();
		}

		@Override
		public void mouseReleased(MouseEvent e) {

		}

		@Override
		public void mouseEntered(MouseEvent e) {

		}

		@Override
		public void mouseExited(MouseEvent e) {

		}

		@Override
		public void mouseDragged(MouseEvent e) {
			Point curPoint = e.getLocationOnScreen();
			double xDiff = curPoint.x - dragPoint.x;
			int modifiersEx = e.getModifiersEx();
			if ((modifiersEx & InputEvent.CTRL_DOWN_MASK) > 0) {
				xDiff *= 25;
			}
			else if ((modifiersEx & InputEvent.SHIFT_DOWN_MASK) > 0) {
				// Nothing
			}
			else {
				xDiff *= 5;
			}
			if (xDiff != 0) {
				controller.setRelativeIndexAutoLive((int) xDiff);
			}
//		log.info("Map Panel Drag: {},{}", xDiff, yDiff);
			dragPoint = curPoint;
		}

		@Override
		public void mouseMoved(MouseEvent e) {

		}
	}

}
