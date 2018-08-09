/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package connector.utils;

import connector.constant.Switch;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Класс для загрузки настройки программы, сделан синглтоном
 *
 * @author Yura
 */
public class ProjectProperties {

    private final String LANGUAGE_FILE = "language_file";
    private final String SERVER_ICON = "server_icon";
    private final String CLIENT_ICON = "client_icon";
    private final String BACKGROUND = "background";
    private final String SERVER_NAME = "server_name";
    private final String CLIENT_NAME = "client_name";
    private final String SOUND_FILE = "sound_file";
    private final String SOUND_SETTING = "sound_setting";
    private final String POP_UP_SETTING = "pop_up_setting";

    private final String S = System.getProperty("file.separator"); // separator
    private final String CONFIG_FILE_NAME = "config.properties";
    private final String PATH_TO_INNER_RESOURCES = "/connector/resources/";
    private final String DEFAULT_ICON = "java.png";

    private static final Logger log = Logger.getLogger(ProjectProperties.class.getName());

    private Properties projProperties;
    private static Properties stringsFile;
    private String LANGUAGE_FILE_NAME;
    private String PATH;

    private Boolean isOuterProperties;

    public String SERVER_NAME_SELECT;
    public String CLIENT_NAME_SELECT;

    public File SOUND_FILE_FILE;

    public Image SERVER_IMAGE;
    public Image CLIENT_IMAGE;
    public Image CLIENT_BACKGROUND = null;

    public Switch SOUND_SWITCH;
    public Switch POP_UP_SWITCH;

    private static ProjectProperties instance;

    public static synchronized ProjectProperties getInstance() {
        instance = instance == null ? new ProjectProperties() : instance;
        return instance;
    }

    public static String getString(String str) {
        return stringsFile.getProperty(str);
    }

    private ProjectProperties() {
        this.isOuterProperties = false;
        projProperties = new Properties();
        stringsFile = new Properties();
        try {
            PATH = getCurrentDir();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "Cannot get current Dir");
        }

        // создаем поток для чтения из файла
        try (FileInputStream propertieStream = new FileInputStream(PATH + CONFIG_FILE_NAME)) {
            projProperties.load(propertieStream);
            isOuterProperties = true;
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Cannot load outer properties");
        }

        // Если из внешних загрузить не получилось, берём проперти из jar
        if (!isOuterProperties) {
            try {
                URL url = getClass().getClassLoader().getResource("connector/resources/" + CONFIG_FILE_NAME);
                if (url != null) {
                    projProperties.load(url.openStream());
                }
            } catch (Exception ex) {
                log.log(Level.SEVERE, "Cannot load inner properties", ex);
            }
        }

        SERVER_NAME_SELECT = projProperties.getProperty(SERVER_NAME);
        CLIENT_NAME_SELECT = projProperties.getProperty(CLIENT_NAME);

        SOUND_SWITCH = projProperties.getProperty(SOUND_SETTING).toLowerCase().equals(Switch.ON.getMode())
                ? Switch.ON
                : Switch.OFF;
        POP_UP_SWITCH = projProperties.getProperty(POP_UP_SETTING).toLowerCase().equals(Switch.ON.getMode())
                ? Switch.ON
                : Switch.OFF;

        LANGUAGE_FILE_NAME = projProperties.getProperty(LANGUAGE_FILE);
        try {
            if (isOuterProperties) {
                SERVER_IMAGE = Toolkit.getDefaultToolkit().getImage(PATH + projProperties.getProperty(SERVER_ICON));
                CLIENT_IMAGE = Toolkit.getDefaultToolkit().getImage(PATH + projProperties.getProperty(CLIENT_ICON));
                CLIENT_BACKGROUND = Toolkit.getDefaultToolkit().getImage(PATH + projProperties.getProperty(BACKGROUND));
                SOUND_FILE_FILE = new File(PATH + projProperties.getProperty(SOUND_FILE));
                try (FileInputStream stringStream = new FileInputStream(PATH + LANGUAGE_FILE_NAME)) {
                    stringsFile.load(stringStream);
                } catch (IOException e) {
                    log.log(Level.SEVERE, "Cannot load outer resources", e);
                }
            } else {
                SERVER_IMAGE = ImageIO.read(getClass().getResourceAsStream(PATH_TO_INNER_RESOURCES + DEFAULT_ICON));
                CLIENT_IMAGE = ImageIO.read(getClass().getResourceAsStream(PATH_TO_INNER_RESOURCES + DEFAULT_ICON));
                try (InputStream inputStream = getClass().getResourceAsStream(PATH_TO_INNER_RESOURCES + LANGUAGE_FILE_NAME)) {
                    stringsFile.load(inputStream);
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Cannot load inner resources", e);
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while load resources", e);
        }
    }

    public Image buildBackground() {
        return Toolkit.getDefaultToolkit().getImage(PATH + projProperties.getProperty(BACKGROUND));
    }

    private String getCurrentDir() throws IOException {
        // определяем текущий каталог
        File currentDir = new File(".");
        return currentDir.getCanonicalPath() + S;
    }

    //<editor-fold defaultstate="collapsed" desc="get-set">
    public Properties getProjProperties() {
        return projProperties;
    }

    public void setProjProperties(Properties projProperties) {
        this.projProperties = projProperties;
    }

    public Properties getStringsFile() {
        return stringsFile;
    }

    public void setStringsFile(Properties stringsFile) {
        this.stringsFile = stringsFile;
    }
    //</editor-fold>
}
