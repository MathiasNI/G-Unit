package inf112.gunit.main;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;

public class Game implements ApplicationListener {
    TiledMap tiledMap;
    TiledMapTileLayer Board, Player, Hole, Flag;
    OrthographicCamera camera;
    OrthogonalTiledMapRenderer tilerender;
    TiledMapRenderer tiledMapRenderer;
    Cell playerCell, playerDiedCell, playerWonCell;
    Vector2 playerPosition;

    @Override
    public void create() {
        tiledMap = new TmxMapLoader().load("src/assets/tiles.tmx");
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);
        MapProperties props = tiledMap.getProperties();

        int tileWidth = props.get("tilewidth", Integer.class);
        int tileHeight = props.get("tileheight", Integer.class);
        int mapWidth = props.get("width", Integer.class);
        int mapHeight = props.get("height", Integer.class);

        Board = (TiledMapTileLayer) tiledMap.getLayers().get("Board");

        camera = new OrthographicCamera();
        camera.setToOrtho(false, tileWidth*mapWidth, tileWidth*mapHeight);
        camera.translate(2.5f, 0);
        camera.update();

        tilerender = new OrthogonalTiledMapRenderer(tiledMap, (float) 1/300*300);

        tilerender.setView(camera);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(1, 0, 0, 1);

        tilerender.render();
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }
}
