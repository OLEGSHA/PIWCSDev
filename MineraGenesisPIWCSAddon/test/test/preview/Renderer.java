package test.preview;

import java.awt.image.BufferedImage;

import javax.swing.JComponent;

public interface Renderer {
	
	void render(BufferedImage img, int mult, int x, int y);
	
	JComponent getComponent();
	
	default int getDefaultZoomLevel() {
		return 0;
	}

}
