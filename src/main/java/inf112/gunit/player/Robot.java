package inf112.gunit.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import inf112.gunit.board.Direction;
import inf112.gunit.main.Main;
import inf112.gunit.player.card.MovementCard;
import inf112.gunit.player.card.ProgramCard;
import inf112.gunit.player.card.RotationCard;
import inf112.gunit.screens.Game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * The Robot class is used to perform all kinds of
 * robot mechanics.
 */
public class Robot extends Sprite {

    public int flagsCollected = 0;

    private final int id;

    // these to are for testing, and (probably) wont be used in the actual game
    // program stores a program to execute
    // while counter is a global variable to keep track of what card to execute
    private ProgramCard[] program;

    private ArrayList<ProgramCard> cardDeck = new ArrayList<>();
    private ArrayList<ProgramCard> programBuffer = new ArrayList<>();
    public boolean isDonePicking = false;
  
    private Vector2 prevPos;

    private final Game game;
    private MapProperties props;

    // the direction the robot is facing
    private Direction dir;

    // the TiledMap layer of the robot, texture-spritesheet and position.
    private final TiledMapTileLayer layer;
    private final TextureRegion[][] textureSplit;
    private Vector2 position;

    private boolean isMoving = false;
    private boolean isRotating = false;
    private int animationTick = 0;
    private int animationTileNum;
    private Direction animationDir;

    // backupMemory is the position where the robot starts, and if he gets a flag,
    // the flags position is now the new position of the robots backupMemory.
    private Vector2 backupMemory;

    // Each robot starts out with 3 lifeTokens, where one token is subtracted
    // each time a robot dies (is killed or falls down a hole/pit).
    private int lifeTokens = 3;

    // Each player/robot starts with 0 damageMarkers, which represents health points.
    private int damageMarkers = 0;

    // Each player has one shot each "round"/phase.
    private boolean hasFired = false;

    // Each robot can search only once each "round"/phase.
    private boolean hasSearched = false;

    private boolean wantsToPowerDown = false;
    private boolean poweredDown = false;
    
    private final Sound slideSound;
    private final Sound damageSound;

    // Boolean for if the robot is dead.
    private boolean dead = false;

    /**
     * The Robot constructor
     * @param game takes the Game object the robot is instantiated from
     * @param id the desired identifier for the robot
     */
    public Robot(Game game, int id, Vector2 startPos) {
        super(TextureRegion.split(new Texture("assets/players_300x300.png"), 300, 300)[0][id]);

        int tileWidth = game.getMap().getProperties().get("tilewidth", Integer.class);
        int tileHeight = game.getMap().getProperties().get("tileheight", Integer.class);

        setScale((float) tileWidth/Main.HEIGHT);

        this.game = game;
        this.dir = Direction.NORTH;
        this.position = startPos;
        this.prevPos = new Vector2(100, 100);
        this.id = id;

        this.backupMemory = startPos.cpy();

        // retrieve the layer
        layer = (TiledMapTileLayer) game.getMap().getLayers().get("player_" + id);

        // load the textures
        textureSplit = TextureRegion.split(new Texture("assets/players_300x300.png"), tileWidth, tileHeight);

        slideSound = Gdx.audio.newSound(Gdx.files.internal("assets/sounds/skrrt.wav"));
        damageSound = Gdx.audio.newSound(Gdx.files.internal("assets/sounds/ouf.wav"));
    }

    /**
     * Set the sprites position according to x and y position
     * @param x the x position
     * @param y the y position
     */
    private void setGridPos(float x, float y) {
        this.setPosition(x, y);
        this.setX(this.getPositionX() * game.tileScale - 109);
        this.setY(this.getPositionY() * game.tileScale - 109);
    }

