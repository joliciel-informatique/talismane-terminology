package com.joliciel.talismane.terminology.viewer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import ch.qos.logback.core.joran.spi.JoranException;
import com.joliciel.talismane.utils.LogUtils;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import joptsimple.AbstractOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

@SuppressWarnings("restriction")
public class TerminologyViewer extends Application {

  public static void main(String[] args) throws Exception {
    final OptionParser parser = new OptionParser();
    parser.acceptsAll(Arrays.asList("?", "help"), "show help").forHelp();

    final OptionSpec<File> logConfigFileSpec = parser.accepts("logConfigFile", "logback configuration file").withRequiredArg().ofType(File.class);

    OptionSet options = parser.parse(args);
    if (options.has("help") || options.has("?")) {
      parser.printHelpOn(System.out);
      return;
    }
    if (options.has(logConfigFileSpec))
      LogUtils.configureLogging(options.valueOf(logConfigFileSpec));

    Application.launch(TerminologyViewer.class, args);
  }

  @Override
  public void start(Stage stage) throws Exception {
    FXMLLoader fxmlLoader = new FXMLLoader();

    URL fxmlURL = TerminologyViewer.class.getResource("resources/terminology_viewer.fxml");
    Parent root = (Parent) fxmlLoader.load(fxmlURL.openStream());

    TerminologyViewerController controller = fxmlLoader.getController();
    controller.setPrimaryStage(stage);

    stage.setTitle("Talismane Terminology Viewer");
    stage.setScene(new Scene(root, 800, 400));
    stage.show();
  }
}
