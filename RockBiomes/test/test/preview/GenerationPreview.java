/*
 * MineraGenesis Rock Biomes Addon
 * Copyright (C) 2019  Javapony/OLEGSHA
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package test.preview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import ru.windcorp.mineragenesis.MineraGenesis;
import ru.windcorp.mineragenesis.interfaces.MGChunkProcessor;
import ru.windcorp.mineragenesis.rb.RockBiomesCP;
import ru.windcorp.mineragenesis.rb.gen.Dimension;
import ru.windcorp.mineragenesis.rb.gen.DimensionComplex;
import test.FakeLoader;

/**
 * @author Javapony
 *
 */
public class GenerationPreview {
	
	private class GPKeyListener implements KeyListener {

		/**
		 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
		 */
		@Override
		public void keyPressed(KeyEvent e) {
			switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				moveImg(0, (int) (-canvas.getHeight() / 4));
				break;
			case KeyEvent.VK_DOWN:
				moveImg(0, (int) (+canvas.getHeight() / 4));
				break;
			case KeyEvent.VK_LEFT:
				moveImg((int) (-canvas.getWidth() / 4), 0);
				break;
			case KeyEvent.VK_RIGHT:
				moveImg((int) (+canvas.getWidth() / 4), 0);
				break;
			case KeyEvent.VK_MINUS:
				zoom *= 2;
				forceRender();
				break;
			case KeyEvent.VK_EQUALS:
				zoom /= 2;
				forceRender();
				break;
			case KeyEvent.VK_R:
				forceRender();
				break;
			case KeyEvent.VK_D:
				discrete = !discrete;
				forceRender();
				break;
			case KeyEvent.VK_G:
				grid = !grid;
				forceRender();
				break;
			default: return;
			}
			
			e.consume();
		}

		@Override
		public void keyTyped(KeyEvent e) {}

