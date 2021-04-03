package tigerworkshop.webapphardwarebridge.websocketservices;

import com.google.gson.Gson;
import com.lowagie.text.DocumentException;
import org.apache.commons.codec.binary.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.interfaces.NotificationListenerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.responses.PrintResult;
import tigerworkshop.webapphardwarebridge.services.DocumentService;
import tigerworkshop.webapphardwarebridge.services.SettingService;
import tigerworkshop.webapphardwarebridge.utils.AnnotatedPrintable;
import tigerworkshop.webapphardwarebridge.utils.ImagePrintable;

import javax.imageio.ImageIO;
import javax.print.*;
import java.awt.*;
import java.awt.print.*;
import java.io.File;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.extend.FontResolver;
import java.io.*; // for file I/O
import java.net.URLDecoder;

public class PrinterWebSocketService implements WebSocketServiceInterface {
    private static final Logger logger = LoggerFactory.getLogger(PrinterWebSocketService.class);
    private WebSocketServerInterface server = null;
    private final Gson gson = new Gson();

    private final SettingService settingService = SettingService.getInstance();
    private NotificationListenerInterface notificationListener;

    public PrinterWebSocketService() {
        logger.info("Starting PrinterWebSocketService");
    }

    public void setNotificationListener(NotificationListenerInterface notificationListener) {
        this.notificationListener = notificationListener;
    }

    @Override
    public void start() {
        server.subscribe(this, getChannel());
    }

    @Override
    public void stop() {
        logger.info("Stopping PrinterWebSocketService");
        server.unsubscribe(this, getChannel());
    }

    @Override
    public void onDataReceived(String message) {
        logger.info("Message recieved "+message);
        try {
            PrintDocument printDocument = gson.fromJson(message, PrintDocument.class);
            DocumentService.getInstance().prepareDocument(printDocument);
            printDocument(printDocument);
        } catch (Exception e) {
            logger.error(e.getClass().getCanonicalName());
            logger.debug(e.getMessage(), e);
        }
    }

    @Override
    public void onDataReceived(byte[] message) {
        logger.error("PrinterWebSocketService onDataReceived: binary data not supported");
    }

    @Override
    public void setServer(WebSocketServerInterface server) {
        this.server = server;
    }

    private String getChannel() {
        return "/printer";
    }

    /**
     * Prints a PrintDocument
     */
    public void printDocument(PrintDocument printDocument) throws Exception {
        try {
            if (notificationListener != null) {
                notificationListener.notify("Printing " + printDocument.getType(), printDocument.getUrl(), TrayIcon.MessageType.INFO);
            }

            if (isRaw(printDocument)) {
                printRaw(printDocument);
            } else if (isHTML(printDocument)) {
                printHTML(printDocument);
            } else if (isImage(printDocument)) {
                printImage(printDocument);
            } else if (isPDF(printDocument)) {
                printPDF(printDocument);
            } else {
                throw new Exception("Unknown file type: " + printDocument.getUrl());
            }

            server.onDataReceived(getChannel(), gson.toJson(new PrintResult(0, printDocument.getId(), "Success")));
        } catch (Exception e) {
            logger.error("Document Print Error, deleting downloaded document");
            DocumentService.deleteFileFromUrl(printDocument.getUrl());

            if (notificationListener != null) {
                notificationListener.notify("Printing Error " + printDocument.getType(), e.getMessage(), TrayIcon.MessageType.ERROR);
            }

            server.onDataReceived(getChannel(), gson.toJson(new PrintResult(1, printDocument.getId(), e.getClass().getName() + " - " + e.getMessage())));

            throw e;
        }
    }

    /**
     * Return name of mapped printer
     */
    private String findMappedPrinter(String type) {
        logger.trace("findMappedPrinter::" + type);
        return settingService.getSetting().getPrinters().get(type);
    }

    /**
     * Return if PrintDocument is raw
     */
    private Boolean isRaw(PrintDocument printDocument) {
        return printDocument.getRawContent() != null && !printDocument.getRawContent().isEmpty();
    }

    /**
     * Return if PrintDocument is image
     */
    private Boolean isImage(PrintDocument printDocument) {
        String url = printDocument.getUrl();
        String filename = url.substring(url.lastIndexOf("/") + 1);

        return filename.matches("^.*\\.(jpg|jpeg|png|gif)$");
    }

    /**
     * Return if PrintDocument is PDF
     */
    private Boolean isPDF(PrintDocument printDocument) {
        String url = printDocument.getUrl();
        String filename = url.substring(url.lastIndexOf("/") + 1);

        return filename.matches("^.*\\.(pdf)$");
    }

    /**
     * Return if PrintDocument is HTML
     */
    private Boolean isHTML(PrintDocument printDocument) {

        return "HTML".equalsIgnoreCase(printDocument.getCategory());

    }


