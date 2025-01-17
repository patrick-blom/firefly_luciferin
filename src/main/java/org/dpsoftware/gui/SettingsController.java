/*
  SettingsController.java

  Firefly Luciferin, very fast Java Screen Capture software designed
  for Glow Worm Luciferin firmware.

  Copyright (C) 2020 - 2021  Davide Perini

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.dpsoftware.gui;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.dpsoftware.FireflyLuciferin;
import org.dpsoftware.JavaFXStarter;
import org.dpsoftware.LEDCoordinate;
import org.dpsoftware.NativeExecutor;
import org.dpsoftware.config.Configuration;
import org.dpsoftware.config.Constants;
import org.dpsoftware.gui.elements.DisplayInfo;
import org.dpsoftware.gui.elements.GlowWormDevice;
import org.dpsoftware.managers.DisplayManager;
import org.dpsoftware.managers.MQTTManager;
import org.dpsoftware.managers.StorageManager;
import org.dpsoftware.managers.dto.ColorDto;
import org.dpsoftware.managers.dto.FirmwareConfigDto;
import org.dpsoftware.managers.dto.GammaDto;
import org.dpsoftware.managers.dto.StateDto;
import org.dpsoftware.utilities.CommonUtility;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;


/**
 * FXML Settings Controller
 */
@Slf4j
public class SettingsController {

    @FXML private TabPane mainTabPane;
    @FXML private TextField screenWidth;
    @FXML private TextField screenHeight;
    @FXML private TextField ledStartOffset;
    @FXML private ComboBox<String> scaling;
    @FXML private ComboBox<String> gamma;
    @FXML private ComboBox<String> whiteTemperature;
    @FXML private ComboBox<Configuration.CaptureMethod> captureMethod;
    @FXML private TextField numberOfThreads;
    @FXML private Button saveLedButton;
    @FXML private Button playButton;
    @FXML private Button saveMQTTButton;
    @FXML private Button saveMiscButton;
    @FXML private Button saveSettingsButton;
    @FXML private Button saveDeviceButton;
    @FXML private Button showTestImageButton;
    @FXML private ComboBox<String> serialPort; // NOTE: for multi display this contain the deviceName of the MQTT device where to stream
    @FXML private ComboBox<String> aspectRatio;
    @FXML private ComboBox<String> multiMonitor;
    @FXML private ComboBox<Integer> monitorNumber;
    @FXML private ComboBox<String> baudRate;
    @FXML private TextField mqttHost;
    @FXML private TextField mqttPort;
    @FXML private TextField mqttTopic;
    @FXML private TextField mqttUser;
    @FXML private PasswordField mqttPwd;
    @FXML private CheckBox mqttEnable;
    @FXML private CheckBox mqttStream;
    @FXML private CheckBox startWithSystem;
    @FXML private CheckBox checkForUpdates;
    @FXML private CheckBox syncCheck;
    @FXML private TextField topLed;
    @FXML private TextField leftLed;
    @FXML private TextField rightLed;
    @FXML private TextField bottomLeftLed;
    @FXML private TextField bottomRightLed;
    @FXML private TextField bottomRowLed;
    @FXML private ComboBox<String> orientation;
    @FXML private Label producerLabel;
    @FXML private Label consumerLabel;
    @FXML private Label version;
    @FXML private final StringProperty producerValue = new SimpleStringProperty("");
    @FXML private final StringProperty consumerValue = new SimpleStringProperty("");
    @FXML private TableView<GlowWormDevice> deviceTable;
    @FXML private TableColumn<GlowWormDevice, String> deviceNameColumn;
    @FXML private TableColumn<GlowWormDevice, String> deviceBoardColumn;
    @FXML private TableColumn<GlowWormDevice, String> deviceIPColumn;
    @FXML private TableColumn<GlowWormDevice, String> deviceVersionColumn;
    @FXML private TableColumn<GlowWormDevice, String> macColumn;
    @FXML private TableColumn<GlowWormDevice, String> gpioColumn;
    @FXML private TableColumn<GlowWormDevice, String> firmwareColumn;
    @FXML private TableColumn<GlowWormDevice, String> baudrateColumn;
    @FXML private TableColumn<GlowWormDevice, String> mqttTopicColumn;
    @FXML private TableColumn<GlowWormDevice, String> numberOfLEDSconnectedColumn;
    @FXML private Label versionLabel;
    public static ObservableList<GlowWormDevice> deviceTableData = FXCollections.observableArrayList();
    @FXML private CheckBox autoStart;
    @FXML private CheckBox eyeCare;
    @FXML private CheckBox splitBottomRow;
    @FXML private ComboBox<String> framerate;
    @FXML private ColorPicker colorPicker;
    @FXML private ToggleButton toggleLed;
    @FXML private Slider brightness;
    @FXML private Label bottomLeftLedLabel;
    @FXML private Label bottomRightLedLabel;
    @FXML private Label bottomRowLedLabel;
    @FXML private Label displayLabel;
    @FXML private Label comWirelessLabel;
    ImageView imageView;
    Image controlImage;
    AnimationTimer animationTimer;
    boolean cellEdit = false;
    Configuration currentConfig;
    StorageManager sm;
    DisplayManager displayManager;


    /**
     * Initialize controller with system's specs
     */
    @FXML
    protected void initialize() {

        Platform.setImplicitExit(false);
        sm = new StorageManager();
        displayManager = new DisplayManager();
        currentConfig = sm.readConfig(false);

        scaling.getItems().addAll("100%", "125%", "150%", "175%", "200%", "225%", "250%", "300%", "350%");
        gamma.getItems().addAll("1.0", "1.8", "2.0", "2.2", "2.4", "4.0", "5.0", "6.0", "8.0", "10.0");
        for (Constants.BaudRate br : Constants.BaudRate.values()) {
            baudRate.getItems().add(br.getBaudRate());
        }
        for (Constants.WhiteTemperature kelvin : Constants.WhiteTemperature.values()) {
            whiteTemperature.getItems().add(kelvin.getWhiteTemperature());
        }
        if (NativeExecutor.isLinux()) {
            producerLabel.textProperty().bind(producerValueProperty());
            consumerLabel.textProperty().bind(consumerValueProperty());
            if (FireflyLuciferin.communicationError) {
                controlImage = setImage(Constants.PlayerStatus.GREY);
            } else if (FireflyLuciferin.RUNNING) {
                controlImage = setImage(Constants.PlayerStatus.PLAY_WAITING);
            } else {
                controlImage = setImage(Constants.PlayerStatus.STOP);
            }
            setButtonImage();
            captureMethod.getItems().addAll(Configuration.CaptureMethod.XIMAGESRC);
            version.setText("by Davide Perini (VERSION)".replaceAll("VERSION", FireflyLuciferin.version));
        }
        orientation.getItems().addAll(Constants.CLOCKWISE, Constants.ANTICLOCKWISE);
        aspectRatio.getItems().addAll(Constants.AspectRatio.FULLSCREEN.getAspectRatio(), Constants.AspectRatio.LETTERBOX.getAspectRatio(),
                Constants.AspectRatio.PILLARBOX.getAspectRatio(), Constants.AUTO_DETECT_BLACK_BARS);
        for (int i=1; i <= displayManager.displayNumber(); i++) {
            monitorNumber.getItems().add(i);
            switch (i) {
                case 1 -> multiMonitor.getItems().add(Constants.MULTIMONITOR_1);
                case 2 -> multiMonitor.getItems().add(Constants.MULTIMONITOR_2);
                case 3 -> multiMonitor.getItems().add(Constants.MULTIMONITOR_3);
            }
        }
        framerate.getItems().addAll("5 FPS", "10 FPS", "15 FPS", "20 FPS", "25 FPS", "30 FPS", "40 FPS", "50 FPS",
                "60 FPS", "90 FPS", "120 FPS", Constants.UNLOCKED);
        showTestImageButton.setVisible(currentConfig != null);
        setSaveButtonText();
        // Init default values
        initDefaultValues();
        // Init tooltips
        setTooltips();
        // Force numeric fields
        setNumericTextField();
        runLater();
        // Device table
        deviceNameColumn.setCellValueFactory(cellData -> cellData.getValue().deviceNameProperty());
        deviceBoardColumn.setCellValueFactory(cellData -> cellData.getValue().deviceBoardProperty());
        deviceIPColumn.setCellValueFactory(cellData -> cellData.getValue().deviceIPProperty());
        deviceVersionColumn.setCellValueFactory(cellData -> cellData.getValue().deviceVersionProperty());
        macColumn.setCellValueFactory(cellData -> cellData.getValue().macProperty());
        gpioColumn.setCellValueFactory(cellData -> cellData.getValue().gpioProperty());
        firmwareColumn.setCellValueFactory(cellData -> cellData.getValue().firmwareTypeProperty());
        baudrateColumn.setCellValueFactory(cellData -> cellData.getValue().baudRateProperty());
        baudrateColumn.setCellValueFactory(cellData -> cellData.getValue().baudRateProperty());
        mqttTopicColumn.setCellValueFactory(cellData -> cellData.getValue().mqttTopicProperty());
        numberOfLEDSconnectedColumn.setCellValueFactory(cellData -> cellData.getValue().numberOfLEDSconnectedProperty());
        deviceTable.setEditable(true);
        deviceTable.setItems(getDeviceTableData());
        initListeners();
        startAnimationTimer();

    }

