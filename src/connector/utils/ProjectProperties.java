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

    private static final String LANGUAGE_FILE = "language_file";
    private static final String SERVER_ICON = "server_icon";
    private static final String CLIENT_ICON = "client_icon";
    private static final String BACKGROUND = "background";
    private static final String SERVER_NAME = "server_name";
    private static final String CLIENT_NAME = "client_name";
    private static final String SOUND_FILE = "sound_file";
    private static final String SOUND_SETTING = "sound_setting";
    private static final String POP_UP_SETTING = "pop_up_setting";

    private static final String S = System.getProperty("file.separator"); // separator
    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String PATH_TO_INNER_RESOURCES = ".." + S + "resources" + S;
    private static final String PATH_TO_INNER_PROPERTIES = PATH_TO_INNER_RESOURCES + CONFIG_FILE_NAME;
    private static final String DEFAULT_ICON = "java.png";
    private static ProjectProperties instance;
    private static Properties projProperties;
    private static Properties stringsFile;
    private static String LANGUAGE_FILE_NAME;
    private static String PATH;

    private static Boolean isInnerProperties = true;

    public static String SERVER_NAME_SELECT;
    public static String CLIENT_NAME_SELECT;

    public static File SOUND_FILE_FILE;

    public static Image SERVER_IMAGE;
    public static Image CLIENT_IMAGE;
    public static Image CLIENT_BACKGROUND = null;

    public static Switch SOUND_SWITCH;
    public static Switch POP_UP_SWITCH;

    public static synchronized ProjectProperties getInstance() {
        return instance == null ? new ProjectProperties() : instance;
    }

    private ProjectProperties() {
        projProperties = new Properties();
        stringsFile = new Properties();

        FileInputStream propertieStream = null;
        try {
            // Читаем из внешних propertie файлов
            PATH = getCurrentDir();
            // создаем поток для чтения из файла
            propertieStream = new FileInputStream(PATH + CONFIG_FILE_NAME);
            projProperties.load(propertieStream);
            isInnerProperties = false;
        } catch (Exception ex) {
            // Если из внешних загрузить не получилось, берём проперти из jar
            Logger.getLogger("Cannot load projProperties from outer file");
//            try (FileInputStream fileInputStream = new FileInputStream(PATH_TO_INNER_PROPERTIES)) {
            try (InputStream inputStream = getClass().getResourceAsStream(PATH_TO_INNER_PROPERTIES)) {
                projProperties.load(inputStream);
            } catch (Exception ex1) {
                Logger.getLogger(ProjectProperties.class.getName()).log(Level.SEVERE, null, ex1);
            }
        } finally {
            if (propertieStream != null) {
                try {
                    propertieStream.close();
                } catch (IOException ex1) {
                    Logger.getLogger(ProjectProperties.class.getName())
                            .log(Level.SEVERE, "Cannot close propertieStream", ex1);
                }
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
            if (!isInnerProperties) {
                SERVER_IMAGE = Toolkit.getDefaultToolkit().getImage(PATH + projProperties.getProperty(SERVER_ICON));
                CLIENT_IMAGE = Toolkit.getDefaultToolkit().getImage(PATH + projProperties.getProperty(CLIENT_ICON));
                CLIENT_BACKGROUND = Toolkit.getDefaultToolkit().getImage(PATH + projProperties.getProperty(BACKGROUND));
                SOUND_FILE_FILE = new File(PATH + projProperties.getProperty(SOUND_FILE));
                try (FileInputStream stringStream = new FileInputStream(PATH + LANGUAGE_FILE_NAME)) {
                    stringsFile.load(stringStream);
                } catch (IOException e) {
                    Logger.getLogger(ProjectProperties.class.getName()).log(Level.SEVERE, "Cannot close stringStream", e);
                }
            } else {
                SERVER_IMAGE = ImageIO.read(getClass().getResourceAsStream(PATH_TO_INNER_RESOURCES + DEFAULT_ICON));
                CLIENT_IMAGE = ImageIO.read(getClass().getResourceAsStream(PATH_TO_INNER_RESOURCES + DEFAULT_ICON));
                try (InputStream inputStream = getClass().getResourceAsStream(PATH_TO_INNER_RESOURCES + LANGUAGE_FILE_NAME)) {
                    stringsFile.load(inputStream);
                } catch (Exception ex1) {
                    Logger.getLogger(ProjectProperties.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        } catch (Exception e) {
        }
//        try (FileInputStream stringStream = new FileInputStream(PATH + LANGUAGE_FILE_NAME)) {
//            stringsFile.load(stringStream);
//        } catch (IOException e) {
//            Logger.getLogger(ProjectProperties.class.getName())
//                    .log(Level.SEVERE, "Cannot close stringStream", e);
//        }
    }

    public Image buildBackground() {
        return Toolkit.getDefaultToolkit().getImage(PATH + projProperties.getProperty(BACKGROUND));
    }

    private static String getCurrentDir() throws IOException {
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