    /**
     * Prints HTML to specified printer.
     */
    private void printHTML(PrintDocument printDocument) throws PrinterException, IOException {
        logger.debug("printHTML::" + printDocument);
        long timeStart = System.currentTimeMillis();
        long unixTime = System.currentTimeMillis() / 1000L;

        String filename = "documents/"+unixTime+".pdf";
        logger.info("File name is "+filename);
        //byte[] bytes = Base64.decodeBase64(printDocument.getRawContent());

        String html = printDocument.getFileContent();
        String xhtml = htmlToXhtml(html);
        try {
            xhtmlToPdf(xhtml, filename);

        }catch (IOException ioEx){
            logger.info("IO exception while printing HTML document ");
        }catch (DocumentException docEx){
            logger.info("Document exception while printing HTML document ");
        }

//        DocPrintJob docPrintJob = getDocPrintJob(printDocument.getType());
//        Doc doc = new SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
//        docPrintJob.print(doc, null);



        DocPrintJob docPrintJob = getDocPrintJob(printDocument.getType());

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(docPrintJob.getPrintService());

        PageFormat pageFormat = getPageFormat(job);

        PDDocument document = null;
        try {
            document = PDDocument.load(new File(filename));

            Book book = new Book();
            for (int i = 0; i < document.getNumberOfPages(); i += 1) {
                // Rotate Page Automatically
                if (settingService.getSetting().getAutoRotation()) {
                    if (document.getPage(i).getCropBox().getWidth() > document.getPage(i).getCropBox().getHeight()) {
                        logger.debug("Auto rotation result: LANDSCAPE");
                        pageFormat.setOrientation(PageFormat.LANDSCAPE);
                    } else {
                        logger.debug("Auto rotation result: PORTRAIT");
                        pageFormat.setOrientation(PageFormat.PORTRAIT);
                    }
                }

                AnnotatedPrintable printable = new AnnotatedPrintable(new PDFPrintable(document, Scaling.SHRINK_TO_FIT, false, settingService.getSetting().getPrinterDPI()));

                for (AnnotatedPrintable.AnnotatedPrintableAnnotation printDocumentExtra : printDocument.getExtras()) {
                    printable.addAnnotation(printDocumentExtra);
                }
                book.append(printable, pageFormat);
            }

            job.setPageable(book);
            job.setJobName("WebApp Hardware Bridge PDF");
            job.setCopies(printDocument.getQty());
            job.print();


        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        long timeFinish = System.currentTimeMillis();
        logger.info("Document html printed in " + (timeFinish - timeStart) + "ms");
    }


    private static String htmlToXhtml(String html) {
        logger.info(html);

        String unescapedString = null;
        try {
            unescapedString = URLDecoder.decode(html,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        logger.info(unescapedString);
        Document document = Jsoup.parse(unescapedString);
        document.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        return document.html();
    }


    private static void xhtmlToPdf(String xhtml, String outFileName) throws IOException ,DocumentException {
        File output = new File(outFileName);
        logger.info("File out put is "+outFileName);
        logger.info(xhtml);
        ITextRenderer iTextRenderer = new ITextRenderer();
        //FontResolver resolver = iTextRenderer.getFontResolver();
        iTextRenderer.setDocumentFromString(xhtml);
        iTextRenderer.layout();
        OutputStream os = new FileOutputStream(output);
        iTextRenderer.createPDF(os);
        os.close();
    }
    /**
     * Prints raw bytes to specified printer.
     */
    private void printRaw(PrintDocument printDocument) throws PrinterException, PrintException {
        logger.debug("printRaw::" + printDocument);
        long timeStart = System.currentTimeMillis();

        byte[] bytes = Base64.decodeBase64(printDocument.getRawContent());

        DocPrintJob docPrintJob = getDocPrintJob(printDocument.getType());
        Doc doc = new SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        docPrintJob.print(doc, null);

        long timeFinish = System.currentTimeMillis();
        logger.info("Document raw printed in " + (timeFinish - timeStart) + "ms");
    }

    /**
     * Prints image to specified printer.
     */
    private void printImage(PrintDocument printDocument) throws PrinterException, IOException {
        logger.debug("printImage::" + printDocument);

        String filename = DocumentService.getFileFromUrl(printDocument.getUrl()).getPath();

        long timeStart = System.currentTimeMillis();

        DocPrintJob docPrintJob = getDocPrintJob(printDocument.getType());

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(docPrintJob.getPrintService());
        PageFormat pageFormat = getPageFormat(job);

        Image image = ImageIO.read(new File(filename));

        Book book = new Book();
        AnnotatedPrintable printable = new AnnotatedPrintable(new ImagePrintable(image));

        for (AnnotatedPrintable.AnnotatedPrintableAnnotation printDocumentExtra : printDocument.getExtras()) {
            printable.addAnnotation(printDocumentExtra);
        }

        book.append(printable, pageFormat);

        job.setPageable(book);
        job.setJobName("WebApp Hardware Bridge Image");
        job.setCopies(printDocument.getQty());
        job.print();

        long timeFinish = System.currentTimeMillis();
        logger.info("Document " + filename + " printed in " + (timeFinish - timeStart) + "ms");
    }

    /**
     * Prints PDF to specified printer.
     */
    private void printPDF(PrintDocument printDocument) throws PrinterException, IOException {
        logger.debug("printPDF::" + printDocument);

        String filename = DocumentService.getFileFromUrl(printDocument.getUrl()).getPath();

        long timeStart = System.currentTimeMillis();

        DocPrintJob docPrintJob = getDocPrintJob(printDocument.getType());

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(docPrintJob.getPrintService());

        PageFormat pageFormat = getPageFormat(job);

        PDDocument document = null;
        try {
            document = PDDocument.load(new File(filename));

            Book book = new Book();
            for (int i = 0; i < document.getNumberOfPages(); i += 1) {
                // Rotate Page Automatically
                if (settingService.getSetting().getAutoRotation()) {
                    if (document.getPage(i).getCropBox().getWidth() > document.getPage(i).getCropBox().getHeight()) {
                        logger.debug("Auto rotation result: LANDSCAPE");
                        pageFormat.setOrientation(PageFormat.LANDSCAPE);
                    } else {
                        logger.debug("Auto rotation result: PORTRAIT");
                        pageFormat.setOrientation(PageFormat.PORTRAIT);
                    }
                }

                AnnotatedPrintable printable = new AnnotatedPrintable(new PDFPrintable(document, Scaling.SHRINK_TO_FIT, false, settingService.getSetting().getPrinterDPI()));

                for (AnnotatedPrintable.AnnotatedPrintableAnnotation printDocumentExtra : printDocument.getExtras()) {
                    printable.addAnnotation(printDocumentExtra);
                }
                book.append(printable, pageFormat);
            }

            job.setPageable(book);
            job.setJobName("WebApp Hardware Bridge PDF");
            job.setCopies(printDocument.getQty());
            job.print();

            long timeFinish = System.currentTimeMillis();
            logger.info("Document " + filename + " printed in " + (timeFinish - timeStart) + "ms");
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Get PageFormat for PrinterJob
     */
    private PageFormat getPageFormat(final PrinterJob job) {
        final PageFormat pageFormat = job.defaultPage();

        logger.debug("PageFormat Size: " + pageFormat.getWidth() + " x " + pageFormat.getHeight());
        logger.debug("PageFormat Imageable Size:" + pageFormat.getImageableWidth() + " x " + pageFormat.getImageableHeight() + ", XY: " + pageFormat.getImageableX() + ", " + pageFormat.getImageableY());
        logger.debug("Paper Size: " + pageFormat.getPaper().getWidth() + " x " + pageFormat.getPaper().getHeight());
        logger.debug("Paper Imageable Size: " + pageFormat.getPaper().getImageableWidth() + " x " + pageFormat.getPaper().getImageableHeight() + ", XY: " + pageFormat.getPaper().getImageableX() + ", " + pageFormat.getPaper().getImageableY());

        // Reset Imageable Area
        if (settingService.getSetting().getResetImageableArea()) {
            logger.debug("PageFormat reset enabled");
            Paper paper = pageFormat.getPaper();
            paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
            pageFormat.setPaper(paper);
        }

        logger.debug("Final Paper Size: " + pageFormat.getPaper().getWidth() + " x " + pageFormat.getPaper().getHeight());
        logger.debug("Final Paper Imageable Size: " + pageFormat.getPaper().getImageableWidth() + " x " + pageFormat.getPaper().getImageableHeight() + ", XY: " + pageFormat.getPaper().getImageableX() + ", " + pageFormat.getPaper().getImageableY());

        return pageFormat;
    }

    /**
     * Get DocPrintJob for specified printer
     */
    private DocPrintJob getDocPrintJob(String type) throws PrinterException {
        String printerName = findMappedPrinter(type);

        if (printerName != null) {
            PrintService[] services = PrinterJob.lookupPrintServices();

            for (PrintService service : services) {
                if (service.getName().equalsIgnoreCase(printerName)) {
                    logger.info("Sending print job type: " + type + " to printer: " + service.getName());
                    return service.createPrintJob();
                }
            }
        }

        if (settingService.getSetting().getFallbackToDefaultPrinter()) {
            logger.info("No mapped print job type: " + type + ", falling back to default printer");
            return PrintServiceLookup.lookupDefaultPrintService().createPrintJob();
        } else {
            throw new PrinterException("No matched printer: " + type);
        }
    }
}
