package finalproject;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class RaycastEngine implements Runnable {
    private static final int WIDTH = 660, HEIGHT = 660; //Screen Width & Height
    private static int FOV = 60, MOVE_SPEED = 4, ROT_SPEED = 3;
    private static int x = 100, y = 100;
    private static double deltaX, deltaY;
    private double angle = 0;
    //private double viewBob = 0.0, vbControl = 0;

    private static Canvas canvas;
    private final Canvas displayCanv;
    private final Color fogColor = new Color(0,0,0); //5/17
    //this creates a really subtle but nice fog effect that greatly effects the atmosphere of the game
    
    private final double FOG_PERCENT = 0.6;
    private static final ArrayList<Line2D.Double> lines = new ArrayList<>(); //arraylist for all lines on screen (NOT rays)
    private static ArrayList<Ray> rays = new ArrayList<>();

    static boolean creatingLines = false;
    private static final Hashtable<Direction, Integer> directionsX = new Hashtable<>();
    private static final Hashtable<Direction, Integer> directionsY = new Hashtable<>();
    private static final int dim = 20;
    private static final boolean[][] maze = new boolean[dim][dim];
    private static boolean MOVE_UP,MOVE_DOWN,TURN_LEFT,TURN_RIGHT;

    /*
     * Initializer
     *
     */
    public RaycastEngine() {
        //lines = makeLines();
        for (boolean[] row : maze) {
            Arrays.fill(row, true);
        }
        directionsX.put(Direction.EAST, 1);
        directionsX.put(Direction.WEST, -1);
        directionsX.put(Direction.SOUTH, 0);
        directionsX.put(Direction.NORTH, 0);
        directionsY.put(Direction.EAST, 0);
        directionsY.put(Direction.WEST, 0);
        directionsY.put(Direction.SOUTH, -1);
        directionsY.put(Direction.NORTH, 1);

        JFrame frame = new JFrame();
        JFrame display = new JFrame("Display");
        display.setSize(WIDTH, HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas = new Canvas();
        canvas.setSize(dim * 40 - 39, dim * 40 - 39);
        frame.add(canvas, BorderLayout.CENTER);
        display.add(displayCanv = new Canvas());
        frame.setFocusable(true);
        display.setFocusable(true);
        frame.addKeyListener(new KeyPressManager());
        display.addKeyListener(new KeyPressManager());
        int response = JOptionPane.showConfirmDialog(null, "Would you like to see the 2D visualization as well?", "HEY!!!", JOptionPane.YES_NO_OPTION);
        canvas.setFocusable(false);
        displayCanv.setFocusable(false);
        
        frame.setLocationRelativeTo(null);
        display.setLocationRelativeTo(null);
        display.setVisible(true);
        deltaX = Math.cos(Math.toRadians(angle)) * 10;
        deltaY = Math.sin(Math.toRadians(angle)) * 10;
        generateMaze(0, 0);
        makeLines();
        frame.pack();
        if(response==0) frame.setVisible(true); //5/18

        new Thread(this).start();
    } //RaycastEngine

    /*
     * Create new raycast engine
     */
    public static void main(String[] args) {
        new RaycastEngine();
    } //main


    /*
     * Run thread, draw on canvas unless lines are being changed (see stateChanged)
     */
    @Override
    //@SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        while (true) {
            if (!creatingLines) {
                draw();
            }
//            if(MOVE_UP||MOVE_DOWN) {
//            viewBob = Math.sin(Math.toRadians(vbControl));
//            vbControl += 0.5;
//            }
            
        }
    } //run


    /*
     * Draw lines and rays onto screen
     */
    private void draw() {
        draw3D();
        BufferStrategy buffStrat = canvas.getBufferStrategy();

        if (buffStrat == null) {
            canvas.createBufferStrategy(2);
            return;
        }
        Graphics g = buffStrat.getDrawGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setColor(Color.RED);

        for (Line2D.Double line : lines) {
            g.drawLine((int) line.x1, (int) line.y1, (int) line.x2, (int) line.y2);
        }


        g.setColor(Color.WHITE);
        if (!creatingLines) {
            rays = getRays(x, y);
        }

        for (Ray ray : rays) {
            g.drawLine((int) ray.getRayOrigin().getX(), (int) ray.getRayOrigin().getY(), (int) ray.getHitPosition().getX(), (int) ray.getHitPosition().getY());

        }

        g.setColor(Color.YELLOW);
        g.fillRect(x, y, 4, 4);

        g.drawLine(x + 2, y + 2, (int) (x + deltaX*5), (int) (y + deltaY*5));


        g.dispose();
        buffStrat.show();
    }



    /*
     * Draw everything that is 3D on the display canvas
     */
    private void draw3D() {

        BufferStrategy buffStrat2 = displayCanv.getBufferStrategy();
        if (buffStrat2 == null) {
            displayCanv.createBufferStrategy(2);
            return;
        }
        Graphics g2 = buffStrat2.getDrawGraphics();
        int i = 0;
        g2.setColor(new Color(30,0,30));
        g2.fillRect(0, 0, displayCanv.getWidth(), displayCanv.getHeight());

        for (Ray ray : rays) {

            double dist = ray.distance();
            double rayAngle = roundAngle(angle - ray.getRayAngle());
            int color = (int) shift(dist, 0, WIDTH, 255, 0);
            color = (int) clamp(color, 0, 255);

            dist *= Math.cos(Math.toRadians(rayAngle)); // fisheye fix!!!! thank god
            double height = (40 * HEIGHT) / dist;
            if (height > 600)
                height = 600;
            
            color = (int)((1-FOG_PERCENT) * color + FOG_PERCENT * fogColor.getGreen());
            //(int)(FOG_PERCENT * fogColor.getRed()), (int)((1-FOG_PERCENT) * color + FOG_PERCENT * fogColor.getGreen()), (int)(FOG_PERCENT * fogColor.getBlue())
            g2.setColor(new Color(color,0,color)); //made it green because the human eye can see the most shades of green out of any color
            g2.fillRect(i * 11, displayCanv.getHeight() / 2 - (int) height / 2, 11, (int) height);
            //Floor casting (we don't really need to cast since we know where the bottom of the floor is

            //g2.setColor(Color.gray);
            //g2.fillRect(i*11, displayCanv.getHeight()/2 + (int)height/2, 11, (int)height);
            // ^ this first attempt at a solution actually produced a pretty cool effect where it felt like you were standing on a glass platform in between the two sides of the walls

            int start = displayCanv.getHeight()/2 + (int)height/2; //600 / 2
            //color = (color >> 1) & 8355711;
            //g2.setColor(new Color(color,0,color));
            // ^ these 2 lines also create a really cool effect that should be tried out if you have time
            g2.setColor(new Color(20,0,20));
            g2.fillRect(i*11,(start),11,(int)(start+ray.distance()));
            //we don't need to ceiling cast since we fill with background color and then draw everything on top although we could , just reverse floor casting

            
            
            
            i++;

        }

        g2.dispose();
        buffStrat2.show();

    }


    /*
     * Creates list of rays
     */
    private ArrayList<Ray> getRays(int x, int y) {
        ArrayList<Ray> rays = new ArrayList<>();
        double direction = Math.toRadians(angle) - Math.toRadians(FOV / 2.0);

        for (int i = -FOV / 2; i < FOV / 2; i++) {

            int maxDistance = 3000;
            double minDist = maxDistance;
            for (Line2D.Double line : RaycastEngine.lines) {
                double dist = ray(x, y, x + Math.cos(direction) * maxDistance, y + Math.sin(direction) * maxDistance, line.x1, line.y1, line.x2, line.y2);

                if (dist < minDist && dist > 0) {
                    minDist = dist;

                }
            }


            if (minDist < maxDistance) //only show rays that actually hit something
                rays.add(new Ray(new Point2D.Double(x, y), new Point2D.Double(x + Math.cos(direction) * minDist, y + Math.sin(direction) * minDist), roundAngle(Math.toDegrees(direction))));
            //rays.add(new Line2D.Double(x, y, x+Math.cos(direction) * minDist, y+Math.sin(direction)*minDist));
            direction += Math.toRadians(1); //1 degree in radians

        }
        return rays;

    }
    
    /*
     * Casts a ray in 2D space from start_x to start_y, and calculates the end of the ray by using cos_dist and sin_dist
     * returns the length of the ray
     */
    public static double ray(double start_x, double start_y, double cos_dist, double sin_dist, double line_x, double line_y, double lineEnd_x, double lineEnd_y) {
        double rayEnd_x, rayEnd_y, line1_x, line1_y;
        rayEnd_x = cos_dist - start_x;
        rayEnd_y = sin_dist - start_y;
        line1_x = lineEnd_x - line_x;
        line1_y = lineEnd_y - line_y;

        double s, t;
        double v = -line1_x * rayEnd_y + rayEnd_x * line1_y; 
        s = (-rayEnd_y * (start_x - line_x) + rayEnd_x * (start_y - line_y)) / v; //surface parameters
        t = (line1_x * (start_y - line_y) - line1_y * (start_x - line_x)) / v; //ray parameters

        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            // Collision detected
            double x = start_x + (t * rayEnd_x); //end x
            double y = start_y + (t * rayEnd_y); //end y

            return distance(start_x, start_y, x, y);
        }

        return -1; // no collision
    }

    //Math functions
    /*
     * Uses euclidean distance formula to calculate distance from x1,y1 to x2,y2
     */
    public static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }
    
    /*
     * clamps value
     * ex: clamp(400,10,20) returns 20
     */
    public static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    /*
     * shifts value from 1 range to another
     * shift(25,0,100,100,200) would remap 25 from 0-100 from 100-200
     */
    public static double shift(double n, double lower1, double upper1, double lower2, double upper2) {
        return ((n - lower1) / (upper1 - lower1)) * (upper2 - lower2) + lower2;
    }
    
    /*
     * rounds angle so its always 0 < angle < 360 (2pi)
     */
    public static double roundAngle(double ang) {
        if (ang > 359) {
            ang -= 360;
        }
        if (ang < 0) {
            ang += 360;
        }
        return ang;
    }
