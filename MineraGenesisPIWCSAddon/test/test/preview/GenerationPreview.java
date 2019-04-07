package test.preview;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import test.FakeLoader;

public class GenerationPreview {
	
	private static final int SIZE = 768;

	private static class CanvasRenderer extends JComponent implements FocusListener {
		private static final long serialVersionUID = 6678527171570933195L;
		
		Dimension size = new Dimension(SIZE + 4, SIZE + 4);
		
		CanvasRenderer() {
			setFocusable(true);
			addFocusListener(this);
		}
		
		@Override
		public Dimension getPreferredSize() {
			return size;
		}
		
		@Override
		public Dimension getMinimumSize() {
			return size;
		}
		
		@Override
		public Dimension getMaximumSize() {
			return size;
		}
		
		@Override
		protected void paintComponent(Graphics g) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			if (isFocusOwner()) {
				g.setColor(Color.WHITE);
				g.fillRect(1, 1, getWidth() - 2, getHeight() - 2);
			}
			g.drawImage(canvas, 2, 2, getWidth() - 4, getHeight() - 4, null);
		}

		@Override
		public void focusGained(FocusEvent e) {
			repaint();
		}

		@Override
		public void focusLost(FocusEvent e) {
			repaint();
		}
	}

	public static void main(String[] args) {
		FakeLoader.load();
		SwingUtilities.invokeLater(GenerationPreview::setupGui);
	}
	
	private static JFrame frame;
	
	private static BufferedImage canvas;
	private static int defaultZoom = 0;
	private static int zoom = 0;
	private static double xOffset = 0, yOffset = 0;
	
	private static Renderer renderer = null;
	
	private static JPanel cardPanel;
	private static CardLayout cardLayout;
	
	private static void setupGui() {
		frame = new JFrame("MGAP Preview");
		
		Container content = frame.getContentPane();
		content.setLayout(new BorderLayout());
		
		CanvasRenderer canvasRenderer = new CanvasRenderer();
		canvasRenderer.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			
			@Override
			public void keyReleased(KeyEvent e) {}
			
			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_UP: yOffset += -0.1; break;
				case KeyEvent.VK_DOWN: yOffset += +0.1; break;
				case KeyEvent.VK_LEFT: xOffset += -0.1; break;
				case KeyEvent.VK_RIGHT: xOffset += +0.1; break;
				case KeyEvent.VK_MINUS: setZoom(zoom - 1, defaultZoom); break;
				case KeyEvent.VK_EQUALS: setZoom(zoom + 1, defaultZoom); break;
				default: return;
				}
				render();
			}
		});
		content.add(canvasRenderer, BorderLayout.CENTER);
		
		JPanel controlPanel = new JPanel(new FlowLayout());
		
		Renderer[] renderers = registerRenderers();
		JComboBox<Renderer> rendererChooser = new JComboBox<>(renderers);
		rendererChooser.addActionListener(GenerationPreview::switchRenderer);
		rendererChooser.setEditable(false);
		controlPanel.add(rendererChooser);
		
		JButton rerenderButton = new JButton("Rerender");
		rerenderButton.addActionListener(event -> render());
		controlPanel.add(rerenderButton);
		
		cardPanel = new JPanel(null);
		cardPanel.setLayout(cardLayout = new CardLayout());
		cardPanel.setBorder(BorderFactory.createTitledBorder("Render settings"));
		for (Renderer renderer : renderers) {
			JComponent comp = renderer.getComponent();
			cardPanel.add(comp == null ? new JLabel("This Renderer has no settings") : comp, renderer.toString());
		}
		
		controlPanel.add(cardPanel);
		
		content.add(controlPanel, BorderLayout.NORTH);
		
		renderer = rendererChooser.getItemAt(0);
		setZoom(0, renderer.getDefaultZoomLevel());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		canvasRenderer.grabFocus();
	}
	
	private static Renderer[] registerRenderers() {
		return new Renderer[] {
				new BiomeRenderer(),
				new SingleDepositRenderer()
		};
	}

	public static int getZoom() {
		return zoom;
	}
	
	public static void setZoom(int zoom, int defaultZoom) {
		double oldZoom = pow2(GenerationPreview.zoom + GenerationPreview.defaultZoom);
		GenerationPreview.zoom = zoom;
		GenerationPreview.defaultZoom = defaultZoom;
		double newZoom = pow2(zoom + defaultZoom);
		
		xOffset = (xOffset) / oldZoom * newZoom;
		yOffset = (yOffset) / oldZoom * newZoom;
		
		if (newZoom > 1) {
			canvas = new BufferedImage((int) (SIZE / newZoom), (int) (SIZE / newZoom), BufferedImage.TYPE_INT_RGB);
		} else if (canvas == null || oldZoom > 1) {
			canvas = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
		}
		render();
	}

	public static void render() {
		if (renderer != null) {
			renderer.render(canvas, zoom < 0 ? (int) pow2(-zoom) : 1, (int) ((xOffset - 0.5) * canvas.getWidth()), (int) ((yOffset - 0.5) * canvas.getHeight()));
		}
		redrawFrame();
	}

	private static void redrawFrame() {
		frame.repaint();
	}

	private static double pow2(int z) {
		if (z == 0) {
			return 1;
		}
		
		double result = 1;
		
		if (z > 0) {
			for (int i = 0; i < z; ++i) {
				result *= 2;
			}
		} else {
			for (int i = 0; i > z; --i) {
				result /= 2;
			}
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	private static void switchRenderer(ActionEvent event) {
		renderer = (Renderer) ((JComboBox<Renderer>) event.getSource()).getSelectedItem();
		cardLayout.show(cardPanel, renderer.toString());
		setZoom(zoom, renderer.getDefaultZoomLevel());
		render();
	}

}