		@Override
		public void keyReleased(KeyEvent e) {}

	}

	private boolean forceRender = false;
	
	private Renderer renderer = null;
	
	private BufferedImage img = null;
	private int centerX = 0, centerY = 0;
	private int dCenterX = 0, dCenterY = 0;
	private double zoom = 1;
	
	public boolean discrete = true;
	private boolean grid = true;
	
	private JLabel tooltip;
	private RenderCanvas canvas;

	public static void main(String[] args) {
		FakeLoader.load(args[0]);
		SwingUtilities.invokeLater(new GenerationPreview()::load);
	}
	
	private void load() {
		JFrame frame = new JFrame("RockBiomes Generation Preview");
		
		JPanel contentPane = new JPanel(new BorderLayout());
		frame.setContentPane(contentPane);
		
		canvas = new RenderCanvas();
		contentPane.add(canvas, BorderLayout.CENTER);
		
		JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		tooltip = new JLabel("N/A");
		tooltip.setFont(Font.decode("Consolas-PLAIN-14"));
		statusPanel.add(tooltip);
		contentPane.add(statusPanel, BorderLayout.SOUTH);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 500);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private class RenderCanvas extends JComponent {
		
		private static final long serialVersionUID = 2844863053420763593L;
		
		public RenderCanvas() {
			setLayout(new FlowLayout(FlowLayout.LEFT));
			
			JComboBox<Renderer> rendererSelector = new JComboBox<>(createRenderers());
			rendererSelector.setEditable(false);
			rendererSelector.addActionListener(action -> {
				renderer = (Renderer) rendererSelector.getSelectedItem();
				forceRender();
			});
			add(rendererSelector);
			
			MouseAdapter adapter = new MouseAdapter() {
				private int dragXOrigin;
				private int dragYOrigin;
				
				@Override
				public void mouseMoved(MouseEvent e) {
					if (img != null) updateTooltip(e.getX(), e.getY());
				}
				@Override
				public void mouseDragged(MouseEvent e) {
					if (img != null) {
						moveImg(dragXOrigin - e.getX(), dragYOrigin - e.getY());
						dragXOrigin = e.getX();
						dragYOrigin = e.getY();
						forceRender();
					}
				}
				@Override
				public void mousePressed(MouseEvent e) {
					dragXOrigin = e.getX();
					dragYOrigin = e.getY();
				}
				@Override
				public void mouseExited(MouseEvent e) {
					if (img != null) updateTooltip(-1, -1);
				}
				@Override
				public void mouseWheelMoved(MouseWheelEvent e) {
					if (e.getWheelRotation() > 0) {
						for (int i = 0; i < e.getWheelRotation(); ++i) {
							zoom *= 2;
						}
					} else {
						for (int i = 0; i < -e.getWheelRotation(); ++i) {
							zoom /= 2;
						}
					}
					forceRender();
				}
			};
			
			addMouseListener(adapter);
			addMouseMotionListener(adapter);
			addMouseWheelListener(adapter);
			
			setFocusable(true);
			addKeyListener(new GPKeyListener());
			addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent e) {
					repaint();
				}
				@Override
				public void focusGained(FocusEvent e) {
					repaint();
				}
			});
		}
		
		private BufferedImage buffer;
		
		@Override
		protected void paintComponent(Graphics g) {
			if (img == null || getWidth() != img.getWidth() || getHeight() != img.getHeight()) {
				img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
				buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
				forceRender = true;
			}
			
			if (forceRender) {
				forceRender = false;
				
				if (
						(dCenterX != 0 || dCenterY != 0) && 
						(Math.abs(dCenterX) < img.getWidth() || Math.abs(dCenterY) < img.getHeight())) {
					renderOptimized();
					dCenterX = 0;
					dCenterY = 0;
				} else {
					render(0, 0, img.getWidth(), img.getHeight());
				}
			}
			
			if (isFocusOwner()) {
				g.setColor(Color.RED);
				g.drawRect(0, 0, getWidth(), getHeight());
			}
			
			g.drawImage(img, 0, 0, null);
		}
		
		private void renderOptimized() {
			int dx = dCenterX, dy = dCenterY;
			int w = img.getWidth(), h = img.getHeight();
			int startX, startY, endX, endY;
			
			BufferedImage tmp = img;
			img = buffer;
			buffer = tmp;
			
			Graphics g = img.createGraphics();
			g.drawImage(buffer, -dx, -dy, null);
			g.dispose();
			
			startX = (dx > 0) ? w - dx : 0;
			startY = 0;
			endX = (dx > 0) ? w : -dx;
			endY = h;
			render(startX, startY, endX, endY);
			
			startX = (dx > 0) ? 0 : -dx;
			startY = (dy > 0) ? h - dy : 0;
			endX = (dx > 0) ? w - dx : w;
			endY = (dy > 0) ? h : -dy;
			render(startX, startY, endX, endY);
		}
		
	}
	
	private void moveImg(int dImgX, int dImgY) {
		this.dCenterX = dImgX;
		this.dCenterY = dImgY;
		this.centerX += dImgX;
		this.centerY += dImgY;
		
		forceRender();
	}
	
	private double translateImgXToWorldX(int imgX) {
		return (imgX - img.getWidth() / 2 + centerX) * zoom;
	}

	private double translateImgYToWorldZ(int imgY) {
		return (imgY - img.getHeight() / 2 + centerY) * zoom;
	}

	private void render(int startX, int startY, int endX, int endY) {
		for (int x = startX; x < endX; ++x) {
			for (int y = startY; y < endY; ++y) {
				double wx = translateImgXToWorldX(x);
				double wz = translateImgYToWorldZ(y);
				
				int wxInt = (int) wx;
				int wzInt = (int) wz;
				
				if (wxInt == 0 || wzInt == 0) {
					img.setRGB(x, y, 0xFFFFFF);
				} else if (wxInt == 1 || wxInt == -1 || wzInt == 1 || wzInt == -1) {
					img.setRGB(x, y, 0x000000);
				} else if (grid && ((wxInt % 1000) == 0 || (wzInt % 1000) == 0)) {
					img.setRGB(x, y, 0xFFFF88);
				} else if (grid && (zoom < 4 && ((wxInt & 0xF) == 0 || (wzInt & 0xF) == 0))) {
					img.setRGB(x, y, 0x888888);
				} else {
					if (zoom > 1 || discrete) {
						img.setRGB(x, y, renderer.getColorAt(wxInt, wzInt));
					} else {
						img.setRGB(x, y, renderer.getColorAt(wx, wz));
					}
				}
			}
		}
	}
	
	private void updateTooltip(int imgX, int imgY) {
		if (imgX < 0 || imgY < 0) {
			tooltip.setText("N/A");
			return;
		}
		
		double wx = translateImgXToWorldX(imgX);
		double wz = translateImgYToWorldZ(imgY);
		tooltip.setText(String.format("(%+10.2f; %+10.2f) %s", wx, wz, renderer.getTooltipAt(wx, wz)));
	}
	
	private void forceRender() {
		forceRender = true;
		canvas.repaint();
	}
	
	private Renderer[] createRenderers() {
		Collection<Renderer> renderers = new ArrayList<>();
		
		MGChunkProcessor cp = MineraGenesis.getProcessor();
		
		if (!(cp instanceof RockBiomesCP)) {
			throw new RuntimeException(cp + " is not a RockBiomesCP");
		}
		
		for (Dimension dim : ((RockBiomesCP) cp).getDimensions()) {
			createRenderers(dim, renderers);
		}
		
		Renderer[] result = renderers.toArray(new Renderer[renderers.size()]);
		renderer = result[0];
		return result;
	}

	private void createRenderers(Dimension dim, Collection<Renderer> renderers) {
		if (dim instanceof DimensionComplex) {
			renderers.add(new RockBiomeRenderer((DimensionComplex) dim));
			renderers.add(new OreRenderer((DimensionComplex) dim));
			renderers.add(new BedrockRockBiomeRenderer((DimensionComplex) dim));
			renderers.add(new BedrockOreRenderer((DimensionComplex) dim));
		}
	}

}
