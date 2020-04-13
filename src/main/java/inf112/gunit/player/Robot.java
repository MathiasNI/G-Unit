package inf112.gunit.player;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.Vector2;
import inf112.gunit.board.Direction;
import inf112.gunit.player.card.MovementCard;
import inf112.gunit.player.card.ProgramCard;
import inf112.gunit.player.card.RotationCard;
import inf112.gunit.screens.Game;

import java.util.Arrays;

/**
 * The Robot class is used to perform all kinds of
 * robot mechanics.
 */
public class Robot {

    public int flagsCollected = 0;

    private int id;

    // these to are for testing, and (probably) wont be used in the actual game
    // program stores a program to execute
    // while counter is a global variable to keep track of what card to execute
    private ProgramCard[] program;

    private Game game;
    private MapProperties props;

    // the direction the robot is facing
    private Direction dir;

    // the TiledMap layer of the robot, texture-spritesheet and position.
    private TiledMapTileLayer layer;
    private Cell[] textures;
    private Vector2 position;

    // backupMemory is the position where the robot starts, and if he gets a flag,
    // the flags position is now the new position of the robots backupMemory.
    private Vector2 backupMemory;

    // Each robot starts out with 3 lifeTokens, where one token is subtracted
    // each time a robot dies (is killed or falls down a hole/pit).
    private int lifeTokens = 3;

    // Each player/robot starts with 10 damageMarkers, which represents health points.
    private int damageMarkers = 10;

    // Each player has one shot each "round"/phase.
    private boolean hasFired = false;

    // Each robot can search only once each "round"/phase.
    private boolean hasSearched = false;

    // The amount of damage a robots weapon takes.
    private int power = 1;

    /**
     * The Robot constructor
     * @param game takes the Game object the robot is instantiated from
     * @param id the desired identifier for the robot
     */
    public Robot(Game game, int id, Vector2 startPos) {
        this.game = game;
        this.props = game.getMap().getProperties();
        this.dir = Direction.NORTH;
        this.position = startPos;
        this.id = id;

        this.backupMemory = startPos.cpy();

        int tileWidth = props.get("tilewidth", Integer.class);
        int tileHeight = props.get("tileheight", Integer.class);

        // retrieve the layer
        layer = (TiledMapTileLayer) game.getMap().getLayers().get("player_" + id);

        // load the textures
        Texture texture = new Texture("assets/players_300x300.png");
        TextureRegion[][] textureSplit = TextureRegion.split(texture, tileWidth, tileHeight);

        // store the textures
        textures = new Cell[4];
        textures[0] = new Cell().setTile(new StaticTiledMapTile(textureSplit[0][0]));
        textures[1] = new Cell().setTile(new StaticTiledMapTile(textureSplit[0][1]));
        textures[2] = new Cell().setTile(new StaticTiledMapTile(textureSplit[0][2]));
        textures[3] = new Cell().setTile(new StaticTiledMapTile(textureSplit[0][3]));

        // initialise the robots texture given the id
        layer.setCell((int) getPositionX(), (int) getPositionY(), textures[id]);
    }

    /**
     * Update the robots texture, rotation and position
     */
    public void update() {
        // the NORMAL-texture is currently the only one being used
        Cell cell = textures[id];

        // set rotation according to direction
        if (dir == Direction.NORTH) {
            cell.setRotation(0);
        } else if (dir == Direction.EAST) {
            cell.setRotation(3);
        } else if (dir == Direction.SOUTH) {
            cell.setRotation(2);
        } else {
            cell.setRotation(1);
        }

        // update the tiled-map
        layer.setCell((int) getPositionX(), (int) getPositionY(), cell);
    }

    /**
     * Move the robot a given distance
     * @param distance how many tiles to move the robot
     */
    public void move(int distance) {
        move(distance, null);
    }

    /**
     * Move the robot a given distance in a given direction
     * @param distance how many tiles to move the robot
     * @param dir the direction to move the robot
     */
    public void move(int distance, Direction dir) {
        Direction direction;

        if (dir != null) direction = dir;
        else direction = this.dir;

        int x = (int) this.getPositionX();
        int y = (int) this.getPositionY();

        switch (direction) {
            case NORTH:
                if (game.moveIsValid(Direction.NORTH, x, y + distance)) {
                    layer.setCell(x, y, null);
                    position.set(x, y + distance);
                    setProperRotation();
                }
                break;
            case EAST:
                if (game.moveIsValid(Direction.EAST, x + distance, y)) {
                    layer.setCell(x, y, null);
                    position.set(x + distance, y);
                    setProperRotation();
                }
                break;
            case SOUTH:
                if (game.moveIsValid(Direction.SOUTH, x, y - distance)) {
                    layer.setCell(x, y, null);
                    position.set(x, y - distance);
                    setProperRotation();
                }
                break;
            case WEST:
                if (game.moveIsValid(Direction.WEST, x - distance, y)) {
                    layer.setCell(x, y, null);
                    position.set(x - distance, y);
                    setProperRotation();
                }
                break;
            default:
                System.err.println("Invalid direction: " + direction + "!");
                System.err.println("Not moving!");
                break;
        }
    }

