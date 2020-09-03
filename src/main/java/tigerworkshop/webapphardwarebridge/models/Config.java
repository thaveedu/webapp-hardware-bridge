package tigerworkshop.webapphardwarebridge.models;

import lombok.Data;

import java.util.HashMap;

@Data
public class Config {
    ConfigWebSocketServer server = new ConfigWebSocketServer();
    ConfigCloudProxy cloudProxy = new ConfigCloudProxy();
    ConfigDocument document = new ConfigDocument();
    ConfigPrint print = new ConfigPrint();
    ConfigAuthentication authentication = new ConfigAuthentication();
    HashMap<String, ConfigPrinter> printers = new HashMap<>();
    HashMap<String, ConfigSerial> serials = new HashMap<>();

    public String getUri() {
        return (getServer().getTlsEnabled() ? "wss" : "ws") + "://" + getServer().getAddress() + ":" + getServer().getPort();
    }

    @Data
    private static class ConfigWebSocketServer {
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
    private static class ConfigDocument {
        Boolean downloadIgnoreTLSCertificateError = false;
        Integer downloadTimeout = 30;
        String path = "documents/";
    }

    @Data
    private static class ConfigPrint {
        Boolean fallbackToDefaultPrinter = false;
        Boolean autoRotatePDF = false;
        Boolean resetImageableArea = true;
        Integer printerDPI = 0;
    }

    @Data
    private static class ConfigCloudProxy {
        Boolean enabled = false;
        String url = "ws://127.0.0.1:22212";
        Integer timeout = 30;
    }

    @Data
    private static class ConfigAuthentication {
        Boolean enabled = false;
        String token = "ws://127.0.0.1:22212";
    }

    @Data
    private static class ConfigPrinter {
        String name;
        // TODO: What to set?
    }

    @Data
    private static class ConfigSerial {
        String name;
        // TODO: What to set?
    }
}