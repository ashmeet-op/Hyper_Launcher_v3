package net.kdt.pojavlaunch.utils;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

@SuppressWarnings("unused")
public class MatrixUtils {


    public static void transformRect(Rect inOutRect, Matrix transformMatrix) {
        transformRect(inOutRect, inOutRect, transformMatrix);
    }


    public static void transformRect(RectF inOutRect, Matrix transformMatrix) {
        transformRect(inOutRect, inOutRect, transformMatrix);
    }


    public static void transformRect(RectF inRect, Rect outRect, Matrix transformMatrix) {
        float[] inOutDecodeRect = createInOutDecodeRect(transformMatrix);
        if(inOutDecodeRect == null) return;
        writeInputRect(inOutDecodeRect, inRect);
        transformPoints(inOutDecodeRect, transformMatrix);
        readOutputRect(inOutDecodeRect, outRect);
    }


    public static void transformRect(Rect inRect, RectF outRect, Matrix transformMatrix) {
        float[] inOutDecodeRect = createInOutDecodeRect(transformMatrix);
        if(inOutDecodeRect == null) return;
        writeInputRect(inOutDecodeRect, inRect);
        transformPoints(inOutDecodeRect, transformMatrix);
        readOutputRect(inOutDecodeRect, outRect);
    }


    public static void transformRect(Rect inRect, Rect outRect, Matrix transformMatrix) {
        float[] inOutDecodeRect = createInOutDecodeRect(transformMatrix);
        if(inOutDecodeRect == null) return;
        writeInputRect(inOutDecodeRect, inRect);
        transformPoints(inOutDecodeRect, transformMatrix);
        readOutputRect(inOutDecodeRect, outRect);
    }


    public static void transformRect(RectF inRect, RectF outRect, Matrix transformMatrix) {
        float[] inOutDecodeRect = createInOutDecodeRect(transformMatrix);
        if(inOutDecodeRect == null) return;
        writeInputRect(inOutDecodeRect, inRect);
        transformPoints(inOutDecodeRect, transformMatrix);
        readOutputRect(inOutDecodeRect, outRect);
    }



    private static void writeInputRect(float[] inOutDecodeRect, RectF inRect) {
        inOutDecodeRect[0] = inRect.left;
        inOutDecodeRect[1] = inRect.top;
        inOutDecodeRect[2] = inRect.right;
        inOutDecodeRect[3] = inRect.bottom;
    }

    private static void writeInputRect(float[] inOutDecodeRect, Rect inRect) {
        inOutDecodeRect[0] = inRect.left;
        inOutDecodeRect[1] = inRect.top;
        inOutDecodeRect[2] = inRect.right;
        inOutDecodeRect[3] = inRect.bottom;
    }

    private static void readOutputRect(float[] inOutDecodeRect, RectF outRect) {
        outRect.left = inOutDecodeRect[4];
        outRect.top = inOutDecodeRect[5];
        outRect.right = inOutDecodeRect[6];
        outRect.bottom = inOutDecodeRect[7];
    }

    private static void readOutputRect(float[] inOutDecodeRect, Rect outRect) {
        outRect.left = (int)inOutDecodeRect[4];
        outRect.top = (int)inOutDecodeRect[5];
        outRect.right = (int)inOutDecodeRect[6];
        outRect.bottom = (int)inOutDecodeRect[7];
    }

    private static float[] createInOutDecodeRect(Matrix transformMatrix) {
        if(transformMatrix.isIdentity()) return null;


        return new float[8];
    }

    private static void transformPoints(float[] inOutDecodeRect, Matrix transformMatrix) {
        transformMatrix.mapPoints(inOutDecodeRect, 4, inOutDecodeRect, 0, 2);
    }


    public static void inverse(Matrix source, Matrix destination) throws IllegalArgumentException {
        if(source.invert(destination)) return;
        float[] matrix = new float[9];
        source.getValues(matrix);
        inverseMatrix(matrix);
        destination.setValues(matrix);
    }


    private static void inverseMatrix(float[] matrix) {
        float determinant = matrix[0] * (matrix[4] * matrix[8] - matrix[5] * matrix[7])
                - matrix[1] * (matrix[3] * matrix[8] - matrix[5] * matrix[6])
                + matrix[2] * (matrix[3] * matrix[7] - matrix[4] * matrix[6]);

        if (determinant == 0) {
            throw new IllegalArgumentException("Matrix is not invertible");
        }

        float invDet = 1 / determinant;

        float temp0 = (matrix[4] * matrix[8] - matrix[5] * matrix[7]);
        float temp1 = (matrix[2] * matrix[7] - matrix[1] * matrix[8]);
        float temp2 = (matrix[1] * matrix[5] - matrix[2] * matrix[4]);
        float temp3 = (matrix[5] * matrix[6] - matrix[3] * matrix[8]);
        float temp4 = (matrix[0] * matrix[8] - matrix[2] * matrix[6]);
        float temp5 = (matrix[2] * matrix[3] - matrix[0] * matrix[5]);
        float temp6 = (matrix[3] * matrix[7] - matrix[4] * matrix[6]);
        float temp7 = (matrix[1] * matrix[6] - matrix[0] * matrix[7]);
        float temp8 = (matrix[0] * matrix[4] - matrix[1] * matrix[3]);
        matrix[0] = temp0 * invDet;
        matrix[1] = temp1 * invDet;
        matrix[2] = temp2 * invDet;
        matrix[3] = temp3 * invDet;
        matrix[4] = temp4 * invDet;
        matrix[5] = temp5 * invDet;
        matrix[6] = temp6 * invDet;
        matrix[7] = temp7 * invDet;
        matrix[8] = temp8 * invDet;
    }
}
