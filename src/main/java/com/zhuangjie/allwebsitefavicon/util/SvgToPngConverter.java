package com.zhuangjie.allwebsitefavicon.util;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
public class SvgToPngConverter {
    public static byte[] convertSvgToPng(byte[] svgCode) throws IOException, TranscoderException {
        InputStream inputStream = new ByteArrayInputStream(svgCode);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TranscoderInput input = new TranscoderInput(inputStream);
        TranscoderOutput output = new TranscoderOutput(outputStream);
        PNGTranscoder transcoder = new PNGTranscoder();
        transcoder.transcode(input, output);
        return outputStream.toByteArray();
    }
}