    /**
     * Set rotation according current tile
     */
    private void setProperRotation() {
        TiledMapTileLayer.Cell cell = ((TiledMapTileLayer) game.getMap().getLayers().get("conveyors")).getCell((int) this.getPositionX(), (int) this.getPositionY());
        if (cell == null) return;

        TiledMapTile tile = cell.getTile();

        if (Boolean.parseBoolean(tile.getProperties().get("rotation").toString()))
            this.setDirection(Direction.lookup(tile.getProperties().get("direction").toString()));
    }

    /**
     * Rotates the robot in 90 degree intervals
     * Only updates the Direction, actual texture-rotation mechanic is handled
     * by the update()-method
     * @param clockwise true if rotation is clockwise, false if counter-clockwise
     * @param numOfRotations number of 90 degree turns
     */
    public void rotate(boolean clockwise, int numOfRotations) {
        for (int i = 0; i < numOfRotations; i++) {
            switch (dir) {
                case NORTH:
                    dir = (clockwise) ? Direction.EAST : Direction.WEST;
                    break;
                case EAST:
                    dir = (clockwise) ? Direction.SOUTH : Direction.NORTH;
                    break;
                case SOUTH:
                    dir = (clockwise) ? Direction.WEST : Direction.EAST;
                    break;
                case WEST:
                    dir = (clockwise) ? Direction.NORTH : Direction.SOUTH;
                    break;
            }
        }
    }
  
    /**
     * Parent method of move() and rotate(), called with a ProgramCard
     * @param programCard the program card to execute
     */
    public void doTurn(ProgramCard programCard) {
        switch (programCard.getType()) {
            case MOVEMENT:
                this.move(((MovementCard) programCard).getDistance());
                break;
            case ROTATION:
                this.rotate(((RotationCard) programCard).isClockwise(), ((RotationCard) programCard).getRotations());
                break;
        }
    }

    /**
     * Set a program for the current round
     * @param program the input program to run on the robot
     */
    public void setProgram(ProgramCard[] program) {
        if (program.length != 5) throw new IllegalArgumentException("Program must be of length 5");
        this.program = Arrays.copyOf(program, 5);
    }

    /**
     * Get the number of life tokes of the robot
     * @return the number of life tokens
     */
    public int getLifeTokens() {
        return lifeTokens;
    }

    /**
     * Get the number of damage markers of the robot
     * @return the number of damage markers
     */
    public int getDamageMarkers() {
        return damageMarkers;
    }

    /**
     * Get the backup memory position
     * @return the Vector2 backup memory
     */
    public Vector2 getBackupMemory() {
        return backupMemory;
    }

    /**
     * Set the position of the robot
     * @param position the desired position
     */
    public void setPosition(Vector2 position) {
        this.position = position;
    }

    /**
     * Checks to see if robot has any lifeToken left, if so,
     * removes 1 lifeToken, and then respawn player/robot at backupMemory.
     * If the player/robot has no lifeTokens left, he/she is removed from the board entirely.
     */
    public void die(){
        // Moves player/robot back to backupMemory and restores damageMarkers.
        this.lifeTokens--;
        this.position = backupMemory;
        this.damageMarkers = 10;
        if (this.lifeTokens <= 0){
            // TODO : Remove/dispose robots, that have zero lifeTokens and zero damageMarkers, from the game.
        }
    }

    /**
     * Get the number of flags collected by the robot
     * @return number of flags collected
     */
    public int getFlagsCollected() {
        return flagsCollected;
    }

    /**
     * Set the number of flags collected by the robot
     * @param flagsCollected number of flags
     */
    public void setFlagsCollected(int flagsCollected) {
        this.flagsCollected = flagsCollected;
    }

    /**
     * Get the current program of the robot
     * @return the current program
     */
    public ProgramCard[] getProgram() {
        return program;
    }

    /**
     * Get the current position of the robot
     * @return the current position
     */
    public Vector2 getPosition() {
        return this.position;
    }

    /**
     * Get the x-coordinate of the robot
     * @return the robots x-position
     */
    public float getPositionX() {
        return position.x;
    }

    /**
     * Get the y-coordinate of the robot
     * @return the robots y-position
     */
    public float getPositionY() {
        return position.y;
    }

    /**
     * Get the direction the robot is currently facing
     * @return the current direction
     */
    public Direction getDirection() {
        return dir;
    }

    /**
     * Set the direction of the robot
     * @param dir the desired direction
     */
    public void setDirection(Direction dir) {
        this.dir = dir;
    }

    /**
     * Get the robots identifier
     * @return the id of the robot
     */
    public int getId() {
        return id;
    }

