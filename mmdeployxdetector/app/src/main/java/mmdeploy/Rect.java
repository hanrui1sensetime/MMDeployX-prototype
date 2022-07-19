package mmdeploy;

import android.graphics.RectF;

public class Rect {
    public float left;
    public float top;
    public float right;
    public float bottom;


    public Rect(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public RectF getRectF(){
        return new RectF(left,top,right,bottom);
    }

}
