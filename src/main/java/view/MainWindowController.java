package view;

import controller.Main;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import model.FTPManager;
import model.FTPManagerResponse;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainWindowController {
    private FTPManager ftpManager;
    private Map<String,Image> icons;
    private Main mainApp;
    private SimpleDateFormat dateFormatter;
    private Map<String,Image> previews;

    @FXML
    private Pane pane;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private MenuBar menuBar;

    @FXML
    private ImageView preview;

    @FXML
    private MenuItem itemDisconnect;

    @FXML
    private MenuItem itemConnect;

    @FXML
    private MenuItem itemExit;

    @FXML
    private MenuItem editDownload;

    @FXML
    private MenuItem editUpload;

    @FXML
    private MenuItem editRename;

    @FXML
    private MenuItem editDelete;

    @FXML
    private MenuItem contextDownload;

    @FXML
    private MenuItem contextUpload;

    @FXML
    private MenuItem contextRename;

    @FXML
    private MenuItem contextDelete;

    @FXML
    private Label typeLabel;

    @FXML
    private Label modifiedDateLabel;

    @FXML
    private Label sizeLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private TreeView<String> treeView;

    public void setMainApp(Main mainApp) {
        this.mainApp = mainApp;
        ftpManager = mainApp.getFtpManager();
    }

    @FXML
    void initialize() {
        assert menuBar != null : "fx:id=\"menuBar\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert preview != null : "fx:id=\"preview\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert progressBar != null : "fx:id=\"progressBar\" was not injected: check your FXML file 'MainWindow.fxml'.";
        assert treeView != null : "fx:id=\"treeView\" was not injected: check your FXML file 'MainWindow.fxml'.";
        dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        itemDisconnect.setDisable(true);
        progressBar.setDisable(true);
        setDisableContextOperations(true);
        showInfoLables(false);
        previews = new HashMap<>();
        Image folder = new Image(MainWindowController.class.getResource("folder.png").toString());

    }

    @FXML
    void handleConnect() {
        mainApp.showConnectDialog();
        FTPManagerResponse<List<FTPFile>> response = ftpManager.fileList();

        if (response.getErrors() != null) {
            showErrorAlert(response.getErrors().toString());
            ftpManager.disconnect();
            return;
        }

        treeView.setRoot(new TreeItem<String>("/"));
        List<FTPFile> dirs = response.getContent();
        setChildrenDirs(treeView.getRoot(), dirs);
        System.out.println(response.getContent());
        treeView.refresh();

        itemConnect.setDisable(true);
        itemDisconnect.setDisable(false);
        setDisableContextOperations(false);

    }

    @FXML
    void handleDisconnect() {
        ftpManager.disconnect();
        treeView.getRoot().setValue("");
        ObservableList<TreeItem<String>> list = treeView.getRoot().getChildren();
        list.remove(0, list.size());
        treeView.refresh();

        itemConnect.setDisable(false);
        itemDisconnect.setDisable(true);
        setDisableContextOperations(true);
        showInfoLables(false);
    }

    @FXML
    void handleExit() {
        FTPManagerResponse<Boolean> response = ftpManager.disconnect();
        Platform.exit();
    }

    @FXML
    void onTreeMouseClicked(MouseEvent me) {
        if (!ftpManager.isConnected()) {
            return;
        }
        if (me.getButton() == MouseButton.PRIMARY) {
            TreeItem<String> treeItem = treeView.getSelectionModel().getSelectedItem();
            String path = getFullPath(treeItem);
            if (me.getClickCount() == 1) {
                showInfoLables(true);
                FTPManagerResponse<FTPFile> response = ftpManager.fileInfo(path);
                FTPFile file = response.getContent();
                if (file != null) {
                    System.out.println(file.toString());
                    switch (file.getType()){
                        case FTPFile.DIRECTORY_TYPE: typeLabel.setText("Directory");break;
                        case FTPFile.FILE_TYPE: {
                            typeLabel.setText("File");
                            sizeLabel.setText(file.getSize()+" bytes");
                            break;
                        }
                        case FTPFile.UNKNOWN_TYPE: typeLabel.setText("Unknown");break;
                        case FTPFile.SYMBOLIC_LINK_TYPE: typeLabel.setText("Symbolic link");break;
                    }
                    modifiedDateLabel.setText(file.getTimestamp().getTime().toString());
                } else {
                    System.out.println("File info error: "+response.getErrors());
                }
            }

            if (me.getClickCount() > 1) {
                FTPManagerResponse<FTPFile> response = ftpManager.fileInfo(path);
                FTPFile file = response.getContent();
                if (file.isDirectory()) {
                    ObservableList<TreeItem<String>> nodeItems = treeItem.getChildren();
                    if (nodeItems.size() > 0) {
                        treeItem.getChildren().remove(0,nodeItems.size());
                    }
                    ftpManager.changeDirectory(getFullPath(treeItem));
                    List<FTPFile> list = ftpManager.fileList().getContent();
                    setChildrenDirs(treeItem, list);
                    treeItem.setExpanded(true);
                }
            }
        }
        if (me.getButton() == MouseButton.SECONDARY) {
            ContextMenu cMenu = treeView.getContextMenu();
            cMenu.show(pane, me.getScreenX(), me.getScreenY());
        }
    }

    @FXML
    void handleContextDownload() {
        progressBar.setDisable(false);
        TreeItem<String> treeItem = treeView.getSelectionModel().getSelectedItem();
        String remotePath = getFullPath(treeItem);
        FTPManagerResponse<FTPFile> response = ftpManager.fileInfo(remotePath);
        FTPFile ftpFile = response.getContent();
        if (ftpFile == null || ftpFile.isDirectory()) {
            showErrorAlert("Chosen file cannot be downloaded\n Probably you are trying to download directory?");
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Download file to");
        String fileName = remotePath.substring(remotePath.lastIndexOf("/")+1, remotePath.length());
        fileChooser.setInitialFileName(fileName);
        File saveFile = fileChooser.showSaveDialog(pane.getScene().getWindow());
        if (saveFile != null) {
            FTPManagerResponse<Task<Boolean>> downloadResponse = ftpManager.downloadWithProgresss(remotePath, saveFile.getAbsolutePath());
            if (downloadResponse.getErrors() != null) {
                System.err.println(downloadResponse.getErrors());
                return;
            }

            Task<Boolean> downloadTask = downloadResponse.getContent();
            progressBar.progressProperty().unbind();
            progressBar.progressProperty().bind(downloadTask.progressProperty());

            downloadTask.setOnSucceeded(event -> {
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().setValue(0);
                progressBar.setDisable(true);
                setDisableContextOperations(false);
            });
            downloadTask.setOnFailed( event -> {
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().setValue(0);
                progressBar.setDisable(true);
                showErrorAlert("Some error occurred.\nUnable to download file");
                setDisableContextOperations(false);
            });

            Thread downloadThread = new Thread(downloadTask);
            downloadThread.start();
            setDisableContextOperations(true);
        }
    }

    @FXML
    void handleContextUpload() {
        progressBar.setDisable(false);
        TreeItem<String> treeItem = treeView.getSelectionModel().getSelectedItem();
        String remotePath = getFullPath(treeItem);

        FileChooser fc = new FileChooser();
        fc.setTitle("Select uploading file");
        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Any file","*.*"));
        File file = fc.showOpenDialog(pane.getScene().getWindow());
        if (file != null) {
            remotePath += "/"+file.getName();
            System.out.println(remotePath);
            FTPManagerResponse<Task<Boolean>> response = ftpManager.uploadWithProgress(remotePath, file.getAbsolutePath());
            if (response.getErrors() != null || response.getContent() == null) {
                System.err.println(response.getErrors());
                return;
            }
            Task<Boolean> uploadTask = response.getContent();
            progressBar.progressProperty().unbind();
            progressBar.progressProperty().bind(uploadTask.progressProperty());

            uploadTask.setOnSucceeded(event -> {
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().setValue(0);
                progressBar.setDisable(true);
                setDisableContextOperations(false);
                //facepalm
                treeItem.getChildren().add(new TreeItem<>(file.getName()));
            });
            uploadTask.setOnFailed(event -> {
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().setValue(0);
                progressBar.setDisable(true);
                showErrorAlert("Some error occurred.\nUnable to upload selected file");
                setDisableContextOperations(false);
            });

            Thread uploadThread = new Thread(uploadTask);
            uploadThread.start();
            setDisableContextOperations(true);
        }
    }

    @FXML
    void handleContextRename() {
        TreeItem<String> item = treeView.getSelectionModel().getSelectedItem();
        String fullPath = getFullPath(item);
        TextInputDialog renameDialog = new TextInputDialog();
        renameDialog.setTitle("Rename");
        renameDialog.setHeaderText(String.format("Please provide a new name for %s",item.getValue()));
        renameDialog.setContentText("New name:");
        Optional<String> result = renameDialog.showAndWait();
        result.ifPresent(str -> {
            FTPManagerResponse<Boolean> response = ftpManager.rename(fullPath, fullPath.replace(item.getValue(),str));
            if (!Boolean.TRUE.equals(response.getContent())) {
                System.err.println("Rename operation error:");
                System.err.println(response.getErrors());
                System.err.println(response.getContent());
                return;
            }
        });
    }

    @FXML
    void handleContextDelete() {

    }

    private void setChildrenDirs(TreeItem<String> parent, List<FTPFile> dirNames) {
        ObservableList<TreeItem<String>> list = parent.getChildren();
        for (FTPFile file : dirNames) {
            list.add(new TreeItem<>(file.getName()));
        }
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Some error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String getFullPath(TreeItem<String> item) {
        StringBuilder sb = new StringBuilder();
        TreeItem<String> cursor = item;
        while (cursor != null) {
            sb.insert(0,"/").insert(0, cursor.getValue());
            cursor = cursor.getParent();
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(0);
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private void setDisableContextOperations(boolean value) {
        contextDownload.setDisable(value);
        contextUpload.setDisable(value);
        contextRename.setDisable(value);
        contextDelete.setDisable(value);

        editDownload.setDisable(value);
        editUpload.setDisable(value);
        editRename.setDisable(value);
        editDelete.setDisable(value);
    }

    private void showInfoLables(boolean value) {
        typeLabel.setVisible(value);
        modifiedDateLabel.setVisible(value);
        sizeLabel.setVisible(value);
    }

//    private String getUsefulSize(long bytes) {
//        if (bytes <= Math.pow(10, 3)) {
//            return bytes+" bytes";
//        } else if (bytes <= Math.pow(10, 6)){
//            return bytes/Math.pow(10,6)+" KB";
//        } else if (bytes <= Math.pow(2, 30)){
//            return bytes/Math.pow(2, 30)+" MB";
//        } else if (bytes <= Math.pow(2, 40)){
//            return bytes/Math.pow(2, 40)+" GB";
//        }
//        return bytes+"bytes";
//    }
}