//end math functions
    
    /*
     * Gets the neighbors of a direction, used for maze generation.
     * Returns HashMap<Direction, Boolean> of each direction, the direction is true if there is a neighbor.
     */
    public static HashMap<Direction, Boolean> getNeighbors(int x1, int y1) {
        HashMap<Direction, Boolean> hashMap = new HashMap<>();
        hashMap.put(Direction.EAST, false);
        hashMap.put(Direction.WEST, false);
        hashMap.put(Direction.SOUTH, false);
        hashMap.put(Direction.NORTH, false);

        if (isValidCoordinate(x1 + 1, y1) && maze[x1 + 1][y1]) {
            hashMap.put(Direction.EAST, true);
        }
        if (isValidCoordinate(x1 - 1, y1) && maze[x1 - 1][y1]) {
            hashMap.put(Direction.WEST, true);

        }
        if (isValidCoordinate(x1, y1 - 1) && maze[x1][y1 - 1]) {
            hashMap.put(Direction.SOUTH, true);

        }
        if (isValidCoordinate(x1, y1 + 1) && maze[x1][y1 + 1]) {
            hashMap.put(Direction.NORTH, true);

        }
        return hashMap;
    }

    /*
     * if a valid coordinate on the maze
     */
    private static boolean isValidCoordinate(int x1, int y1) {
        return x1 < dim && y1 < dim && y1 >= 0 && x1 >= 0;
    }

    /*
     * set maze[y1][x1] to false (no wall)
     */
    private static void setPath(int x1, int y1) {
        maze[y1][x1] = false;
    }

    /*
     * if x1,y1, is a valid coordinate, returns true if the position is true ( a wall)
     */
    private static boolean isWall(int x1, int y1) {
        if (isValidCoordinate(x1,y1)) {
            return maze[y1][x1];
        } else {
            return false;
        }
    }

    /*
     * not quite sure what to call this algorithm,
     * it was based on recursive backtracking but i made a bunch of modifications in order to actually understand what this function does
     */
    private static void generateMaze(int x1, int y1) {
        setPath(x1, y1);
        ArrayList<Direction> directions = new ArrayList<>();
        directions.add(Direction.NORTH);
        directions.add(Direction.SOUTH);
        directions.add(Direction.EAST);
        directions.add(Direction.WEST);
        Collections.shuffle(directions, new Random());
        while (directions.size() > 0) {
            Direction d = directions.remove(0);
            int nx, ny;
            nx = x1 + (directionsX.get(d) * 2);
            ny = y1 + (directionsY.get(d) * 2);
            if (isWall(nx, ny)) {
                int lx, ly;
                lx = x1 + directionsX.get(d);
                ly = y1 + directionsY.get(d);
                setPath(lx, ly);
                generateMaze(nx, ny);
            }
        }
    }

    /*
     * draws the lines represented by the maze array
     */
    private static void makeLines() {
        lines.add(new Line2D.Double(0, 0, canvas.getWidth(), 0));
        lines.add(new Line2D.Double(0, 0, 0, canvas.getHeight()));
        lines.add(new Line2D.Double(0, canvas.getHeight(), canvas.getWidth(), canvas.getHeight()));
        lines.add(new Line2D.Double(canvas.getWidth(), canvas.getHeight(), canvas.getWidth(), 0));

        for (int y1 = 0; y1 < maze.length; y1++) {
            for (int x1 = 0; x1 < maze[0].length; x1++) {
                //for each point
                //check EACH neighbor

                if (maze[x1][y1]) {
                    HashMap<Direction, Boolean> neighbors = getNeighbors(x1, y1);
                    if (neighbors.get(Direction.EAST)) {
                        lines.add(new Line2D.Double(x1 * 40, y1 * 40, x1 * 40 + 40, y1 * 40));
                    }
                    if (neighbors.get(Direction.WEST)) {
                        lines.add(new Line2D.Double(x1 * 40, y1 * 40, x1 * 40 - 40, y1 * 40));
                    }
                    if (neighbors.get(Direction.NORTH)) {
                        lines.add(new Line2D.Double(x1 * 40, y1 * 40, x1 * 40, y1 * 40 + 40));
                    }
                    if (neighbors.get(Direction.SOUTH)) {
                        lines.add(new Line2D.Double(x1 * 40, y1 * 40, x1 * 40, y1 * 40 - 40));
                    }
                }
            }
        }
    }

    /*
     * just represents directions, no real purpose but it makes it a lot easier to read the code
     */
    private enum Direction {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    /*
     * manages keypresses
     */
    class KeyPressManager implements KeyListener {
    	
        @Override
        public void keyPressed(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) MOVE_DOWN=true;
            if(e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) MOVE_UP=true;
            if(e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) TURN_RIGHT=true;
            if(e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) TURN_LEFT=true;
            if(e.getKeyCode() == KeyEvent.VK_R) x=y=100;
            boolean wallInFront=false,wallBehind = false;
            Line2D.Double behind = new Line2D.Double(x, y, (x - deltaX ),  (y - deltaY ));
            Line2D.Double front = new Line2D.Double(x, y, (x + deltaX ),  (y + deltaY ));
            

            for(Line2D.Double line : lines) {
                if(line.intersectsLine(behind) && !wallBehind) {
                    wallBehind = true;
                }
                if(line.intersectsLine(front) && !wallInFront) {
                    wallInFront = true;
                }
            }
            //This collision detection isn't perfect, as you can still glitch through walls

            if (MOVE_DOWN && !wallBehind) {
                x -= deltaX / MOVE_SPEED;
                y -= deltaY / MOVE_SPEED;
            }
            if (MOVE_UP && !wallInFront) {
                x += deltaX / MOVE_SPEED;
                y += deltaY / MOVE_SPEED;
            }
            if (TURN_RIGHT) {
                angle += ROT_SPEED;
                angle = roundAngle(angle);

            }
            if (TURN_LEFT) {
                angle -= ROT_SPEED;
                angle = roundAngle(angle);

            }
            deltaX = Math.cos(Math.toRadians(angle)) * 10;
            deltaY = Math.sin(Math.toRadians(angle)) * 10;

        }

        @Override
        public void keyReleased(KeyEvent e) {
            if(e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) { MOVE_DOWN=false; }
            if(e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) { MOVE_UP=false; }
            if(e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) { TURN_RIGHT=false; }
            if(e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) { TURN_LEFT=false; }
        }

        @Override
        public void keyTyped(KeyEvent e) {

        }
    }

}
