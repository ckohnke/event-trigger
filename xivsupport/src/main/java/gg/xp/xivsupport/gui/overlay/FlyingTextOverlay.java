package gg.xp.xivsupport.gui.overlay;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.events.misc.pulls.PullStartedEvent;
import gg.xp.xivsupport.gui.CommonGuiSetup;
import gg.xp.xivsupport.gui.Refreshable;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import gg.xp.xivsupport.speech.CalloutEvent;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FlyingTextOverlay extends XivOverlay {

	private final List<VisualCalloutItem> currentCallouts = new ArrayList<>();
	private final Font font;
	private final SimpleAttributeSet attribs;
	private volatile List<VisualCalloutItem> currentCalloutsTmp = Collections.emptyList();
	private final Object lock = new Object();
	// Will be rubber-stamped like a table cell renderer
	private static final Color color = new Color(255, 255, 64, 255);
	private static final Color backdropColor = new Color(20, 21, 22, 128);
	private static final Color transparentColor = new Color(0, 0, 0, 0);
	private static final int textPadding = 2;
	private final InnerPanel innerPanel;
	private final int templateHeight;

	public FlyingTextOverlay(PersistenceProvider pers, OverlayConfig oc) {
		super("Callout Text", "callout-text-overlay", oc, pers);
		font = new JLabel().getFont().deriveFont(new AffineTransform(2, 0, 0, 2, 0, 0));
		JLabel templateJLabel = new JLabel();
		templateJLabel.setFont(font);
		templateJLabel.setText("A");
		templateHeight = templateJLabel.getPreferredSize().height;
		RefreshLoop<FlyingTextOverlay> refresher = new RefreshLoop<>("CalloutOverlay", this, FlyingTextOverlay::refresh, i -> i.calculateUnscaledFrameTime(50));
		innerPanel = new InnerPanel();
		innerPanel.setPreferredSize(new Dimension(400, 220));
		getPanel().add(innerPanel);
		attribs = new SimpleAttributeSet();
		StyleConstants.setAlignment(attribs, StyleConstants.ALIGN_CENTER);
		refresher.start();
	}

	private final class VisualCalloutItem {
		private final CalloutEvent event;
		private final JTextPane text;
		private final int centerX;
		private final int width;
		private String prevText = "";
		private final int leftGradientBound;
		private final int rightGradientBound;
		private final int heightOfThisItem;
		private volatile int leftTextBound;
		private volatile int rightTextBound;
		private final @Nullable Component extraComponent;
		private volatile int extraComponentLeftPad;

		private VisualCalloutItem(CalloutEvent event) {
			this.event = event;
			text = new JTextPane();
			text.setParagraphAttributes(attribs, false);
//			recheckText();
			text.setAlignmentX(Component.CENTER_ALIGNMENT);
			text.setOpaque(false);
			text.setFont(font);
			text.setEditable(false);
			width = innerPanel.getWidth();
			text.setBorder(null);
			text.setFocusable(false);
			centerX = width >> 1;
			final int newLeftBound;
			{
				extraComponent = event.graphicalComponent();
				if (extraComponent == null) {
					newLeftBound = 0;
				}
				else {
					Dimension oldPref = extraComponent.getPreferredSize();
					int newPrefHeight = templateHeight;
					// New width is template height times preferred aspect ratio, but not to be more than 4:1
					int newPrefWidth = (int) Math.min(newPrefHeight * 4, oldPref.getWidth() / oldPref.getHeight() * newPrefHeight);
					if (newPrefWidth == 0) {
						// If no preferred size set, just assume 1:1 is fine
						//noinspection SuspiciousNameCombination
						newPrefWidth = newPrefHeight;
					}
					extraComponent.setBounds(0, 0, newPrefWidth, newPrefHeight);
					newLeftBound = newPrefWidth + textPadding;
					extraComponent.validate();
				}
			}
			this.text.setBounds(newLeftBound, 0, width, 1);
			int preferredHeight = text.getPreferredSize().height;
//			text.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
			this.text.setBounds(newLeftBound, 0, width, preferredHeight);
			this.text.setForeground(color);
			int preferredTextWidth;
			recheckText();
			heightOfThisItem = this.text.getPreferredSize().height;
			preferredTextWidth = this.text.getPreferredSize().width;
			// TODO: these aren't the actual bounds
			leftTextBound = Math.max(centerX - (preferredTextWidth >> 1), 10);
			rightTextBound = Math.min(centerX + (preferredTextWidth >> 1), width - 10);
			int gradientWidth = 50;
			leftGradientBound = Math.max(leftTextBound - gradientWidth - newLeftBound, 0);
			rightGradientBound = Math.min(rightTextBound + gradientWidth, width);
		}

		private void paint(Graphics2D graphics) {
			GradientPaint paintLeft = new GradientPaint(leftGradientBound, 0, transparentColor, leftTextBound, 0, backdropColor);
			graphics.setPaint(paintLeft);
			graphics.fillRect(leftGradientBound, 0, leftTextBound - leftGradientBound, heightOfThisItem);
			graphics.setPaint(backdropColor);
			graphics.fillRect(leftTextBound, 0, rightTextBound - leftTextBound, heightOfThisItem);
			GradientPaint paintRight = new GradientPaint(rightTextBound, 0, backdropColor, rightGradientBound, 0, transparentColor);
			graphics.setPaint(paintRight);
			graphics.fillRect(rightTextBound, 0, rightGradientBound - rightTextBound, heightOfThisItem);
			graphics.setFont(text.getFont());
			this.text.paint(graphics);
			Component extra = this.extraComponent;
			if (extra != null) {
				graphics.translate(extraComponentLeftPad, 0);
				extra.paint(graphics);
			}
		}

		public boolean isExpired() {
			return event.isExpired();
		}

		public int getHeight() {
			return text.getPreferredSize().height;
		}

		public void recheckText() {
			String newText = event.getVisualText();
			if (!Objects.equals(newText, prevText)) {
				text.setText(prevText = newText);
				if (extraComponent != null) {
					int preferredTextWidth = this.text.getPreferredSize().width;
					// TODO: these aren't the actual bounds
					leftTextBound = Math.max(centerX - (preferredTextWidth >> 1), 10);
					rightTextBound = Math.min(centerX + (preferredTextWidth >> 1), width - 10);
					Rectangle oldBounds = extraComponent.getBounds();
					extraComponentLeftPad = Math.max(0, leftTextBound - oldBounds.width - textPadding);
				}
				else {
					extraComponentLeftPad = 0;
				}
			}
			if (extraComponent != null && extraComponent instanceof Refreshable ref) {
				ref.refresh();
			}
		}
	}


	private void addCallout(CalloutEvent callout) {
		synchronized (lock) {
			for (int i = 0; i < currentCallouts.size(); i++) {
				if (currentCallouts.get(i).event == callout.replaces()) {
					currentCallouts.set(i, new VisualCalloutItem(callout));
					return;
				}
			}
			currentCallouts.add(new VisualCalloutItem(callout));
		}
	}

	private void refreshCallouts() {
		synchronized (lock) {
			currentCallouts.removeIf(VisualCalloutItem::isExpired);
			currentCallouts.forEach(VisualCalloutItem::recheckText);
		}
	}

	@HandleEvents
	public void handleEvent(EventContext context, CalloutEvent event) {
		if (event.getVisualText() != null && !event.getVisualText().isBlank()) {
			addCallout(event);
		}
	}

	@HandleEvents
	public void pullEnded(EventContext context, PullStartedEvent pse) {
		synchronized (lock) {
			currentCallouts.clear();
		}
	}

	private class InnerPanel extends JPanel {
		@Serial
		private static final long serialVersionUID = 6727734196395717257L;

		@Override
		public boolean isOpaque() {
			return false;
		}

		@Override
		public void paint(Graphics g) {
			if (currentCalloutsTmp.isEmpty()) {
				return;
			}
			Graphics2D graphics = (Graphics2D) g;
			AffineTransform oldTransform = graphics.getTransform();
			AffineTransform newTransform = new AffineTransform(oldTransform);
//			g.clearRect(0, 0, getWidth(), getHeight());
			int curY = 0;
			for (VisualCalloutItem ce : currentCalloutsTmp) {
				if (curY + ce.getHeight() > getHeight()) {
					break;
				}
				ce.paint(graphics);
				int height = ce.getHeight();
				newTransform.translate(0, height + 5);
				curY += height;
				graphics.setTransform(newTransform);
			}
			graphics.setTransform(oldTransform);
		}
	}


	private void refresh() {
		refreshCallouts();
		synchronized (lock) {
			currentCalloutsTmp = new ArrayList<>(currentCallouts);
		}
		SwingUtilities.invokeLater(innerPanel::repaint);
	}
	// TODO: smooth transition

	public static void main(String[] args) {
		CommonGuiSetup.setup();
		{
//			InMemoryMapPersistenceProvider pers = new InMemoryMapPersistenceProvider();
//			FlyingTextOverlay overlay = new FlyingTextOverlay(pers);
//			overlay.finishInit();
//			overlay.setVisible(true);
//			overlay.setEditMode(true);
//			overlay.getEnabled().set(true);
//			double scaleFactor = 1.5;
//			overlay.setScale(scaleFactor);
//			overlay.addCallout(new BasicCalloutEvent(null, "One", 5000));
//			overlay.addCallout(new BasicCalloutEvent(null, "This second callout is longer", 15000));
//			overlay.addCallout(new BasicCalloutEvent(null, "Three", 255000));
//			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
//			overlay.addCallout(new BasicCalloutEvent(null, "This one is so long, it isn't going to fit on the screen", 255000));
//			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
//			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
//			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
//			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
//			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
//			overlay.addCallout(new BasicCalloutEvent(null, "Lots of Callouts", 255000));
		}

	}

}