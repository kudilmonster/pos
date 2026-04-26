package toko.aplikasipos;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class EscPosPrinter {
    // Legacy support for system default printer
    public static boolean printBytes(byte[] data) {
        return printBytes(data, javax.print.PrintServiceLookup.lookupDefaultPrintService());
    }

    // Modern support for selected printer
    public static boolean printBytes(byte[] data, PrintService printer) {
        if (printer == null) {
            return false;
        }
        try {
            DocPrintJob job = printer.createPrintJob();
            Doc doc = new SimpleDoc(data, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            job.print(doc, null);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static byte[] toReceiptBytes(String receipt) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            // ESC/POS Initialize
            bos.write(new byte[]{0x1B, 0x40});
            // Content (ASCII)
            bos.write(receipt.getBytes(StandardCharsets.US_ASCII));
            // Cut command (partial cut)
            bos.write(new byte[]{0x1D, 0x56, 0x01});
            bos.write('\n');
            return bos.toByteArray();
        } catch (Exception e) {
            return receipt.getBytes(StandardCharsets.US_ASCII);
        }
    }
}
