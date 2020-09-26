package tigerworkshop.webapphardwarebridge.models;

import lombok.Data;

import java.util.HashMap;

@Data
public class Config {
    ConfigWebSocketServer server = new ConfigWebSocketServer();
    ConfigCloudProxy cloudProxy = new ConfigCloudProxy();
    ConfigDownloader downloader = new ConfigDownloader();
    ConfigPrint print = new ConfigPrint();
    ConfigAuthentication authentication = new ConfigAuthentication();
    HashMap<String, ConfigPrinter> printers = new HashMap<>();
    HashMap<String, ConfigSerial> serials = new HashMap<>();

    public String getUri() {
        return (getServer().getTlsEnabled() ? "wss" : "ws") + "://" + getServer().getAddress() + ":" + getServer().getPort();
    }

    @Data
    public static class ConfigWebSocketServer {
        String address = "127.0.0.1";
        String bind = "0.0.0.0";
        Integer port = 12212;
        Boolean tlsEnabled = false;
        Boolean tlsSelfSigned = true;
        String tlsCert = "tls/default-cert.pem";
        String tlsKey = "tls/default-key.pem";
        String tlsCaBundle = "";
    }

    @Data
    public static class ConfigDownloader {
        Boolean ignoreTLSCertificateError = false;
        String path = "documents/";
        Integer timeout = 30;
    }

    @Data
    public static class ConfigPrint {
        Boolean fallbackToDefaultPrinter = false;
        Boolean autoRotatePDF = false;
        Boolean resetImageableArea = true;
        Integer printerDPI = 0;
    }

    @Data
    public static class ConfigCloudProxy {
        Boolean enabled = false;
        String url = "ws://127.0.0.1:22212";
        Integer timeout = 30;
    }

    @Data
    public static class ConfigAuthentication {
        Boolean enabled = false;
        String token = "ws://127.0.0.1:22212";
    }

    @Data
    public static class ConfigPrinter {
        String name;
        // TODO: What to set?
    }

    @Data
    public static class ConfigSerial {
        String name;
        // TODO: What to set?
    }
}