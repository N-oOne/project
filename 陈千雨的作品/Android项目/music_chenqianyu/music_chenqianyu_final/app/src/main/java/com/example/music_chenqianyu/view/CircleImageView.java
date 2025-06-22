package com.example.music_chenqianyu.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

import com.example.music_chenqianyu.R;

public class CircleImageView extends AppCompatImageView {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float borderWidth = 0;
    private int borderColor = Color.WHITE;

    public CircleImageView(Context context) {
        super(context);
        init(context, null);
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView);
            borderWidth = a.getDimension(R.styleable.CircleImageView_civ_border_width, 0);
            borderColor = a.getColor(R.styleable.CircleImageView_civ_border_color, Color.WHITE);
            a.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 创建圆形路径
        Path path = new Path();
        float radius = (Math.min(getWidth(), getHeight()) / 2f) - borderWidth;
        path.addCircle(
                getWidth() / 2f,
                getHeight() / 2f,
                radius,
                Path.Direction.CCW
        );

        // 绘制圆形图片
        canvas.save();
        canvas.clipPath(path);
        super.onDraw(canvas);
        canvas.restore();

        // 绘制边框
        if (borderWidth > 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(borderWidth);
            paint.setColor(borderColor);
            canvas.drawCircle(
                    getWidth() / 2f,
                    getHeight() / 2f,
                    radius - borderWidth / 2,
                    paint
            );
        }
    }
}