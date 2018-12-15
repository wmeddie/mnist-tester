package io.skymind;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class App extends JFrame {
    private TextField endpoint;
    private PaintSurface paintSurface;

    public static void main(String[] args) {
        new App();
    }

    public App() {
        this.setSize(300, 350);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        paintSurface = new PaintSurface();
        this.add(paintSurface, BorderLayout.CENTER);

        Box controls = Box.createVerticalBox();

        endpoint = new TextField();
        Button recognizeButton = new Button("Recognize Number");
        recognizeButton.addActionListener((e) -> {
            BufferedImage bi = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
            Graphics2D ig2 = bi.createGraphics();

            ig2.setPaint(Color.BLACK);
            ig2.fillRect(0, 0, 300, 300);
            paintSurface.drawGrid = false;
            paintSurface.paint(ig2);
            paintSurface.drawGrid = true;

            Image image = bi.getScaledInstance(28, 28, BufferedImage.SCALE_SMOOTH);
            BufferedImage image2 = new BufferedImage(28, 28, BufferedImage.TYPE_INT_RGB);

            Graphics2D g2d = image2.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();



            try {
                ImageIO.write(image2, "png", new File("/tmp/img.png"));
            } catch (IOException e1) {
                e1.printStackTrace();
            }


            float[] pixels = new float[28 * 28];
            for (int y = 0; y < 28; y++) {
                for (int x = 0; x < 28; x++) {
                    int color = image2.getRGB(x, y);

                    int red   = (color >>> 16) & 0xFF;
                    int green = (color >>>  8) & 0xFF;
                    int blue  = (color >>>  0) & 0xFF;

                    float luminance = (red * 0.2126f + green * 0.7152f + blue * 0.0722f) / 255;
                    pixels[(y * 28) + x] = luminance;
                }
            }

            for (int y = 0; y < 28; y++) {
                for (int x = 0; x < 28; x++) {
                    System.out.print(pixels[(y * 28) + x]);
                }
                System.out.println();
            }

            System.out.println("Sending: " + Arrays.toString(pixels));

            SkilClient client = new SkilClient("admin", "admin");

            try {
                ClassifyResult result = client.classify(endpoint.getText(), pixels);

                int top = result.getResults().get(0);
                float topProb = result.getProbabilities().get(0);

                JOptionPane.showMessageDialog(
                        this,
                        "SKIL Recognizes: " + top + " (Probability: " + topProb + ")"
                );

            } catch (IOException e2) {
                e2.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: " + e2.getMessage());
            }

            paintSurface.shapes.clear();
            paintSurface.repaint();
        });

        controls.add(new Label("Endpoint:"));
        controls.add(endpoint);
        controls.add(recognizeButton);

        this.add(controls, BorderLayout.SOUTH);
        this.setVisible(true);
    }

    private class PaintSurface extends JComponent {
        Color color = Color.WHITE;
        ArrayList<Shape> shapes = new ArrayList<Shape>();
        Point startDrag, endDrag;
        boolean drawGrid = true;

        public PaintSurface() {
            this.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    startDrag = new Point(e.getX(), e.getY());
                    endDrag = startDrag;
                    repaint();
                }

                public void mouseReleased(MouseEvent e) {
                    startDrag = null;
                    endDrag = null;
                    repaint();
                }
            });

            this.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    endDrag = new Point(e.getX(), e.getY());

                    if (endDrag.distance(startDrag) > 10) {

                        Rectangle2D.Float r = makeRectangle(startDrag.x, startDrag.y, e.getX(), e.getY());
                        if (r.width < 20) {
                            r.width = 20;
                        }

                        if (r.height < 20) {
                            r.height = 20;
                        }

                        shapes.add(r);

                        startDrag = new Point(e.getX(), e.getY());
                        endDrag = startDrag;
                    }

                    repaint();
                }
            });
        }
        private void paintBackground(Graphics2D g2){
            g2.setPaint(Color.BLACK);
            g2.fillRect(0, 0, getSize().width, getSize().height);

            if (drawGrid) {
                g2.setPaint(Color.LIGHT_GRAY);
                for (int i = 0; i < getSize().width; i += 10) {
                    Shape line = new Line2D.Float(i, 0, i, getSize().height);
                    g2.draw(line);
                }

                for (int i = 0; i < getSize().height; i += 10) {
                    Shape line = new Line2D.Float(0, i, getSize().width, i);
                    g2.draw(line);
                }
            }
        }

        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintBackground(g2);

            g2.setStroke(new BasicStroke(2));
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            for (Shape s : shapes) {
                //g2.setPaint(Color.BLACK);
                //g2.draw(s);
                g2.setPaint(color);
                g2.fill(s);
            }

            if (startDrag != null && endDrag != null) {
                g2.setPaint(Color.LIGHT_GRAY);
                Shape r = makeRectangle(startDrag.x, startDrag.y, endDrag.x, endDrag.y);
                g2.draw(r);
            }
        }

        private Rectangle2D.Float makeRectangle(int x1, int y1, int x2, int y2) {
            return new Rectangle2D.Float(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x1 - x2), Math.abs(y1 - y2));
        }
    }
}