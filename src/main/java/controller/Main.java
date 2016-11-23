package controller; /**
 * Created by doctor-googler on 10/28/2016.
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.FTPManager;
import view.ConnectDialogController;
import view.MainWindowController;

import java.io.IOException;

public class Main extends Application{
    private Stage primaryStage;
    private FTPManager ftpManager;

    public static final void main(String... args) {
        Application.launch(Main.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.ftpManager = new FTPManager();

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(Main.class.getResource("/MainWindow.fxml"));
        Pane basePane = (Pane) loader.load();
        Scene mainScene = new Scene(basePane);

        MainWindowController mainWindowController = loader.getController();
        mainWindowController.setMainApp(this);

        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    public void showConnectDialog() {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(Main.class.getResource("/ConnectDialog.fxml"));
        AnchorPane anchorPane = null;
        try {
            anchorPane = (AnchorPane) loader.load();
            ConnectDialogController dialogController = loader.getController();

            Scene dialogScene = new Scene(anchorPane);
            Stage dialogStage = new Stage();
            dialogController.setMainApp(this);
            dialogController.setStage(dialogStage);
            dialogStage.setScene(dialogScene);
            dialogStage.setTitle("Connect to FTP");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);

            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FTPManager getFtpManager() {
        return ftpManager;
    }
}