    /**
     * Init form values
     */
    void initDefaultValues() {

        versionLabel.setText(Constants.FIREFLY_LUCIFERIN + " (v" + FireflyLuciferin.version + ")");
        brightness.setMin(0);
        brightness.setMax(100);
        brightness.setMajorTickUnit(10);
        brightness.setMinorTickCount(5);
        brightness.setShowTickMarks(true);
        brightness.setBlockIncrement(10);
        brightness.setShowTickLabels(true);
        monitorNumber.setValue(1);
        comWirelessLabel.setText(Constants.SERIAL_PORT);
        initOutputDeviceChooser(true);

        if (currentConfig == null) {
            DisplayInfo screenInfo = displayManager.getFirstInstanceDisplay();
            double scaleX = screenInfo.getScaleX();
            double scaleY = screenInfo.getScaleY();
            screenWidth.setText(String.valueOf((int) (screenInfo.width * scaleX)));
            screenHeight.setText(String.valueOf((int) (screenInfo.height * scaleY)));
            scaling.setValue(((int) (screenInfo.getScaleX() * 100)) + Constants.PERCENT);
            multiMonitor.setValue(Constants.MULTIMONITOR_1);
            monitorNumber.setValue(screenInfo.getFxDisplayNumber());
            ledStartOffset.setText(String.valueOf(0));
            if (NativeExecutor.isWindows()) {
                captureMethod.setValue(Configuration.CaptureMethod.DDUPL);
            } else if (NativeExecutor.isMac()) {
                captureMethod.setValue(Configuration.CaptureMethod.DDUPL);
            } else {
                captureMethod.setValue(Configuration.CaptureMethod.XIMAGESRC);
            }
            gamma.setValue(Constants.GAMMA_DEFAULT);
            whiteTemperature.setValue(Constants.WhiteTemperature.UNCORRECTEDTEMPERATURE.getWhiteTemperature());
            baudRate.setValue(Constants.DEFAULT_BAUD_RATE);
            baudRate.setDisable(true);
            mqttTopic.setDisable(true);
            serialPort.setValue(Constants.SERIAL_PORT_AUTO);
            numberOfThreads.setText("1");
            aspectRatio.setValue(Constants.AUTO_DETECT_BLACK_BARS);
            framerate.setValue("30 FPS");
            mqttHost.setText(Constants.DEFAULT_MQTT_HOST);
            mqttPort.setText(Constants.DEFAULT_MQTT_PORT);
            mqttTopic.setText(Constants.MQTT_BASE_TOPIC);
            orientation.setValue(Constants.CLOCKWISE);
            topLed.setText("33");
            leftLed.setText("18");
            rightLed.setText("18");
            bottomLeftLed.setText("13");
            bottomRightLed.setText("13");
            bottomRowLed.setText("26");
            checkForUpdates.setSelected(true);
            syncCheck.setSelected(true);
            toggleLed.setSelected(false);
            brightness.setValue(255);
            bottomLeftLed.setVisible(true);
            bottomRightLed.setVisible(true);
            bottomRowLed.setVisible(false);
            bottomLeftLedLabel.setVisible(true);
            bottomRightLedLabel.setVisible(true);
            bottomRowLedLabel.setVisible(false);
            splitBottomRow.setSelected(true);
        } else {
            initValuesFromSettingsFile();
        }
        deviceTable.setPlaceholder(new Label(Constants.NO_DEVICE_FOUND));

    }

