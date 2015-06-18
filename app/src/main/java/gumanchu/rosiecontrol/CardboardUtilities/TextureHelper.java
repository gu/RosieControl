package gumanchu.rosiecontrol.CardboardUtilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.support.annotation.NonNull;
import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public class TextureHelper {

    private static final String TAG = "TextureHelper";
    static Bitmap bitmap;

    static boolean streaming;

    static Mat img = new Mat(480, 640, CvType.CV_8UC3);
    static byte[] bytes = new byte[img.rows() * img.cols() * img.channels()];
    static ByteBuffer bBuff;

    public static Bitmap frame = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);

    public static int loadTexture(final Context context, final int resourceId)
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;	// No pre-scaling

            // Read in the resource
            if (CardboardRenderer.streaming) {
                bitmap = Bitmap.createBitmap(frame);
            } else {
                bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
            }

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }

    public static void setMat(Mat mat) {
        mat.copyTo(img);
    }

    public static int loadMatTexture() {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            img.get(0, 0, bytes);

            bBuff = ByteBuffer.wrap(bytes);

            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, img.cols(), img.rows(),
                    0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, bBuff);

        }


        return textureHandle[0];
    }

    public static void setStreaming(boolean s) {
        streaming = s;
    }
}