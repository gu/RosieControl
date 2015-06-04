package gumanchu.rosiecontrol;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * Renderer for stuff
 *
 * Created by gu on 6/3/15.
 */
public class CardboardRenderer implements CardboardView.StereoRenderer {
    private static final String TAG = "CardboardRenderer";

    private final float[] mtrxProjection = new float[16];
    private final float[] mtrxView = new float[16];
    private final float[] mtrxProjectionAndView = new float[16];

    public static float vertices[];
    public static short indices[];
    public FloatBuffer vertexBuffer;
    public ShortBuffer drawListBuffer;

    float   mScreenWidth = 1280;
    float   mScreenHeight = 768;

    Context mContext;
    long mLastTime;


    public CardboardRenderer(Context context)
        {
            mContext = context;
            mLastTime = System.currentTimeMillis() + 100;

    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {// Create the triangle
        SetupTriangle();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);

        int vertexShader = GLUtilities.loadShader(GLES20.GL_VERTEX_SHADER, GLUtilities.vs_SolidColor);
        int fragmentShader = GLUtilities.loadShader(GLES20.GL_FRAGMENT_SHADER, GLUtilities.fs_SolidColor);

        GLUtilities.sp_SolidColor = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(GLUtilities.sp_SolidColor, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(GLUtilities.sp_SolidColor, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(GLUtilities.sp_SolidColor);                  // creates OpenGL ES program executables

        GLES20.glUseProgram(GLUtilities.sp_SolidColor);

    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;

        GLES20.glViewport(0, 0, (int)mScreenWidth, (int)mScreenHeight);

        for(int i=0;i<16;i++)
        {
            mtrxProjection[i] = 0.0f;
            mtrxView[i] = 0.0f;
            mtrxProjectionAndView[i] = 0.0f;
        }

        Matrix.orthoM(mtrxProjection, 0, 0f, mScreenWidth, 0.0f, mScreenHeight, 0, 50);

        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        Matrix.multiplyMM(mtrxProjectionAndView, 0, mtrxProjection, 0, mtrxView, 0);

    }


    @Override
    public void onRendererShutdown() {

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {

    }

    @Override
    public void onDrawEye(Eye eye) {
        long now = System.currentTimeMillis();

        if (mLastTime > now) return;

        Render(mtrxProjectionAndView);

        mLastTime = now;


    }

    private void Render(float[] m) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        int mPositionHandle = GLES20.glGetAttribLocation(GLUtilities.sp_SolidColor, "vPosition");

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                0, vertexBuffer);

        int mtrxhandle = GLES20.glGetUniformLocation(GLUtilities.sp_SolidColor, "uMVPMatrix");

        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);

    }

    public void SetupTriangle()
    {
        vertices = new float[] {
                        0.0f, 600f, 0.0f,
                        0.0f, 0f, 0.0f,
                        600f, 0f, 0.0f,
                        600f, 600f, 0.0f,
                };

        indices = new short[] {0, 1, 2, 0, 2, 3}; // loop in the android official tutorial opengles why different order.

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);

    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

}