    /**
     * Init form values by reading existing config file
     */
    private void initValuesFromSettingsFile() {

        if (currentConfig.getMultiMonitor() == 2 || currentConfig.getMultiMonitor() == 3) {
            serialPort.getItems().remove(0);
        }
        switch (JavaFXStarter.whoAmI) {
            case 1:
                if ((currentConfig.getMultiMonitor() == 1)) {
                    displayLabel.setText(Constants.MAIN_DISPLAY);
                } else {
                    displayLabel.setText(Constants.RIGHT_DISPLAY);
                }
                break;
            case 2:
                if ((currentConfig.getMultiMonitor() == 2)) {
                    displayLabel.setText(Constants.LEFT_DISPLAY);
                } else {
                    displayLabel.setText(Constants.CENTER_DISPLAY);
                }
                break;
            case 3: displayLabel.setText(Constants.LEFT_DISPLAY); break;
        }
        if (NativeExecutor.isWindows()) {
            startWithSystem.setSelected(currentConfig.isStartWithSystem());
        } else if (currentConfig.isAutoStartCapture()){
            controlImage = setImage(Constants.PlayerStatus.PLAY_WAITING);
            setButtonImage();
        }
        screenWidth.setText(String.valueOf(currentConfig.getScreenResX()));
        screenHeight.setText(String.valueOf(currentConfig.getScreenResY()));
        ledStartOffset.setText(String.valueOf(currentConfig.getLedStartOffset()));
        scaling.setValue(currentConfig.getOsScaling() + Constants.PERCENT);
        captureMethod.setValue(Configuration.CaptureMethod.valueOf(currentConfig.getCaptureMethod()));
        gamma.setValue(String.valueOf(currentConfig.getGamma()));
        whiteTemperature.setValue(Constants.WhiteTemperature.values()[currentConfig.getWhiteTemperature()-1].getWhiteTemperature());
        if (currentConfig.isMqttStream() && currentConfig.getSerialPort().equals(Constants.SERIAL_PORT_AUTO) && currentConfig.getMultiMonitor() == 1) {
            serialPort.setValue(FireflyLuciferin.config.getSerialPort());
        } else {
            serialPort.setValue(currentConfig.getSerialPort());
        }
        numberOfThreads.setText(String.valueOf(currentConfig.getNumberOfCPUThreads()));
        if (currentConfig.isAutoDetectBlackBars()) {
            aspectRatio.setValue(Constants.AUTO_DETECT_BLACK_BARS);
        } else {
            aspectRatio.setValue(currentConfig.getDefaultLedMatrix());
        }
        switch (currentConfig.getMultiMonitor()) {
            case 2 -> multiMonitor.setValue(Constants.MULTIMONITOR_2);
            case 3 -> multiMonitor.setValue(Constants.MULTIMONITOR_3);
            default -> multiMonitor.setValue(Constants.MULTIMONITOR_1);
        }
        monitorNumber.setValue(currentConfig.getMonitorNumber());
        framerate.setValue(currentConfig.getDesiredFramerate() + ((currentConfig.getDesiredFramerate().equals(Constants.UNLOCKED)) ? "" : " FPS"));
        mqttHost.setText(currentConfig.getMqttServer().substring(0, currentConfig.getMqttServer().lastIndexOf(":")));
        mqttPort.setText(currentConfig.getMqttServer().substring(currentConfig.getMqttServer().lastIndexOf(":") + 1));
        mqttTopic.setText(currentConfig.getMqttTopic().equals(Constants.DEFAULT_MQTT_TOPIC) ? Constants.MQTT_BASE_TOPIC : currentConfig.getMqttTopic());
        mqttUser.setText(currentConfig.getMqttUsername());
        mqttPwd.setText(currentConfig.getMqttPwd());
        mqttEnable.setSelected(currentConfig.isMqttEnable());
        autoStart.setSelected(currentConfig.isAutoStartCapture());
        eyeCare.setSelected(currentConfig.isEyeCare());
        mqttStream.setSelected(currentConfig.isMqttStream());
        checkForUpdates.setSelected(currentConfig.isCheckForUpdates());
        syncCheck.setSelected(currentConfig.isSyncCheck());
        orientation.setValue(currentConfig.getOrientation());
        topLed.setText(String.valueOf(currentConfig.getTopLed()));
        leftLed.setText(String.valueOf(currentConfig.getLeftLed()));
        rightLed.setText(String.valueOf(currentConfig.getRightLed()));
        bottomLeftLed.setText(String.valueOf(currentConfig.getBottomLeftLed()));
        bottomRightLed.setText(String.valueOf(currentConfig.getBottomRightLed()));
        bottomRowLed.setText(String.valueOf(currentConfig.getBottomRowLed()));
        String[] color = (FireflyLuciferin.config.getColorChooser().equals(Constants.DEFAULT_COLOR_CHOOSER)) ?
                currentConfig.getColorChooser().split(",") : FireflyLuciferin.config.getColorChooser().split(",");
        colorPicker.setValue(Color.rgb(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2]), Double.parseDouble(color[3])/255));
        brightness.setValue((Double.parseDouble(color[3])/255)*100);
        baudRate.setValue(currentConfig.getBaudRate());
        baudRate.setDisable(false);
        mqttTopic.setDisable(false);
        if ((FireflyLuciferin.config.isToggleLed())) {
            toggleLed.setText(Constants.TURN_LED_OFF);
        } else {
            toggleLed.setText(Constants.TURN_LED_ON);
        }
        toggleLed.setSelected(FireflyLuciferin.config.isToggleLed());
        splitBottomRow.setSelected(currentConfig.isSplitBottomRow());
        splitBottomRow();

    }

    /**
     * Run Later after GUI Init
     */
    private void runLater() {

        Platform.runLater(() -> {
            Stage stage = (Stage) mainTabPane.getScene().getWindow();
            if (stage != null) {
                stage.setOnCloseRequest(evt -> {
                    if (!NativeExecutor.isSystemTraySupported() || NativeExecutor.isLinux()) {
                        FireflyLuciferin.exit();
                    } else {
                        animationTimer.stop();
                    }
                });
            }
            setTableEdit();
            orientation.requestFocus();
        });

    }

    /**
     * Devices Table edit manager
     */
    private void setTableEdit() {

        gpioColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        gpioColumn.setOnEditStart(t -> cellEdit = true);
        gpioColumn.setOnEditCommit(t -> {
            cellEdit = false;
            GlowWormDevice device = t.getTableView().getItems().get(t.getTablePosition().getRow());
            if (t.getNewValue().equals(String.valueOf(2)) || t.getNewValue().equals(String.valueOf(5))
                    || t.getNewValue().equals(String.valueOf(16))) {
                Optional<ButtonType> result = FireflyLuciferin.guiManager.showAlert(Constants.GPIO_OK_TITLE, Constants.GPIO_OK_HEADER,
                        Constants.GPIO_OK_CONTEXT, Alert.AlertType.CONFIRMATION);
                ButtonType button = result.orElse(ButtonType.OK);
                if (button == ButtonType.OK) {
                    log.debug("Setting GPIO" + t.getNewValue() + " on " + device.getDeviceName());
                    device.setGpio(t.getNewValue());
                    if (FireflyLuciferin.guiManager != null) {
                        FireflyLuciferin.guiManager.stopCapturingThreads(true);
                    }
                    if (FireflyLuciferin.config != null && FireflyLuciferin.config.isMqttEnable()) {
                        FirmwareConfigDto gpioDto = new FirmwareConfigDto();
                        gpioDto.setGpio(Integer.parseInt(t.getNewValue()));
                        gpioDto.setMAC(device.getMac());
                        MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_FIRMWARE_CONFIG),
                                CommonUtility.writeValueAsString(gpioDto));
                    } else if (FireflyLuciferin.config != null && !FireflyLuciferin.config.isMqttEnable()) {
                        FireflyLuciferin.gpio = Integer.parseInt(t.getNewValue());
                        sendSerialParams();
                    }
                }
            } else {
                log.debug("Unsupported GPIO");
                FireflyLuciferin.guiManager.showAlert(Constants.GPIO_TITLE, Constants.GPIO_HEADER,
                        Constants.GPIO_CONTEXT, Alert.AlertType.ERROR);
            }
        });

    }

    /**
     * Send serialParams, this will cause a reboot on the microcontroller
     */
    void sendSerialParams() {

        java.awt.Color[] leds = new java.awt.Color[1];
        try {
            leds[0] = new java.awt.Color((int)(colorPicker.getValue().getRed() * 255),
                    (int)(colorPicker.getValue().getGreen() * 255),
                    (int)(colorPicker.getValue().getBlue() * 255));
            FireflyLuciferin.sendColorsViaUSB(leds);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }

    /**
     * Manage animation timer to update the UI every seconds
     */
    private void startAnimationTimer() {

        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0 ;
            @Override
            public void handle(long now) {
                now = now / 1_000_000_000;
                if (now - lastUpdate >= 1) {
                    lastUpdate = now;
                    if (NativeExecutor.isWindows()) {
                        manageDeviceList();
                    } else {
                        manageDeviceList();
                        setProducerValue("Producing @ " + FireflyLuciferin.FPS_PRODUCER + " FPS");
                        setConsumerValue("Consuming @ " + FireflyLuciferin.FPS_GW_CONSUMER + " FPS");
                        if (FireflyLuciferin.RUNNING && controlImage != null && controlImage.getUrl().contains("waiting")) {
                            controlImage = setImage(Constants.PlayerStatus.PLAY);
                            setButtonImage();
                        }
                    }
                }
            }
        };
        animationTimer.start();

    }

    /**
     * Init all the settings listener
     */
    private void initListeners() {

        // Toggle LED button listener
        toggleLed.setOnAction(e -> {
            if ((toggleLed.isSelected())) {
                toggleLed.setText(Constants.TURN_LED_OFF);
                turnOnLEDs(currentConfig, true);
                if (FireflyLuciferin.config != null) {
                    FireflyLuciferin.config.setToggleLed(true);
                }
            } else {
                toggleLed.setText(Constants.TURN_LED_ON);
                turnOffLEDs(currentConfig);
                if (FireflyLuciferin.config != null) {
                    FireflyLuciferin.config.setToggleLed(false);
                }
            }
        });
        // Color picker listener
        EventHandler<ActionEvent> colorPickerEvent = e -> turnOnLEDs(currentConfig, true);
        colorPicker.setOnAction(colorPickerEvent);
        // Gamma can be changed on the fly
        gamma.valueProperty().addListener((ov, t, gamma) -> {
            if (currentConfig != null && currentConfig.isMqttEnable()) {
                GammaDto gammaDto = new GammaDto();
                gammaDto.setGamma(Double.parseDouble(gamma));
                MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_GAMMA),
                        CommonUtility.writeValueAsString(gammaDto));
            }
            FireflyLuciferin.config.setGamma(Double.parseDouble(gamma));
        });
        // White temperature can be changed on the fly
        whiteTemperature.valueProperty().addListener((ov, t, kelvin) -> {
            FireflyLuciferin.whiteTemperature = whiteTemperature.getSelectionModel().getSelectedIndex() + 1;
            if (currentConfig != null && currentConfig.isMqttEnable()) {
                StateDto stateDto = new StateDto();
                stateDto.setState(Constants.ON);
                if (!(currentConfig.isMqttEnable() && FireflyLuciferin.RUNNING)) {
                    stateDto.setEffect(Constants.SOLID);
                }
                stateDto.setWhitetemp(FireflyLuciferin.whiteTemperature);
                MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), CommonUtility.writeValueAsString(stateDto));
            }
        });
        multiMonitor.valueProperty().addListener((ov, t, value) -> {
            if (!serialPort.isFocused()) {
                if (!value.equals(Constants.MULTIMONITOR_1)) {
                    if (serialPort.getItems().size() > 0 && serialPort.getItems().get(0).equals(Constants.SERIAL_PORT_AUTO)) {
                        serialPort.getItems().remove(0);
                        if (NativeExecutor.isWindows()) {
                            serialPort.setValue(Constants.SERIAL_PORT_COM + 1);
                        } else {
                            serialPort.setValue(Constants.SERIAL_PORT_TTY + 1);
                        }
                    }
                } else {
                    if (!serialPort.getItems().contains(Constants.SERIAL_PORT_AUTO)) {
                        serialPort.getItems().add(0, Constants.SERIAL_PORT_AUTO);
                    }
                }
            }
        });
        setSerialPortAvailableCombo();
        monitorNumber.valueProperty().addListener((ov, t, value) -> {
            DisplayInfo screenInfo = displayManager.getDisplayList().get(1);
            double scaleX = screenInfo.getScaleX();
            double scaleY = screenInfo.getScaleY();
            screenWidth.setText(String.valueOf((int) (screenInfo.width * scaleX)));
            screenHeight.setText(String.valueOf((int) (screenInfo.height * scaleY)));
            scaling.setValue(((int) (screenInfo.getScaleX() * 100)) + Constants.PERCENT);
        });
        brightness.valueProperty().addListener((ov, old_val, new_val) -> turnOnLEDs(currentConfig, false));
        splitBottomRow.setOnAction(e -> splitBottomRow());
        mqttEnable.setOnAction(e -> {
            if (!mqttEnable.isSelected()) mqttStream.setSelected(false);
        });
        mqttStream.setOnAction(e -> {
            if (mqttStream.isSelected()) mqttEnable.setSelected(true);
            initOutputDeviceChooser(false);
        });

    }

    /**
     * Add bold style to the available serial ports
     */
    void setSerialPortAvailableCombo() {
        Map<String, Boolean> availableDevices = FireflyLuciferin.getAvailableDevices();
        serialPort.setCellFactory(new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item);
                            this.getStyleClass().remove(Constants.CSS_CLASS_BOLD);
                            availableDevices.forEach((portName, isAvailable) -> {
                                if (item.contains(portName) && isAvailable) {
                                    this.getStyleClass().add(Constants.CSS_CLASS_BOLD);
                                } else if (item.contains(portName) && !isAvailable) {
                                    this.getStyleClass().add(Constants.CSS_CLASS_BOLD);
                                    this.getStyleClass().add(Constants.CSS_CLASS_RED);
                                }
                            });
                        } else {
                            setText(null);
                        }
                    }
                };
            }
        });
    }

    /**
     * Manage the device list tab update
     */
    void manageDeviceList() {

        if (!cellEdit) {
            Calendar calendar = Calendar.getInstance();
            Calendar calendarTemp = Calendar.getInstance();
            ObservableList<GlowWormDevice> deviceTableDataToRemove = FXCollections.observableArrayList();
            deviceTableData.forEach(glowWormDevice -> {
                calendar.setTime(new Date());
                calendarTemp.setTime(new Date());
                calendar.add(Calendar.SECOND, - 20);
                calendarTemp.add(Calendar.SECOND, - 60);
                try {
                    if (calendar.getTime().after(FireflyLuciferin.formatter.parse(glowWormDevice.getLastSeen()))
                            && FireflyLuciferin.formatter.parse(glowWormDevice.getLastSeen()).after(calendarTemp.getTime())) {
                        deviceTableDataToRemove.add(glowWormDevice);
                    }
                } catch (ParseException e) {
                    log.error(e.getMessage());
                }
            });
            deviceTableData.removeAll(deviceTableDataToRemove);
            deviceTable.refresh();
            if (mqttStream.isSelected()) {
                initOutputDeviceChooser(true);
            }
        }

    }

    /**
     * Init Save Button Text
     */
    private void setSaveButtonText() {

        if (currentConfig == null) {
            saveLedButton.setText(Constants.SAVE);
            saveSettingsButton.setText(Constants.SAVE);
            saveMQTTButton.setText(Constants.SAVE);
            saveMiscButton.setText(Constants.SAVE);
            saveDeviceButton.setText(Constants.SAVE);
            if (NativeExecutor.isWindows()) {
                saveLedButton.setPrefWidth(95);
                saveSettingsButton.setPrefWidth(95);
                saveMQTTButton.setPrefWidth(95);
                saveMiscButton.setPrefWidth(95);
                saveDeviceButton.setPrefWidth(95);
            } else {
                saveLedButton.setPrefWidth(125);
                saveSettingsButton.setPrefWidth(125);
                saveMQTTButton.setPrefWidth(125);
                saveMiscButton.setPrefWidth(125);
                saveDeviceButton.setPrefWidth(125);
            }
        } else {
            saveLedButton.setText(Constants.SAVE_AND_CLOSE);
            saveSettingsButton.setText(Constants.SAVE_AND_CLOSE);
            saveMQTTButton.setText(Constants.SAVE_AND_CLOSE);
            saveMiscButton.setText(Constants.SAVE_AND_CLOSE);
            saveDeviceButton.setText(Constants.SAVE_AND_CLOSE);
        }

    }

    /**
     * Show hide bottom row options
     */
    private void splitBottomRow() {
        if (splitBottomRow.isSelected()) {
            bottomLeftLed.setVisible(true);
            bottomRightLed.setVisible(true);
            bottomRowLed.setVisible(false);
            bottomLeftLedLabel.setVisible(true);
            bottomRightLedLabel.setVisible(true);
            bottomRowLedLabel.setVisible(false);
        } else {
            bottomLeftLed.setVisible(false);
            bottomRightLed.setVisible(false);
            bottomRowLed.setVisible(true);
            bottomLeftLedLabel.setVisible(false);
            bottomRightLedLabel.setVisible(false);
            bottomRowLedLabel.setVisible(true);
        }
    }

    /**
     * Save button event
     * @param e event
     */
    @FXML
    public void save(InputEvent e) {

        // No config found, init with a default config
        LEDCoordinate ledCoordinate = new LEDCoordinate();
        LinkedHashMap<Integer, LEDCoordinate> ledFullScreenMatrix = ledCoordinate.initFullScreenLedMatrix(Integer.parseInt(screenWidth.getText()),
                Integer.parseInt(screenHeight.getText()), Integer.parseInt(bottomRightLed.getText()), Integer.parseInt(rightLed.getText()),
                Integer.parseInt(topLed.getText()), Integer.parseInt(leftLed.getText()), Integer.parseInt(bottomLeftLed.getText()),
                Integer.parseInt(bottomRowLed.getText()), splitBottomRow.isSelected());
        LinkedHashMap<Integer, LEDCoordinate> ledLetterboxMatrix = ledCoordinate.initLetterboxLedMatrix(Integer.parseInt(screenWidth.getText()),
                Integer.parseInt(screenHeight.getText()), Integer.parseInt(bottomRightLed.getText()), Integer.parseInt(rightLed.getText()),
                Integer.parseInt(topLed.getText()), Integer.parseInt(leftLed.getText()), Integer.parseInt(bottomLeftLed.getText()),
                Integer.parseInt(bottomRowLed.getText()), splitBottomRow.isSelected());
        LinkedHashMap<Integer, LEDCoordinate> fitToScreenMatrix = ledCoordinate.initPillarboxMatrix(Integer.parseInt(screenWidth.getText()),
                Integer.parseInt(screenHeight.getText()), Integer.parseInt(bottomRightLed.getText()), Integer.parseInt(rightLed.getText()),
                Integer.parseInt(topLed.getText()), Integer.parseInt(leftLed.getText()), Integer.parseInt(bottomLeftLed.getText()),
                Integer.parseInt(bottomRowLed.getText()), splitBottomRow.isSelected());
        try {
            Configuration config = new Configuration(ledFullScreenMatrix, ledLetterboxMatrix, fitToScreenMatrix);
            config.setNumberOfCPUThreads(Integer.parseInt(numberOfThreads.getText()));
            setCaptureMethod(config);
            config.setConfigVersion(FireflyLuciferin.version);
            config.setSerialPort(serialPort.getValue());
            config.setScreenResX(Integer.parseInt(screenWidth.getText()));
            config.setScreenResY(Integer.parseInt(screenHeight.getText()));
            config.setLedStartOffset(Integer.parseInt(ledStartOffset.getText()));
            config.setOsScaling(Integer.parseInt((scaling.getValue()).replace(Constants.PERCENT,"")));
            config.setGamma(Double.parseDouble(gamma.getValue()));
            config.setWhiteTemperature(whiteTemperature.getSelectionModel().getSelectedIndex() + 1);
            config.setSerialPort(serialPort.getValue());
            config.setDefaultLedMatrix(aspectRatio.getValue().equals(Constants.AUTO_DETECT_BLACK_BARS) ?
                    Constants.AspectRatio.FULLSCREEN.getAspectRatio() : aspectRatio.getValue());
            config.setAutoDetectBlackBars(aspectRatio.getValue().equals(Constants.AUTO_DETECT_BLACK_BARS));
            switch (multiMonitor.getValue()) {
                case Constants.MULTIMONITOR_2 -> config.setMultiMonitor(2);
                case Constants.MULTIMONITOR_3 -> config.setMultiMonitor(3);
                default -> config.setMultiMonitor(1);
            }
            config.setMonitorNumber(monitorNumber.getValue());
            config.setDesiredFramerate(framerate.getValue().equals(Constants.UNLOCKED) ?
                    framerate.getValue() : framerate.getValue().split(" ")[0]);
            config.setMqttServer(mqttHost.getText() + ":" + mqttPort.getText());
            config.setMqttTopic(mqttTopic.getText());
            config.setMqttUsername(mqttUser.getText());
            config.setMqttPwd(mqttPwd.getText());
            config.setMqttEnable(mqttEnable.isSelected());
            config.setEyeCare(eyeCare.isSelected());
            config.setAutoStartCapture(autoStart.isSelected());
            config.setMqttStream(mqttStream.isSelected());
            config.setCheckForUpdates(checkForUpdates.isSelected());
            config.setSyncCheck(syncCheck.isSelected());
            config.setToggleLed(toggleLed.isSelected());
            config.setColorChooser((int)(colorPicker.getValue().getRed()*255) + "," + (int)(colorPicker.getValue().getGreen()*255) + ","
                    + (int)(colorPicker.getValue().getBlue()*255) + "," + (int)(colorPicker.getValue().getOpacity()*255));
            if (JavaFXStarter.whoAmI != 1) {
                Configuration mainConfig = sm.readConfig(true);
                mainConfig.setGamma(config.getGamma());
                mainConfig.setWhiteTemperature(config.getWhiteTemperature());
                mainConfig.setCheckForUpdates(checkForUpdates.isSelected());
                mainConfig.setSyncCheck(syncCheck.isSelected());
                mainConfig.setMultiMonitor(config.getMultiMonitor());
                mainConfig.setToggleLed(config.isToggleLed());
                mainConfig.setColorChooser(config.getColorChooser());
                if (NativeExecutor.isWindows()) {
                    mainConfig.setStartWithSystem(startWithSystem.isSelected());
                }
                sm.writeConfig(mainConfig, Constants.CONFIG_FILENAME);
            }
            if (config.getMultiMonitor() > 1) {
                switch (JavaFXStarter.whoAmI) {
                    case 1:
                        writeOtherConfig(config, Constants.CONFIG_FILENAME_2);
                        if (config.getMultiMonitor() == 3) writeOtherConfig(config, Constants.CONFIG_FILENAME_3);
                        break;
                    case 2:
                        if (config.getMultiMonitor() == 3) writeOtherConfig(config, Constants.CONFIG_FILENAME_3);
                        break;
                    case 3:
                        writeOtherConfig(config, Constants.CONFIG_FILENAME_2);
                        break;
                }
            }
            config.setTopLed(Integer.parseInt(topLed.getText()));
            config.setLeftLed(Integer.parseInt(leftLed.getText()));
            config.setRightLed(Integer.parseInt(rightLed.getText()));
            config.setBottomLeftLed(Integer.parseInt(bottomLeftLed.getText()));
            config.setBottomRightLed(Integer.parseInt(bottomRightLed.getText()));
            config.setBottomRowLed(Integer.parseInt(bottomRowLed.getText()));
            config.setOrientation(orientation.getValue());
            config.setBaudRate(baudRate.getValue());
            config.setBrightness((int)(brightness.getValue()/100 *255));
            config.setSplitBottomRow(splitBottomRow.isSelected());
            sm.writeConfig(config, null);
            boolean firstStartup = FireflyLuciferin.config == null;
            FireflyLuciferin.config = config;
            if (firstStartup || (JavaFXStarter.whoAmI == 1 && ((config.getMultiMonitor() == 2 && !sm.checkIfFileExist(Constants.CONFIG_FILENAME_2))
                    || (config.getMultiMonitor() == 3 && (!sm.checkIfFileExist(Constants.CONFIG_FILENAME_2) || !sm.checkIfFileExist(Constants.CONFIG_FILENAME_3)))) ) ) {
                writeOtherConfigNew(config);
                cancel(e);
            }
            if (!firstStartup) {
                String oldBaudrate = currentConfig.getBaudRate();
                boolean isBaudRateChanged = !baudRate.getValue().equals(currentConfig.getBaudRate());
                boolean isMqttTopicChanged = (!mqttTopic.getText().equals(currentConfig.getMqttTopic()) && config.isMqttEnable());
                if (isBaudRateChanged || isMqttTopicChanged) {
                    programFirmware(config, e, oldBaudrate, mqttTopic.getText(), isBaudRateChanged, isMqttTopicChanged);
                } else {
                    exit(e);
                }
            }
        } catch (IOException | CloneNotSupportedException ioException) {
            log.error("Can't write config file.");
        }

    }

    /**
     * Set capture method, write preferences to Windows Registry
     * @param config preferences
     */
    void setCaptureMethod(Configuration config) {

        NativeExecutor nativeExecutor = new NativeExecutor();
        if (NativeExecutor.isWindows()) {
            switch (captureMethod.getValue()) {
                case DDUPL -> config.setCaptureMethod(Configuration.CaptureMethod.DDUPL.name());
                case WinAPI -> config.setCaptureMethod(Configuration.CaptureMethod.WinAPI.name());
                case CPU -> config.setCaptureMethod(Configuration.CaptureMethod.CPU.name());
            }
            if (startWithSystem.isSelected()) {
                nativeExecutor.writeRegistryKey();
            } else {
                nativeExecutor.deleteRegistryKey();
            }
            config.setStartWithSystem(startWithSystem.isSelected());
        } else if (NativeExecutor.isMac()) {
            if (captureMethod.getValue() == Configuration.CaptureMethod.AVFVIDEOSRC) {
                config.setCaptureMethod(Configuration.CaptureMethod.AVFVIDEOSRC.name());
            }
        } else {
            if (captureMethod.getValue() == Configuration.CaptureMethod.XIMAGESRC) {
                config.setCaptureMethod(Configuration.CaptureMethod.XIMAGESRC.name());
            }
        }

    }

    /**
     * Program firmware, set baud rate and mqtt topic on all instances
     * @param config configuration on file
     * @param e event that launched
     * @param oldBaudrate baud rate before the change
     * @param mqttTopic swap microcontroller mqtt topic
     * @param isBaudRateChanged condition that monitor if baudrate is changed
     * @param isMqttTopicChanged condition that monitor if mqtt topipc is changed
     */
    void programFirmware(Configuration config, InputEvent e, String oldBaudrate, String mqttTopic, boolean isBaudRateChanged, boolean isMqttTopicChanged) throws IOException {

        FirmwareConfigDto firmwareConfigDto = new FirmwareConfigDto();
        if (currentConfig.isMqttEnable()) {
            if (deviceTableData != null && deviceTableData.size() > 0) {
                if (Constants.SERIAL_PORT_AUTO.equals(serialPort.getValue())) {
                    firmwareConfigDto.setMAC(deviceTableData.get(0).getMac());
                }
                deviceTableData.forEach(glowWormDevice -> {
                    if (glowWormDevice.getDeviceName().equals(serialPort.getValue()) || glowWormDevice.getDeviceIP().equals(serialPort.getValue())) {
                        firmwareConfigDto.setMAC(glowWormDevice.getMac());
                    }
                });
            }
            if (firmwareConfigDto.getMAC() == null || firmwareConfigDto.getMAC().isEmpty()) {
                log.error("No device can be programed");
            }
        }
        if (isBaudRateChanged) {
            Optional<ButtonType> result = FireflyLuciferin.guiManager.showAlert(Constants.BAUDRATE_TITLE, Constants.BAUDRATE_HEADER,
                    Constants.BAUDRATE_CONTEXT, Alert.AlertType.CONFIRMATION);
            ButtonType button = result.orElse(ButtonType.OK);
            if (button == ButtonType.OK) {
                if (currentConfig.isMqttEnable()) {
                    firmwareConfigDto.setBaudrate(String.valueOf(Constants.BaudRate.valueOf(Constants.BAUD_RATE_PLACEHOLDER + baudRate.getValue()).ordinal() + 1));
                    if (isMqttTopicChanged) {
                        firmwareConfigDto.setMqttopic(mqttTopic);
                    }
                    MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_FIRMWARE_CONFIG), CommonUtility.writeValueAsString(firmwareConfigDto));
                } else {
                    FireflyLuciferin.baudRate = Constants.BaudRate.valueOf(Constants.BAUD_RATE_PLACEHOLDER + baudRate.getValue()).ordinal() + 1;
                    sendSerialParams();
                }
                exit(e);
            } else if (button == ButtonType.CANCEL) {
                config.setBaudRate(oldBaudrate);
                baudRate.setValue(oldBaudrate);
                sm.writeConfig(config, null);
            }
        } else if (isMqttTopicChanged && currentConfig.isMqttEnable()) {
            firmwareConfigDto.setMqttopic(mqttTopic);
            MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_FIRMWARE_CONFIG), CommonUtility.writeValueAsString(firmwareConfigDto));
            exit(e);
        }

    }

    /**
     * Write other config files if there are more than one instance running
     * @param config configuration
     */
    void writeOtherConfigNew(Configuration config) throws IOException, CloneNotSupportedException {

        if (config.getMultiMonitor() == 2 || config.getMultiMonitor() == 3) {
            writeSingleConfig(config, Constants.CONFIG_FILENAME_2, 22, 1);
        }
        if (config.getMultiMonitor() == 3) {
            writeSingleConfig(config, Constants.CONFIG_FILENAME_3, 23, 2);
        }

    }

    /**
     * Write other config files if there are more than one instance running
     * @param config configuration
     * @param otherConfigFilename file to write
     */
    void writeOtherConfig(Configuration config, String otherConfigFilename) throws IOException, CloneNotSupportedException {

        Configuration otherConfig = sm.readConfig(otherConfigFilename);
        if (otherConfig != null) {
            otherConfig.setCheckForUpdates(checkForUpdates.isSelected());
            otherConfig.setSyncCheck(syncCheck.isSelected());
            otherConfig.setGamma(config.getGamma());
            otherConfig.setWhiteTemperature(config.getWhiteTemperature());
            otherConfig.setMultiMonitor(config.getMultiMonitor());
            otherConfig.setToggleLed(config.isToggleLed());
            otherConfig.setColorChooser(config.getColorChooser());
            if (NativeExecutor.isWindows()) {
                otherConfig.setStartWithSystem(startWithSystem.isSelected());
            }
            sm.writeConfig(otherConfig, otherConfigFilename);
        }

    }

    /**
     * Write a config file for an instance
     * @param config configuration
     * @param filename filename to write
     * @param comPort comport to use as defaults
     * @param monitorNum monitor number, it's relative to the instance number
     * @throws CloneNotSupportedException file exception
     * @throws IOException file exception
     */
    void writeSingleConfig(Configuration config, String filename, int comPort, int monitorNum) throws CloneNotSupportedException, IOException {

        Configuration tempConfiguration = (Configuration) config.clone();
        tempConfiguration.setSerialPort(Constants.SERIAL_PORT_COM + comPort);
        DisplayInfo screenInfo = displayManager.getDisplayList().get(monitorNum);
        double scaleX = screenInfo.getScaleX();
        double scaleY = screenInfo.getScaleY();
        tempConfiguration.setScreenResX((int) (screenInfo.width * scaleX));
        tempConfiguration.setScreenResY((int) (screenInfo.height * scaleY));
        tempConfiguration.setOsScaling((int) (screenInfo.getScaleX() * 100));
        tempConfiguration.setMonitorNumber(screenInfo.getFxDisplayNumber());

        sm.writeConfig(tempConfiguration, filename);

    }

    /**
     * Save and Exit button event
     * @param event event
     */
    @FXML
    public void exit(InputEvent event) {

        cancel(event);
        NativeExecutor.restartNativeInstance();

    }

    /**
     * Cancel button event
     */
    @FXML
    public void cancel(InputEvent event) {

        if (event != null) {
            animationTimer.stop();
            final Node source = (Node) event.getSource();
            final Stage stage = (Stage) source.getScene().getWindow();
            stage.hide();
        }

    }

    /**
     * Open browser to the GitHub project page
     * @param link GitHub
     */
    @FXML
    public void onMouseClickedGitHubLink(ActionEvent link) {

        FireflyLuciferin.guiManager.surfToGitHub();

    }

    /**
     * Start and stop capturing
     * @param e InputEvent
     */
    @FXML
    public void onMouseClickedPlay(InputEvent e) {

        controlImage = setImage(Constants.PlayerStatus.GREY);
        if (!FireflyLuciferin.communicationError) {
            if (FireflyLuciferin.RUNNING) {
                controlImage = setImage(Constants.PlayerStatus.STOP);
            } else {
                controlImage = setImage(Constants.PlayerStatus.PLAY_WAITING);
            }
            setButtonImage();
            if (FireflyLuciferin.RUNNING) {
                FireflyLuciferin.guiManager.stopCapturingThreads(true);
            } else {
                FireflyLuciferin.guiManager.startCapturingThreads();
            }
        }

    }

    /**
     * Show a canvas containing a test image for the LED Matrix in use
     * @param e event
     */
    @FXML
    public void showTestImage(InputEvent e) {

        TestCanvas testCanvas = new TestCanvas();
        testCanvas.buildAndShowTestImage(e);

    }

    /**
     * Turn ON LEDs
     * @param currentConfig stored config
     * @param setBrightness brightness level
     */
    void turnOnLEDs(Configuration currentConfig, boolean setBrightness) {

        if (setBrightness) {
            brightness.setValue((int)(colorPicker.getValue().getOpacity()*100));
        } else {
            colorPicker.setValue(Color.rgb((int)(colorPicker.getValue().getRed() * 255), (int)(colorPicker.getValue().getGreen() * 255),
                    (int)(colorPicker.getValue().getBlue() * 255), (brightness.getValue()/100)));
        }
        if (toggleLed.isSelected() || !setBrightness) {
            if (currentConfig != null && currentConfig.isMqttEnable()) {
                StateDto stateDto = new StateDto();
                stateDto.setState(Constants.ON);
                if (!(currentConfig.isMqttEnable() && FireflyLuciferin.RUNNING)) {
                    stateDto.setEffect(Constants.SOLID);
                }
                ColorDto colorDto = new ColorDto();
                colorDto.setR((int)(colorPicker.getValue().getRed() * 255));
                colorDto.setG((int)(colorPicker.getValue().getGreen() * 255));
                colorDto.setB((int)(colorPicker.getValue().getBlue() * 255));
                stateDto.setColor(colorDto);
                stateDto.setBrightness((int)((brightness.getValue() / 100) * 255));
                stateDto.setWhitetemp(FireflyLuciferin.config.getWhiteTemperature());
                MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), CommonUtility.writeValueAsString(stateDto));
                FireflyLuciferin.usbBrightness = (int)((brightness.getValue() / 100) * 255);
            } else if (currentConfig != null && !currentConfig.isMqttEnable()) {
                FireflyLuciferin.usbBrightness = (int)((brightness.getValue() / 100) * 255);
                sendSerialParams();
            }
        }

    }

    /**
     * Turn ON LEDs
     * @param currentConfig stored config
     */
    void turnOffLEDs(Configuration currentConfig) {

        if (currentConfig != null) {
            if (currentConfig.isMqttEnable()) {
                StateDto stateDto = new StateDto();
                stateDto.setState(Constants.OFF);
                stateDto.setEffect(Constants.SOLID);
                stateDto.setBrightness(FireflyLuciferin.config.getBrightness());
                stateDto.setWhitetemp(FireflyLuciferin.config.getWhiteTemperature());
                MQTTManager.publishToTopic(MQTTManager.getMqttTopic(Constants.MQTT_SET), CommonUtility.writeValueAsString(stateDto));
            } else {
                java.awt.Color[] leds = new java.awt.Color[1];
                try {
                    leds[0] = new java.awt.Color(0, 0, 0);
                    FireflyLuciferin.usbBrightness = 0;
                    FireflyLuciferin.sendColorsViaUSB(leds);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }

    }

    /**
     * Force TextField to be numeric
     * @param textField numeric fields
     */
    void addTextFieldListener(TextField textField) {

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches("\\d*")) return;
            textField.setText(newValue.replaceAll("[^\\d]", ""));
        });

    }

    /**
     * Set form tooltips
     */
    void setTooltips() {

        topLed.setTooltip(createTooltip(Constants.TOOLTIP_TOPLED));
        leftLed.setTooltip(createTooltip(Constants.TOOLTIP_LEFTLED));
        rightLed.setTooltip(createTooltip(Constants.TOOLTIP_RIGHTLED));
        bottomLeftLed.setTooltip(createTooltip(Constants.TOOLTIP_BOTTOMLEFTLED));
        bottomRightLed.setTooltip(createTooltip(Constants.TOOLTIP_BOTTOMRIGHTLED));
        bottomRowLed.setTooltip(createTooltip(Constants.TOOLTIP_BOTTOMROWLED));
        orientation.setTooltip(createTooltip(Constants.TOOLTIP_ORIENTATION));
        screenWidth.setTooltip(createTooltip(Constants.TOOLTIP_SCREENWIDTH));
        screenHeight.setTooltip(createTooltip(Constants.TOOLTIP_SCREENHEIGHT));
        ledStartOffset.setTooltip(createTooltip(Constants.TOOLTIP_LEDSTARTOFFSET));
        scaling.setTooltip(createTooltip(Constants.TOOLTIP_SCALING));
        gamma.setTooltip(createTooltip(Constants.TOOLTIP_GAMMA));
        whiteTemperature.setTooltip(createTooltip(Constants.TOOLTIP_WHITE_TEMP));
        if (NativeExecutor.isWindows()) {
            captureMethod.setTooltip(createTooltip(Constants.TOOLTIP_CAPTUREMETHOD));
            startWithSystem.setTooltip(createTooltip(Constants.TOOLTIP_START_WITH_SYSTEM));
        } else if (NativeExecutor.isMac()) {
            captureMethod.setTooltip(createTooltip(Constants.TOOLTIP_MACCAPTUREMETHOD));
        } else {
            captureMethod.setTooltip(createTooltip(Constants.TOOLTIP_LINUXCAPTUREMETHOD));
        }
        numberOfThreads.setTooltip(createTooltip(Constants.TOOLTIP_NUMBEROFTHREADS));
        serialPort.setTooltip(createTooltip(Constants.TOOLTIP_SERIALPORT));
        aspectRatio.setTooltip(createTooltip(Constants.TOOLTIP_ASPECTRATIO));
        multiMonitor.setTooltip(createTooltip(Constants.TOOLTIP_MULTIMONITOR));
        monitorNumber.setTooltip(createTooltip(Constants.TOOLTIP_MONITORNUMBER));
        framerate.setTooltip(createTooltip(Constants.TOOLTIP_FRAMERATE));
        mqttHost.setTooltip(createTooltip(Constants.TOOLTIP_MQTTHOST));
        mqttPort.setTooltip(createTooltip(Constants.TOOLTIP_MQTTPORT));
        mqttTopic.setTooltip(createTooltip(Constants.TOOLTIP_MQTTTOPIC));
        mqttUser.setTooltip(createTooltip(Constants.TOOLTIP_MQTTUSER));
        mqttPwd.setTooltip(createTooltip(Constants.TOOLTIP_MQTTPWD));
        mqttEnable.setTooltip(createTooltip(Constants.TOOLTIP_MQTTENABLE));
        eyeCare.setTooltip(createTooltip(Constants.TOOLTIP_EYE_CARE));
        autoStart.setTooltip(createTooltip(Constants.TOOLTIP_AUTOSTART));
        mqttStream.setTooltip(createTooltip(Constants.TOOLTIP_MQTTSTREAM));
        checkForUpdates.setTooltip(createTooltip(Constants.TOOLTIP_CHECK_UPDATES));
        syncCheck.setTooltip(createTooltip(Constants.TOOLTIP_SYNC_CHECK));
        brightness.setTooltip(createTooltip(Constants.TOOLTIP_BRIGHTNESS));
        splitBottomRow.setTooltip(createTooltip(Constants.TOOLTIP_SPLIT_BOTTOM_ROW));
        baudRate.setTooltip(createTooltip(Constants.TOOLTIP_BAUD_RATE));
        if (currentConfig == null) {
            if (!NativeExecutor.isWindows()) {
                playButton.setTooltip(createTooltip(Constants.TOOLTIP_PLAYBUTTON_NULL, 50, 6000));
            }
            saveLedButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVELEDBUTTON_NULL));
            saveMQTTButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON_NULL));
            saveMiscButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON_NULL));
            saveSettingsButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVESETTINGSBUTTON_NULL));
            saveDeviceButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEDEVICEBUTTON_NULL));
        } else {
            if (!NativeExecutor.isWindows()) {
                playButton.setTooltip(createTooltip(Constants.TOOLTIP_PLAYBUTTON, 200, 6000));
            }
            saveLedButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVELEDBUTTON,200, 6000));
            saveMQTTButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON,200, 6000));
            saveMiscButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEMQTTBUTTON,200, 6000));
            saveSettingsButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVESETTINGSBUTTON,200, 6000));
            saveDeviceButton.setTooltip(createTooltip(Constants.TOOLTIP_SAVEDEVICEBUTTON,200, 6000));
            showTestImageButton.setTooltip(createTooltip(Constants.TOOLTIP_SHOWTESTIMAGEBUTTON,200, 6000));
        }

    }

    /**
     * Set tooltip properties
     * @param text tooltip string
     */
    public Tooltip createTooltip(String text) {

        Tooltip tooltip;
        tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(500));
        tooltip.setHideDelay(Duration.millis(6000));
        return tooltip;

    }

    /**
     * Set tooltip properties width delays
     * @param text tooltip string
     * @param showDelay delay used to show the tooltip
     * @param hideDelay delay used to hide the tooltip
     */
    public Tooltip createTooltip(String text, int showDelay, int hideDelay) {

        Tooltip tooltip;
        tooltip = new Tooltip(text);
        tooltip.setShowDelay(Duration.millis(showDelay));
        tooltip.setHideDelay(Duration.millis(hideDelay));
        return tooltip;

    }

    /**
     * Lock TextField in a numeric state
     */
    void setNumericTextField() {

        addTextFieldListener(screenWidth);
        addTextFieldListener(screenHeight);
        addTextFieldListener(ledStartOffset);
        addTextFieldListener(numberOfThreads);
        addTextFieldListener(mqttPort);
        addTextFieldListener(topLed);
        addTextFieldListener(leftLed);
        addTextFieldListener(rightLed);
        addTextFieldListener(bottomLeftLed);
        addTextFieldListener(bottomRightLed);
        addTextFieldListener(bottomRowLed);

    }

    /**
     * Set and return LED tab image
     * @param playerStatus PLAY, STOP, GREY
     * @return tray icon
     */
    Image setImage(Constants.PlayerStatus playerStatus) {

        String imgPath = "";
        if (currentConfig == null) {
            imgPath = Constants.IMAGE_CONTROL_PLAY;
        } else {
            switch (playerStatus) {
                case PLAY:
                    switch (JavaFXStarter.whoAmI) {
                        case 1:
                            if ((currentConfig.getMultiMonitor() == 1)) {
                                imgPath = Constants.IMAGE_CONTROL_PLAY;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_RIGHT;
                            }
                            break;
                        case 2:
                            if ((currentConfig.getMultiMonitor() == 2)) {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_LEFT;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_CENTER;
                            }
                            break;
                        case 3:
                            imgPath = Constants.IMAGE_CONTROL_PLAY_LEFT;
                            break;
                    }
                    break;
                case PLAY_WAITING:
                    switch (JavaFXStarter.whoAmI) {
                        case 1:
                            if ((currentConfig.getMultiMonitor() == 1)) {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING_RIGHT;
                            }
                            break;
                        case 2:
                            if ((currentConfig.getMultiMonitor() == 2)) {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING_LEFT;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING_CENTER;
                            }
                            break;
                        case 3:
                            imgPath = Constants.IMAGE_CONTROL_PLAY_WAITING_LEFT;
                            break;
                    }
                    break;
                case STOP:
                    switch (JavaFXStarter.whoAmI) {
                        case 1:
                            if ((currentConfig.getMultiMonitor() == 1)) {
                                imgPath = Constants.IMAGE_CONTROL_LOGO;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_LOGO_RIGHT;
                            }
                            break;
                        case 2:
                            if ((currentConfig.getMultiMonitor() == 2)) {
                                imgPath = Constants.IMAGE_CONTROL_LOGO_LEFT;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_LOGO_CENTER;
                            }
                            break;
                        case 3:
                            imgPath = Constants.IMAGE_CONTROL_LOGO_LEFT;
                            break;
                    }
                    break;
                case GREY:
                    switch (JavaFXStarter.whoAmI) {
                        case 1:
                            if ((currentConfig.getMultiMonitor() == 1)) {
                                imgPath = Constants.IMAGE_CONTROL_GREY;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_GREY_RIGHT;
                            }
                            break;
                        case 2:
                            if ((currentConfig.getMultiMonitor() == 2)) {
                                imgPath = Constants.IMAGE_CONTROL_GREY_LEFT;
                            } else {
                                imgPath = Constants.IMAGE_CONTROL_GREY_CENTER;
                            }
                            break;
                        case 3:
                            imgPath = Constants.IMAGE_CONTROL_GREY_LEFT;
                            break;
                    }
                    break;
            }
        }
        return new Image(this.getClass().getResource(imgPath).toString(), true);

    }

    /**
     * Initilize output device chooser
     * @param initCaptureMethod re-init capture method
     */
    void initOutputDeviceChooser(boolean initCaptureMethod) {

        if (!mqttStream.isSelected()) {
            String deviceInUse = serialPort.getValue();
            comWirelessLabel.setText(Constants.OUTPUT_DEVICE);
            serialPort.getItems().clear();
            serialPort.getItems().add(Constants.SERIAL_PORT_AUTO);
            if (initCaptureMethod) {
                captureMethod.getItems().clear();
                if (NativeExecutor.isWindows()) {
                    captureMethod.getItems().addAll(Configuration.CaptureMethod.DDUPL, Configuration.CaptureMethod.WinAPI, Configuration.CaptureMethod.CPU);
                } else if (NativeExecutor.isMac()) {
                    captureMethod.getItems().addAll(Configuration.CaptureMethod.AVFVIDEOSRC);
                }
            }
            if (NativeExecutor.isWindows()) {
                for (int i=0; i<=256; i++) {
                    serialPort.getItems().add(Constants.SERIAL_PORT_COM + i);
                }
            } else {
                for (int i=0; i<=256; i++) {
                    serialPort.getItems().add(Constants.SERIAL_PORT_TTY + i);
                }
            }
            serialPort.setValue(deviceInUse);
        } else {
            comWirelessLabel.setText(Constants.OUTPUT_DEVICE);
            if (!serialPort.isFocused()) {
                String deviceInUse = serialPort.getValue();
                serialPort.getItems().clear();
                deviceTableData.forEach(glowWormDevice -> serialPort.getItems().add(glowWormDevice.getDeviceName()));
                serialPort.setValue(deviceInUse);
            }
        }

    }

    /**
     * Set button image
     */
    private void setButtonImage() {

        imageView = new ImageView(controlImage);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);
        playButton.setGraphic(imageView);

    }

    /**
     * Return the observable devices list
     * @return devices list
     */
    public ObservableList<GlowWormDevice> getDeviceTableData() {
        return deviceTableData;
    }

    public StringProperty producerValueProperty() {
        return producerValue;
    }

    public void setProducerValue(String producerValue) {
        this.producerValue.set(producerValue);
    }

    public StringProperty consumerValueProperty() {
        return consumerValue;
    }

    public void setConsumerValue(String consumerValue) {
        this.consumerValue.set(consumerValue);
    }

}
