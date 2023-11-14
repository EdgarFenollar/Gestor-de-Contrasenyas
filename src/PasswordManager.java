import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.util.Base64;
import java.util.Objects;
import java.util.prefs.Preferences;

public class PasswordManager extends Application {

    private TextField appNameField;
    private TextArea passwordTextArea;
    private TextArea decryptedTextArea;
    private SecretKey secretKey;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Gestor de Contraseñas");
        primaryStage.setResizable(false);

        BorderPane borderPane = new BorderPane();

        StackPane centerPane = new StackPane();

        // Carga la imagen del icono desde el archivo en tu proyecto
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("img/candado.png")));

        // Establece el icono en la ventana principal
        primaryStage.getIcons().add(icon);

        primaryStage.setScene(new Scene(new StackPane(), 800, 600));
        primaryStage.show();

        Image image = new Image("img/candado.png");
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(200);
        imageView.setFitHeight(150);

        Label titleLabel = new Label("Gestor de Contraseñas");
        titleLabel.setStyle("-fx-font-size: 36px; -fx-text-fill: white;");

        Label appNameLabel = new Label("Nombre de la Aplicación/Sitio Web:");
        appNameField = new TextField();
        Button generateButton = new Button("Generar Contraseña");
        Button showDecryptedButton = new Button("Mostrar Contraseñas Desencriptadas");
        passwordTextArea = new TextArea();
        passwordTextArea.setEditable(false);
        decryptedTextArea = new TextArea();
        decryptedTextArea.setEditable(false);

        VBox rightPane = new VBox(10);
        rightPane.getChildren().addAll(appNameLabel, appNameField, generateButton, passwordTextArea, showDecryptedButton, decryptedTextArea);

        BorderPane.setMargin(centerPane, new Insets(20));
        BorderPane.setMargin(rightPane, new Insets(20));

        centerPane.getChildren().addAll(imageView, titleLabel);

        borderPane.setCenter(centerPane);
        borderPane.setRight(rightPane);

        // Carga o genera una clave aleatoria de 128 bits (16 bytes)
        secretKey = loadOrGenerateSecretKey();

        generateButton.setOnAction(e -> {
            int passwordLength = 12;
            String generatedPassword = generateRandomPassword(passwordLength);

            String encryptedAppName = encryptText(appNameField.getText());
            String encryptedPassword = encryptText(generatedPassword);

            passwordTextArea.setText(encryptedAppName + " -> " + encryptedPassword);

            savePasswordToFile(encryptedAppName, encryptedPassword);
        });

        showDecryptedButton.setOnAction(e -> {
            String enteredPassword = getPasswordFromUser();
            if (enteredPassword != null) {
                // Ver contraseñas desencriptadas cuando se proporciona la contraseña secreta
                String secretPassword = "020703"; // Cambia esto a tu contraseña secreta
                if (enteredPassword.equals(secretPassword)) {
                    showDecryptedPasswords();
                }
            }
        });

        Scene scene = new Scene(borderPane);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("styles.css")).toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private String encryptText(String text) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(text.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private SecretKey loadOrGenerateSecretKey() {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String encodedKey = prefs.get("secretKey", null);
        if (encodedKey != null) {
            return decodeSecretKey(encodedKey);
        } else {
            SecretKey newSecretKey = generateRandom128BitKey();
            saveSecretKey(newSecretKey);
            return newSecretKey;
        }
    }

    private SecretKey generateRandom128BitKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return keyGenerator.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveSecretKey(SecretKey secretKey) {
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        String encodedKey = encodeSecretKey(secretKey);
        prefs.put("secretKey", encodedKey);
    }

    private SecretKey decodeSecretKey(String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    private String encodeSecretKey(SecretKey secretKey) {
        byte[] encodedKey = secretKey.getEncoded();
        return Base64.getEncoder().encodeToString(encodedKey);
    }

    private String generateRandomPassword(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * characters.length());
            password.append(characters.charAt(index));
        }
        return password.toString();
    }

    private void savePasswordToFile(String appName, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("contraseñas.txt", true))) {
            writer.write(appName + " -> " + password);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getPasswordFromUser() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Ingrese la contraseña secreta");
        dialog.setContentText("Contraseña secreta:");
        dialog.initStyle(StageStyle.UTILITY); // Oculta los botones de maximizar y minimizar
        Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(new Image("img/candado.png"));

        // Aplicar estilos CSS al cuadro de diálogo
        dialog.getDialogPane().getStylesheets().add(Objects.requireNonNull(getClass().getResource("styles.css")).toExternalForm());

        dialog.showAndWait();
        return dialog.getResult();
    }

    private void showDecryptedPasswords() {
        decryptedTextArea.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader("contraseñas.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" -> ");
                if (parts.length == 2) {
                    String decryptedAppName = decryptText(parts[0]);
                    String decryptedPassword = decryptText(parts[1]);
                    decryptedTextArea.appendText(decryptedAppName + " -> " + decryptedPassword + "\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String decryptText(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
