package com.example.bluetooth_print;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import com.gprinter.command.CpclCommand;
import com.gprinter.command.EscCommand;
import com.gprinter.command.LabelCommand;

import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * @author thon
 */
public class PrintContent {
      private static final String TAG = PrintContent.class.getSimpleName();

      /**
       * 票据打印对象转换
       */
 public static Vector<Byte> mapToReceipt(Map<String,Object> config, List<Map<String,Object>> list) {
    EscCommand esc = new EscCommand();
    esc.addInitializePrinter();
    esc.addPrintAndFeedLines((byte) 1);

    for (Map<String,Object> m : list) {
        String type = (String) m.get("type");
        String content = (String) m.get("content");
        int align = (int)(m.get("align") == null ? 0 : m.get("align"));
        int size = (int)(m.get("size") == null ? 3 : m.get("size"));
        int weight = (int)(m.get("weight") == null ? 0 : m.get("weight"));
        int width = (int)(m.get("width") == null ? 0 : m.get("width"));
        int height = (int)(m.get("height") == null ? 0 : m.get("height"));
        int linefeed = (int)(m.get("linefeed") == null ? 0 : m.get("linefeed"));

        Log.d("PrintDebug", "Type: " + type + ", Content: " + content + ", Width: " + width + ", Height: " + height);

        esc.addSelectJustification(align == 0 ? EscCommand.JUSTIFICATION.LEFT : 
                                   (align == 1 ? EscCommand.JUSTIFICATION.CENTER : EscCommand.JUSTIFICATION.RIGHT));

        if ("text".equals(type)) {
            esc.addSelectPrintModes(EscCommand.FONT.FONTA, weight > 0 ? EscCommand.ENABLE.ON : EscCommand.ENABLE.OFF,
                                    height > 0 ? EscCommand.ENABLE.ON : EscCommand.ENABLE.OFF,
                                    width > 0 ? EscCommand.ENABLE.ON : EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
            esc.addText(content);
            esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
        } else if ("barcode".equals(type)) {
            esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);
            esc.addSetBarcodeHeight((byte) 60);
            esc.addSetBarcodeWidth((byte) 2);
            esc.addCODE128(esc.genCodeB(content));
        } else if ("qrcode".equals(type)) {
            esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
            esc.addSelectSizeOfModuleForQRCode((byte) size);
            esc.addStoreQRCodeData(content);
            esc.addPrintQRCode();
        } else if ("image".equals(type)) {
            byte[] bytes = Base64.decode(content, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            int imgWidth = width > 0 ? width : bitmap.getWidth();
            int imgHeight = height > 0 ? height : bitmap.getHeight();

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, imgWidth, imgHeight, false);
            Log.d("PrintDebug", "Resized Image Width: " + imgWidth + ", Height: " + imgHeight);

            esc.addRastBitImage(resizedBitmap, imgWidth, 0);
        }

        if (linefeed == 1) {
            esc.addPrintAndLineFeed();
        }
    }

    esc.addPrintAndFeedLines((byte) 1);
    esc.addCutPaper();
    return esc.getCommand();
}


      /**
       * 标签打印对象转换
       */
      public static Vector<Byte> mapToLabel(Map<String,Object> config, List<Map<String,Object>> list) {
    LabelCommand tsc = new LabelCommand();

    int width = (int)(config.get("width") == null ? 60 : config.get("width")); // mm
    int height = (int)(config.get("height") == null ? 75 : config.get("height")); // mm
    int gap = (int)(config.get("gap") == null ? 0 : config.get("gap")); // mm

    tsc.addSize(width, height);
    tsc.addGap(gap);
    tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL);
    tsc.addQueryPrinterStatus(LabelCommand.RESPONSE_MODE.ON);
    tsc.addReference(0, 0);
    tsc.addDensity(LabelCommand.DENSITY.DNESITY4);
    tsc.addTear(EscCommand.ENABLE.ON);
    tsc.addCls();

    for (Map<String,Object> m : list) {
        String type = (String) m.get("type");
        String content = (String) m.get("content");
        int x = (int)(m.get("x") == null ? 0 : m.get("x"));
        int y = (int)(m.get("y") == null ? 0 : m.get("y"));

        if ("text".equals(type)) {
            tsc.addText(x, y, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1, content);
        } else if ("barcode".equals(type)) {
            tsc.add1DBarcode(x, y, LabelCommand.BARCODETYPE.CODE128, 100, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, content);
        } else if ("qrcode".equals(type)) {
            tsc.addQRCode(x, y, LabelCommand.EEC.LEVEL_L, 5, LabelCommand.ROTATION.ROTATION_0, content);
        } else if ("image".equals(type)) {
            byte[] bytes = Base64.decode(content, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            int imgWidth = (int)(m.get("width") == null ? bitmap.getWidth() : m.get("width"));
            int imgHeight = (int)(m.get("height") == null ? bitmap.getHeight() : m.get("height"));

            // Resize the image
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, imgWidth, imgHeight, false);

            // Print image with correct dimensions
            tsc.addBitmap(x, y, LabelCommand.BITMAP_MODE.OVERWRITE, imgWidth, resizedBitmap);
        }
    }

    tsc.addPrint(1, 1);
    tsc.addSound(2, 100);
    tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);

    return tsc.getCommand();
}

      /**
       * 面单打印对象转换
       */
      public static Vector<Byte> mapToCPCL(Map<String,Object> config, List<Map<String,Object>> list) {
            CpclCommand cpcl = new CpclCommand();


            Vector<Byte> datas = cpcl.getCommand();
            return datas;
      }

}
