package tigerworkshop.webapphardwarebridge.responses;

import tigerworkshop.webapphardwarebridge.utils.AnnotatedPrintable;

import java.util.ArrayList;

public class PrintDocument {
    String category;
    String type;
    String url;
    String id;
    Integer qty = 1;
    String file_content;
    String raw_content;
    ArrayList<AnnotatedPrintable.AnnotatedPrintableAnnotation> extras = new ArrayList<>();

    public String getType() {
        return this.type;
    }

    public String getUrl() {
        return this.url;
    }

    public String getCategory() {
        return this.category;
    }


    public String getId() {
        return id;
    }

    public Integer getQty() {
        return qty;
    }

    public String getFileContent() {
        return file_content;
    }

    public String getRawContent() {
        return raw_content;
    }

    public ArrayList<AnnotatedPrintable.AnnotatedPrintableAnnotation> getExtras() {
        return extras;
    }

    @Override
    public String toString() {
        return "PrintDocument{" +
                "category='" + category + '\'' +
                "type='" + type + '\'' +
                ", url='" + url + '\'' +
                ", id='" + id + '\'' +
                ", qty=" + qty +
                ", file_content='" + file_content + '\'' +
                ", raw_content='" + raw_content + '\'' +
                ", extras=" + extras +
                '}';
    }
}