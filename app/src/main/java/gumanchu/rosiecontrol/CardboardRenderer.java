package gumanchu.rosiecontrol;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

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
    // Our matrices
    private final float[] mtrxProjection = new float[16];
    private final float[] mtrxView = new float[16];
    private final float[] mtrxProjectionAndView = new float[16];

    // Geometric variables
    public static float vertices[];
    public static short indices[];
    public FloatBuffer vertexBuffer;
    public ShortBuffer drawListBuffer;

    // Our screenresolution
    float	mScreenWidth = 1280;
    float	mScreenHeight = 768;

    // Misc
    Context mContext;
    long mLastTime;
    int mProgram;

    public CardboardRenderer(Context context) {
        mContext = context;
        mLastTime = System.currentTimeMillis() + 100;
    }

    protected String getVertexShader()
    {
        // Define our per-pixel lighting shader.
        final String perPixelVertexShader =
                "uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
                        + "uniform mat4 u_MVMatrix;       \n"		// A constant representing the combined model/view matrix.

                        + "attribute vec4 a_Position;     \n"		// Per-vertex position information we will pass in.
                        + "attribute vec4 a_Color;        \n"		// Per-vertex color information we will pass in.
                        + "attribute vec3 a_Normal;       \n"		// Per-vertex normal information we will pass in.

                        + "varying vec3 v_Position;       \n"		// This will be passed into the fragment shader.
                        + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.
                        + "varying vec3 v_Normal;         \n"		// This will be passed into the fragment shader.

                        // The entry point for our vertex shader.
                        + "void main()                                                \n"
                        + "{                                                          \n"
                        // Transform the vertex into eye space.
                        + "   v_Position = vec3(u_MVMatrix * a_Position);             \n"
                        // Pass through the color.
                        + "   v_Color = a_Color;                                      \n"
                        // Transform the normal's orientation into eye space.
                        + "   v_Normal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));      \n"
                        // gl_Position is a special variable used to store the final position.
                        // Multiply the vertex by the matrix to get the final point in normalized screen coordinates.
                        + "   gl_Position = u_MVPMatrix * a_Position;                 \n"
                        + "}                                                          \n";

        return perPixelVertexShader;
    }

    protected String getFragmentShader()
    {
        final String perPixelFragmentShader =
                "precision mediump float;       \n"		// Set the default precision to medium. We don't need as high of a
                        // precision in the fragment shader.
                        + "uniform vec3 u_LightPos;       \n"	    // The position of the light in eye space.

                        + "varying vec3 v_Position;		\n"		// Interpolated position for this fragment.
                        + "varying vec4 v_Color;          \n"		// This is the color from the vertex shader interpolated across the
                        // triangle per fragment.
                        + "varying vec3 v_Normal;         \n"		// Interpolated normal for this fragment.

                        // The entry point for our fragment shader.
                        + "void main()                    \n"
                        + "{                              \n"
                        // Will be used for attenuation.
                        + "   float distance = length(u_LightPos - v_Position);                  \n"
                        // Get a lighting direction vector from the light to the vertex.
                        + "   vec3 lightVector = normalize(u_LightPos - v_Position);             \n"
                        // Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
                        // pointing in the same direction then it will get max illumination.
                        + "   float diffuse = max(dot(v_Normal, lightVector), 0.1);              \n"
                        // Add attenuation.
                        + "   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));  \n"
                        // Multiply the color by the diffuse illumination level to get final output color.
                        + "   gl_FragColor = v_Color * diffuse;                                  \n"
                        + "}                                                                     \n";

        return perPixelFragmentShader;
    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {

        // Create the triangle
        SetupTriangle();

        // Set the clear color to black
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1);

        // Create the shaders
        int vertexShader = GLUtilities.loadShader(GLES20.GL_VERTEX_SHADER, GLUtilities.vs_SolidColor);
        int fragmentShader = GLUtilities.loadShader(GLES20.GL_FRAGMENT_SHADER, GLUtilities.fs_SolidColor);

        GLUtilities.sp_SolidColor = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(GLUtilities.sp_SolidColor, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(GLUtilities.sp_SolidColor, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(GLUtilities.sp_SolidColor);                  // creates OpenGL ES program executables

        // Set our shader programm
        GLES20.glUseProgram(GLUtilities.sp_SolidColor);

    }

    @Override
    public void onSurfaceChanged(int width, int height) {

        // We need to know the current width and height.
        mScreenWidth = width;
        mScreenHeight = height;

        // Redo the Viewport, making it fullscreen.
        GLES20.glViewport(0, 0, (int)mScreenWidth, (int)mScreenHeight);

        // Clear our matrices
        for(int i=0;i<16;i++)
        {
            mtrxProjection[i] = 0.0f;
            mtrxView[i] = 0.0f;
            mtrxProjectionAndView[i] = 0.0f;
        }

        // Setup our screen width and height for normal sprite translation.
        Matrix.orthoM(mtrxProjection, 0, 0f, mScreenWidth, 0.0f, mScreenHeight, 0, 50);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mtrxView, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
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

        // Get the current time
        long now = System.currentTimeMillis();

        // We should make sure we are valid and sane
        if (mLastTime > now) return;

        // Get the amount of time the last frame took.
        long elapsed = now - mLastTime;

        // Update our example

        // Render our example
        Render(mtrxProjectionAndView);

        // Save the current time to see how long it took :).
        mLastTime = now;
    }

    private void Render(float[] m) {

        // clear Screen and Depth Buffer, we have set the clear color as black.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // get handle to vertex shader's vPosition member
        int mPositionHandle = GLES20.glGetAttribLocation(GLUtilities.sp_SolidColor, "vPosition");

        // Enable generic vertex attribute array
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                0, vertexBuffer);

        // Get handle to shape's transformation matrix
        int mtrxhandle = GLES20.glGetUniformLocation(GLUtilities.sp_SolidColor, "uMVPMatrix");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);

    }

    

    public void SetupTriangle()
    {
        // We have to create the vertices of our triangle.
        vertices = new float[] {
                -1.0f, 1.0f, 0.0f,
                -1.0f,-1.0f,0.0f,
                1.0f,-1.0f,0.0f,
                1.0f,1.0f,0.0f
                };

        indices = new short[] {0, 1, 2, 0, 2, 3};//, 0, 2, 3}; // The order of vertexrendering.

        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(indices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(indices);
        drawListBuffer.position(0);


    }

}
