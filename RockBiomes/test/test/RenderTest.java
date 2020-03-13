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
package test;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * @author Javapony
 *
 */
public abstract class RenderTest implements Runnable {

	protected boolean rerender = false;
	
	protected abstract void render(BufferedImage img);
	protected abstract void populateControls(JPanel panel);
	
	@Override
	public void run() {
		JFrame frame = new JFrame(getClass().getSimpleName());
		
		JPanel contentPane = new JPanel(new BorderLayout());
		frame.setContentPane(contentPane);
		
		TestCanvas canvas = new TestCanvas();
		contentPane.add(canvas, BorderLayout.CENTER);
		
		JPanel controls = new JPanel();
		contentPane.add(controls, BorderLayout.SOUTH);
		
		controls.add(new JButton(new AbstractAction("Render") {
			private static final long serialVersionUID = 196339117444926025L;

			@Override
			public void actionPerformed(ActionEvent e) {
				rerender = true;
				canvas.repaint();
			}
		}));
		populateControls(controls);
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 640);
		frame.setVisible(true);
	}

	private class TestCanvas extends JComponent {
		private static final long serialVersionUID = 3407924087557878726L;
		
		private BufferedImage img = null;
		
		@Override
		protected void paintComponent(Graphics g) {
			if (img == null || getWidth() != img.getWidth() || getHeight() != img.getHeight()) {
				img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
				rerender = true;
			}
			
			if (rerender) {
				rerender = false;
				render(img);
			}
			
			g.drawImage(img, 0, 0, null);
		}
	}

}
