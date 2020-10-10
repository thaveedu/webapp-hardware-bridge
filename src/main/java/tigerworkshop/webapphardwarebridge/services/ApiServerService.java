package tigerworkshop.webapphardwarebridge.services;

import com.google.gson.Gson;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.SimpleWebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.models.Config;

import java.io.File;
import java.io.IOException;
public class ApiServerService extends SimpleWebServer {
    private static final Logger logger = LoggerFactory.getLogger("HTTPConfiguratorService");
    private static final ConfigService configService = ConfigService.getInstance();
    private static final Gson gson = new Gson();

    public ApiServerService(){
        super(configService.getConfig().getApi().getBind(), configService.getConfig().getApi().getPort(), new File("web"), true);
    }

    public void start() throws IOException  {
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        logger.info("HTTP Configurator started on {}", configService.getConfig().getApiUri());
    }

    @Override
    public Response serve(IHTTPSession session) {
        logger.trace(session.getMethod() + " " + session.getUri());

        try {
            // Simple REST API for config.json
            if (session.getUri().equals("/config.json")) {
                if (session.getMethod() == Method.GET) {
                    return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(configService.getConfig()));
                } else if (session.getMethod() == Method.PUT) {
                    int contentLength = Integer.parseInt(session.getHeaders().get("content-length"));
                    byte[] buffer = new byte[contentLength];
                    session.getInputStream().read(buffer, 0, contentLength);

                    String json = new String(buffer);
                    Config config = gson.fromJson(json, Config.class);

                    configService.replaceConfig(config);

                    return newFixedLengthResponse(Response.Status.OK, "application/json", gson.toJson(configService.getConfig()));
                }
            }
        } catch (Exception e) {
            logger.error("Error on HTTP request {} {}: {}", session.getMethod(), session.getUri(), e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }

        // Serve file by parent routine
        return super.serve(session);
    }
}
