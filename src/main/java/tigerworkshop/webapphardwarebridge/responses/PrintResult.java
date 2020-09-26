package tigerworkshop.webapphardwarebridge.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class PrintResult {
    private int status;
    private String id;
    private String message;
}