    /**
     * Get the TiledMapTileLayer of the robot
     * @return
     */
    public TiledMapTileLayer getLayer() {
        return layer;
    }

    /**
     * Get the boolean value hasFired of the robot.
     * @return hasFired.
     */
    public boolean getHasFired() {
        return hasFired;
    }

    /**
     * Set hasFired to true or false.
     * @param hasFired
     */
    public void setHasFired(boolean hasFired) {
        this.hasFired = hasFired;
    }

    /**
     * Set hasSearched to true or false.
     * @param hasSearched
     */
    public void setHasSearched(boolean hasSearched) {
        this.hasSearched = hasSearched;
    }

    /**
     * This is called on a robot that is taking damage.
     * @param power is the amount of damage taken.
     */
    public void handleDamage(int power){
        this.damageMarkers -= power;
    }

    /**
     * This method is called when the robots shoot.
     */
    public void shootLaser() {
        int x = (int) this.getPositionX();
        int y = (int) this.getPositionY();
        // Check if robot hasn't fired this "round"/phase.
        if(!this.hasFired){
            // Checks which direction this robot is facing.
            if (this.getDirection() == Direction.NORTH) {
                //Checks how many cells are left on the board from the robot to the edge of the board
                for (int i = 0; i < (game.getProps().get("height", Integer.class) - (y - 1)); i++) {
                    // See if this robot has already searched or shot this round.
                    if(!this.hasSearched && !this.hasFired){
                        // See if any of the cells have a robot on it.
                        if (game.cellIsOccupied(x, y + i + 1)) {
                            // See which player/robot is occupying that cell and cause damage on it, plus set hasFired to true on the robot doing the shooting.
                            Robot robot = game.getEnemyRobot(x, y + i + 1);
                            robot.handleDamage(power);
                            this.hasFired = true;
                            this.setHasSearched(true);
                            System.out.println("The " + this.toString() + " robot, at pos: " + this.getPosition() + ", shot the " + robot.toString() + " robot, at pos: " + robot.getPosition() + ", while facing " + this.getDirection() + ".");
                        }
                    }
                }
                // After searching set hasSearched to true.
                this.setHasSearched(true);
            }
            else if (this.getDirection() == Direction.SOUTH) {
                for (int i = 0; i < game.getProps().get("height", Integer.class) - (game.getProps().get("height", Integer.class) - (y + 1)); i++){
                    if(!this.hasSearched && !this.hasFired){
                        if (game.cellIsOccupied(x, y - i - 1)){
                            Robot robot = game.getEnemyRobot(x, y - i - 1);
                            robot.handleDamage(power);
                            this.hasFired = true;
                            this.setHasSearched(true);
                            System.out.println("The " + this.toString() + " robot, at pos: " + this.getPosition() + ", shot the " + robot.toString() + " robot, at pos: " + robot.getPosition() + ", while facing " + this.getDirection() + ".");
                        }
                    }
                }
                this.setHasSearched(true);
            }
            else if (this.getDirection() == Direction.EAST) {
                for (int i = 0; i < (game.getProps().get("width", Integer.class) - (x + 1)); i++) {
                    if(!this.hasSearched && !this.hasFired){
                        System.out.println("i: " + i);
                        if (game.cellIsOccupied(x + i + 1, y)) {
                            System.out.println("i2: "+ i);
                            System.out.println("x: " + x);
                            Robot robot = game.getEnemyRobot(x + i + 1, y);
                            robot.handleDamage(power);
                            this.hasFired = true;
                            this.setHasSearched(true);
                            System.out.println("The " + this.toString() + " robot, at pos: " + this.getPosition() + ", shot the " + robot.toString() + " robot, at pos: " + robot.getPosition() + ", while facing " + this.getDirection() + ".");
                        }
                    }
                }
                this.setHasSearched(true);
            }
            else if (this.getDirection() == Direction.WEST) {
                for (int i = 0; i < game.getProps().get("width", Integer.class) - (game.getProps().get("width", Integer.class) - (x + 1)); i++){
                    if(!this.hasSearched && !this.hasFired){
                        if (game.cellIsOccupied(x - i - 1, y)){
                            Robot robot = game.getEnemyRobot(x - i - 1, y);
                            robot.handleDamage(power);
                            this.hasFired = true;
                            this.setHasSearched(true);
                            System.out.println("The " + this.toString() + " robot, at pos: " + this.getPosition() + ", shot the " + robot.toString() + " robot, at pos: " + robot.getPosition() + ", while facing " + this.getDirection() + ".");
                        }
                    }
                }
                this.setHasSearched(true);
            }
        }
    }

    @Override
    public String toString() {
        switch (id) {
            case 0:
                return "Red";
            case 1:
                return "Green";
            case 2:
                return "Yellow";
            case 3:
                return "Cyan";
            default:
                return "" + id;
        }
    }
}