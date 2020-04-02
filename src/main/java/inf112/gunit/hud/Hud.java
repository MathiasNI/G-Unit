package inf112.gunit.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import inf112.gunit.main.Main;
import inf112.gunit.screens.Game;

public class Hud extends ClickListener implements Disposable {

    private final Game game;
    private final Texture bg = new Texture("assets/program_sheet_bg.png");
    private final Texture DAMAGE_TOKEN_FILLED = new Texture("assets/damage_token.png");
    private final Texture DAMAGE_TOKEN = new Texture("assets/damage_token_grey.png");
    private final Texture LIFE_TOKEN_FILLED = new Texture("assets/life_token.png");
    private final Texture LIFE_TOKEN = new Texture("assets/life_token_grey.png");
    private final Texture POWER_DOWN = new Texture("assets/power_down.png");
    private final Texture POWER_DOWN_GREY = new Texture("assets/power_down_grey.png");
    private final Texture SUBMIT_GREEN = new Texture("assets/submit_green.png");
    private final Texture SUBMIT_GREY = new Texture("assets/submit_grey.png");

    private final int TEXTURE_PADDING = 30;

    private Table damageTokenTable;
    private Table lifeTokenTable;
    private Table cardTable;

    private ImageButton powerDownButton;
    private ImageButton submitButton;

    public Stage stage;

    private Viewport viewport;

    public Hud(SpriteBatch batch, Game game) {
        this.game = game;

        viewport = new FitViewport(Main.WIDTH, Main.HEIGHT, new OrthographicCamera());
        stage = new Stage(viewport, batch);

        Gdx.input.setInputProcessor(stage);

        damageTokenTable = new Table();
        damageTokenTable.top();
        damageTokenTable.setFillParent(true);
        damageTokenTable.setPosition(500, -400);

        lifeTokenTable = new Table();
        lifeTokenTable.top();

        lifeTokenTable.setFillParent(true);
        lifeTokenTable.setPosition(400, -250);

        ImageButton.ImageButtonStyle powerDownStyle = new ImageButton.ImageButtonStyle();
        powerDownStyle.imageUp = new TextureRegionDrawable(new TextureRegion(POWER_DOWN_GREY));
        powerDownStyle.imageChecked = new TextureRegionDrawable(new TextureRegion(POWER_DOWN));
        // TODO: make own texture for this
        powerDownStyle.imageDown = new TextureRegionDrawable(new TextureRegion(POWER_DOWN));
        powerDownStyle.imageOver = new TextureRegionDrawable(new TextureRegion(POWER_DOWN));
        powerDownButton = new ImageButton(powerDownStyle);
        powerDownButton.setPosition(Main.WIDTH - 500 + TEXTURE_PADDING, Main.HEIGHT - POWER_DOWN.getHeight() - TEXTURE_PADDING);
        powerDownButton.addListener(addButtonListener(powerDownButton));
        powerDownButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                System.out.println("powerdown");
            }
        });

        ImageButton.ImageButtonStyle submitStyle = new ImageButton.ImageButtonStyle();
        submitStyle.imageUp = new TextureRegionDrawable(new TextureRegion(SUBMIT_GREY));
        submitStyle.imageChecked = new TextureRegionDrawable(new TextureRegion(SUBMIT_GREEN));
        // TODO: make own texture for this
        submitStyle.imageDown = new TextureRegionDrawable(new TextureRegion(SUBMIT_GREEN));
        submitStyle.imageOver = new TextureRegionDrawable(new TextureRegion(SUBMIT_GREEN));
        submitButton = new ImageButton(submitStyle);
        submitButton.setPosition(Main.WIDTH - 200, 200);
        submitButton.addListener(addButtonListener(submitButton));
        submitButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                System.out.println("submit");
            }
        });

        stage.addActor(damageTokenTable);
        stage.addActor(lifeTokenTable);
        stage.addActor(powerDownButton);
        stage.addActor(submitButton);

        updateDamageTokens();
        updateLifeTokens();
    }

    private ClickListener addButtonListener(final ImageButton button) {
        return new ClickListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                button.setChecked(true);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                button.setChecked(false);
            }
        };
    }

    private void updateDamageTokens() {
        damageTokenTable.reset();
        int damageTokens = game.getPlayerRobot().getDamageMarkers();

        for (int i = 0; i < 10; i++) {
            if (i < damageTokens) damageTokenTable.add(new Image(DAMAGE_TOKEN_FILLED));
            else damageTokenTable.add(new Image(DAMAGE_TOKEN));
            if (damageTokenTable.getCells().size % 5 == 0) damageTokenTable.row();
        }
    }

    private void updateLifeTokens() {
        lifeTokenTable.reset();
        int lifeTokens = game.getPlayerRobot().getLifeTokens();

        for (int i = 0; i < 3; i++) {
            if (i < lifeTokens) lifeTokenTable.add(new Image(LIFE_TOKEN_FILLED));
            else lifeTokenTable.add(new Image(LIFE_TOKEN));
        }

    }

    public void draw() {
        stage.act(Gdx.graphics.getDeltaTime());

        stage.getBatch().begin();
        stage.getBatch().draw(bg, 1000, 0, 500, 1000);
        stage.getBatch().draw(game.getPlayerRobot().getTexture(), Main.WIDTH - 200 - TEXTURE_PADDING, Main.HEIGHT - 200 - TEXTURE_PADDING, 200, 200);
        stage.getBatch().end();

        updateDamageTokens();
        updateLifeTokens();

        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
