package test.preview;

import java.awt.image.BufferedImage;

public abstract class AbstractRenderer implements Renderer {

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
	
	protected boolean paintChunkGrid(BufferedImage img, int x, int y, int xCoord, int yCoord) {
		boolean isRow = (xCoord & 15) == 0;
		boolean isColumn = (yCoord & 15) == 0;
		
		if (!isRow && !isColumn) {
			return false;
		}
		
		if (!isRow) {
			if (yCoord == 0) {
				img.setRGB(x, y, 0xFFFF00);
			} else {
				img.setRGB(x, y, 0x777700);
			}
		} else {
			if (xCoord == 0) {
				if (yCoord == 0) {
					img.setRGB(x, y, 0xFFFF77);
				} else {
					img.setRGB(x, y, 0xFFFF00);
				}
			} else {
				img.setRGB(x, y, 0x777700);
			}
		}
		
		return true;
	}
	
	protected boolean paintGrid(BufferedImage img, int x, int y, int xCoord, int yCoord, int step) {
		if (xCoord % step == 0) {
			img.setRGB(x, y, 0x444444);
			return true;
		}
		
		if (yCoord % step == 0) {
			img.setRGB(x, y, 0x444444);
			return true;
		}
		
		return false;
	}

}
