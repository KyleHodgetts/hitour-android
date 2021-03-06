package stranders.hitour.fragment;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import stranders.hitour.R;

public class ImageDialogFragment extends DialogFragment {

    /**
     * Static String to identify the fragment.
     */
    public static String FRAGMENT_TAG = "IMAGE_DIALOG_FRAGMENT";

    /**
     * Bitmap for an image that is displayed in the fragment.
     */
    private static Bitmap mBitmap;

    // These matrices will be used to move and zoom image
    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    // We can be in one of these 3 states
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    // Remember some things for zooming
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;

    // Remember some things for the full screen functionality
    private int LANDSCAPE = Configuration.ORIENTATION_LANDSCAPE;
    private int PORTRAIT = Configuration.ORIENTATION_PORTRAIT;
    private static int orient = 0;
    private static DisplayMetrics displaymetrics;
    private static int displayHeight;
    static int displayWidth;
    private static ImageDialogFragment frag;
    static Activity act;

    /**
     * Empty constructor required
     */
    public ImageDialogFragment() { }

    /**
     * Get a new instance of the dialog fragment with an image in it
     *
     * @param arg      argument for the bundle
     * @param bmp      {@link Bitmap} image to be put in the dialog
     * @param activity {@link Activity} get the parent activity
     * @return the fragment to be shown
     */
    public static ImageDialogFragment newInstance(int arg, Bitmap bmp, Activity activity) {
        frag = new ImageDialogFragment();
        Bundle args = new Bundle();
        args.putInt("count", arg);
        frag.setArguments(args);

        // Get width and height of device
        act = activity;
        displaymetrics = new DisplayMetrics();
        frag.setBitmap(bmp);
        fullScreen();
        return frag;
    }

    /**
     * Set the image to work on
     *
     * @param bmp {@link Bitmap} the full screen image
     */
    private void setBitmap(Bitmap bmp) {
        mBitmap = bmp;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.image_dialog_fragment, container, false);

        ImageView mImageView = (ImageView) view.findViewById(R.id.image_dialog);
        if (orient != getResources().getConfiguration().orientation) {
            if (orient == LANDSCAPE) {
                orient = PORTRAIT;
                fullScreen();
            } else if(orient == PORTRAIT) {
                orient = LANDSCAPE;
                fullScreen();
            } else {
                orient = getResources().getConfiguration().orientation;
            }
        }

        mImageView.setImageBitmap(mBitmap);

        mImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView view = (ImageView) v;

                // Handle touch events here...
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        savedMatrix.set(matrix);
                        start.set(event.getX(), event.getY());
                        mode = DRAG;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDist = spacing(event);
                        if (oldDist > 10f) {
                            savedMatrix.set(matrix);
                            midPoint(mid, event);
                            mode = ZOOM;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAG) {
                            matrix.set(savedMatrix);
                            matrix.postTranslate(event.getX() - start.x, event.getY()
                                    - start.y);
                        } else if (mode == ZOOM) {
                            float[] f = new float[9];

                            float newDist = spacing(event);
                            if (newDist > 10f) {
                                matrix.set(savedMatrix);
                                float scale = newDist / oldDist;
                                matrix.postScale(scale, scale, mid.x, mid.y);
                            }

                            matrix.getValues(f);
                            float scaleX = f[Matrix.MSCALE_X];
                            float scaleY = f[Matrix.MSCALE_Y];

                            if (scaleX <= 0.7f) {
                                matrix.postScale((0.7f) / scaleX, (0.7f) / scaleY, mid.x, mid.y);
                            } else if (scaleX >= 2.5f) {
                                matrix.postScale((2.5f) / scaleX, (2.5f) / scaleY, mid.x, mid.y);
                            }
                        }
                        break;
                }
                limitDrag(matrix, view, view.getWidth(), view.getHeight());
                view.setImageMatrix(matrix);
                return true;
            }
        });

        return view;
    }

    /**
     * Determine the space between the first two fingers
     *
     * @param event {@link MotionEvent} type of event
     * @return distance between the first two fingers
     */
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * Calculate the mid point of the first two fingers
     *
     * @param point {@link PointF} point of fingers
     * @param event {@link MotionEvent} motion event that makes it zoom
     */
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    /**
     * Limit how much to drag the image
     *
     * @param m           {@link Matrix} matrix of the current position of image
     * @param view        {@link ImageView} the current image
     * @param imageWidth  width of current image
     * @param imageHeight height of image
     */
    private void limitDrag(Matrix m, ImageView view, int imageWidth, int imageHeight) {
        float[] values = new float[9];
        m.getValues(values);
        float[] orig = new float[]{0, 0, imageWidth, imageHeight};
        float[] trans = new float[4];
        m.mapPoints(trans, orig);

        float transLeft = trans[0];
        float transTop = trans[1];
        float transRight = trans[2];
        float transBottom = trans[3];
        float transWidth = transRight - transLeft;
        float transHeight = transBottom - transTop;

        float xOffset = 0;
        if (transWidth > view.getWidth()) {
            if (transLeft > 0) {
                xOffset = -transLeft;
            } else if (transRight < view.getWidth()) {
                xOffset = view.getWidth() - transRight;
            }
        } else {
            if (transLeft < 0) {
                xOffset = -transLeft;
            } else if (transRight > view.getWidth()) {
                xOffset = -(transRight - view.getWidth());
            }
        }

        float yOffset = 0;
        if (transHeight > view.getHeight()) {
            if (transTop > 0) {
                yOffset = -transTop;
            } else if (transBottom < view.getHeight()) {
                yOffset = view.getHeight() - transBottom;
            }
        } else {
            if (transTop < 0) {
                yOffset = -transTop;
            } else if (transBottom > view.getHeight()) {
                yOffset = -(transBottom - view.getHeight());
            }
        }

        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];

        values[Matrix.MTRANS_X] = transX + xOffset;
        values[Matrix.MTRANS_Y] = transY + yOffset;
        m.setValues(values);
    }

    /**
     * Method that scales the image as wide or as high as the display
     */
    private static void fullScreen() {
        act.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        displayHeight = displaymetrics.heightPixels;
        displayWidth = displaymetrics.widthPixels;

        // Get width and height of image
        int bmpWidth = mBitmap.getWidth();
        int bmpHeight = mBitmap.getHeight();

        // Resize the image to full screen
        Bitmap bitmap;
        if (bmpHeight > displayHeight || displayHeight < displayWidth) {
            bitmap = Bitmap.createScaledBitmap(mBitmap, displayHeight * bmpWidth / bmpHeight, displayHeight, false);
        } else {
            bitmap = Bitmap.createScaledBitmap(mBitmap, displayWidth, displayWidth * bmpHeight / bmpWidth, false);
        }

        frag.setBitmap(bitmap);
    }
}