    /**
     * Animate one tick if robot is moving
     * @param numTiles number of tiles to animate
     * @param moveDir direction which robot is moving
     */
    private void moveAnimation(int numTiles, Direction moveDir) {
        if (moveDir == Direction.NORTH) {
            setY(getY() + animationTick * game.tileScale / (100 / (float) numTiles));
        } else if (moveDir == Direction.EAST) {
            setX(getX() + animationTick * game.tileScale / (100 / (float) numTiles));
        } else if (moveDir == Direction.SOUTH) {
            setY(getY() - animationTick * game.tileScale / (100 / (float) numTiles));
        } else if (moveDir == Direction.WEST) {
            setX(getX() - animationTick * game.tileScale / (100 / (float) numTiles));
        } else {
            System.err.println("UNKNOWN DIRECTION: " + dir);
        }
    }

    /**
     * Animate on tick if robot is rotating
     * @param animationDir direction from
     */
    private void rotationAnimation(Direction animationDir) {

        if (animationDir == Direction.NORTH) {
            setRotation(getRotation() - (Direction.calcDegDiff(Direction.NORTH, dir) / (float) 15));
        } else if (animationDir == Direction.EAST) {
            setRotation(getRotation() - (Direction.calcDegDiff(Direction.EAST, dir) / (float) 15));
        } else if (animationDir == Direction.SOUTH) {
            setRotation(getRotation() - (Direction.calcDegDiff(Direction.SOUTH, dir) / (float) 15));
        } else if (animationDir == Direction.WEST) {
            setRotation(getRotation() - (Direction.calcDegDiff(Direction.WEST, dir) / (float) 15));
        } else {
            System.err.println("UNKNOWN DIRECTION: " + animationDir);
        }
    }

    /**
     * Animate one tick
     */
    private void animate() {
        int ANIMATION_DELTA = 15;
        if (animationTick == ANIMATION_DELTA) {
            isMoving = false;
            isRotating = false;
            this.animationTick = 0;
            return;
        }

        if (isRotating) rotationAnimation(animationDir);
        if (isMoving) moveAnimation(animationTileNum, animationDir);

        animationTick++;
    }

    /**
     * Update the robots texture, rotation and position
     * Also animate the robot
     */
    public void update() {
        if (isRotating) this.animate();
        if (isMoving) this.animate();
        else this.setGridPos(this.getPositionX(), this.getPositionY());
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
        Random rand = new Random();
        
        if (rand.nextInt(50) == 49)
            slideSound.play(1.0f);

        if (dir != null)
            direction = dir;
        else
            direction = this.dir;

        int x;
        int y;

        animationTileNum = 0;
        animationDir = direction;

        if (direction == Direction.NORTH) {
            for (int i = 1; i <= distance; i++) {
                x = (int) this.getPositionX();
                y = (int) this.getPositionY();

                if (game.moveIsValid(this, Direction.NORTH, x, y + 1)) {
                    position.set(x, y + 1);
                    setProperRotation();
                    isMoving = true;
                    animationTileNum++;
                    if (game.fallIntoHole(this)) break;
                }
                else
                    break;
            }
        }
        else if (direction == Direction.EAST) {
            for (int i = 1; i <= distance; i++) {
                x = (int) this.getPositionX();
                y = (int) this.getPositionY();

                if (game.moveIsValid(this, Direction.EAST, x + 1, y)) {
                    position.set(x + 1, y);
                    setProperRotation();
                    isMoving = true;
                    animationTileNum++;
                    if (game.fallIntoHole(this)) break;
                }
                else
                    break;
            }
        }
        else if (direction == Direction.SOUTH) {
            for (int i = 1; i <= distance; i++) {
                x = (int) this.getPositionX();
                y = (int) this.getPositionY();

                if (game.moveIsValid(this, Direction.SOUTH, x, y - 1)) {
                    position.set(x, y - 1);
                    setProperRotation();
                    isMoving = true;
                    animationTileNum++;
                    if (game.fallIntoHole(this)) break;
                }
                else
                    break;
            }
        }
        else if (direction == Direction.WEST) {
            for (int i = 1; i <= distance; i++) {
                x = (int) this.getPositionX();
                y = (int) this.getPositionY();

                if (game.moveIsValid(this, Direction.WEST, x - 1, y)) {
                    position.set(x - 1, y);
                    setProperRotation();
                    isMoving = true;
                    animationTileNum++;
                    if (game.fallIntoHole(this)) break;
                }
                else
                    break;
            }
        }
        else {
            System.err.println("Invalid direction: " + direction + "!");
            System.err.println("Not moving!");
        }
    }

