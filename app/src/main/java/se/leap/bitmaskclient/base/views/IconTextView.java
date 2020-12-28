package se.leap.bitmaskclient.base.views;


import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.appcompat.widget.AppCompatTextView;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.util.AttributeSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IconTextView extends AppCompatTextView {

    private int imageResource = 0;
    /**
     * Regex pattern that looks for embedded images of the format: [img src=imageName/]
     */
    public static final String PATTERN = "\\Q[img src]\\E";

    public IconTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public IconTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IconTextView(Context context) {
        super(context);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        final Spannable spannable = getTextWithImages(getContext(), text, getLineHeight(), getCurrentTextColor());
        super.setText(spannable, BufferType.SPANNABLE);
    }

    public void setIcon(int imageResource) {
        this.imageResource = imageResource;
    }

    private Spannable getTextWithImages(Context context, CharSequence text, int lineHeight, int colour) {
        final Spannable spannable = Spannable.Factory.getInstance().newSpannable(text);
        addImages(context, spannable, lineHeight, colour);
        return spannable;
    }

    private void addImages(Context context, Spannable spannable, int lineHeight, int colour) {
        final Pattern refImg = Pattern.compile(PATTERN);

        final Matcher matcher = refImg.matcher(spannable);
        while (matcher.find()) {
            boolean set = true;
            for (ImageSpan span : spannable.getSpans(matcher.start(), matcher.end(), ImageSpan.class)) {
                if (spannable.getSpanStart(span) >= matcher.start()
                        && spannable.getSpanEnd(span) <= matcher.end()) {
                    spannable.removeSpan(span);
                } else {
                    set = false;
                    break;
                }
            }
            if (set && imageResource != 0) {
                spannable.setSpan(makeImageSpan(context, imageResource, lineHeight, colour),
                        matcher.start(),
                        matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
    }

    /**
     * Create an ImageSpan for the given icon drawable. This also sets the image size and colour.
     * Works best with a white, square icon because of the colouring and resizing.
     *
     * @param context       The Android Context.
     * @param drawableResId A drawable resource Id.
     * @param size          The desired size (i.e. width and height) of the image icon in pixels.
     *                      Use the lineHeight of the TextView to make the image inline with the
     *                      surrounding text.
     * @param colour        The colour (careful: NOT a resource Id) to apply to the image.
     * @return An ImageSpan, aligned with the bottom of the text.
     */
    private ImageSpan makeImageSpan(Context context, int drawableResId, int size, int colour) {
        final Drawable drawable = context.getResources().getDrawable(drawableResId);
        drawable.mutate();
        drawable.setColorFilter(colour, PorterDuff.Mode.MULTIPLY);
        drawable.setBounds(0, 0, size, size);
        return new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM);
    }

}