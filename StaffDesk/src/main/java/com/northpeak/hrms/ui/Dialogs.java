package com.northpeak.hrms.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Window;

/** Confirmation dialogs (JOptionPane-style) sharing the application stylesheet. */
public final class Dialogs {

    private Dialogs() {
    }

    public static Alert confirm(Window owner, String title, String message, String okLabel, boolean danger) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.initOwner(owner);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(message);
        ButtonType ok = new ButtonType(okLabel, ButtonBar.ButtonData.OK_DONE);
        a.getButtonTypes().setAll(ButtonType.CANCEL, ok);
        style(a);
        a.getDialogPane().lookupButton(ok).getStyleClass().add(danger ? "btn-danger" : "btn-success");
        return a;
    }

    public static void style(Dialog<?> d) {
        var url = Dialogs.class.getResource("/css/styles.css");
        if (url != null) d.getDialogPane().getStylesheets().add(url.toExternalForm());
        d.getDialogPane().getStyleClass().add("app-dialog");
    }
}