    /**
     * Set rotation according current tile
     */
    private void setProperRotation() {
        TiledMapTileLayer.Cell cell = ((TiledMapTileLayer) game.getMap().getLayers().get("conveyors")).getCell((int) this.getPositionX(), (int) this.getPositionY());
        if (cell == null) return;

        TiledMapTile tile = cell.getTile();

        if (Boolean.parseBoolean(tile.getProperties().get("rotation").toString())) {
            animationDir = dir;

            dir = Direction.lookup(tile.getProperties().get("direction").toString());
            isRotating = true;
        }
    }

    /**
     * Rotates the robot in 90 degree intervals
     * Only updates the Direction, actual texture-rotation mechanic is handled
     * by the update()-method
     * @param clockwise true if rotation is clockwise, false if counter-clockwise
     * @param numOfRotations number of 90 degree turns
     */
    public void rotate(boolean clockwise, int numOfRotations) {
        animationDir = dir;

        for (int i = 0; i < numOfRotations; i++) {
            switch (dir) {
                case NORTH:
                    dir = (clockwise) ? Direction.EAST : Direction.WEST;
                    isRotating = true;
                    break;
                case EAST:
                    dir = (clockwise) ? Direction.SOUTH : Direction.NORTH;
                    isRotating = true;
                    break;
                case SOUTH:
                    dir = (clockwise) ? Direction.WEST : Direction.EAST;
                    isRotating = true;
                    break;
                case WEST:
                    dir = (clockwise) ? Direction.NORTH : Direction.SOUTH;
                    isRotating = true;
                    break;
                default:
                    isRotating = false;
                    dir = Direction.NORTH;
                    break;
            }
        }
    }
  
