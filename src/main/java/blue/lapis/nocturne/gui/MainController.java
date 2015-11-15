/*
 * Nocturne
 * Copyright (c) 2015, Lapis <https://github.com/LapisBlue>
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
package blue.lapis.nocturne.gui;

import blue.lapis.nocturne.Main;
import blue.lapis.nocturne.mapping.MappingSet;
import blue.lapis.nocturne.mapping.io.reader.SrgReader;
import blue.lapis.nocturne.mapping.io.writer.SrgWriter;
import blue.lapis.nocturne.util.Constants;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

/**
 * The main JavaFX controller.
 */
public class MainController implements Initializable {

    public MenuItem openJarButton;
    public MenuItem closeJarButton;
    public MenuItem loadMappingsButton;
    public MenuItem clearMappingsButton;
    public MenuItem saveMappingsButton;
    public MenuItem saveMappingsAsButton;
    public MenuItem closeButton;
    public MenuItem aboutButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        openJarButton.setDisable(true); //TODO: temporary
    }

    public void openJar(ActionEvent actionEvent) {
        //TODO: close current JAR if applicable
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select JAR File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JAR Files", "*.jar")
        );
        File selectedFile = fileChooser.showOpenDialog(Main.mainStage);
        //TODO
        closeJarButton.setDisable(false);
    }

    public void closeJar(ActionEvent actionEvent) {
        //TODO
        closeJarButton.setDisable(true);
    }

    public void loadMappings(ActionEvent actionEvent) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Mapping File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SRG Mapping Files", "*.srg"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File selectedFile = fileChooser.showOpenDialog(Main.mainStage);

        if (selectedFile != null && selectedFile.exists()) {
            SrgReader reader = new SrgReader(new BufferedReader(new FileReader(selectedFile)));
            MappingSet mappingSet = reader.read();
            if (Main.mappings == null) {
                Main.mappings = mappingSet;
            } else {
                Main.mappings.merge(mappingSet);
            }

            Main.currentMappingsPath = selectedFile.toPath();
        }
    }

    public void clearMappings(ActionEvent actionEvent) {
        //TODO: prompt to save if applicable
        Main.mappings = null;
    }

    public void saveMappings(ActionEvent actionEvent) throws IOException {
        if (Main.currentMappingsPath == null) {
            saveMappingsAs(actionEvent);
            return;
        }

        saveMappings();
    }

    public void saveMappingsAs(ActionEvent actionEvent) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Destination File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SRG Mapping Files", "*.srg"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File selectedFile = fileChooser.showSaveDialog(Main.mainStage);

        if (selectedFile == null) {
            return;
        }

        if (!selectedFile.exists()) {
            Files.createFile(selectedFile.toPath());
        }

        Main.currentMappingsPath = selectedFile.toPath();

        saveMappings();
    }

    private void saveMappings() throws IOException {
        SrgWriter writer = new SrgWriter(new PrintWriter(Main.currentMappingsPath.toFile()));
        writer.write(Main.mappings);
    }

    public void onClose(ActionEvent actionEvent) {
        System.exit(0);
    }

    public void showAbout(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Nocturne");
        alert.setHeaderText("Nocturne " + Constants.VERSION);
        alert.setContentText("Copyright (c) 2015 Lapis.\n"
                + "This software is made available under the MIT license.\n"
                + "\n"
                + "Github: https://github.com/LapisBlue/Nocturne");
        alert.showAndWait();
    }

}
