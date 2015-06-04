package gumanchu.rosiecontrol;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * Renderer for stuff
 *
 * Created by gu on 6/3/15.
 */
public class CardboardRenderer implements CardboardView.StereoRenderer {
    private static final String TAG = "CardboardRenderer";

    private Triangle mTriangleLeft;
    private Triangle mTriangleRight;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mRotationMatrix = new float[16];

    private float mAngle;


    public CardboardRenderer(Context context) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {

        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        mTriangleLeft = new Triangle(0);
        mTriangleRight = new Triangle(1);


    }

    @Override
    public void onSurfaceChanged(int width, int height) {

        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

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
        float[] scratch = new float[16];

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);


        // Create a rotation for the triangle

        // Use the following code to generate constant rotation.
        // Leave this code out when using TouchEvents.
        // long time = SystemClock.uptimeMillis() % 4000L;
        // float angle = 0.090f * ((int) time);

        Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, 1.0f);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);

        // Draw triangle
        mTriangleRight.draw(scratch);
        mTriangleLeft.draw(scratch);


    }



    /**
     * Utility method for compiling a OpenGL shader.
     *
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    /**
     * Returns the rotation angle of the triangle shape (mTriangleLeft).
     *
     * @return - A float representing the rotation angle.
     */
    public float getAngle() {
        return mAngle;
    }

    /**
     * Sets the rotation angle of the triangle shape (mTriangleLeft).
     */
    public void setAngle(float angle) {
        mAngle = angle;
    }

}