    /**
     * Parent method of move() and rotate(), called with a ProgramCard
     * @param programCard the program card to execute
     */
    public void doTurn(ProgramCard programCard) {
        if (!poweredDown) {
            switch (programCard.getType()) {
                case MOVEMENT:
                    this.move(((MovementCard) programCard).getDistance());
                    break;
                case ROTATION:
                    this.rotate(((RotationCard) programCard).isClockwise(), ((RotationCard) programCard).getRotations());
                    break;
            }
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
     * Add a card to the program buffer
     * @param card the card to add
     */
    public void addBufferCard(ProgramCard card) {
        programBuffer.add(card);
    }

    /**
     * Get the program buffer
     * @return the program buffer
     */
    public ArrayList<ProgramCard> getProgramBuffer() {
        return programBuffer;
    }

    /**
     * At the start of each phase, deal random program cards to each player
     */
    public void dealCards() {
        Random r = new Random();
        cardDeck = new ArrayList<>();
        programBuffer = new ArrayList<>();

        //compute number of cards to be dealt based on damage tokens
        int numOfCards = (damageMarkers >= 4) ? 5 : 9 - damageMarkers;
        for (int i = 0; i < numOfCards; i++) {
            int isMoveCard = r.nextInt(3); // 67% chance of getting a movement card per card

            if (isMoveCard > 0) {
                int distance = r.nextInt(3) + 1;
                cardDeck.add(new MovementCard(distance));
            } else {
                boolean clockwise = r.nextBoolean();
                int rotations = r.nextInt(2) + 1;
                cardDeck.add(new RotationCard(rotations, clockwise));
            }
        }
    }

    /**
     * The AI used when the player selects 'easy'.
     * This function just returns random moves.
     *
     * @param flagPosList list of where all the flags are
     * @return a list of moves the robot can execute
     */
    public ArrayList<ProgramCard> easyAI(ArrayList<Vector2> flagPosList) {
        ArrayList<ProgramCard> cards = new ArrayList<>();
        for (ProgramCard c : cardDeck) {
            if (cards.size() < 5)
                cards.add(c);
        }
        
        return cards;
    }

    /**
     * The AI used when the player selects 'hard'.
     * Uses the 'pathTo()' method to recursively find the best path.
     *
     * @param flagPosList a list of where all the flags are
     * @return a list of moves the robot should execute
     */
    public ArrayList<ProgramCard> hardAI(ArrayList<Vector2> flagPosList) {
        if (flagsCollected == 4)
            return new ArrayList<ProgramCard>();
        
        ArrayList<ProgramCard> newCards = pathTo(new ArrayList<ProgramCard>(), flagPosList, position, flagPosList.get(flagsCollected), dir);
        if (position.equals(prevPos)) {
            prevPos = position.cpy();

            return addSomeSpice(newCards);
        }

        prevPos = position.cpy();

        return newCards;
    }

    /**
     * Takes a list of program cards and adds in some random program cards.
     * This is used in the hard AI so it doesn't get stuck.
     *
     * @param cards list of cards to spice up
     * @return a new list with some of the program cards replaced with random ones
     */
    public ArrayList<ProgramCard> addSomeSpice(ArrayList<ProgramCard> cards) {
        Random rand = new Random();
        cards.set(rand.nextInt(cards.size()), new MovementCard(2));
        cards.set(rand.nextInt(cards.size()), new RotationCard(rand.nextInt(3) + 1, rand.nextBoolean()));

        return cards;
    }

    /**
     *
     * @param currentPath the current path the AI has found
     * @param flagPosList list of all the flag positions
     * @param from where the AI is trying to go from
     * @param to where the AI is trying to go to
     * @param curDir the direction the robot is currently facing in the current path
     * @return a list of program cards the AI executes
     */
    public ArrayList<ProgramCard> pathTo(ArrayList<ProgramCard> currentPath, ArrayList<Vector2> flagPosList, Vector2 from, Vector2 to, Direction curDir) {
        if (currentPath.size() == 5) {
            return currentPath;
        }
        
        Random rand = new Random();
        float deltaX = from.x - to.x;
        float deltaY = from.y - to.y;
        // If the AI should prioritize getting closest to the x-coordinate of the next flag, or the y-coordinate
        boolean xAxisFirst = rand.nextBoolean();

        if (xAxisFirst) {
            if (deltaX > 0) {
                if (curDir == Direction.WEST) {
                    TiledMapTileLayer.Cell convCellOne = ((TiledMapTileLayer) game.getMap().getLayers().get("conveyors")).getCell((int) from.x - 1, (int) from.y);
                    TiledMapTileLayer.Cell convCellTwo = ((TiledMapTileLayer) game.getMap().getLayers().get("conveyors")).getCell((int) from.x - 2, (int) from.y);
                    int extraDist = getExtraDist(curDir, convCellOne, convCellTwo);

                    TiledMapTileLayer.Cell wallCellOne = ((TiledMapTileLayer) game.getMap().getLayers().get("walls")).getCell((int) from.x, (int) from.y + 1);
                    TiledMapTileLayer.Cell wallCellTwo = ((TiledMapTileLayer) game.getMap().getLayers().get("walls")).getCell((int) from.x, (int) from.y + 2);

                    if (wallCellOne != null) {
                        Direction cellDir = Direction.lookup(wallCellOne.getTile().getProperties().get("direction").toString());
                        if (cellDir == curDir || Direction.flip(cellDir) == curDir) {
                            currentPath.add(new RotationCard(1, true));
                            return pathTo(currentPath, flagPosList, from, to, Direction.getClockwiseDirection(curDir));
                        }
                    } else if (wallCellTwo != null) {
                        Direction cellDir = Direction.lookup(wallCellTwo.getTile().getProperties().get("direction").toString());
                        if (cellDir == curDir || Direction.flip(cellDir) == curDir) {
                            currentPath.add(new RotationCard(1, true));
                            return pathTo(currentPath, flagPosList, from, to, Direction.getClockwiseDirection(curDir));
                        }
                    }

                    if (deltaX >= 3) {
                        currentPath.add(new MovementCard(3));
                        return pathTo(currentPath, flagPosList, new Vector2(from.x - 3, from.y), to, curDir);
                    } else if (deltaX >= 2) {
                        currentPath.add(new MovementCard(2 + extraDist));
                        return pathTo(currentPath, flagPosList, new Vector2(from.x - 2 - extraDist, from.y), to, curDir);
                    } else if (deltaX >= 1) {
                        currentPath.add(new MovementCard(1 + extraDist));
                        return pathTo(currentPath, flagPosList, new Vector2(from.x - 1 - extraDist, from.y), to, curDir);
                    }
                } else if (curDir == Direction.SOUTH) {
                    currentPath.add(new RotationCard(1, true));
                    return pathTo(currentPath, flagPosList, from, to, Direction.getClockwiseDirection(curDir));
                } else if (curDir == Direction.EAST) {
                    currentPath.add(new RotationCard(2, true));
                    return pathTo(currentPath, flagPosList, from, to, Direction.flip(curDir));
                } else if (curDir == Direction.NORTH) {
                    currentPath.add(new RotationCard(1, false));
                    return pathTo(currentPath, flagPosList, from, to, Direction.getAntiClockwiseDirection(curDir));
                }
            } else if (deltaX < 0) {
                if (curDir == Direction.EAST) {
                    TiledMapTileLayer.Cell convCellOne = ((TiledMapTileLayer) game.getMap().getLayers().get("conveyors")).getCell((int) from.x + 1, (int) from.y);
                    TiledMapTileLayer.Cell convCellTwo = ((TiledMapTileLayer) game.getMap().getLayers().get("conveyors")).getCell((int) from.x + 2, (int) from.y);
                    int extraDist = getExtraDist(curDir, convCellOne, convCellTwo);

                    TiledMapTileLayer.Cell wallCellOne = ((TiledMapTileLayer) game.getMap().getLayers().get("walls")).getCell((int) from.x, (int) from.y + 1);
                    TiledMapTileLayer.Cell wallCellTwo = ((TiledMapTileLayer) game.getMap().getLayers().get("walls")).getCell((int) from.x, (int) from.y + 2);

                    if (wallCellOne != null) {
                        Direction cellDir = Direction.lookup(wallCellOne.getTile().getProperties().get("direction").toString());
                        if (cellDir == curDir || Direction.flip(cellDir) == curDir) {
                            currentPath.add(new RotationCard(1, true));
                            return pathTo(currentPath, flagPosList, from, to, Direction.getClockwiseDirection(curDir));
                        }
                    } else if (wallCellTwo != null) {
                        Direction cellDir = Direction.lookup(wallCellTwo.getTile().getProperties().get("direction").toString());
                        if (cellDir == curDir || Direction.flip(cellDir) == curDir) {
                            currentPath.add(new RotationCard(1, true));
                            return pathTo(currentPath, flagPosList, from, to, Direction.getClockwiseDirection(curDir));
                        }
                    }

                    if (deltaX <= -3) {
                        currentPath.add(new MovementCard(3));
                        return pathTo(currentPath, flagPosList, new Vector2(from.x + 3, from.y), to, curDir);
                    } else if (deltaX <= -2) {
                        currentPath.add(new MovementCard(2 + extraDist));
                        return pathTo(currentPath, flagPosList, new Vector2(from.x + 2 + extraDist, from.y), to, curDir);
                    } else if (deltaX <= -1) {
                        currentPath.add(new MovementCard(1 + extraDist));
                        return pathTo(currentPath, flagPosList, new Vector2(from.x + 1 + extraDist, from.y), to, curDir);
                    }
                } else if (curDir == Direction.SOUTH) {
                    currentPath.add(new RotationCard(1, false));
                    return pathTo(currentPath, flagPosList, from, to, Direction.getAntiClockwiseDirection(curDir));
                } else if (curDir == Direction.WEST) {
                    currentPath.add(new RotationCard(2, true));
                    return pathTo(currentPath, flagPosList, from, to, Direction.flip(curDir));
                } else if (curDir == Direction.NORTH) {
                    currentPath.add(new RotationCard(1, true));
                    return pathTo(currentPath, flagPosList, from, to, Direction.getClockwiseDirection(curDir));
                }
            }
        }
        else {
            if (deltaY > 0) {
                if (curDir == Direction.SOUTH) {
                    TiledMapTileLayer.Cell convCellOne = ((TiledMapTileLayer) game.getMap().getLayers().get("conveyors")).getCell((int) from.x, (int) from.y - 1);
                    TiledMapTileLayer.Cell convCellTwo = ((TiledMapTileLayer) game.getMap().getLayers().get("conveyors")).getCell((int) from.x, (int) from.y - 2);
                    int extraDist = getExtraDist(curDir, convCellOne, convCellTwo);

                    TiledMapTileLayer.Cell wallCellOne = ((TiledMapTileLayer) game.getMap().getLayers().get("walls")).getCell((int) from.x, (int) from.y + 1);
                    TiledMapTileLayer.Cell wallCellTwo = ((TiledMapTileLayer) game.getMap().getLayers().get("walls")).getCell((int) from.x, (int) from.y + 2);

                    if (wallCellOne != null) {
                        Direction cellDir = Direction.lookup(wallCellOne.getTile().getProperties().get("direction").toString());
                        if (cellDir == curDir || Direction.flip(cellDir) == curDir) {
                            currentPath.add(new RotationCard(1, true));
                            return pathTo(currentPath, flagPosList, from, to, Direction.getClockwiseDirection(curDir));
                        }
                    } else if (wallCellTwo != null) {
                        Direction cellDir = Direction.lookup(wallCellTwo.getTile().getProperties().get("direction").toString());
                        if (cellDir == curDir || Direction.flip(cellDir) == curDir) {
                            currentPath.add(new RotationCard(1, true));
                            return pathTo(currentPath, flagPosList, from, to, Direction.getClockwiseDirection(curDir));
                        }
                    }

                    if (deltaY >= 3) {
                        currentPath.add(new MovementCard(3));
                        return pathTo(currentPath, flagPosList, new Vector2(from.x, from.y - 3), to, curDir);
                    } else if (deltaY <= 2) {
                        currentPath.add(new MovementCard(2 + extraDist));
                        return pathTo(currentPath, flagPosList, new Vector2(from.x, from.y - 2 - extraDist), to, curDir);
                    } else if (deltaY >= 1) {
                        currentPath.add(new MovementCard(1 + extraDist));
                        return pathTo(currentPath, flagPosList, new Vector2(from.x, from.y - 1 - extraDist), to, curDir);
                    }
                } else if (curDir == Direction.WEST) {
                    currentPath.add(new RotationCard(1, false));
                    return pathTo(currentPath, flagPosList, from, to, Direction.getAntiClockwiseDirection(curDir));
                } else if (curDir == Direction.NORTH) {
                    currentPath.add(new RotationCard(2, true));
                    return pathTo(currentPath, flagPosList, from, to, Direction.flip(curDir));
                } else if (curDir == Direction.EAST) {
                    currentPath.add(new RotationCard(1, true));
                    return pathTo(currentPath, flagPosList, from, to, Direction.getClockwiseDirection(curDir));
                }
            } else if (deltaY < 0) {
                if (curDir == Direction.NORTH) {
                    TiledMapTileLayer.Cell convCellOne = ((TiledMapTileLayer) game.getMap().getLayers().get("conveyors")).getCell((int) from.x, (int) from.y + 1);
                    TiledMapTileLayer.Cell convCellTwo = ((TiledMapTileLayer) game.getMap().getLayers().get("conveyors")).getCell((int) from.x, (int) from.y + 2);
                    int extraDist = getExtraDist(curDir, convCellOne, convCellTwo);

                    TiledMapTileLayer.Cell wallCellOne = ((TiledMapTileLayer) game.getMap().getLayers().get("walls")).getCell((int) from.x, (int) from.y + 1);
                    TiledMapTileLayer.Cell wallCellTwo = ((TiledMapTileLayer) game.getMap().getLayers().get("walls")).getCell((int) from.x, (int) from.y + 2);

                    if (wallCellOne != null) {
                        Direction cellDir = Direction.lookup(wallCellOne.getTile().getProperties().get("direction").toString());
                        if (cellDir == curDir || Direction.flip(cellDir) == curDir) {
                            currentPath.add(new RotationCard(1, true));
                            return pathTo(currentPath, flagPosList, from, to, Direction.getClockwiseDirection(curDir));
                        }
                    } else if (wallCellTwo != null) {
                        Direction cellDir = Direction.lookup(wallCellTwo.getTile().getProperties().get("direction").toString());
                        if (cellDir == curDir || Direction.flip(cellDir) == curDir) {
                            currentPath.add(new RotationCard(1, true));
                            return pathTo(currentPath, flagPosList, from, to, Direction.getClockwiseDirection(curDir));
                        }
                    }

                    if (deltaY <= -3) {
                        currentPath.add(new MovementCard(3));
                        return pathTo(currentPath, flagPosList, new Vector2(from.x, from.y + 3), to, curDir);
                    } else if (deltaY <= -2) {
                        currentPath.add(new MovementCard(2 + extraDist));
                        return pathTo(currentPath, flagPosList, new Vector2(from.x, from.y + 2 + extraDist), to, curDir);
                    } else if (deltaY <= -1) {
                        currentPath.add(new MovementCard(1 + extraDist));
                        return pathTo(currentPath, flagPosList, new Vector2(from.x, from.y + 1 + extraDist), to, curDir);
                    }
                } else if (curDir == Direction.EAST) {
                    currentPath.add(new RotationCard(1, false));
                    return pathTo(currentPath, flagPosList, from, to, Direction.getAntiClockwiseDirection(curDir));
                } else if (curDir == Direction.SOUTH) {
                    currentPath.add(new RotationCard(2, true));
                    return pathTo(currentPath, flagPosList, from, to, Direction.flip(curDir));
                } else if (curDir == Direction.WEST) {
                    currentPath.add(new RotationCard(1, true));
                    return pathTo(currentPath, flagPosList, from, to, Direction.getClockwiseDirection(curDir));
                }
            }
        }

        // If a flag has been collected in the middle of the path, find the next flag
        if (flagPosList.get(flagsCollected) != to) {
            return pathTo(currentPath, flagPosList, from, flagPosList.get(flagsCollected), dir);
        }
        
        int rem = 5 - currentPath.size();
        for (int i = 0; i < rem; i++) {
            currentPath.add(cardDeck.get(i));
        }
        
        return currentPath;
    }

    /**
     * Determines if a conveyor belt is facing the robot.
     * If so, return the extra distance the robot should
     * go to not get pushed by the conveyor belt.
     *
     * @param curDir the current direction of the robot
     * @param convCellOne the conveyor cell one cell ahead of the robot
     * @param convCellTwo the conveyor cell two cells ahead
     * @return 1 if a conveyor is facing the robot, 0 otherwise
     */
    private int getExtraDist(Direction curDir, TiledMapTileLayer.Cell convCellOne, TiledMapTileLayer.Cell convCellTwo) {
        if (convCellOne != null) {
            Direction cellOneDir = Direction.lookup(convCellOne.getTile().getProperties().get("direction").toString());
            if (Direction.flip(cellOneDir) == curDir)
                return 1;
        } else if (convCellTwo != null) {
            Direction cellTwoDir = Direction.lookup(convCellTwo.getTile().getProperties().get("direction").toString());
            if (Direction.flip(cellTwoDir) == curDir)
                return 1;
        }
        
        return 0;
    }

    /**
     * Get the robots card deck
     * @return the card deck
     */
    public ArrayList<ProgramCard> getCardDeck() {
        return cardDeck;
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
        damageSound.play(1.0f);

        this.lifeTokens--;
        this.position = backupMemory.cpy();

        dir = Direction.NORTH;
        setRotation(0);

        this.damageMarkers = 0;
        // If robot has zero life, set dead to true and remove it from the board.
        if (this.lifeTokens <= 0){
            this.dead = true;
            this.position.x = -1;
            this.position.y = -id;
        }
    }

    /**
     * Update the powerdown status. Called by the HUD
     */
    public void updatePowerDownDesire() {
        wantsToPowerDown = !wantsToPowerDown;
    }

    /**
     * Get the powerdown status.
     */
    public boolean getPowerDownDesire() {
        return wantsToPowerDown;
    }

    /**
     * Get the power status of the robot.
     */
    public boolean isPoweredDown() {
        return poweredDown;
    }

    /**
     * Set the power status of the robot
     */
    public void setPoweredDown(boolean onOff) {
        poweredDown = onOff;
        if (onOff) {
            this.damageMarkers = 0;
            program = new ProgramCard[]{
                    new MovementCard(1),
                    new MovementCard(1),
                    new MovementCard(1),
                    new MovementCard(1),
                    new MovementCard(1)
            };
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

    public TextureRegion getTextureRegion() {
        return textureSplit[0][id];
    }

    /**
     * Get the TiledMapTileLayer of the robot
     * @return the layer of the robot
     */
    public TiledMapTileLayer getLayer() {
        return layer;
    }

    /**
     * Set hasFired to true or false.
     * @param hasFired boolean
     */
    public void setHasFired(boolean hasFired) {
        this.hasFired = hasFired;
    }

    /**
     * Set hasSearched to true or false.
     * @param hasSearched boolean
     */
    public void setHasSearched(boolean hasSearched) {
        this.hasSearched = hasSearched;
    }

    /**
     * This is called on a robot that is taking damage.
     * @param power is the amount of damage taken.
     */
    public void handleDamage(int power){
        this.damageMarkers += power;
    }

    public void repair() {
        this.damageMarkers = 0;
    }

    /**
     * Get a robots power.
     * @return power
     */
    public int getPower() {
        // The amount of damage a robots weapon takes.
        return 1;
    }

    public void setBackupMemory(Vector2 backupMemory) {
        this.backupMemory = backupMemory;
    }

    /**
     * Get a robots dead.
     * @return dead
     */
    public boolean isDead() {
        return dead;
    }

    /**
     * This method is called when it's time for the robots to shoot.
     */
    public void fire() {
        if (!this.isDead()){
            int x = (int) this.getPositionX();
            int y = (int) this.getPositionY();
            Direction direction = this.getDirection();

            // Check which direction this robot is facing.
            switch (direction){
                case NORTH:
                    //Checks how many cells are left on the board from the robot to the edge of the board
                    for (int i = 0; i < (game.getMap().getProperties().get("height", Integer.class) - (y - 1)); i++) {
                        // See if this robot has already searched or shot this round.
                        if (!this.hasSearched && !this.hasFired) {
                            // Call searchAndDestroy which deals with damage.
                            game.searchAndDestroy(x, y + i + 1, this);
                        }
                    }
                    // After searching set hasSearched to true.
                    this.setHasSearched(true);
                    break;
                case SOUTH:
                    for (int i = 0; i < game.getMap().getProperties().get("height", Integer.class) - (game.getMap().getProperties().get("height", Integer.class) - (y + 1)); i++) {
                        if (!this.hasSearched && !this.hasFired) {
                            game.searchAndDestroy(x, y - i - 1, this);
                        }
                    }
                    this.setHasSearched(true);
                    break;
                case EAST:
                    for (int i = 0; i < (game.getMap().getProperties().get("width", Integer.class) - (x + 1)); i++) {
                        if (!this.hasSearched && !this.hasFired) {
                            game.searchAndDestroy(x + i + 1, y, this);
                        }
                    }
                    this.setHasSearched(true);
                    break;
                case WEST:
                    for (int i = 0; i < game.getMap().getProperties().get("width", Integer.class) - (game.getMap().getProperties().get("width", Integer.class) - (x + 1)); i++) {
                        if (!this.hasSearched && !this.hasFired) {
                            game.searchAndDestroy(x - i - 1, y, this);
                        }
                    }
                    this.setHasSearched(true);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * The color of the robot, used as the "name" of the player
     *
     * @return the name of the player
     */
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
