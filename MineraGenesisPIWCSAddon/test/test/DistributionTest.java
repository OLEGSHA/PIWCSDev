package test;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import ru.windcorp.mineragenesis.noise.FractalNoise;
import ru.windcorp.mineragenesis.piwcs.gen.BlockCollector;
import ru.windcorp.mineragenesis.piwcs.gen.BlockList;

@SuppressWarnings("unused")
public class DistributionTest {

	public static void main(String[] args) {
		
		int resultZoom = 1;
		
		final BufferedImage image = new BufferedImage(1024 / resultZoom, 1024 / resultZoom, BufferedImage.TYPE_INT_ARGB);
		
//		testBlockCollector(image);
		testBias(image);
//		testDiscrete(image);
		
		SwingUtilities.invokeLater(() -> {
			JFrame frame = new JFrame("MineraGenesis Test");
			frame.setContentPane(new JComponent() {

				private static final long serialVersionUID = 2255949411034737009L;

				@Override
				protected void paintComponent(Graphics g) {
					g.drawImage(image, 0, 0, image.getWidth() * resultZoom, image.getHeight() * resultZoom, null);
				}
				
			});
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setSize(image.getWidth() * resultZoom, image.getHeight() * resultZoom);
			frame.setVisible(true);
		});
	}

	private static void testBias(BufferedImage image) {
		FractalNoise densityN = new FractalNoise(6656, 15, 0, 0.01, 4, 0.02, 2, 0.04, 1);
		FractalNoise heightN = new FractalNoise(78, 64, 0,    0.02, 4, 0.04, 2);
		
		for (int x = 0; x < image.getWidth(); ++x) {
			for (int y = 0; y < image.getHeight(); ++y) {
				if (x % 16 == 0 || y % 16 == 0) {
					if (x == image.getWidth() / 2 && y == image.getHeight() / 2) {
						image.setRGB(x, y, 0xFF_FFFF00);
					} else {
						image.setRGB(x, y, 0xFF_444400);
					}
					continue;
				}
				
				double n = densityN.getValuePseudoNormalized(x - image.getWidth() / 2, y - image.getHeight() / 2);
				if (n <= 0) image.setRGB(x, y, 0xFF_000088);
				else if (n >= 1) image.setRGB(x, y, 0xFF_FF0000);
				else image.setRGB(x, y, getColorWithIntesity(0xFF_FFFFFF, ((int)(n * 4)) / 4.0f));
			}
		}
	}

	private static void testBlockCollector(BufferedImage image) {
		BlockCollector collector = new BlockCollector(16, 4);
		Random random = new Random();
		
		int[] table = new int[] {0xFF_000000, 0xFF_FF0000, 0xFF_00FF00, 0xFF_0000FF, 0xFF_FFFFFF};
		
		short[][] env = new short[][] {
			{ 4,  4,  4},
			{ 4,  0,  4},
			{ 4,  4,  4}
		};
		
		for (int x = 0; x < image.getWidth(); ++x) {
			for (int y = 0; y < image.getHeight(); ++y) {
				
				for (int px = -1; px <= 1; ++px) {
					for (int pz = -1; pz <= 1; ++pz) {
						if (px == 0 && pz == 0) {
							collector.add(env[1][1], 1);
							continue;
						}
						if (env[px + 1][pz + 1] == -1) {
							continue;
						}
						
						collector.add(env[px + 1][pz + 1], pwr(Math.min(
								transform(px, ((float) x) / image.getWidth()),
								transform(pz, ((float) y) / image.getWidth())
								)));
					}
				}
				
				image.setRGB(x, y, table[collector.get(random)]);
				
				collector.reset();
			}
		}
	}
	
	private static float transform(int position, float coord) {
		switch (position) {
		case -1:
			return 1 - coord;
		case  0:
			return 1;
		case +1:
			return coord;
		default:
			throw new IllegalArgumentException("position = " + position);
		}
	}
	
	private static float pwr(float x) {
		return x * x * x * x;
	}

	private static int getColorWithIntesity(int color, double intensity) {
		int a = color & 0xFF000000;
		int r = (color >> (8*2)) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;
		
		return a | (((int) (r * intensity)) << (8*2)) |
				(((int) (g * intensity)) << 8) |
				((int) (b * intensity));
	}

}
