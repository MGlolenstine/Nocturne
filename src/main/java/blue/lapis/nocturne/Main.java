/*
 * Nocturne
 * Copyright (c) 2015-2016, Lapis <https://github.com/LapisBlue>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package blue.lapis.nocturne;

import blue.lapis.nocturne.gui.io.mappings.MappingsSaveDialogHelper;
import blue.lapis.nocturne.gui.scene.control.WebLink;
import blue.lapis.nocturne.jar.model.ClassSet;
import blue.lapis.nocturne.mapping.MappingContext;
import blue.lapis.nocturne.mapping.io.writer.MappingWriterType;
import blue.lapis.nocturne.util.helper.PropertiesHelper;
import blue.lapis.nocturne.util.helper.SceneHelper;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public class Main extends Application {

    public final boolean testingEnv;

    private static Main instance;

    private static final EventHandler<WindowEvent> CLOSE_HANDLER = event -> {
        try {
            if (MappingsSaveDialogHelper.doDirtyConfirmation()) {
                event.consume();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    };

    private static final Logger LOGGER = Logger.getLogger("Nocturne");
    private static final Logger FERNFLOWER_LOGGER = Logger.getLogger("FernFlower");

    private PropertiesHelper propertiesHelper;

    private String locale; // reassigned on reload
    private ResourceBundle resourceBundle; // reassigned on reload

    private Stage mainStage;
    private Scene scene;

    private final MappingContext mappingContext = new MappingContext();
    private Path currentMappingsPath;
    private MappingWriterType currentWriterType;
    private ClassSet loadedJar;

    static {
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(new ConsoleHandler() {
            @Override
            protected void setOutputStream(OutputStream out) throws SecurityException {
                super.setOutputStream(System.out);
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    public Main(boolean testingEnv) {
        super();
        instance = this;
        if (!testingEnv) {
            initialize();
        }
        this.testingEnv = testingEnv;
    }

    public Main() {
        this(false);
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static Logger getFernFlowerLogger() {
        return FERNFLOWER_LOGGER;
    }

    public void initialize() {
        propertiesHelper = new PropertiesHelper();
        locale = getPropertiesHelper().getProperty(PropertiesHelper.Key.LOCALE);
        initLocale();
    }

    public void initLocale() {
        try {
            resourceBundle = ResourceBundle.getBundle("lang." + locale);
        } catch (MissingResourceException ex) {
            if (locale.equals("en_US")) {
                Main.getLogger().severe("Cannot locate default locale (" + locale + ") - giving up");
                throw ex;
            }

            Main.getLogger().warning("Current locale (" + locale + ") is not available - resetting to en_US");
            locale = "en_US";
            initLocale();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainStage = primaryStage;

        String[] icons = new String[] {
                "icon-16", "icon-24", "icon-32", "icon-48",
                "icon-64", "icon-128", "icon-256", "icon-512"
        };
        for (String icon : icons) {
            mainStage.getIcons().add(new Image(getClass().getResourceAsStream("/img/icons/" + icon + ".png")));
        }

        loadView(locale);

        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(resourceBundle.getString("exception.title"));
            alert.setHeaderText(resourceBundle.getString("exception.header"));

            TextFlow description = new TextFlow(
                    new Text(resourceBundle.getString("exception.dialog1") + "\n"),
                    new Text(resourceBundle.getString("exception.dialog2") + "\n\n"),
                    new Text(resourceBundle.getString("exception.dialog3")),
                    new Text(" "),
                    new WebLink("https://github.com/LapisBlue/Nocturne/issues"),
                    new Text("\n\n"),
                    new Text(resourceBundle.getString("exception.dialog4"))
            );
            description.setLayoutX(20);
            description.setLayoutY(25);

            TextArea exceptionText = new TextArea();
            StringWriter exceptionWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(exceptionWriter));
            exceptionText.setText(exceptionWriter.toString());
            exceptionText.setLayoutX(20);
            exceptionText.setLayoutY(140);

            Pane contentPane = new Pane(description, exceptionText);
            alert.getDialogPane().setContent(contentPane);

            alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CLOSE);

            alert.showAndWait();

            if (alert.getResult() == ButtonType.CLOSE) {
                System.exit(0);
            }
        });
    }

    public static Main getInstance() {
        return instance;
    }

    public void loadView(String lang) throws IOException {
        locale = lang;
        resourceBundle = ResourceBundle.getBundle("lang." + lang);
        FXMLLoader loader = new FXMLLoader(ClassLoader.getSystemResource("fxml/main.fxml"));
        loader.setResources(resourceBundle);
        Parent root = loader.load();

        if (scene == null) {
            scene = new Scene(root);
            SceneHelper.addStdStylesheet(scene);
            mainStage.setTitle("Nocturne");
            mainStage.setScene(scene);
            mainStage.setOnCloseRequest(CLOSE_HANDLER);
            mainStage.show();
        } else {
            scene.setRoot(root);
        }
    }

    public static PropertiesHelper getPropertiesHelper() {
        return getInstance().propertiesHelper;
    }

    public static String getCurrentLocale() {
        return getInstance().locale;
    }

    public static ResourceBundle getResourceBundle() {
        return getInstance().resourceBundle;
    }

    public static Stage getMainStage() {
        return getInstance().mainStage;
    }

    public static MappingContext getMappingContext() {
        return getInstance().mappingContext;
    }

    public static Path getCurrentMappingsPath() {
        return getInstance().currentMappingsPath;
    }

    public static void setCurrentMappingsPath(Path path) {
        getInstance().currentMappingsPath = path;
    }

    public static MappingWriterType getCurrentWriterType() {
        return getInstance().currentWriterType;
    }

    public static void setCurrentWriterType(MappingWriterType currentWriterType) {
        getInstance().currentWriterType = currentWriterType;
    }

    public static ClassSet getLoadedJar() {
        return getInstance().loadedJar;
    }

    public static void setLoadedJar(ClassSet classSet) {
        getInstance().loadedJar = classSet;
        updateTitle();
    }

    public static void updateTitle() {
        if (getInstance().testingEnv) {
            return;
        }

        String title;
        if (getLoadedJar() == null) {
            title = "Nocturne";
        } else {
            title = "Nocturne - " + (getMappingContext().isDirty() ? "*" : "") + getLoadedJar().getName();
        }

        if (!getMainStage().getTitle().equals(title)) {
            getMainStage().setTitle(title);
        }
    }

}
