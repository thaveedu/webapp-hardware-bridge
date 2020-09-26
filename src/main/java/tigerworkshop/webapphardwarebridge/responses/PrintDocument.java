package tigerworkshop.webapphardwarebridge.responses;

import lombok.Getter;
import lombok.Setter;
import tigerworkshop.webapphardwarebridge.utils.AnnotatedPrintable;

import java.util.ArrayList;

@Getter
@Setter
public class PrintDocument {
    String type;
    String url;
    String id;
    Integer qty = 1;
    String file_content;
    String raw_content;
    ArrayList<AnnotatedPrintable.AnnotatedPrintableAnnotation> extras = new ArrayList<>();
}